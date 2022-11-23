package org.pygisserver.web.aop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.web.factory.PySpiServiceFactory;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.utils.UserUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@Slf4j
public class MapLayerAnalysisAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getDemByPostion(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getDemByLinePoint(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getDemByRegion(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getSlopeByPostion(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getAspectByLinePoint(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getAspectByRegion(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getAspectByPostion(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getSlopeByRegion(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapLayerAnalysisController.getSlopeByLinePoint(..))")
	private void mapLayerAnalysis() {};
	
	@Autowired
	private MapServerService mapServerService;
	
	@Resource
	private PySpiServiceFactory spserviceFactory;
	
	private static Map<String,LayerBaseInfo> layerBaseInfoCache = new HashMap<String,LayerBaseInfo>();
	
	private static Map<String,ReferencedEnvelope> layerReferenceEnvelopeCache = new HashMap<String,ReferencedEnvelope>();
	
	@Before("mapLayerAnalysis()")
	public void beforeQueryLayerInfo(JoinPoint joinPoint) {
		//优先判断是否有地形插件
		spserviceFactory.findClazzInstance("MapLayerAnalysis");
		Object[] param = joinPoint.getArgs();
		String layerName = param[0].toString();
		if (StringUtils.isBlank(layerName)) {
			return;
		}
		if (layerBaseInfoCache.containsKey(layerName)) {
			return;
		}
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		HttpServletRequest request = servletRequestAttributes.getRequest();
		String reqIP = UserUtil.getRequestIp(request);
		LayerBaseInfo layerBaseInfo = mapServerService.getLayerBaseInfo(layerName, reqIP);
		if (layerBaseInfo == null) {
			throw new BaseException(ExceptionsUtil.ERROR_500, String.format("分析的图层%s不存在",layerName)) ;
		}
		String nativeBbox = layerBaseInfo.getNativaBbox();
		if (StringUtils.isBlank(nativeBbox)) {
			return;
		}
		String[] bboxArray = nativeBbox.split(";");
		ReferencedEnvelope envelope = null;
		try {
			envelope = new ReferencedEnvelope(Double.parseDouble(bboxArray[0]),
											 Double.parseDouble(bboxArray[2]),
											 Double.parseDouble(bboxArray[1]),
											 Double.parseDouble(bboxArray[3]),
											 CRS.decode("EPSG:4326",true));
		} catch (MismatchedDimensionException | NumberFormatException | FactoryException e) {
			log.error("MapLayerAnalysisAdvices beforeQueryLayerInfo has error:" + e.getLocalizedMessage());
		}
		layerBaseInfoCache.put(layerName, layerBaseInfo);
		layerReferenceEnvelopeCache.put(layerName, envelope);
	}
	public LayerBaseInfo getLayerBaseInfo(String layerName) {
	
		return layerBaseInfoCache.get(layerName);
	}
	
	public ReferencedEnvelope getLayerEnvelope(String layerName) {
		return layerReferenceEnvelopeCache.get(layerName);
	}
	
	public void delteLayerCache(List<String> layerNames) {
		for (String layerName : layerNames) {
			Iterator<Entry<String,LayerBaseInfo>> it = layerBaseInfoCache.entrySet().iterator();
			while(it.hasNext()) {
				String layer = it.next().getKey();
				if (layerName.equals(layer)) {
					it.remove();
				}
			}
			
			Iterator<Entry<String,ReferencedEnvelope>> it1 = layerReferenceEnvelopeCache.entrySet().iterator();
			while(it1.hasNext()) {
				String layer = it1.next().getKey();
				if (layerName.equals(layer)) {
					it1.remove();
				}
			}
		}
	}
}
