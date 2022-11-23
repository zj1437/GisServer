package org.pygisserver.render.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * @Description: PYGISServer 矢量点抽稀
 * @author: 刘洋 
 * @date: 2021-12-03
 */
public class PyPointSimplifier {

	/**
	 * 每个线程最终的需要显示的点数
	 * 对4096个像素，分成256个块，每一块按照16*16的大小来计算值
	 */
	public static ThreadLocal<List<Set<Point>>> threadPoints = new ThreadLocal<List<Set<Point>>>();
	
	/**
	 * 
	 * @param geom
	 * @return
	 */
	public static Geometry simplify(Geometry geom) {
		if (geom instanceof Point) {
			List<Set<Point>> currentPoints = threadPoints.get();
			if (currentPoints == null) {
				currentPoints = Arrays.asList(new HashSet[16*16]);
				normalPoints((Point) geom,currentPoints);
				threadPoints.set(currentPoints);
			}else {
				normalPoints((Point) geom,currentPoints);
				threadPoints.set(currentPoints);
			}
		}
		return geom;
	}
	
	/**
	 * 
	 * @param point
	 */
	private static void normalPoints(Point point,List<Set<Point>> pointList) {
		double lon = point.getX();
		double lat = point.getY();
		int offX = (int) Math.round( (lon / 256));
		offX = offX < 0 ? 0 : offX > 15 ? 15 : offX;
//		if (offX < 0 || offX > 15) {
//			return;
//		}
		int offy = (int) Math.round((lat / 256));
		offy = offy < 0 ? 0 : offy > 15 ? 15 : offy;
//		if (offy < 0 || offy > 15) {
//			return;
//		}
		int index = offy*16 + offX;
		if (!pointList.isEmpty() && pointList.get(index) != null && pointList.get(index).size() > 0) {
			Set<Point> points = pointList.get(index);
			points.add(point);
		}else {
			Set<Point> points = new HashSet<Point>();
			points.add(point);
			pointList.set(index, points);
		}
		
	}
	
}
