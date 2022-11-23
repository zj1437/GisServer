package org.pygisserver.render.service.impl;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.MaxSimpleFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PyFeatureSource implements SimpleFeatureSource {

	protected SimpleFeatureSource source;

	static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

	protected SimpleFeatureType schema;

	protected Filter definitionQuery = Filter.INCLUDE;

	protected CoordinateReferenceSystem declareCrs;
	
	private boolean forceDeclare = true;
	
	protected Double linearizationTolerance;

	public PyFeatureSource(FeatureSource<SimpleFeatureType, SimpleFeature> source, SimpleFeatureType schema,
			Filter definitionQuery, CoordinateReferenceSystem crs) {
		this.source = DataUtilities.simple(source);
		this.schema = schema;
		this.definitionQuery = definitionQuery;
		this.declareCrs = crs;
		if (this.definitionQuery == null) {
			this.definitionQuery = Filter.INCLUDE;
		}
	}

	@Override
	public Name getName() {
		return schema.getName();
	}

	@Override
	public ResourceInfo getInfo() {
		return source.getInfo();
	}

	@Override
	public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
		return (DataStore) source.getDataStore();
	}

	@Override
	public QueryCapabilities getQueryCapabilities() {
		return source.getQueryCapabilities();
	}

	@Override
	public void addFeatureListener(FeatureListener listener) {
		source.addFeatureListener(listener);
	}

	@Override
	public void removeFeatureListener(FeatureListener listener) {
		source.removeFeatureListener(listener);
	}

	@Override
	public SimpleFeatureType getSchema() {
		return schema;
	}

	@Override
	public ReferencedEnvelope getBounds() throws IOException {
		if (definitionQuery == Filter.INCLUDE) {
			return source.getBounds();
		} else {
			Query query = new Query(getSchema().getTypeName(), definitionQuery);

			return source.getBounds(query);
		}
	}

	@Override
	public ReferencedEnvelope getBounds(Query query) throws IOException {
		try {
			query = makeDefinitionQuery(query, schema);
		} catch (IOException ex) {
			return null;
		}
		return source.getBounds(query);
	}

	protected Query makeDefinitionQuery(Query query, SimpleFeatureType schema) throws IOException {
		if (definitionQuery == null && linearizationTolerance == null) {
			return query;
		}
		try {
			String[] propNames = extractAllowedAttributes(query, schema);
			Filter filter = query.getFilter();
			filter = makeDefinitionFilter(filter);
			Query defQuery = new Query(query);
			defQuery.setFilter(filter);
			defQuery.setPropertyNames(propNames);
			defQuery.setCoordinateSystem(query.getCoordinateSystem());
			if (query.getSortBy() != null) {
				defQuery.setSortBy(query.getSortBy());
			}
			if (linearizationTolerance != null) {
				query.getHints().put(Hints.LINEARIZATION_TOLERANCE, linearizationTolerance);
			}
			return defQuery;
		} catch (Exception ex) {
			ex.getStackTrace();
		}
		return null;
	}

	protected Filter makeDefinitionFilter(Filter filter) {
		Filter newFilter = filter;
		try {
			if (definitionQuery == Filter.INCLUDE) {
				return filter;
			}
			SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
			Filter simplifiedDefinitionQuery = (Filter) definitionQuery.accept(visitor, null);
			if (filter == Filter.INCLUDE) {
				newFilter = simplifiedDefinitionQuery;
			} else if (simplifiedDefinitionQuery != Filter.INCLUDE) {
				newFilter = ff.and(simplifiedDefinitionQuery, filter);
			}
		} catch (Exception ex) {
			ex.getStackTrace();
		}

		return newFilter;
	}

	private String[] extractAllowedAttributes(Query query, SimpleFeatureType schema) {
		String[] propNames = null;
		if (query.retrieveAllProperties()) {
			List<String> props = new ArrayList();

			for (int i = 0; i < schema.getAttributeCount(); i++) {
				AttributeDescriptor att = schema.getDescriptor(i);
				if (Feature.class.isAssignableFrom(att.getType().getBinding()) && !query.getJoins().isEmpty()) {
					continue;
				}
				props.add(att.getLocalName());
			}
			propNames = props.toArray(new String[props.size()]);
		} else {
			String[] queriedAtts = query.getPropertyNames();
			int queriedAttCount = queriedAtts.length;
			List allowedAtts = new LinkedList();
			for (int i = 0; i < queriedAttCount; i++) {
				if (schema.getDescriptor(queriedAtts[i]) != null) {
					allowedAtts.add(queriedAtts[i]);
				} else {
					log.info("queried a not allowed property: " + queriedAtts[i] + ". Ommitting it from query");
				}
			}
			propNames = (String[]) allowedAtts.toArray(new String[allowedAtts.size()]);
		}
		return propNames;
	}

	@Override
	public int getCount(Query query) throws IOException {
		try {
            query = makeDefinitionQuery(query, schema);
        } catch (IOException ex) {
            return -1;
        }
        try {
            return source.getCount(query);
        } catch (IOException e) {
            return 0;
        }
	}

	@Override
	public Set<Key> getSupportedHints() {
		return source.getSupportedHints();
	}

	@Override
	public SimpleFeatureCollection getFeatures() throws IOException {
		return getFeatures(Query.ALL);
	}

	@Override
	public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
		return getFeatures(new Query(schema.getTypeName(), filter));
	}

	@Override
	public SimpleFeatureCollection getFeatures(Query query) throws IOException {
		SortBy[] sortBy = query.getSortBy();
        Integer offset = null, maxFeatures = null;
        if (sortBy != null && sortBy != SortBy.UNSORTED) {
            if (!source.getQueryCapabilities().supportsSorting(sortBy)) {
                query.setSortBy(null);
                offset = query.getStartIndex();
                maxFeatures =
                        query.getMaxFeatures() == Integer.MAX_VALUE ? null : query.getMaxFeatures();

                query.setStartIndex(null);
                query.setMaxFeatures(Query.DEFAULT_MAX);
            } else {
                sortBy = null;
            }
        }
        if (query.getStartIndex() != null) {
            if (!source.getQueryCapabilities().isOffsetSupported()) {
                offset = query.getStartIndex();
                maxFeatures = query.getMaxFeatures() == Integer.MAX_VALUE ? null : query.getMaxFeatures();
                query.setStartIndex(null);
                query.setMaxFeatures(Query.DEFAULT_MAX);
            }
        }
        Query reprojected = reprojectFilter(query);
        Query newQuery = makeDefinitionQuery(reprojected, schema);
        CoordinateReferenceSystem targetCRS = query.getCoordinateSystemReproject();
        try {
            SimpleFeatureCollection fc = source.getFeatures(newQuery);
            if (sortBy != null && sortBy != SortBy.UNSORTED) {
                fc = new SortedSimpleFeatureCollection(fc, sortBy);
            }
            if (offset != null || maxFeatures != null) {
                fc = new MaxSimpleFeatureCollection(fc,offset == null ? 0 : offset,
                                					maxFeatures == null ? Integer.MAX_VALUE : maxFeatures);
            }
            return applyProjectionPolicies(targetCRS, fc);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }
		return null;
	}
	
	private Query reprojectFilter(Query query) throws IOException {
        SimpleFeatureType nativeFeatureType = source.getSchema();
        final GeometryDescriptor geom = nativeFeatureType.getGeometryDescriptor();
        if (geom == null) return query;
        try {
            CoordinateReferenceSystem defaultCRS = null;
            CoordinateReferenceSystem nativeCRS = geom.getCoordinateReferenceSystem();
            if(nativeCRS == null) {
            	defaultCRS = declareCrs;
            	nativeFeatureType = FeatureTypes.transform(nativeFeatureType, declareCrs);
            }else if (forceDeclare){
            	defaultCRS = declareCrs;
        		nativeFeatureType = FeatureTypes.transform(nativeFeatureType, declareCrs);
            } else {
            	defaultCRS = nativeCRS;
            	nativeFeatureType = FeatureTypes.transform(nativeFeatureType, nativeCRS);
            }
            DefaultCRSFilterVisitor defaultCRSVisitor = new DefaultCRSFilterVisitor(ff, defaultCRS);
            Filter filter = query.getFilter() != null ? query.getFilter() : Filter.INCLUDE;
            Filter defaultedFilter = (Filter) filter.accept(defaultCRSVisitor, null);
            ReprojectingFilterVisitor reprojectingVisitor =
                    new ReprojectingFilterVisitor(ff, nativeFeatureType);
            Filter reprojectedFilter = (Filter) defaultedFilter.accept(reprojectingVisitor, null);
            Query reprojectedQuery = new Query(query);
            reprojectedQuery.setFilter(reprojectedFilter);
            reprojectedQuery.setCoordinateSystem(defaultCRS);
            return reprojectedQuery;
        } catch (Exception e) {
           log.error("Had troubles handling filter reprojection...", e);
           return null;
        }
    }
	
	protected SimpleFeatureCollection applyProjectionPolicies(
            CoordinateReferenceSystem targetCRS, SimpleFeatureCollection fc) throws OperationNotFoundException, NoSuchElementException, IOException, SchemaException, TransformException, FactoryException {
        if (fc.getSchema().getGeometryDescriptor() == null) {
            return fc;
        }
        CoordinateReferenceSystem nativeCRS =
                fc.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
        if (targetCRS != null) {
            if (nativeCRS == null) {
                fc = new ReprojectFeatureResults(fc, targetCRS);
            } else {
                if (!CRS.equalsIgnoreMetadata(nativeCRS, targetCRS)) {
                    fc = new ReprojectFeatureResults(fc, targetCRS);
                }
            }
        }
        return fc;
    }
}
