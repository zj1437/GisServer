package org.pygisserver.render.style;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;

import javax.media.jai.TiledImage;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.renderer.i18n.ErrorKeys;
import org.geotools.renderer.i18n.Errors;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.Description;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.UserLayer;
import org.geotools.styling.visitor.DpiRescaleStyleVisitor;
import org.geotools.styling.visitor.RescaleStyleVisitor;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.xml.styling.SLDParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.style.ChannelSelection;
import org.opengis.util.InternationalString;
import org.xml.sax.EntityResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StyleUtils {
	/** Default channel name for {@link ChannelSelection} elements. */
    public static final String DEFAULT_CHANNEL_NAME = "1";
    
    public static final Color DEFAULT_BORDER_COLOR = Color.black;
    
    /** padding percentage factor at both sides of the legend. */
    public static final float rowPaddingFactor = 0.15f;
    /** top & bottom padding percentage factor for the legend */
    public static final float columnPaddingFactor = 0.15f;

    /** padding percentage factor at both sides of the legend. */
    public static final float marginFactor = 0.015f;
    
    /** Tolerance used to compare doubles for equality */
    public static final double TOLERANCE = 1e-6;
	
	protected static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    /** Legend layouts */
    public enum LegendLayout {
        HORIZONTAL,
        VERTICAL;
    }
	
    public enum VAlign {
        TOP,
        MIDDLE,
        BOTTOM;
    }

    public enum HAlign {
        LEFT,
        CENTERED,
        RIGHT,
        JUSTIFIED;
    }
	
    public static SLDParser createSld10Parser(Object input, ResourceLocator resourceLocator, EntityResolver entityResolver){
        SLDParser parser;
        try {
        	if (input instanceof File) {
                parser = new SLDParser(styleFactory, (File) input);
            } else {
                parser = new SLDParser(styleFactory, toReader(input));
            }

            if (resourceLocator != null) {
                parser.setOnLineResourceLocator(resourceLocator);
            }
            if (entityResolver != null) {
                parser.setEntityResolver(entityResolver);
            }
            return parser;
		} catch (IOException e) {
			log.error("StyleUtils createSld10Parser has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "样式解析器对象"));
		}
    }
	
    public static Reader toReader(Object input){
    	try {
    		if (input instanceof Reader) {
                return (Reader) input;
            }

            if (input instanceof InputStream) {
                return new InputStreamReader((InputStream) input);
            }

            if (input instanceof String) {
                return new StringReader((String) input);
            }

            if (input instanceof URL) {
                return new InputStreamReader(((URL) input).openStream());
            }

            if (input instanceof File) {
                return new FileReader((File) input);
            }
            throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "输入流转换为Reader对象"));
		} catch (Exception e) {
			log.error("StyleUtils toReader has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "输入流转换为Reader对象"));
		}
    }
    
    public static Style style(StyledLayerDescriptor sld) {
        for (int i = 0; i < sld.getStyledLayers().length; i++) {
            Style[] styles = null;
            if (sld.getStyledLayers()[i] instanceof NamedLayer) {
                NamedLayer layer = (NamedLayer) sld.getStyledLayers()[i];
                styles = layer.getStyles();
            } else if (sld.getStyledLayers()[i] instanceof UserLayer) {
                UserLayer layer = (UserLayer) sld.getStyledLayers()[i];
                styles = layer.getUserStyles();
            }

            if (styles != null) {
                for (int j = 0; j < styles.length; j++) {
                    if (!(styles[j] instanceof NamedStyle)) {
                        return styles[j];
                    }
                }
            }
        }
        return null;
    }
    
    public static void parseKVPInfo(String kvp, LegendGraphicEntity legend){
    	if(StringUtils.isBlank(kvp)) throw new NullPointerException("kvp info must be exist");
    	
    	Map<String,Object> kvpMap = new HashMap<String, Object>();
    	String[] values = kvp.split("&");
    	for (String str : values) {
    		String[] kav = str.split("=");
    		String key = kav[0];
    		String value = kav[1];
			
			if(key.equals("REQUEST")) {
				kvpMap.put(key, value);
			}
			
			if(key.equals("VERSION")) {
				kvpMap.put(key, value);
			}
			
			if(key.equals("FORMAT")) {
				kvpMap.put(key, value);
				legend.setFormat(value);
			}
			
			if(key.equals("WIDTH")) {
				kvpMap.put(key, value);
				legend.setWidth(Integer.valueOf(value));
			}
			
			if(key.equals("HEIGHT")) {
				kvpMap.put(key, value);
				legend.setHeight(Integer.valueOf(value));
			}
			
			if(key.equals("STRICT")) {
				kvpMap.put(key, value);
				legend.setStrict(value.equals("true") ? true : false);
			}
			
			if(key.equals("style")) {
				kvpMap.put(key, value);
			}
			
			if(key.equals("TRANSPARENT")) {
				kvpMap.put(key, value);
				legend.setTransparent(value.equals("true") ? true : false);
			}
		}
    	
    	legend.setKvp(kvpMap);
    }
    
    public static Style resizeStyleDPI(LegendGraphicEntity entity, Style gt2Style) {
    	int w = entity.getWidth();
    	int h = entity.getHeight();
    	double scale = entity.getScale();
    	Map<String, Object> legendOptions = entity.getLegendOptions();
        double dpi = RendererUtilities.getDpi(legendOptions);
        double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        if (dpi != standardDpi) {
            double scaleFactor = dpi / standardDpi;
            w = ((int) Math.round(w * scaleFactor));
            h = ((int) Math.round(h * scaleFactor));
            DpiRescaleStyleVisitor dpiVisitor = new DpiRescaleStyleVisitor(scaleFactor);
            dpiVisitor.visit(gt2Style);
            gt2Style = (Style) dpiVisitor.getCopy();
        }
        // apply UOM rescaling if we have a scale
        if (scale > 0) {
            double pixelsPerMeters = RendererUtilities.calculatePixelsPerMeterRatio(scale, legendOptions);
            UomRescaleStyleVisitor rescaleVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
            rescaleVisitor.visit(gt2Style);
            gt2Style = (Style) rescaleVisitor.getCopy();
        }
        entity.setWidth(w);
        entity.setHeight(h);;
        return gt2Style;
    }
    
    public static boolean checkRasterSymbolizer(final Style style) {
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule r : fts.rules()) {
                for (Symbolizer s : r.symbolizers()) {
                    if (s instanceof RasterSymbolizer) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public static boolean checkGridLayer(final FeatureType layer) {
    	if(layer == null) return false;
        if (!(layer instanceof SimpleFeatureType)) return false;
        boolean found = false;
        final Collection<PropertyDescriptor> descriptors = layer.getDescriptors();
        for (PropertyDescriptor descriptor : descriptors) {
            // get the type
            final PropertyType type = descriptor.getType();
            if (type.getBinding().isAssignableFrom(GridCoverage2D.class) || type.getBinding().isAssignableFrom(GridCoverage2DReader.class)) {
                found = true;
                break;
            }
        }
        return found;
    }
    
    public static SimpleFeature createSampleFeature() {
        SimpleFeatureType type;
        try {
            type = DataUtilities.createType("Sample", "the_geom:Geometry");
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return SimpleFeatureBuilder.template(type, null);
    }
    
    public static Feature createSampleFeature(FeatureType schema) throws RuntimeException {
        Feature sampleFeature;
        try {
            if (schema instanceof SimpleFeatureType) {
                if (hasMixedGeometry((SimpleFeatureType) schema)) {
                    // we can't create a sample for a generic Geometry type
                    sampleFeature = null;
                } else {
                    sampleFeature = SimpleFeatureBuilder.template((SimpleFeatureType) schema, null);
                }
            } else {
                sampleFeature = DataUtilities.templateFeature(schema);
            }
        } catch (IllegalAttributeException e) {
            throw new RuntimeException(e);
        }
        return sampleFeature;
    }
    
    public static boolean hasMixedGeometry(SimpleFeatureType schema) {
        for (AttributeDescriptor attDesc : schema.getAttributeDescriptors()) {
            if (isMixedGeometry(attDesc)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isMixedGeometry(AttributeDescriptor attDesc) {
        if (attDesc instanceof GeometryDescriptor && attDesc.getType().getBinding() == Geometry.class) {
            return true;
        }
        return false;
    }
    
    public static Rule[] getApplicableRules(final FeatureTypeStyle[] ftStyles, double scaleDenominator) {
    	ensureNotNull(ftStyles, "FeatureTypeStyle array ");
        final List<Rule> ruleList = new ArrayList<Rule>();

        // get applicable rules at the current scale
        for (int i = 0; i < ftStyles.length; i++) {
            FeatureTypeStyle fts = ftStyles[i];
            for (Rule r : fts.rules()) {
                if (isWithInScale(r, scaleDenominator)) {
                	ruleList.add(r);
                }
            }
        }
        return ruleList.toArray(new Rule[ruleList.size()]);
    }
    
    public static void ensureNotNull(final Object argument, final String message) {
        if (message == null) throw new NullPointerException("Message cannot be null");
        if (argument == null) throw new NullPointerException(message + " cannot be null");
    }
    
    public static void ensureNotNull(final Object argument) {
        ensureNotNull(argument, "Argument cannot be null");
    }
    
    public static boolean isWithInScale(final Rule r, final double scaleDenominator) {
        return (scaleDenominator == -1)
                || (((r.getMinScaleDenominator() - TOLERANCE) <= scaleDenominator)
                && ((r.getMaxScaleDenominator() + TOLERANCE) > scaleDenominator));
    }
    
    public static Rule getRule(FeatureTypeStyle[] fts, String rule) {
        Rule sldRule = null;
        for (int i = 0; i < fts.length; i++) {
            for (Rule r : fts[i].rules()) {
                if (rule.equalsIgnoreCase(r.getName())) {
                    sldRule = r;
                    break;
                }
            }
        }
        return sldRule;
    }
    //=====================================================================================
    public static double[] calcSymbolSize(double defaultMaxSize,double defaultMinSize,Feature feature,final Rule[] rules) {
        // check for max and min size in rendered symbols
        double minSize = defaultMaxSize;
        double maxSize = defaultMinSize;

        final int ruleCount = rules.length;
        for (int i = 0; i < ruleCount; i++) {
            Feature sample = getSampleFeatureForRule(feature, rules[i]);
            MetaBufferEstimator estimator = new MetaBufferEstimator(sample);
            for (Symbolizer symbolizer : rules[i].symbolizers()) {
                if (symbolizer instanceof PointSymbolizer || symbolizer instanceof LineSymbolizer) {
                    double size = getSymbolizerSize(estimator, symbolizer, defaultMaxSize);
                    // a line symbolizer is depicted as a line of the requested size, so don't go
                    // below min
                    if (size < minSize && !(symbolizer instanceof LineSymbolizer)) {
                        minSize = size;
                    }
                    if (size > maxSize) {
                        maxSize = size;
                    }
                }
            }
        }
        return new double[] {minSize, maxSize};
    }
    
    public static Feature getSampleFeatureForRule(Feature sample, final Rule rule) {
        if (sample == null) {
            int dimensionality = 1;
            for (Symbolizer symbolizer : rule.symbolizers()) {
                if (LineSymbolizer.class.isAssignableFrom(symbolizer.getClass())) {
                    dimensionality = 2;
                }
                if (PolygonSymbolizer.class.isAssignableFrom(symbolizer.getClass())) {
                    dimensionality = 3;
                }
            }
            return null;
        } else {
            return sample;
        }
    }
    
    public static Class<?> getGeometryForDimensionality(int dimensionality) {
        if (dimensionality == 1) {
            return Point.class;
        }
        if (dimensionality == 2) {
            return LineString.class;
        }
        return Polygon.class;
    }
    
    public static double getSymbolizerSize(MetaBufferEstimator estimator, Symbolizer symbolizer, double defaultSize) {
        estimator.reset();
        symbolizer.accept(estimator);
        int buffer = estimator.getBuffer();
        if (buffer > 0) {
            return buffer;
        } else {
            return defaultSize;
        }
    }
    
    //=============================================================
    public static BufferedImage createImage(final int width, int height, final IndexColorModel palette, final boolean transparent) {
        // WARNING: whenever this method is changed, change getDrawingSurfaceMemoryUse accordingly
        if (palette != null) {
            final WritableRaster raster = Raster.createInterleavedRaster(palette.getTransferType(), width, height, 1, null);
            return new BufferedImage(palette, raster, false, null);
        }
        if (transparent) {
            return new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        }
        // in case there was no active rule, the height is going to be zero, push it up so that we build a transparent image
        if (height == 0) {
            height = 1;
        }
        // don't use alpha channel if the image is not transparent (load testing shows this image setup is the fastest to draw and encode on
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }
    
    public static Graphics2D prepareTransparency(final boolean transparent,final Color bgColor,final RenderedImage preparedImage,
            final Map<RenderingHints.Key, Object> extraHints) {
    	
        final Graphics2D graphic;

        if (preparedImage instanceof BufferedImage) {
            graphic = ((BufferedImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof TiledImage) {
            graphic = ((TiledImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof VolatileImage) {
            graphic = ((VolatileImage) preparedImage).createGraphics();
        } else {
            throw new RuntimeException("Unrecognized back-end image type");
        }

        // fill the background with no antialiasing
        Map<RenderingHints.Key, Object> hintsMap;
        if (extraHints == null) {
            hintsMap = new HashMap<RenderingHints.Key, Object>();
        } else {
            hintsMap = new HashMap<RenderingHints.Key, Object>(extraHints);
        }
        hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphic.setRenderingHints(hintsMap);
        if (transparent) {
            int type = AlphaComposite.SRC;
            graphic.setComposite(AlphaComposite.getInstance(type));

            Color c = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
            graphic.setBackground(bgColor);
            graphic.setColor(c);
            graphic.fillRect(0, 0, preparedImage.getWidth(), preparedImage.getHeight());
            type = AlphaComposite.SRC_OVER;
            graphic.setComposite(AlphaComposite.getInstance(type));
        } else {
            graphic.setColor(bgColor);
            graphic.fillRect(0, 0, preparedImage.getWidth(), preparedImage.getHeight());
        }
        return graphic;
    }
    
    public static final Color DEFAULT_BG_COLOR = Color.WHITE;
    
    public static Color getBackgroundColor(final LegendGraphicEntity req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        if (legendOptions == null) return DEFAULT_BG_COLOR;
        Object clr = legendOptions.get("bgColor");
        if (clr instanceof Color) {
            return (Color) clr;
        } else if (clr == null) {
            return DEFAULT_BG_COLOR;
        }
        try {
            return color((String) clr);
        } catch (NumberFormatException e) {
            return DEFAULT_BG_COLOR;
        }
    }
    
    public static Color color(String hex) {
        ensureNotNull(hex, "hex value");
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        return Color.decode(hex);
    }
    
    @SuppressFBWarnings({
        "NP_NULL_ON_SOME_PATH",
        "NP_NULL_PARAM_DEREF"
    }) // SP does not recognize the check in ensureNotNull
    public static Color color(final ColorMapEntry entry) {
        ensureNotNull(entry, "entry");
        Expression color = entry.getColor();
        ensureNotNull(color, "color");
        String colorString = color.evaluate(null, String.class);
        if (colorString != null && colorString.startsWith("${")) {
            color = ExpressionExtractor.extractCqlExpressions(colorString);
            colorString = color.evaluate(null, String.class);
        }
        ensureNotNull(colorString, "colorString");
        return color(colorString);
    }
    
    public static double getOpacity(final ColorMapEntry entry)
            throws IllegalArgumentException, MissingResourceException {

        ensureNotNull(entry, "ColorMapEntry");
        // //
        //
        // As stated in <a
        // href="https://portal.opengeospatial.org/files/?artifact_id=1188">
        // OGC Styled-Layer Descriptor Report (OGC 02-070) version
        // 1.0.0.</a>:
        // "Not all systems can support opacity in colormaps. The default
        // opacity is 1.0 (fully opaque)."
        //
        // //
        Expression opacity = entry.getOpacity();
        Double opacityValue = null;
        if (opacity != null) opacityValue = opacity.evaluate(null, Double.class);
        else return 1.0;
        if (opacityValue == null && opacity instanceof Literal) {
            String opacityExp = opacity.evaluate(null, String.class);
            opacity = ExpressionExtractor.extractCqlExpressions(opacityExp);
            opacityValue = opacity.evaluate(null, Double.class);
        }
        if (opacityValue == null
                || (opacityValue.doubleValue() - 1) > 0
                || opacityValue.doubleValue() < 0) {
            throw new IllegalArgumentException(
                    Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2, "Opacity", opacityValue));
        }
        return opacityValue.doubleValue();
    }
    
    public static String getLabel(final ColorMapEntry entry) {
        ensureNotNull(entry, "entry");
        String labelString = entry.getLabel();
        if (labelString != null && labelString.startsWith("${")) {
            Expression label = ExpressionExtractor.extractCqlExpressions(labelString);
            labelString = label.evaluate(null, String.class);
        }
        return labelString;
    }
    
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH") // SP does not recognize the check in ensureNotNull
    public static double getQuantity(final ColorMapEntry entry) {
        ensureNotNull(entry, "entry");
        Expression quantity = entry.getQuantity();
        ensureNotNull(quantity, "quantity");
        Double quantityString = quantity.evaluate(null, Double.class);
        if (quantityString == null && quantity instanceof Literal) {
            String quantityExp = quantity.evaluate(null, String.class);
            quantity = ExpressionExtractor.extractCqlExpressions(quantityExp);
            quantityString = quantity.evaluate(null, Double.class);
        }
        ensureNotNull(quantityString, "quantityString");
        return quantityString.doubleValue();
    }
    
    public static BufferedImage hMergeBufferedImages(
            final BufferedImage left,
            final BufferedImage center,
            final BufferedImage right,
            final Map<Key, Object> hintsMap,
            final boolean transparent,
            final Color backgroundColor,
            final double dx) {
        if (right == null && center == null) return left;
        if (left == null) throw new NullPointerException("Left image cannot be null");
        int totalHeight =
                (int)
                        (Math.max(
                                        left.getHeight(),
                                        Math.max(
                                                (center != null
                                                        ? center.getHeight()
                                                        : Double.NEGATIVE_INFINITY),
                                                right != null ? right.getHeight() : 0))
                                + 0.5);
        final int totalWidth =
                (int)
                        (left.getWidth()
                                + (center != null ? center.getWidth() : 0)
                                + (right != null ? right.getWidth() : 0)
                                + 2 * dx
                                + 0.5);
        final BufferedImage finalImage = createImage(
                        totalWidth, totalHeight, (IndexColorModel) null, transparent);
        final Graphics2D finalGraphics = prepareTransparency(transparent, backgroundColor, finalImage, hintsMap);

        // place the left element
        int offsetX = 0;
        finalGraphics.drawImage(left, offsetX, 0, null);

        /// place the central element
        offsetX = (int) (left.getWidth() + dx + 0.5);
        if (center != null) {
            finalGraphics.drawImage(center, offsetX, 0, null);
            offsetX += (int) (center.getWidth() + dx + 0.5);
        }

        /// place the right element in case we have it
        if (right != null) {
            finalGraphics.drawImage(right, offsetX, 0, null);
        }

        finalGraphics.dispose();
        return (BufferedImage) finalImage;
    }
    
    public static boolean isFontAntiAliasing(final LegendGraphicEntity req) {
        if (req.getLegendOptions().get("fontAntiAliasing") instanceof String) {
            String aaVal = (String) req.getLegendOptions().get("fontAntiAliasing");
            if (aaVal.equalsIgnoreCase("on")
                    || aaVal.equalsIgnoreCase("true")
                    || aaVal.equalsIgnoreCase("yes")
                    || aaVal.equalsIgnoreCase("1")) {
                return true;
            }
        }

        return false;
    }
    
    private static final LegendLayout DEFAULT_LAYOUT = LegendLayout.VERTICAL;
    
    public static LegendLayout getLayout(final LegendGraphicEntity req) {
        ensureNotNull(req, "LegendGraphicEntity");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        LegendLayout layout = DEFAULT_LAYOUT;
        if (legendOptions != null && legendOptions.get("layout") != null) {
            try {
                layout = LegendLayout.valueOf(((String) legendOptions.get("layout")).toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        return layout;
    }
    
    public static LegendLayout getGroupLayout(final LegendGraphicEntity req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        LegendLayout layout = DEFAULT_LAYOUT;
        if (legendOptions != null && legendOptions.get("grouplayout") != null) {
            try {
                layout =
                        LegendLayout.valueOf(
                                ((String) legendOptions.get("grouplayout")).toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        return layout;
    }
    
    /** default column height is not limited */
    private static final int DEFAULT_COLUMN_HEIGHT = 0;

    /** default row width is not limited */
    private static final int DEFAULT_ROW_WIDTH = 0;

    /** default column number is not limited */
    private static final int DEFAULT_COLUMNS = 0;

    /** default row number is not limited */
    private static final int DEFAULT_ROWS = 0;
    
    public static int getRowWidth(final LegendGraphicEntity req) {
        ensureNotNull(req, "LegendGraphicEntity");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int rowwidth = DEFAULT_ROW_WIDTH;
        if (legendOptions != null && legendOptions.get("rowwidth") != null) {
            try {
                rowwidth = Integer.parseInt((String) legendOptions.get("rowwidth"));
            } catch (NumberFormatException e) {
            }
        }
        return rowwidth;
    }
    
    public static int getRows(final LegendGraphicEntity req) {
        ensureNotNull(req, "LegendGraphicEntity");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int rows = DEFAULT_ROWS;
        if (legendOptions != null && legendOptions.get("rows") != null) {
            try {
                rows = Integer.parseInt((String) legendOptions.get("rows"));
            } catch (NumberFormatException e) {
            }
        }
        return rows;
    }
    
    public static int getColumnHeight(final LegendGraphicEntity req) {
        ensureNotNull(req, "LegendGraphicEntity");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int columnheight = DEFAULT_COLUMN_HEIGHT;
        if (legendOptions != null && legendOptions.get("columnheight") != null) {
            try {
                columnheight = Integer.parseInt((String) legendOptions.get("columnheight"));
            } catch (NumberFormatException e) {
            }
        }
        return columnheight;
    }
    
    public static int getColumns(final LegendGraphicEntity req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        int columns = DEFAULT_COLUMNS;
        if (legendOptions != null && legendOptions.get("columns") != null) {
            try {
                columns = Integer.parseInt((String) legendOptions.get("columns"));
            } catch (NumberFormatException e) {
            }
        }
        return columns;
    }
    
    public static final Font DEFAULT_FONT = new Font("Sans-Serif", Font.PLAIN, 12);
    /** Default {@link Font} name for legends. */
    public static final String DEFAULT_FONT_NAME = "Sans-Serif";
    /** Default {@link Font} for legends. */
    public static final int DEFAULT_FONT_TYPE = Font.PLAIN;
    /** Default {@link Font} for legends. */
    public static final int DEFAULT_FONT_SIZE = 12;
    
    public static Font getLabelFont(final LegendGraphicEntity req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        if (legendOptions == null) return DEFAULT_FONT;
        String legendFontName = DEFAULT_FONT_NAME;
        if (legendOptions.get("fontName") != null) {
            legendFontName = (String) legendOptions.get("fontName");
        }

        int legendFontFamily = DEFAULT_FONT_TYPE;
        if (legendOptions.get("fontStyle") != null) {
            String legendFontFamily_ = (String) legendOptions.get("fontStyle");
            if (legendFontFamily_.equalsIgnoreCase("italic")) {
                legendFontFamily = Font.ITALIC;
            } else if (legendFontFamily_.equalsIgnoreCase("bold")) {
                legendFontFamily = Font.BOLD;
            }
        }

        int legendFontSize = DEFAULT_FONT_SIZE;
        if (legendOptions.get("fontSize") != null) {
            try {
                legendFontSize = Integer.valueOf((String) legendOptions.get("fontSize"));
            } catch (NumberFormatException e) {
                legendFontSize = DEFAULT_FONT_SIZE;
            }
        }

        double dpi = RendererUtilities.getDpi(req.getLegendOptions());
        double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        if (dpi != standardDpi) {
            double scaleFactor = dpi / standardDpi;
            legendFontSize = (int) Math.ceil(legendFontSize * scaleFactor);
        }

        if (legendFontFamily == DEFAULT_FONT_TYPE
                && legendFontName.equalsIgnoreCase(DEFAULT_FONT_NAME)
                && (legendFontSize == DEFAULT_FONT_SIZE || legendFontSize <= 0))
            return DEFAULT_FONT;

        return new Font(legendFontName, legendFontFamily, legendFontSize);
    }
    
    public static String getRuleLabel(Rule rule, LegendGraphicEntity req) {
        // What's the label on this rule? We prefer to use
        // the 'title' if it's available, but fall-back to 'name'
        final Description description = rule.getDescription();

        String label = "";
        if (description != null && description.getTitle() != null) {
            final InternationalString title = description.getTitle();
            if (req.getLocale() != null) {
                label = title.toString(req.getLocale());
            } else {
                label = title.toString();
            }
        } else if (rule.getName() != null) {
            label = rule.getName();
        }
        return label;
    }
    
    public static BufferedImage renderLabel(final String label, final Graphics2D g, final LegendGraphicEntity req) {
        ensureNotNull(label);
        ensureNotNull(g);
        ensureNotNull(req);
        // We'll accept '/n' as a text string
        // to indicate a line break, as well as a traditional 'real' line-break in the XML.
        BufferedImage renderedLabel;
        Color labelColor = getLabelFontColor(req);
        if ((label.indexOf("\n") != -1) || (label.indexOf("\\n") != -1)) {
            // this is a label WITH line-breaks...we need to figure out it's height *and*
            // width, and then adjust the legend size accordingly
            Rectangle2D bounds = new Rectangle2D.Double(0, 0, 0, 0);
            ArrayList<Integer> lineHeight = new ArrayList<Integer>();
            // four backslashes... "\\" -> '\', so "\\\\n" -> '\' + '\' + 'n'
            final String realLabel = label.replaceAll("\\\\n", "\n");
            StringTokenizer st = new StringTokenizer(realLabel, "\n\r\f");

            while (st.hasMoreElements()) {
                final String token = st.nextToken();
                Rectangle2D thisLineBounds = g.getFontMetrics().getStringBounds(token, g);

                // if this is directly added as thisLineBounds.getHeight(), then there are rounding
                // errors
                // because we can only DRAW fonts at discrete integer coords.
                final int thisLineHeight = (int) Math.ceil(thisLineBounds.getHeight());
                bounds.add(0, thisLineHeight + bounds.getHeight());
                bounds.add(thisLineBounds.getWidth(), 0);
                lineHeight.add((int) Math.ceil(thisLineBounds.getHeight()));
            }

            // make the actual label image
            renderedLabel =
                    new BufferedImage(
                            (int) Math.ceil(bounds.getWidth()),
                            (int) Math.ceil(bounds.getHeight()),
                            BufferedImage.TYPE_INT_ARGB);

            st = new StringTokenizer(realLabel, "\n\r\f");

            Graphics2D rlg = renderedLabel.createGraphics();
            rlg.setColor(labelColor);
            rlg.setFont(g.getFont());
            rlg.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
            rlg.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    g.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));
            int y = 0 - g.getFontMetrics().getDescent();
            int c = 0;

            while (st.hasMoreElements()) {
                y += lineHeight.get(c++).intValue();
                rlg.drawString(st.nextToken(), 0, y);
            }
            rlg.dispose();
        } else {
            // this is a traditional 'regular-old' label.  Just figure the
            // size and act accordingly.
            int height = (int) Math.ceil(g.getFontMetrics().getStringBounds(label, g).getHeight());
            int width = (int) Math.ceil(g.getFontMetrics().getStringBounds(label, g).getWidth());
            renderedLabel = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D rlg = renderedLabel.createGraphics();
            rlg.setColor(labelColor);
            rlg.setFont(g.getFont());
            rlg.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
            rlg.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    g.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));
            rlg.drawString(label, 0, height - rlg.getFontMetrics().getDescent());
            rlg.dispose();
        }

        return renderedLabel;
    }
    
    public static final Color DEFAULT_FONT_COLOR = Color.BLACK;
    
    public static Color getLabelFontColor(final LegendGraphicEntity req) {
        ensureNotNull(req, "GetLegendGraphicRequestre");
        final Map<String, Object> legendOptions = req.getLegendOptions();
        final String color = legendOptions != null ? (String) legendOptions.get("fontColor") : null;
        if (color == null) {
            // return the default
            return DEFAULT_FONT_COLOR;
        }
        try {
            return color(color);
        } catch (NumberFormatException e) {
            return DEFAULT_FONT_COLOR;
        }
    }
    
    //===================================================
    public static Symbolizer rescaleSymbolizer(Symbolizer symbolizer, double size, double newSize) {
        // perform a unit-less rescale
        double scaleFactor = newSize / size;
        RescaleStyleVisitor rescaleVisitor =
                new RescaleStyleVisitor(scaleFactor) {
                    @Override
                    protected Expression rescale(Expression expr) {
                        if (expr == null) {
                            return null;
                        } else if (expr instanceof Literal) {
                            Double value = expr.evaluate(null, Double.class);
                            return ff.literal(value * scaleFactor);
                        } else {
                            return ff.multiply(expr, ff.literal(scaleFactor));
                        }
                    }
                };
        symbolizer.accept(rescaleVisitor);
        symbolizer = (Symbolizer) rescaleVisitor.getCopy();
        return symbolizer;
    }
}
