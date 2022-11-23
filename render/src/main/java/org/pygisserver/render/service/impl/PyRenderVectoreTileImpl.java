package org.pygisserver.render.service.impl;

import static org.geotools.renderer.lite.PyVectorRenderUtils.getStyleQuery;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.render.entity.PyMapContent;
import org.pygisserver.render.entity.PyRenderedParam;
import org.pygisserver.render.service.PyRenderVectoreTile;
import org.pygisserver.render.vector.Pipeline;
import org.pygisserver.render.vector.PipelineBuilder;
import org.pygisserver.render.vector.VectorTileBuilder;
import org.pygisserver.render.vector.VectorTileBuilderFactory;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.renderer.lite.PyVectorRenderUtils;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Stopwatch;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 矢量瓦片数据渲染
 * @author:  
 * @date: 2021-03-22
 */
@Slf4j
public class PyRenderVectoreTileImpl extends PyRenderImpl implements PyRenderVectoreTile {

	private VectorTileBuilderFactory tileBuilderFactory;
	
	private boolean clipToMapBounds = true;

    private double overSamplingFactor = 2.0;

    private boolean transformToScreenCoordinates = true;

    private static Map<Geometry,Feature> pointFeatureInfo = new HashMap<Geometry,Feature>();
	
	public PyRenderVectoreTileImpl(VectorTileBuilderFactory tileBuilderFactory,
								   boolean clipToMapBounds,
								   double overSamplingFactor,
								   boolean transformToScreenCoordinates) {
		this.tileBuilderFactory = tileBuilderFactory;
		this.clipToMapBounds = clipToMapBounds;
		this.overSamplingFactor = overSamplingFactor;
		this.transformToScreenCoordinates = transformToScreenCoordinates;
	}
	
