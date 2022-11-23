package org.pygisserver.web.service;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ServerProtocolInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.BboxInfoEntity;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.MapServerBatchInfoEntity;
import org.geoserver.catalog.LayerInfo;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Description: 地图服务service
 * @author: 
 * @date: 2018-10-29
 */
public interface MapServerService {

	boolean isPublished(String layer);

	boolean ispublisedRest(String layer);

	void singlePublishServerData(DataServerEntity dataServer,String reqIP);

	void singlePublishRestData(DataServerEntity dataServer,String reqIP);

	/**
	 * 批量发布数据
	 * @param infoEntity 服务数据
	 */
	String batchPublishServerData(MapServerBatchInfoEntity infoEntity, String reqIP);

	/**
	 * 批量发布rest数据
	 * @param infoEntity 服务数据
	 */
	String batchPublishRestData(MapServerBatchInfoEntity infoEntity, String reqIP);

	/**
	 * 查询服务数据总量
	 * @return 返回服务数据列表
	 */
	List<DataServerEntity> queryServerDataList(RequestQueryCondition queryCondition,String reqIP);

	/**
	 * 查询rest
	 * 服务数据总量
	 * @param queryCondition
	 * @param reqIP
	 * @return
	 */
	List<DataServerEntity> queryRestDataList(RequestQueryCondition queryCondition,String reqIP);

	/**
	 * 根据服务id获取当前的服务信息
	 * @param serverId
	 * @return
	 */
	DataServerEntity queryServerInfoById(int serverId);

	/**
	 * 根据服务id获取当前的rest服务信息
	 * @param serverId
	 * @return
	 */
	DataServerEntity queryRestInfoById(int serverId);

	/**
	 * 查询服务数据总量
	 * @return
	 */
	int queryServerDataCount(RequestQueryCondition  queryCondition);

	/**
	 * 查询rest服务总量
	 * @param queryCondition
	 * @return
	 */
	int queryRestDataCount(RequestQueryCondition  queryCondition);

	/**
	 * 获取当前数据的BBOX
	 * @param file
	 * @return
	 */
	BboxInfoEntity queryBboxFromFile(String file,String fileType,DataServerEntity serverEntity, String reqIP);
	
	
	/**
	 * 编辑服务数据
	 * @param dataServer
	 */
	void editServerData(DataServerEntity dataServer,String reqIP);

	/**
	 * 编辑rest服务数据
	 * @param dataServer
	 */
	void editRestData(DataServerEntity dataServer,String reqIP);
	
	/**
	 * **
	 * 删除已经发布的服务
	 *  @param serverId
	 */
	void deleteServerData(List<Integer> ids,String reqIP);


	/**
	 * 删除已经发布的rest服务
	 * @param ids
	 * @param reqIP
	 */
	void deleteRestData(List<Integer> ids,String reqIP);

	/**
	 * 暂停当前服务
	 * @param serverId 服务id
	 */
	void updateServerStatus(int serverId,String reqIP);

	/**
	 * 暂停当前rest服务
	 * @param serverId
	 * @param reqIP
	 */
	void updateRestStatus(int serverId,String reqIP);

	/**
	 * 获取系统版本信息
	 * @return 返回版本信息
	 */
	List<ServerProtocolInfo> queryServerProtocolInfo();
	
	/**
	 * 修改本地缓存信息
	 * @param layerInfo 图层信息
	 * @param flag 是否新增标识
	 */
	void modifyCacheInfo(LayerInfo layerInfo,boolean flag);
	
	/**
	 * 获取级别
	 * @param layer
	 * @return
	 */
	int queryZoomLevel(String layer);
	
	/**
	 * 获取token值
	 * @param token
	 * @return
	 */
	String getLayerNameByToken(String token);
	
	/**
	 * 根据服务ids获取服务状态
	 * @param ids
	 * @return
	 */
	List<Integer> getServerStatusByIds(List<Integer> ids);
	
	/**
	 * 查询当前系统下的服务名称
	 * @return
	 */
	Map<String,Date> queryServerName(List<String> serverName);
	
	/**
	 * 根据文件路径获取服务的id
	 * @param fileUrl
	 * @return
	 */
	int getServerIdByServerUrl(String fileUrl);
	
	/**
	 * 计算当前级别
	 * @param defaultLevel
	 * @param tileWidth
	 * @param srs 投影格式
	 * @return
	 */
	int calcuCurrentLevel(int defaultLevel,int tileWidth,String srs);
	
	LayerBaseInfo getLayerBaseInfo(String layerName,String reqIP);
	
	void dealThumbal(String reqIP,DataServerEntity...dataServer);
	
	List<String> getLayerNameByDataType(int dataType);

	void stopOverView(int serverId, String reqIP);

	Map<String, Object> queryPreviewParameter(String resourceSrs, int maxlevel);

	Map<String, Object> queryOgcProtocolInfo(String reqIp, String protocol, String serverName);
}
