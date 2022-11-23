package org.pygisserver.common.mapper;

import java.util.List;
import java.util.Map;

import org.pygisserver.common.entity.*;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.entity.*;
import org.springframework.stereotype.Repository;

/**
 * 地图服务Dao
 * 
 * @author 
 *
 */
@Repository
public interface MapServerDataMapper {

	/**
	 * 获取当前服务和版本下的图层信息
	 * @param serverType
	 * @param version
	 * @return
	 */
	List<String> queryLayerNames(Map<String,String> param);
	
	/**
	 * 根据协议名称和版本号查询对应的协议信息
	 * @param serverType
	 * @param version
	 * @return
	 */
	ServerProtocolInfo queryServerProtocol(Map<String,String> param);
	
	
	/**
	 * 获取服务提供商
	 * @return
	 */
	ServerContact queryServerContact();
	
	
	/**
	 * 
	 * @param layerName
	 * @return
	 */
	List<LayerGroup> queryLayerGroupInfo(String layerName);
	
	/**
     * 获取图层状态信息
     * @param layerName 
     * @return
     */
	Integer queryLayerStatus(String layerName);
	
	/**
	 * 获取金字塔信息数据
	 * @param layerNames
	 * @return
	 */
	List<MapPyramidInfo> queryMapPyramidInfo(String layerName);
	
	/**
	 * 
	 * @param styleName
	 * @return
	 */
	PyStyleInfo queryStyleInfoByName(String styleName);
	
	
	/**
	 * 获取图层信息
	 * @param layerName
	 * @return
	 */
	LayerBaseInfo queryLayerInfo(String layerName);
	
	List<LayerGroup> queryLayerGroupsByServerName(String serverName);
	
	String getSeaChartScaleByServerName(String serverName);
	
	/**
	 * 获取epsg的相关信息
	 * @return
	 */
//	List<EpsgInfo> queryEpsgInfo();

	List<LayerBaseInfo> queryLayerInfos(Map<String,String> param);


	List<LayerGroup> queryLayerGroupInfoTile(String layerName);

	int queryServerDataType(String serverName);
	
	int isLayerGroup(String serverName);

	//获取图层的样式信息
	String queryLayerStyleName(String layerName);

	PostGisConnectEntity queryPostGisBySourceName(String dataSourceName);
}