	@Override
	public byte[] vectorTiles(PyRenderRequestParam requestParam) {
		constructRenderRequestParam(requestParam);
		PyRenderedParam renderParam = constructRenderParam(requestParam);
		int mapWidth = requestParam.getMapWidth();
		int mapHeight = requestParam.getMapHeight();
		Rectangle paintArea = new Rectangle(mapWidth, mapHeight);
		PyMapContent mapContent = null;
		try {
			mapContent = constructMapContent(requestParam, renderParam.getEnvelope());
			//设置viewpoint
			if (this.tileBuilderFactory.shouldOversampleScale()) {
	            paintArea = new Rectangle(this.tileBuilderFactory.getOversampleX() * mapWidth,
	                            		  this.tileBuilderFactory.getOversampleY() * mapHeight);
	        }
			VectorTileBuilder vectorTileBuilder = this.tileBuilderFactory.newBuilder(paintArea, renderParam.getEnvelope());
	        CoordinateReferenceSystem sourceCrs;
	        for (Layer layer : mapContent.layers()) {
	        	mapContent.getViewport().setBounds(renderParam.getEnvelope());
	            FeatureSource<?, ?> featureSource = layer.getFeatureSource();
	            GeometryDescriptor geometryDescriptor = featureSource.getSchema().getGeometryDescriptor();
	            if (null == geometryDescriptor) {
	                continue;
	            }
	            sourceCrs = geometryDescriptor.getType().getCoordinateReferenceSystem();
				 int buffer = PyVectorRenderUtils.getComputedBuffer(mapContent.getBuffer(),
						 	  PyVectorRenderUtils.getFeatureStyles(layer,
				                        						   paintArea,
				                        						   PyVectorRenderUtils.getMapScale(mapContent, renderParam.getEnvelope()),
				                        						   (FeatureType) featureSource.getSchema()));
				if (this.tileBuilderFactory.shouldOversampleScale()) {
					buffer *= Math.max(Math.max(this.tileBuilderFactory.getOversampleX(),
									            this.tileBuilderFactory.getOversampleY()),
												1);
				}
				Pipeline pipeline = getPipeline(mapContent, renderParam.getEnvelope(), paintArea, sourceCrs, buffer);
				Query query = getStyleQuery(layer, mapContent);
				query.getHints().remove(Hints.SCREENMAP);
				FeatureCollection<?, ?> features = featureSource.getFeatures(query);
				if (features.size() == 0) {
					continue;
				}
				run(features, pipeline, geometryDescriptor, vectorTileBuilder, layer);
	        }
	        return vectorTileBuilder.build(mapContent);
		}catch (IOException | FactoryException e) {
			log.error("PYRenderImageImpl renderImageByRequestParam has error:{}", e.getMessage());
		} finally {
			mapContent.dispose();
		}
        return null;
	}
	
	
	protected Pipeline getPipeline(
            final PyMapContent mapContent,
            final ReferencedEnvelope renderingArea,
            final Rectangle paintArea,
            CoordinateReferenceSystem sourceCrs,
            int buffer) {
        Pipeline pipeline;
        try {
            final PipelineBuilder builder = PipelineBuilder.newBuilder(renderingArea, paintArea, sourceCrs, overSamplingFactor, buffer);
            pipeline = builder.preprocess().transform(transformToScreenCoordinates)
                            .clip(clipToMapBounds, transformToScreenCoordinates)
                            .simplify(transformToScreenCoordinates)
                            .collapseCollections()
                            .build();
        } catch (FactoryException e) {
        	throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.BASE_ERROR);
        }
        return pipeline;
    }

    private Map<String, Object> getProperties(ComplexAttribute feature) {
        Map<String, Object> props = new TreeMap<>();
        for (Property p : feature.getProperties()) {
            if (!(p instanceof Attribute) || (p instanceof GeometryAttribute)) {
                continue;
            }
            String name = p.getName().getLocalPart();
            Object value;
            if (p instanceof ComplexAttribute) {
                value = getProperties((ComplexAttribute) p);
            } else {
                value = p.getValue();
            }
            if (value != null) {
                props.put(name, value);
            }
        }
        return props;
    }

    void run(
            FeatureCollection<?, ?> features,
            Pipeline pipeline,
            GeometryDescriptor geometryDescriptor,
            VectorTileBuilder vectorTileBuilder,
            Layer layer) {
        Stopwatch sw = Stopwatch.createStarted();
        int count = 0;
        int total = 0;
        Feature feature;
        boolean isPoint = false;

        try (FeatureIterator<?> it = features.features()) {
            while (it.hasNext()) {
                feature = it.next();
                total++;
                Geometry originalGeom;
                Geometry finalGeom;

                originalGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                try {
                    finalGeom = pipeline.execute(originalGeom);
                } catch (Exception processingException) {
                    processingException.printStackTrace();
                    continue;
                }
                if (finalGeom.isEmpty()) {
                    continue;
                }

                final String layerName = feature.getName().getLocalPart();
                final String featureId = feature.getIdentifier().toString();
                final String geometryName = geometryDescriptor.getName().getLocalPart();

                final Map<String, Object> properties = getProperties(feature); 

                //线面数据，需要一次插入
//                if (originalGeom.getDimension() != 0) {
                	vectorTileBuilder.addFeature(
                            layerName, featureId, geometryName, finalGeom, properties);
                    count++;
//                }else {
//                	//点数据，缓存每一次
//                	isPoint = true; 
//                	pointFeatureInfo.put(finalGeom, feature);
//                }
            }
            //对于点数据
//            if (isPoint) {
//            	List<Set<Point>> list = FePointSimplifier.threadPoints.get();
//            	for (Iterator iterator = list.iterator(); iterator.hasNext();) {
//					Set<Point> points = (Set<Point>) iterator.next();
//					if (points == null || points.isEmpty()) {
//						continue;
//					}
//					//排序之后,取中间数据，保证点的均匀
//					List<Point> listPoint = points.stream().sorted((p1,p2) -> {
//						return p1.compareTo(p2);
//					}).collect(Collectors.toList());
//					//取中间数
//					Point mediaPoint = listPoint.get(listPoint.size() / 2);
//					Feature feature1 = pointFeatureInfo.get(mediaPoint);
//					addFeature(feature1,vectorTileBuilder,geometryDescriptor,mediaPoint);
//                    count++;
//				}
//            }
            
        }finally {
//        	FePointSimplifier.threadPoints.remove();
        	sw.stop();
        	String msg = String.format("Added %,d out of %,d features of '%s' in %s",
        			count, total, layer.getTitle(), sw);
        	log.info(msg);
        }
    }
    
    public void addFeature(Feature feature,
    					   VectorTileBuilder vectorTileBuilder,
    					   GeometryDescriptor geometryDescriptor,
    					   Geometry finalGeom) {

		final String layerName = feature.getName().getLocalPart();
        final String featureId = feature.getIdentifier().toString();
        final String geometryName = geometryDescriptor.getName().getLocalPart();

        final Map<String, Object> properties = getProperties(feature); 
		
		vectorTileBuilder.addFeature(
				layerName, featureId, geometryName, finalGeom, properties);
    	
    }
    
}
