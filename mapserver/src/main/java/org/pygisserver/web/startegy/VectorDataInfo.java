package org.pygisserver.web.startegy;

import java.io.IOException;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.web.entity.StyleInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VectorDataInfo extends AbstractReadData {

	private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
	
	public VectorDataInfo(FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
		this.featureSource = featureSource;
	}

	@Override
	public ReferencedEnvelope getBboxFromFile() {
		try {
			ReferencedEnvelope envelope = featureSource.getBounds();
			CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
			if (crs == null) {
				crs = CRS.decode("EPSG:4326",true);
			} else {
				return envelope;
			}
			return new ReferencedEnvelope(envelope.getMinimum(0), envelope.getMaximum(0), envelope.getMinimum(1), 
					envelope.getMaximum(1), crs);
		} catch (Exception e) {
			log.error("VectorDataInfo getBboxFromFile has error:" +  ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "矢量数据Bbox"));
		}
	}


	@Override
	public CoordinateReferenceSystem getResourceSrs() {
		try {
			CoordinateReferenceSystem crs = featureSource.getBounds().getCoordinateReferenceSystem();
			if (crs != null) {
				return crs;
			}else {
				return CRS.decode("EPSG:4326",true);
			}
		} catch (IOException | FactoryException e) {
			log.error("VectorDataInfo getResourceSrs has error:" +  ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "矢量数据Bbox"));
		}
	}

	@Override
	public int getDataType() {
		GeometryDescriptor gd = featureSource.getSchema().getGeometryDescriptor();
		Class gtype = gd.getType().getBinding();
		if (Point.class.isAssignableFrom(gtype) || MultiPoint.class.isAssignableFrom(gtype)) {
			return StyleInfo.POINT;
		} else if (LineString.class.isAssignableFrom(gtype) || MultiLineString.class.isAssignableFrom(gtype)) {
			return StyleInfo.LINE;
		} else if (Polygon.class.isAssignableFrom(gtype) || MultiPolygon.class.isAssignableFrom(gtype)) {
			return StyleInfo.POLYGON;
		}
		return StyleInfo.POLYGON;
	}

	@Override
	public int featuresSize() {
		try {
			return featureSource.getCount(Query.ALL);
		} catch (IOException e) {
			log.error("VectorDataInfo featuresSize has error:" +  ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "矢量数据Features信息"));
		}
	}

	@Override
	public int getMaxLevel(ReferencedEnvelope originalEnvelope) {
		return 21;
	}

}
