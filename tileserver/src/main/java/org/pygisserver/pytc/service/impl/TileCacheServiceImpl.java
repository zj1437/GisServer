package org.pygisserver.pytc.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.pygisserver.common.pytc.entity.LayerStoreTypeInfo;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.pytc.configuration.DataSourceContextHolder;
import org.pygisserver.pytc.dao.CommonMapper;
import org.pygisserver.pytc.util.SeedTaskUtil;
import org.pygisserver.tileserver.service.TileCacheService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//使用dubbo注册
@DubboService(version = "3.0.0")
@Slf4j
public class TileCacheServiceImpl implements TileCacheService {


	@Autowired
	private SeedTaskUtil seedTaskUtil;

	@Autowired
	private CommonMapper commonMapper;
	
	private static final Map<String,List<LayerStoreTypeInfo>> layerStoreTypes = new HashMap<String,List<LayerStoreTypeInfo>>(); 


	/**
	 * 获取wmts的值
	 */
	@Override
	public byte[] getWmtsTileInfo(TileRequestInfo tileInfo,String reqIP) {
//		//设置全局变量
//		System.setProperty("USER_VISIT_CACHE_DIR", tileInfo.getSystemCachePath());
		// 检查存储介质
		return inspectLocalStorInfo(tileInfo);
	}

	private String createTileIndex(TileRequestInfo tileInfo) {
		StringBuilder sb = new StringBuilder(tileInfo.getLayerName());
		sb.append("_").append(tileInfo.getGridSet().split(":")[1]).append("_").append(SeedTaskUtil.simpleMineType(tileInfo.getMineType())).append("_").append(tileInfo.getTileWidth());
		return sb.toString();
	}
	
	public byte[] inspectLocalStorInfo(TileRequestInfo tileInfo) {
		DataSourceContextHolder.setDataSource("default");
		List<LayerStoreTypeInfo> storeTypes = null;
		String layerIndex = createTileIndex(tileInfo);
		if (layerStoreTypes.containsKey(layerIndex)) {
			storeTypes = layerStoreTypes.get(layerIndex);
		}
		if (storeTypes == null || storeTypes.isEmpty()){
			String mineType = tileInfo.getMineType();
			tileInfo.setMineType(SeedTaskUtil.simpleMineType(tileInfo.getMineType()));
			storeTypes = commonMapper.queryLayerStoreByReq(tileInfo);
			tileInfo.setMineType(mineType);
			layerStoreTypes.put(layerIndex, storeTypes);
		}
		byte[] buf = null;
		if (storeTypes != null && !storeTypes.isEmpty()) {
			for (LayerStoreTypeInfo layerStoreTypeInfo : storeTypes) {
				int storeTypeId = layerStoreTypeInfo.getStoreTypeId();
				String suffixSourName = seedTaskUtil.getDbName(tileInfo, storeTypeId);
				suffixSourName = suffixSourName.replace(tileInfo.getLayerName(), "");
				String sourceName = layerStoreTypeInfo.getDataSource();
				if (!sourceName.endsWith(suffixSourName)) {
					continue;
				}
				String tableName = null;
//				if (StoreType.getStoreType(storeTypeId).equals(StoreType.POSTGRRSQL)) {
//					tableName = seedTaskUtil.createTableName(tileInfo.getZoomLevel(), tileInfo.getTileRow(),
//							tileInfo.getTileCol());
//					// 如果当前数据库下有这个表，在处理，没有则不处理
//					List<String> tableNames = seedTaskUtil.getTblNames(sourceName);
//					if (tableNames == null || !tableNames.contains(tableName)) {
//						continue;
//					}
//				}
				int[] tileIndex = { tileInfo.getTileCol(), tileInfo.getTileRow(), tileInfo.getZoomLevel() };
				buf = seedTaskUtil.getResourceFromDB(sourceName, tableName, storeTypeId, layerStoreTypeInfo.getStorePath(),
						layerStoreTypeInfo.getLayerName(), layerStoreTypeInfo.getTileFormat(), tileIndex,tileInfo.getGridSet(),tileInfo.getTileWidth());
				if (buf != null) {
					break;
				}
			}
		}
		return buf;
	}

	@Override
	public void cleanStoreTypeCache() {
		layerStoreTypes.clear();
	}

}
