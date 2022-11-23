package org.pygisserver.tileserver.service;

import org.pygisserver.common.pytc.entity.TileRequestInfo;

public interface TileCacheService {

	
	void cleanStoreTypeCache();
	
	/**
	 * 
	 * @param tileInfo 请求参数信息
	 * @return
	 */
	byte[] getWmtsTileInfo(TileRequestInfo tileInfo,String reqIP);

}
