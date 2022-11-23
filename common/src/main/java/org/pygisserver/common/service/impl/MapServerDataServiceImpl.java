package org.pygisserver.common.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.entity.EpsgInfo;
import org.pygisserver.common.entity.PyStyleInfo;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ServerContact;
import org.pygisserver.common.entity.ServerProtocolInfo;
import org.pygisserver.common.mapper.MapServerDataMapper;
import org.pygisserver.common.service.MapServerDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 地图服务service实现类
 * 
 * @author 
 * @since 2018-08-17
 */
@Service
@Slf4j
public class MapServerDataServiceImpl implements MapServerDataService{

	@Resource
	private MapServerDataMapper mapServerDataMapper;
	
	@Autowired
	private RedisUtils redisUtils;
	
	private static List<EpsgInfo> EPSG_INFO_CACHE = new ArrayList<EpsgInfo>();
	
	@Override
	public List<String> queryServerEntity(String serviceName, String version,String token) {
		List<String> layerNames = null;
		Map<String,String> param = new HashMap<String,String>();
		try {
			//先根据协议名称和版本号查询协议信息，如果返回为null，则说明请求参数有误，返回空，如果不为null，则根据协议名称查询服务名称
			param.put("serviceName", serviceName);
			param.put("version", version);
			ServerProtocolInfo protocol = mapServerDataMapper.queryServerProtocol(param);
			if(protocol == null) return layerNames;
			
			param.clear();
			param.put("serviceName", serviceName);
			if (StringUtils.isNotBlank(token)) {
				param.put("token", token);
			}
			layerNames = mapServerDataMapper.queryLayerNames(param);
			return layerNames;
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl queryServerEntity has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, serviceName + "服务详细"));
		}
	}

	@Override
	public ServerContact getServerContact() {
		try {
			return mapServerDataMapper.queryServerContact();
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl getServerContact has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "服务提供商"));
		}
	}

	@Override
	public List<LayerGroup> queryLayerGroup(String layerName) {
		try {
			return mapServerDataMapper.queryLayerGroupInfo(layerName);
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl queryLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, layerName + "图层组"));
		}
	}

	@Override
	public int queryLayerStatus(String layerName) {
		try {
			return mapServerDataMapper.queryLayerStatus(layerName);
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl queryLayerStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, layerName + "图层状态"));
		}
	}

	@Override
	public PyStyleInfo queryStyleInfo(String styleName) {
		try {
			return mapServerDataMapper.queryStyleInfoByName(styleName);
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl queryStyleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, styleName + "样式"));
		}
	}

	@Override
	public LayerBaseInfo queryLayerBaseInfo(String layerName) {
		try {
			return mapServerDataMapper.queryLayerInfo(layerName);
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl queryLayerBaseInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, layerName + "图层"));
		}
	}

	@Override
	public List<LayerGroup> getLayerGroups(String serverName) {
		List<LayerGroup> layerGroup = (List<LayerGroup>) redisUtils.getValue(serverName+"-group");
		if (layerGroup != null && layerGroup.size() != 0) {
			return layerGroup;
		}
		layerGroup = mapServerDataMapper.queryLayerGroupsByServerName(serverName);
		return layerGroup;
	}

	@Override
	public List<EpsgInfo> queryEpsgInfo() {
		if (EPSG_INFO_CACHE == null || EPSG_INFO_CACHE.isEmpty()) {
//			EPSG_INFO_CACHE = mapServerDataMapper.queryEpsgInfo();
		}
		return EPSG_INFO_CACHE;
	}

	@Override
	public List<LayerBaseInfo> queryServerInfo(String serviceName,String version,String token) {
		List<LayerBaseInfo> LayerBaseInfos = null;
		Map<String,String> param = new HashMap<String,String>();
		try {
			//先根据协议名称和版本号查询协议信息，如果返回为null，则说明请求参数有误，返回空，如果不为null，则根据协议名称查询服务名称
			param.put("serviceName", serviceName);
			param.put("version", version);
			ServerProtocolInfo protocol = mapServerDataMapper.queryServerProtocol(param);
			if(protocol == null) {
				return LayerBaseInfos;
			}
			param.clear();
			param.put("serviceName", serviceName);
			if (StringUtils.isNotBlank(token)) {
				param.put("token", token);
			}
			LayerBaseInfos = mapServerDataMapper.queryLayerInfos(param);
			return LayerBaseInfos;
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl queryServerEntity has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, serviceName + "服务详细"));
		}
	}

	@Override
	public List<LayerGroup> queryLayerGroupTile(String layerName) {
		try {
			List<LayerGroup> layerGroups = mapServerDataMapper.queryLayerGroupInfoTile(layerName);
			layerGroups.stream().forEach(doc ->{
				doc.setDataType(mapServerDataMapper.queryServerDataType(doc.getLayerName()));
			});
			return layerGroups;
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl queryLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, layerName + "图层组"));
		}
	}

	@Override
	public boolean isLayerGroup(String name) {
		String serverName = "";
		if(name.contains("localhost")) {
			serverName = name.substring(12, name.length() - 1);
		}else if (name.contains("[") && name.contains("]")){
			serverName = name.substring(1, name.length() - 1);
		}else {
			serverName = name;
		}
		try {
			int isLayerGroup = mapServerDataMapper.isLayerGroup(serverName);
			return isLayerGroup != 0 ? true : false;
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl isLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET));
		}
		
	}
}
