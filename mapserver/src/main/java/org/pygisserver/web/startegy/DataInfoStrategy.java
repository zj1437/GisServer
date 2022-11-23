package org.pygisserver.web.startegy;

import java.util.Map;

import org.pygisserver.web.service.ReadDataInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @Description: 数据信息策略类 
 * @author: 
 * @date: 2019-07-18
 */
public class DataInfoStrategy {
    
	private ReadDataInfo readDataInfo;
	
	public DataInfoStrategy(ReadDataInfo readDataInfo) {
		this.readDataInfo = readDataInfo;
	}
	
	public ReferencedEnvelope getEnvelope() {
		return readDataInfo.getBboxFromFile();
	}
	
	public int getImageWidth() {
		return readDataInfo.getImgWidth();
	}
	
	public int getImgHeight() {
		return readDataInfo.getImgHeight();
	}
	
	public CoordinateReferenceSystem getResourceSrs() {
		return readDataInfo.getResourceSrs();
	}
	
	public int getDataType() {
		return readDataInfo.getDataType();
	}
	
	public int getTotalFeatures() {
		return readDataInfo.featuresSize();
	}
	
	public String getTileExtension() {
		return readDataInfo.getTileExtension();
	}
	
	public int getMaxLevel(ReferencedEnvelope originalEnvelope){
		return readDataInfo.getMaxLevel(originalEnvelope);
	}
	
	public  Map<Object, Object> getScaleMap(){
		return readDataInfo.scaleMap();
	}
	
	public String getSeaChartIdentifier(){
		return readDataInfo.getSeaChartIdentifier();
	}
	
	public int getOrigin() {
		return readDataInfo.getTileOriginFlag();
	}
}
