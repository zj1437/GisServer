package org.pygisserver.render.mapper;

import org.apache.dubbo.config.annotation.DubboService;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.springframework.stereotype.Repository;

import java.util.List;

@DubboService(version = "3.0.0")
public interface RenderMapperTest {


	/**
	 * 根据服务名称获取图层组信息
	 * @param serverId
	 * @return
	 */
	List<LayerGroup> queryLayerGroupsByServerName(String serverName);
	
	/**
	 * 获取系统配置信息
	 * @param serverId
	 * @return
	 */
	String querySysStylePath();
}
