package org.pygisserver.web.utils;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * @Description: 主要用来处理 集合对象装换的工具类
 * @author: 刘洋 
 * @date: 2022-06-22
 */
public class JTSUtil {

	private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory();
	
	/**
	 * 
	 * @param coordinates geometry点坐标
	 * @param dataType 集合数据类型
	 * @return 返回对应的bbox
	 */
	public static Envelope getInnerEnvelope(int dataType,Coordinate...coordinates) {
		Geometry geometry = GEOMETRY_FACTORY.createMultiPointFromCoords(coordinates);
		return geometry.getEnvelopeInternal();
	}
	
}
