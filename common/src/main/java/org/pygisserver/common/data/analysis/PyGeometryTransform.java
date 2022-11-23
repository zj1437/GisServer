package org.pygisserver.common.data.analysis;

import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import com.google.common.base.Preconditions;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 处理geometry的投影转化问题
 * @author: 刘洋
 * @date: 2022-05-20
 */
@Slf4j
public class PyGeometryTransform {

	private MathTransform sourceToTarget;

	public PyGeometryTransform(String sourceSrs, String targetSrs) {
		try {
			this.sourceToTarget = buildTransform(CRS.decode(sourceSrs, true),CRS.decode(targetSrs, true));
		} catch (FactoryException e) {
			log.error("FeGeometryTransform 构造函数报错{}",e.getLocalizedMessage());
		}
		
	}

	/**
	 * 构造投影变化矩阵
	 * @param sourceCRS
	 * @param destCRS
	 * @return
	 */
	public MathTransform buildTransform(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem destCRS){
		Preconditions.checkNotNull(sourceCRS, "sourceCRS");
		Preconditions.checkNotNull(destCRS, "destCRS");
		MathTransform transform = null;
		try {
			if (sourceCRS.getCoordinateSystem().getDimension() >= 3) {
				MathTransform toWgs84_3d = CRS.findMathTransform(sourceCRS, DefaultGeographicCRS.WGS84_3D);
				MathTransform toWgs84_2d = CRS.findMathTransform(DefaultGeographicCRS.WGS84_3D, DefaultGeographicCRS.WGS84);
				transform = ConcatenatedTransform.create(toWgs84_3d, toWgs84_2d);
				sourceCRS = DefaultGeographicCRS.WGS84;
			}
			MathTransform2D sourceToTarget;
			sourceToTarget = (MathTransform2D) CRS.findMathTransform(sourceCRS, destCRS, true);
			if (transform == null) {
				return sourceToTarget;
			}
			if (sourceToTarget.isIdentity()) {
				return  transform;
			}
			return ConcatenatedTransform.create(transform, sourceToTarget);
		} catch (FactoryException e) {
			log.error("FeGeometryTransform buildTransform has error {}",e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),"投影矩阵变换失败", e.getCause());
		}
	}
	
	/**
	 * @param sourceGeom 原始几何体
	 * @return  返回投影转化后的几何体
	 * @throws MismatchedDimensionException
	 * @throws TransformException
	 */
	public Geometry transform(Geometry sourceGeom) throws MismatchedDimensionException, TransformException {
		 return JTS.transform(sourceGeom, this.sourceToTarget);
	}
}
