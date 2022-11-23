package org.pygisserver.common.raster;

import org.geotools.styling.Stroke;
import org.geotools.styling.*;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.*;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PyFeatureInfoStylePreprocessor extends PySymbolizerFilteringVisitor {

    StyleBuilder sb = new StyleBuilder();

    FeatureType schema;

    Set<Expression> geometriesOnPolygonSymbolizer = new HashSet<>();

    Set<Expression> geometriesOnLineSymbolizer = new HashSet<>();

    Set<Expression> geometriesOnPointSymbolizer = new HashSet<>();

    Set<Expression> geometriesOnTextSymbolizer = new HashSet<>();

    Set<Rule> extraRules = new HashSet<>();

    private PropertyName defaultGeometryExpression;

    private boolean addSolidLineSymbolier;

    public PyFeatureInfoStylePreprocessor(FeatureType schema) {
        this.schema = schema;
        this.defaultGeometryExpression = ff.property("");
    }

    public void visit(TextSymbolizer ts) {
        pages.push(null);
        addGeometryExpression(ts.getGeometry(), geometriesOnTextSymbolizer);
    }

    @Override
    public void visit(Fill fill) {
        super.visit(fill);
        Fill copy = (Fill) pages.peek();
        if (copy.getGraphicFill() != null) {
            copy.setGraphicFill(null);
            copy.setColor(sb.colorExpression(Color.BLACK));
        }
    }

    /**
     * Force a solid color, otherwise we might decide the user did not click on the polygon because
     * the area in which he clicked is fully transparent
     */
    @Override
    public void visit(PolygonSymbolizer poly) {
        super.visit(poly);
        PolygonSymbolizer copy = (PolygonSymbolizer) pages.peek();
        Fill fill = copy.getFill();
        if (fill == null || isStaticTransparentFill(fill)) {
            copy.setFill(sb.createFill());
        }
        Stroke stroke = copy.getStroke();
        addStrokeSymbolizerIfNecessary(stroke);
        addGeometryExpression(poly.getGeometry(), geometriesOnPolygonSymbolizer);
    }

    private boolean isStaticTransparentFill(Fill fill) {
        if (fill.getOpacity() instanceof Literal) {
            // weird case of people setting opacity to 0. In case the opacity is really attribute
            // driven,
            // we'll leave it be
            Double staticOpacity = fill.getOpacity().evaluate(null, Double.class);
            if (staticOpacity == null || staticOpacity == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(LineSymbolizer line) {
        super.visit(line);
        LineSymbolizer copy = (LineSymbolizer) pages.peek();
        Stroke stroke = copy.getStroke();
        addStrokeSymbolizerIfNecessary(stroke);
        addGeometryExpression(line.getGeometry(), geometriesOnLineSymbolizer);
    }

    @Override
    public void visit(PointSymbolizer ps) {
        super.visit(ps);
        addGeometryExpression(ps.getGeometry(), geometriesOnPointSymbolizer);
    }

    private void addGeometryExpression(Expression geometry, Set<Expression> expressions) {
        if (isDefaultGeometry(geometry)) {
            expressions.add(defaultGeometryExpression);
        } else {
            expressions.add(geometry);
        }
    }

    private boolean isDefaultGeometry(Expression geometry) {
        if (geometry == null) {
            return true;
        }

        if (!(geometry instanceof PropertyName)) {
            return false;
        }

        PropertyName pn = (PropertyName) geometry;
        if ("".equals(pn.getPropertyName())) {
            return true;
        }

        GeometryDescriptor gd = schema.getGeometryDescriptor();
        if (gd == null) {
            return false;
        }
        return gd.getLocalName().equals(pn.getPropertyName());
    }

    @Override
    public void visit(Style style) {
        super.visit(style);
        Style copy = (Style) pages.peek();
        // merge the feature type styles sharing the same transformation
        List<FeatureTypeStyle> featureTypeStyles = copy.featureTypeStyles();
        List<FeatureTypeStyle> reduced = new ArrayList<>();
        FeatureTypeStyle current = null;
        for (FeatureTypeStyle fts : featureTypeStyles) {
            if (current == null
                    || !sameTranformation(current.getTransformation(), fts.getTransformation())) {
                current = fts;
                reduced.add(current);
            } else {
                // flatten, we don't need to draw a pretty picture and having multiple FTS
                // would result in the feature being returned twice, since we cannot
                // assume feature ids to be stable either
                current.rules().addAll(fts.rules());
            }
        }

        // replace
        copy.featureTypeStyles().clear();
        copy.featureTypeStyles().addAll(reduced);
    }

    private boolean sameTranformation(Expression t1, Expression t2) {
        return (t1 == null && t2 == null) || (t1 != null && t1.equals(t2));
    }

    @Override
    public void visit(FeatureTypeStyle fts) {
        extraRules.clear();
        super.visit(fts);
        if (!extraRules.isEmpty()) {
            FeatureTypeStyle copy = (FeatureTypeStyle) pages.peek();
            copy.rules().addAll(extraRules);
        }
    }

    @Override
    public void visit(Rule rule) {
        geometriesOnLineSymbolizer.clear();
        geometriesOnPolygonSymbolizer.clear();
        geometriesOnPointSymbolizer.clear();
        geometriesOnTextSymbolizer.clear();
        addSolidLineSymbolier = false;
        super.visit(rule);
        Rule copy = (Rule) pages.peek();
        if (addSolidLineSymbolier) {
            // add also a black line to make sure we get something in output even
            // if the user clicks in between symbols or dashes
            LineSymbolizer ls = sb.createLineSymbolizer(Color.BLACK);
            copy.symbolizers().add(ls);
        }
        // check all the geometries that are on line, but not on polygon
        geometriesOnLineSymbolizer.removeAll(geometriesOnPolygonSymbolizer);
        for (Expression geom : geometriesOnLineSymbolizer) {
            Object result = geom.evaluate(schema);
            Class geometryType = getTargetGeometryType(result);
            if (org.locationtech.jts.geom.Polygon.class.isAssignableFrom(geometryType)
                    || MultiPolygon.class.isAssignableFrom(geometryType)) {
                // we know it's a polygon type, but there is no polygon symbolizer, add one
                // in the current rule
                copy.symbolizers().add(sb.createPolygonSymbolizer());
            } else if (geometryType.equals(Geometry.class)) {
                // dynamic, we need to add an extra rule then to paint as polygon
                // only if the actual geometry is a polygon type
                RuleImpl extra =
                        buildDynamicGeometryRule(
                                copy,
                                geom,
                                sb.createPolygonSymbolizer(),
                                "Polygon",
                                "MultiPolygon");
                extraRules.add(extra);
            }
        }
        // check all the geometries that are on text, but not on any other symbolizer (pure labels)
        // that we won't hit otherwise
        geometriesOnTextSymbolizer.removeAll(geometriesOnPolygonSymbolizer);
        geometriesOnTextSymbolizer.removeAll(geometriesOnLineSymbolizer);
        geometriesOnTextSymbolizer.removeAll(geometriesOnPointSymbolizer);
        for (Expression geom : geometriesOnTextSymbolizer) {
            Object result = geom.evaluate(schema);
            Class geometryType = getTargetGeometryType(result);
            if (org.locationtech.jts.geom.Polygon.class.isAssignableFrom(geometryType)
                    || MultiPolygon.class.isAssignableFrom(geometryType)) {
                copy.symbolizers().add(sb.createPolygonSymbolizer());
            } else if (LineString.class.isAssignableFrom(geometryType)
                    || MultiLineString.class.isAssignableFrom(geometryType)) {
                copy.symbolizers().add(sb.createLineSymbolizer());
            } else if (Point.class.isAssignableFrom(geometryType)
                    || MultiPoint.class.isAssignableFrom(geometryType)) {
                copy.symbolizers().add(sb.createPointSymbolizer());
            } else {
                // ouch, it's a generic geometry... now this is going to be painful, we have to
                // build a dynamic symbolizer for each possible geometry type
                RuleImpl extra =
                        buildDynamicGeometryRule(
                                copy,
                                geom,
                                sb.createPolygonSymbolizer(),
                                "Polygon",
                                "MultiPolygon");
                extraRules.add(extra);
                extra =
                        buildDynamicGeometryRule(
                                copy,
                                geom,
                                sb.createLineSymbolizer(),
                                "LineString",
                                "LinearRing",
                                "MultiLineString");
                extraRules.add(extra);
                extra =
                        buildDynamicGeometryRule(
                                copy, geom, sb.createPointSymbolizer(), "Point", "MultiPoint");
                extraRules.add(extra);
            }
        }
    }

    private RuleImpl buildDynamicGeometryRule(
            Rule base, Expression geom, Symbolizer symbolizer, String... geometryTypes) {
        List<Filter> typeChecks = new ArrayList<>();
        for (String geometryType : geometryTypes) {
            typeChecks.add(
                    ff.equal(ff.function("geometryType", geom), ff.literal(geometryType), false));
        }
        Filter geomCheck = ff.or(typeChecks);
        Filter ruleFilter = base.getFilter();
        Filter filter =
                ruleFilter == null || ruleFilter == Filter.INCLUDE
                        ? geomCheck
                        : ff.and(geomCheck, ruleFilter);
        RuleImpl extra = new RuleImpl(base);
        extra.setFilter(filter);
        extra.symbolizers().clear();
        extra.symbolizers().add(symbolizer);
        return extra;
    }

    private Class getTargetGeometryType(Object descriptor) {
        if (!(descriptor instanceof GeometryDescriptor)) {
            // we don't know what this will be, we probably evaluated a filter function
            return Geometry.class;
        } else {
            // see if we are dealing with a polygon
            return ((GeometryDescriptor) descriptor).getType().getBinding();
        }
    }

    private void addStrokeSymbolizerIfNecessary(Stroke stroke) {
        if (stroke != null) {
            List<Expression> dashArray = stroke.dashArray();
            Graphic graphicStroke = stroke.getGraphicStroke();
            if (graphicStroke != null || dashArray != null && !dashArray.isEmpty()) {
                addSolidLineSymbolier = true;
            }
        }
    }
}
