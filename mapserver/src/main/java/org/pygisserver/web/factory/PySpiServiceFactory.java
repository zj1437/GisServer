package org.pygisserver.web.factory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.netanalysis.service.NetworkMapLayerAnalysis;
import org.pygisserver.spi.service.PySpiService;
import org.pygisserver.terrainanalysis.service.MapLayerAnalysis;
import org.springframework.stereotype.Component;

/**
 * @Description:  PYGISServer spi实现类的构造工厂
 * @author:  
 * @date: 2021-06-05
 */
@Component
public class PySpiServiceFactory {
	
	private static Map<String,PySpiService> SERVICE_INSTACNE_CACHE = new HashMap<String,PySpiService>();
	
	/**
	 * 构造地形分析服务实现类
	 * @return
	 */
	public MapLayerAnalysis createMapLayerAnalysisBean() {
		try {
			ServiceLoader<MapLayerAnalysis> mapLayerAnalysis = ServiceLoader.load(MapLayerAnalysis.class);
			Iterator<MapLayerAnalysis> it = mapLayerAnalysis.iterator();
			if (it != null && it.hasNext()) {
				MapLayerAnalysis maAnalysis = it.next();
				return maAnalysis;
			}
		} catch (Exception e) {
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.TERRAIN_ANALYSIS_PLUG_FAIL, "地形分析"));
		}
		throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.TERRAIN_ANALYSIS_PLUG, "地形分析"));
	}
	
	/**
	 * 构造网络分析服务实现类
	 * @return
	 */
	public NetworkMapLayerAnalysis createNetWorkAnalysisBean() {
		try {
			ServiceLoader<NetworkMapLayerAnalysis> netWorkAnalysis = ServiceLoader.load(NetworkMapLayerAnalysis.class);
			Iterator<NetworkMapLayerAnalysis> it = netWorkAnalysis.iterator();
			if (it != null && it.hasNext()) {
				NetworkMapLayerAnalysis maAnalysis = it.next();
				return maAnalysis;
			}
		} catch (Exception e) {
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.TERRAIN_ANALYSIS_PLUG_FAIL, "网络分析"));
		}
		throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.TERRAIN_ANALYSIS_PLUG, "网络分析"));
	}
	
	/**
	 * 获取服务实现类
	 * @param class1
	 * @param clazzName
	 * @return
	 */
	public PySpiService findClazzInstance(String clazzName){
		if (SERVICE_INSTACNE_CACHE.containsKey(clazzName)) {
			return SERVICE_INSTACNE_CACHE.get(clazzName);
		}
		PySpiService pySpiService = null;
		if (clazzName.equalsIgnoreCase("MapLayerAnalysis")) {
			pySpiService = createMapLayerAnalysisBean();
		}else if (clazzName.equalsIgnoreCase("NetAnalysis")) {
			pySpiService = createNetWorkAnalysisBean();
		}
		SERVICE_INSTACNE_CACHE.put(clazzName, pySpiService);
		return pySpiService;
	}
}
