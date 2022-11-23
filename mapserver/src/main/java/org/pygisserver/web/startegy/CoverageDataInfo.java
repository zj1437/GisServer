package org.pygisserver.web.startegy;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.io.IOException;

import javax.media.jai.ImageLayout;

import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.sqlite.util.SQLitePoolManage;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.web.entity.StyleInfo;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 读取geotiff数据的信息
 * @author: 
 * @date: 2019-07-17
 */
@Slf4j
public class CoverageDataInfo extends AbstractReadData {

	private int imageWidth;
	
	private Envelope originalEnvelope;
	
	private String fileType;

	private String fileUrl;
	
	private int dataType;
	
	private GridCoverage2DReader reader;
	
	public CoverageDataInfo(GridCoverage2DReader coverageReader,String fileType,String fileUrl) {
		this.fileType = fileType;
		this.fileUrl = fileUrl;
		this.reader = coverageReader;
		this.originalEnvelope = reader.getOriginalEnvelope();
		GridEnvelope originalRange = reader.getOriginalGridRange();
		this.imageWidth = originalRange.getSpan(0);
		try {
			setDataType(reader.getImageLayout());
		} catch (IOException e) {
			log.error("CoverageDataInfo construct has error {}",e.getLocalizedMessage());
		} finally {
			try {
				reader.dispose();
			} catch (IOException e) {
				log.error("CoverageDataInfo construct has error {}","关闭reader出错");
			}
		}
	}

	@Override
	public ReferencedEnvelope getBboxFromFile() {
		if (originalEnvelope != null) {
			return new ReferencedEnvelope(originalEnvelope.getMinimum(0),
										  originalEnvelope.getMaximum(0),
										  originalEnvelope.getMinimum(1),
										  originalEnvelope.getMaximum(1),
										  originalEnvelope.getCoordinateReferenceSystem());
		}
		
		return null;
	}
	
	@Override
	public CoordinateReferenceSystem getResourceSrs() {
		CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
		if (crs != null) {
			return crs;
		} else {
			try {
				return CRS.decode("EPSG:4326", true);
			} catch (FactoryException e) {
				log.error("CoverageDataInfo getResourceSrs has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "EPSG:4326坐标系"));
			}
		}
		
	}
	
	private void setDataType(ImageLayout imageLayout) {
		if (imageLayout == null) {
			this.dataType = StyleInfo.RASTER;
			return;
		}
		ColorModel colorModel = imageLayout.getColorModel(null);
		int band = colorModel.getNumComponents();
		ColorSpace colorSpace = colorModel.getColorSpace();
		// 当前颜色分量通道只有一位
		if (band == 1 && colorSpace.getType() == ColorSpace.TYPE_GRAY) {
			this.dataType = StyleInfo.RASTER_dem;
		} else {
			this.dataType =  StyleInfo.RASTER;
		}
	}

	@Override
	public int getDataType() {
		return dataType;
	}


	@Override
	public int getMaxLevel(ReferencedEnvelope originalEnvelope) {
		if (DataTypeEnum.MBTILES.getFileType().equalsIgnoreCase(fileType)) {
			//查询数据库获取最大结果
			SQLitePoolManage.createConnectionPool("jdbc:sqlite:" + fileUrl);
			return SQLitePoolManage.getMaxLevel("jdbc:sqlite:" + fileUrl);
		}else {
			double resolution = (originalEnvelope.getMaximum(0) - originalEnvelope.getMinimum(0)) / imageWidth;
			return GeometryUtil.get4326CurrentLevel(resolution,256);
		}
	}
	
	@Override
	public String getTileExtension() {
		if (DataTypeEnum.MBTILES.getFileType().equalsIgnoreCase(fileType)) {
			if (reader instanceof PyMbtileReader) {
				return ((PyMbtileReader)reader).getTileExtension();
			}
		}
		return null;
	}

}
