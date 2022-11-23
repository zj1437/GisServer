package org.pygisserver.web.service;

import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.web.entity.DataServerEntity;

import java.util.List;
import java.util.Map;

/**
 * @Description: 图层组服务service
 * @author: 张亨特
 * @date: 2022-02-22
 */
public interface LayerGroupServerService {
    /**
     * 发布图层组信息
     *
     * @param layerGroupInfos
     * @param reqIP
     */
    void publishedLayerGroup(DataServerEntity layerGroupInfos, String reqIP);

    /**
     * 发布对外的图层组数据
     *
     * @param layerGroup
     * @param reqIP
     */
    void publishedLayerGroup(List<LayerGroup> layerGroup, String reqIP);


    /**
     * 编辑图层组信息
     *
     * @param layerGroupInfos
     * @param reqIP
     */
    void updateLayerGroup(DataServerEntity layerGroupInfos,String reqIP);

    /**
     * 构造图层组信息
     *
     * @param layerName
     * @param list
     * @param serverId
     * @param layerGroupInfos
     * @param reqIP
     */
    void createLayerGroup(String layerName, List<LayerGroup> list, int serverId, DataServerEntity layerGroupInfos, String reqIP
    );

    /**
     * 查询图形组名称
     *
     * @param layerNames
     * @return map
     */
    Map<String,List<String>> queryLayerGroupNameByLayerInfo(List<String> layerNames);
}
