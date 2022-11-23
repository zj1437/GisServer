package org.pygisserver.common.service;

import java.util.List;

import org.pygisserver.common.entity.EpsgInfo;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PyStyleInfo;
import org.pygisserver.common.entity.ServerContact;
import org.pygisserver.common.ui.entity.LayerGroup;

/**
 * @Description: 地图服务中间模块数据处理 
 * @author: 
 * @date: 2018-12-26
 */

public interface MapServerDataService 
{
    List<String> queryServerEntity(String serviceName,String version,String token);
    
    ServerContact getServerContact();

    /**
     * 获取图层组信息
     * @param layerName 
     * @return
     */
    List<LayerGroup> queryLayerGroup(String layerName);
    
    /**
     * 获取图层状态信息
     * @param layerName 
     * @return
     */
    int queryLayerStatus(String layerName);
    
    /**
     * 
     * @param styleName
     * @return
     */
    PyStyleInfo queryStyleInfo(String styleName);
    
    LayerBaseInfo queryLayerBaseInfo(String layerName);
    
    List<LayerGroup> getLayerGroups(String serverName);
    
    List<EpsgInfo> queryEpsgInfo();

    List<LayerBaseInfo> queryServerInfo(String serviceName,String version,String token);

    List<LayerGroup> queryLayerGroupTile(String layerName);
    
    boolean isLayerGroup(String name);
    
}
