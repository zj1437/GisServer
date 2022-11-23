package org.pygisserver.web.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.pygisserver.common.data.analysis.PyShapeFileAnalysis;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.entity.HeatmapAnalysisResult;
import org.pygisserver.web.service.AnalysisLayerServerService;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.type.AttributeDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 
 * @author: 刘洋 
 * @date: 2022-05-27
 */
@Service
@Slf4j
public class AnalysisLayerServerServiceImpl implements AnalysisLayerServerService {

	@Autowired
	private MapServerMapper mapServerMapper;
	
	@Override
	public List<String> queryAnalysisVectorLayer() {
		try {
			return mapServerMapper.queryVectorLayer();
		} catch (Exception e) {
			log.error("AnalysisLayerServerServiceImpl queryAnalysisVectorLayer has error {}", e.getLocalizedMessage());
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(), "矢量图层sql查询异常", e.getCause());
		}
	}

	
	@Override
	public HeatmapAnalysisResult analysisVectorLayer(String fileUrl) {
		PyShapeFileAnalysis shapeFileAnalysis = new PyShapeFileAnalysis(fileUrl);
		ReferencedEnvelope envelope = shapeFileAnalysis.getEnvelope();
		HeatmapAnalysisResult heatMapResult = new HeatmapAnalysisResult();
		//默认核密度半径等于bbox最小范围除以30
		float searchRadius = (float) (Math.min(envelope.getSpan(0), envelope.getSpan(1)) / 30);
		heatMapResult.setDefaultKernelRadius(searchRadius);
		//默认像元大小等于bbox最小范围除以250
		float cellPixel = (float) (Math.min(envelope.getSpan(0), envelope.getSpan(1)) / 250);
		heatMapResult.setCeilPiexl(cellPixel);
		List<AttributeDescriptor> attrDescriptor = shapeFileAnalysis.getAttrbutesName();
		List<String> list = new ArrayList<String>();
		for (AttributeDescriptor attributeDescriptor : attrDescriptor) {
			if (!"the_geom".equals(attributeDescriptor.getLocalName()) && !"geom".equals(attributeDescriptor.getLocalName())) {
				list.add(attributeDescriptor.getLocalName());
			}
		}
		heatMapResult.setAttrNames(list);
		return heatMapResult;
	}

}
