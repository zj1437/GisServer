package org.pygisserver.render.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.springframework.stereotype.Repository;

@Repository(value="renderMapper")
public interface RenderMapper {

	/**
	 * 查询服务基础信息
	 * @param layerName
	 * @return
	 */
	List<LayerBaseInfo> queryLayerBaseInfo(RequestQueryCondition layerName);
	
	/**
	 * 根据服务名称获取图层组信息
	 * @param serverName
	 * @return
	 */
	List<LayerGroup> queryLayerGroupsByServerName(String serverName);
	
	/**
	 * 获取系统配置信息
	 * @param
	 * @return
	 */
	String querySysStylePath();
}
