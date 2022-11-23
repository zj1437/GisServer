package org.pygisserver.pytc.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
import org.springframework.stereotype.Component;

/**
 * @Description: 数据资源管理管理类 
 * @author: 
 * @date: 2020-01-08
 */
@Component
public class ResourceInfoManager {

	private volatile Map<TileRequestInfo, List<TileResourceInfo>> produceResourceInfo = new HashMap<TileRequestInfo, List<TileResourceInfo>>();
	
	/**
	 * 
	 * @param requestInfo
	 * @param resourceInfo
	 */
	public synchronized void addResourceInfo(TileRequestInfo requestInfo,List<TileResourceInfo> resourceInfo) {
		if (produceResourceInfo.containsKey(requestInfo)) {
			produceResourceInfo.get(requestInfo).addAll(resourceInfo);
		}
		produceResourceInfo.put(requestInfo, resourceInfo);
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized Map<TileRequestInfo, List<TileResourceInfo>> getResourceInfo(){
		Map<TileRequestInfo,List<TileResourceInfo>> storeResourceInfo = new HashMap<TileRequestInfo,List<TileResourceInfo>>();
		for (Map.Entry<TileRequestInfo, List<TileResourceInfo>> entry : produceResourceInfo.entrySet()) {
			storeResourceInfo.put(entry.getKey(), entry.getValue());
			produceResourceInfo.remove(entry.getKey(), entry.getValue());
		}
		return storeResourceInfo;
	}
	
	public synchronized boolean startStoreResource() {
		return !produceResourceInfo.isEmpty();
	}
}
