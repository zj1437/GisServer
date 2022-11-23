package org.pygisserver.render.util;

import java.util.List;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.render.dao.RenderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class LayerBaseInfoUtil {

	@Autowired
	private RenderMapper renderMapper;
	
	@Autowired
	private RedisUtils redisUtils;
	
	/**
	 * 获取图层的基础信息
	 * @return
	 */
	public LayerBaseInfo queryServerBaseInfo(String layerName) {
		//查询数据库，并缓存，只缓存到本地，不处理redis，redis的数据有mapserver负责更新、
		RequestQueryCondition condition = new RequestQueryCondition();
		condition.setServerName(layerName);
		List<LayerBaseInfo> layerBaseInfo = renderMapper.queryLayerBaseInfo(condition);

		LayerBaseInfo baseInfo = (LayerBaseInfo) redisUtils.getValue(layerName);
		if (baseInfo != null) {
			return baseInfo;
		}
		//查询数据库，并缓存，只缓存到本地，不处理redis，redis的数据有mapserver负责更新、
//		RequestQueryCondition condition = new RequestQueryCondition();
//		condition.setServerName(layerName);
//		List<LayerBaseInfo> layerBaseInfo = renderMapper.queryLayerBaseInfo(condition);
		if (layerBaseInfo != null && !layerBaseInfo.isEmpty()) {
			return layerBaseInfo.get(0);
		}
		return null;
	}
	
	/**
	 * 获取图层的基础信息
	 * @return
	 */
	public List<LayerGroup> getLayerGroups(String serverName) {
		List<LayerGroup> layerGroup = (List<LayerGroup>) redisUtils.getValue(serverName+"-group");
		if (layerGroup != null && layerGroup.size() != 0) {
			return layerGroup;
		}
		renderMapper.querySysStylePath();
		layerGroup = renderMapper.queryLayerGroupsByServerName(serverName);
		return layerGroup;
	}


	/**
	 * 获取图层的基础信息
	 * @return
	 */
	public List<LayerGroup> getLayerGroups2(String serverName) {
		Object value = redisUtils.getValue(serverName);
		List<LayerGroup> layerGroup = null;
		if (value instanceof List){
			layerGroup = (List<LayerGroup>) value;
			if (layerGroup != null && layerGroup.size() != 0) {
				return layerGroup;
			}
		} else {
			layerGroup = renderMapper.queryLayerGroupsByServerName(serverName);
		}
		return layerGroup;
	}


}
