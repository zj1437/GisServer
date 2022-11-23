package org.pygisserver.render.tiled;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.service.MapServerDataService;
import org.pygisserver.common.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/**
 * @Description: 瓦片服务的策略类 
 * @author: 刘洋 
 * @date: 2020-08-24
 */
@Component
@Scope("prototype")
public class TiledInfoStrategy {
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;
	
	@Autowired
	private MapServerDataService mapServerDataService;
	
	private TiledInfoRead tiledInfo;
	
	public BufferedImage getWmsImg() {
		return (BufferedImage) tiledInfo.getWmsImg();
	}
	
	public byte[] getWmsResource() {
		return tiledInfo.getWmsResource();
	}
	
	public byte[] getWmtsResource() {
		return tiledInfo.getWmtsResource();
	}
	
	public byte[] getTmsResource() {
		return tiledInfo.getWmtsResource();
	}
	
	public void setTiledInfo(TileRequestInfo requestInfo,LayerBaseInfo baseInfo) {
		//获取瓦片基本信息
		switch (baseInfo.getFileType()) {
		case "FlatGis":
			tiledInfo = new FlatGisReadData(requestInfo,baseInfo);
			break;
		case "TMS":
			tiledInfo = new TMSReadData(requestInfo,baseInfo);
			break;
		case "TDB":
			tiledInfo = new TDBReadData(requestInfo,baseInfo);
			break;
		case "google_raster":
		case "google_combine":
		case "google_map":
		case "gaode_raster":
		case "gaode_map":
		case "mapbox":
		case "osm":
		case "tianditu":
			tiledInfo = new OnlineTileData(requestInfo,baseInfo);
			break;
		case "MBTiles":
			try {
				tiledInfo = new MBtilesReadData(requestInfo,baseInfo);
			} catch (SqlException e) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.formatInfo(ExceptionsUtil.SQLITE_DB_INIT_FAIL,requestInfo.getLayerName()), 
						"System","Sqlite服务", ExceptionsUtil.getServerIp(null)));
				throw new SqlException(e.getErrorCode(),e.getErrorMsg());
			}
			
			break;
		default:
			break;
		}
	}
	
}
