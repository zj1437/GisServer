package org.pygisserver.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.enumm.ServerStatusEnum;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.StatusException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.service.LayerGroupServerService;
import org.pygisserver.web.wmsprotocol.entity.WmsRequestParam;
import org.pygisserver.web.wmsprotocol.service.WmsProtocolService;
import org.pygisserver.tileserver.service.RenderTileService;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.FeatureInfo;
import org.pygisserver.web.entity.GuassCoordinates;
import org.pygisserver.web.entity.MapServerBatchInfoEntity;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.service.StyleInfoService;
import org.pygisserver.web.utils.MapServerUtils;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.TiledInfoImpl;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @Description: 图层组服务service实现类
 * @author: 张亨特
 * @date: 2022-02-22
 */
@Service
@Slf4j
public class LayerGroupServerServiceImpl implements LayerGroupServerService {
	@Resource
	private MapServerMapper mapServer;

	@DubboReference(version = "3.0.0")
	private RenderTileService renderTileService;

	@Autowired
	private WmsProtocolService wmsProtocolService;

	@Autowired
	private ExceptionsUtil exceptionsUtil;

	@Autowired
	private SystemLogService systemLogService;

	@Autowired
	private MapServerServiceImpl mapServerService;

	@Autowired
	private RedisUtils redisUtils;

	@Resource
	private MapStatsticService statsticService;

	@Autowired
	private StyleInfoService styleInfoService;

	@Autowired
	private GuassCoordinates guassCoordinates;

	/**
	 * 注入wmsServiceTarget
	 */
//	@Autowired
//	private DefaultWebMapService webMapService;

	@Autowired
	private WebSocketManager webSocket;

	private boolean isSameCoordinatesGroup;

	@Override
	@Transactional
	public void publishedLayerGroup(DataServerEntity layerGroupInfos, String reqIP) {
		String oldLayerName = null;
		boolean isVectorGroup = true;
		// 服务名称长度不能超过20，过长需要截取
		String tempSerName = layerGroupInfos.getServerName();
		if (tempSerName.length() > 20) {
			tempSerName = tempSerName.substring(0, 20);
		}
		layerGroupInfos.setServerName(tempSerName);
		layerGroupInfos.setLayerName(tempSerName);
		if (layerGroupInfos != null) {
			//编组图层全为矢量则支持wfs
//			List<LayerGroup> layerGroups = layerGroupInfos.getLayerGroups();
//			RequestQueryCondition queryCondition = new RequestQueryCondition();
//			for (LayerGroup layerGroup : layerGroups) {
//				queryCondition.setServerName(layerGroup.getLayerName());
//				String dataUrl = mapServer.queryServerData(queryCondition).get(0).getDataUrl();
//				String type = dataUrl.substring(dataUrl.lastIndexOf(".") + 1);
//				if(!"shp".equals(type)) {
//					isVectorGroup = false;
//				}
//			}
//			if(isVectorGroup) {
//				layerGroupInfos.setPublisedProctol("WMTS;WMS;TMS;WFS");
//			}
			// 编辑已有图层组
			if (layerGroupInfos.getServerId() != 0) {
				List<String> layerNames = mapServer
						.queryLayerNamesByServerIds(Arrays.asList(layerGroupInfos.getServerId()));
				oldLayerName = layerNames.get(0);
				LayerGroupInfo oldGroup = mapServerService.getCataLogData().getLayerGroupByName(oldLayerName);
				DataServerEntity olGroupServerEntity = mapServer.queryServerDataByIds(Arrays.asList(layerGroupInfos.getServerId())).get(0);
				mapServerService.getCacheLayerList().remove(oldLayerName);
				if (oldGroup != null) {
					mapServerService.getCataLogData().remove(oldGroup);
				}
				// 如果是修改样式，或者是新增图层/删除图层，则需要将之前的缓存删除掉
				List<LayerGroup> oldLayerGroup = mapServer.queryLayerGroupByServerId(layerGroupInfos.getServerId());
				List<LayerGroup> newLayerGroup = layerGroupInfos.getLayerGroups();
				boolean flag = false;
				if (oldLayerGroup.size() != newLayerGroup.size()) {
					flag = true;
				} else {
					for (int i = 0; i < oldLayerGroup.size(); i++) {
						if (newLayerGroup.get(i).getLayerName().equals(oldLayerGroup.get(i).getLayerName())) {
							if (!newLayerGroup.get(i).getStyleName().equals(oldLayerGroup.get(i).getStyleName())) {
								flag = true;
								break;
							}
							if (newLayerGroup.get(i).getStyleName().equals(oldLayerGroup.get(i).getStyleName())
									&& (newLayerGroup.get(i).getMinLevel() != oldLayerGroup.get(i).getMinLevel()
											|| newLayerGroup.get(i).getMaxlevel() != oldLayerGroup.get(i)
													.getMaxlevel())) {
								flag = true;
								break;
							}
						} else {
							flag = true;
							break;
						}
					}
				}
				if (flag) {
					try {
						renderTileService.flushTileTask(oldLayerName, false, true);
						// 清空之前的样式缓存
						mapServerService.delTempFile(oldLayerName);
						// 原内存缓存代码，暂时屏蔽
//						clearCacheTiles(oldLayerName,reqIP);
					} catch (Exception e) {
						if (ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
							systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(
									ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil
											.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR, oldLayerName + "服务缓存清理"),
									UserUtil.getUserName(), "Pytc服务", reqIP));
						}
						log.error("MapServerServiceImpl updateServerStatus has error:"
								+ ExceptionsUtil.stackTraceToStr(e));
						throw new StatusException(ExceptionsUtil.ERROR_500,
								ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC, oldLayerName + "服务"));
					}
				}
				// 如果修改图层组名称，则修改原有图层组对应的map_server_info表字段，删除缓存信息，删除缩略图
				if (!oldLayerName.equals(layerGroupInfos.getLayerName()) || olGroupServerEntity.getLayerGroupFlag() != layerGroupInfos.getLayerGroupFlag()) {
					layerGroupInfos.setUpdateTime(new Date());
					layerGroupInfos.setStatus(ServerStatusEnum.PUBLISHED.getCode());
					mapServer.editServerData(layerGroupInfos);
					// 清除之前图层组的状态缓存
					redisUtils.deleteKey(reqIP, oldLayerName);
				}
				File file = new File(new StringBuilder(MapServerUtils.getThumpath()).append(File.separator)
						.append(oldLayerName).append(".png").toString());
				if (file.exists()) {
					file.delete();
				}
				mapServer.deleteLayerGroupData(Arrays.asList(layerGroupInfos.getServerId()));
				layerGroupInfos.setUpdateTime(new Date());
				layerGroupInfos.setStatus(ServerStatusEnum.PUBLISHED.getCode());
				mapServer.editServerData(layerGroupInfos);
				statsticService.updateRequestServerName(layerGroupInfos.getServerId(), layerGroupInfos.getServerName());
				statsticService.updateVisitServerName(layerGroupInfos.getServerId(), layerGroupInfos.getServerName());
			} else {
				mapServerService.saveMapServerData(layerGroupInfos);
			}
		}
		// 保存图层组对应的图层信息
		List<LayerGroup> list = layerGroupInfos.getLayerGroups();
		GetMapRequest getMapRequest = null;
		if (list.size() > 0) {
			createLayerGroup(layerGroupInfos.getLayerName(), list, layerGroupInfos.getServerId(), layerGroupInfos,
					reqIP);
			if (layerGroupInfos.getOpenTiled() == 1) {
				//处理分布式事务，异步下发切图任务
				CompletableFuture.runAsync(() ->{
					renderTileService.seedTaskByLayerName(layerGroupInfos.getLayerName(), layerGroupInfos.getServerId(),
							reqIP, 0);
				});
			}
			mapServer.saveLayerGroups(list);
			// 调用wms服务，获取渲染的对应的数据
			getMapRequest = constructGetMapRequest(layerGroupInfos, "image/png", 210, 140);
			final GetMapRequest request = getMapRequest;
			mapServerService.getCacheLayerList().add(layerGroupInfos.getServerName());
			// 发不完之后，删除缓存中的数据
//			redisUtils.deleteKey(reqIP, layerGroupInfos.getLayerName() + "_group");
			final String groupName = tempSerName;
			new Thread(() -> {
				StringBuilder thumPath = new StringBuilder(MapServerUtils.getThumpath())
						.append(File.separator)
						.append(layerGroupInfos.getServerName())
						.append(".png");
				WmsRequestParam wmsRequestParam = new WmsRequestParam();
				wmsRequestParam.setLayerName(layerGroupInfos.getLayerName());
				wmsRequestParam.setSrs(layerGroupInfos.getFeatureInfo().getResourceSrs());
				wmsRequestParam.setBbox(layerGroupInfos.getFeatureInfo().getResourceBbox().replaceAll(";", ","));
				wmsRequestParam.setFormat("image/png");
				wmsRequestParam.setWidth(210);
				wmsRequestParam.setHeight(140);
				wmsRequestParam.setTransparent(true);
				byte[] wmsImageByte = wmsProtocolService.getWmsImageByte(wmsRequestParam, reqIP);
				JSONObject jsonObject = new JSONObject();
				try(BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(thumPath.toString()))) {
					outputStream.write(wmsImageByte);
					jsonObject.put("msg", MessageFormat.format("图层组{0}缩略图生成成功！", groupName));
					jsonObject.put("msgKey", "mapServer");
					webSocket.sendMessage(jsonObject.toJSONString());
				} catch (IOException e) {
					jsonObject.put("msg", MessageFormat.format("图层组{0}缩略图生成失败！", groupName));
					jsonObject.put("msgKey", "mapServer");
					webSocket.sendMessage(jsonObject.toJSONString());
					log.error(
							"MapServerServiceImpl publishedLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new StatusException(ExceptionsUtil.ERROR_500,
							ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_SEND_MESSAGE, "Socket"));
				}
			}).start();
		}
	}

	@Override
	public void createLayerGroup(String layerName, List<LayerGroup> list, int serverId,
			DataServerEntity layerGroupInfos, String reqIP) {
		if (list == null || list.isEmpty()) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,
					ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, "图层组图层信息"));
		}
		// 构造原有的layerGroupInfo接口
		LayerGroupInfoImpl layerGroupInfo = new LayerGroupInfoImpl();
		List<PublishedInfo> publishables = new ArrayList<>();
		List<org.geoserver.catalog.StyleInfo> styleInfos = new ArrayList<>();
		ReferencedEnvelope tempEnvelope = null;
		ReferencedEnvelope tempGuassEnvelope = null;
		String guassSrs = null;
		for (LayerGroup layerGroup : list) {
			LayerBaseInfo baseInfo = mapServerService.getLayerBaseInfo(layerGroup.getLayerName(), reqIP);
			String srs = baseInfo.getResourceSrs();
			if (guassCoordinates.getGuassCoordinates().contains(srs)) {
				if (guassSrs == null) {
					guassSrs = srs;
					isSameCoordinatesGroup = true;
				}
				if (!(guassSrs.equals(srs))) {
					isSameCoordinatesGroup = false;
					break;
				}
			} else {
				isSameCoordinatesGroup = false;
				break;
			}
		}
		try {
			if (isSameCoordinatesGroup) {
				tempGuassEnvelope = new ReferencedEnvelope(CRS.decode(guassSrs, true));
			}
			tempEnvelope = new ReferencedEnvelope(CRS.decode("EPSG:4326", true));

		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("MapServerServiceImpl createLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,
					ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "图层组图层信息"));
		}
		final ReferencedEnvelope envelope = tempEnvelope;
		final ReferencedEnvelope guassEnvelope = tempGuassEnvelope;
		list.stream().forEach((layerGroup) -> {
			layerGroup.setLayerGroupId(serverId);
			LayerInfo layerInfo = mapServerService.getCataLogData().getLayerByName(layerGroup.getLayerName());
			LayerBaseInfo layerBaseInfo = mapServerService.getLayerBaseInfo(layerGroup.getLayerName(), "");
			ResourceInfo resourceInfo = null;
			if (layerInfo != null) {
				resourceInfo = layerInfo.getResource();
			} else {
				layerInfo = new LayerInfoImpl();
				layerInfo.setType(PublishedType.ONLINE);
				TiledInfoImpl tiledInfo = new TiledInfoImpl();
				tiledInfo.setName(layerGroup.getLayerName());
				layerInfo.setResource(tiledInfo);
				layerInfo.setDefaultStyle(mapServerService.getCataLogData().getStyleByName("栅格"));
			}
			publishables.add(layerInfo);
			ReferencedEnvelope envelope1 = null;
			ReferencedEnvelope guassEnvelope1 = null;
			if (resourceInfo != null) {
				String srs = resourceInfo.getSRS();
				if (StringUtils.isBlank(srs)) {
					try {
						srs = mapServerService.getSrs(layerInfo.getResource().getNativeCRS());
					} catch (FactoryException e) {
						e.printStackTrace();
					}
				}
				envelope1 = (ReferencedEnvelope) GeometryUtil.transformCoordinateSystem(resourceInfo.getNativeBoundingBox(), "EPSG:4326");
				if (isSameCoordinatesGroup) {
					guassEnvelope1 = (ReferencedEnvelope) convertBoundingBox(layerBaseInfo.getResourceBbox(), srs);
				}
			} else {
				try {
					envelope1 = new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326", true));
				} catch (MismatchedDimensionException | FactoryException e) {
					log.error("MapServerServiceImpl createLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new ResourceException(ExceptionsUtil.ERROR_500,
							ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "图层组图层信息"));
				}
			}
			envelope.expandToInclude(envelope1);
			if(isSameCoordinatesGroup) {
				guassEnvelope.expandToInclude(guassEnvelope1);
			}
			styleInfos.add(mapServerService.getCataLogData().getStyleByName(layerGroup.getStyleName()));
		});
		// 数据库存储
		// 存储到catalog中
		layerGroupInfo.setName(layerName);
		// bbox求多个图层的并集
		layerGroupInfo.setBounds(envelope);
		layerGroupInfo.setLayers(publishables);
		layerGroupInfo.setStyles(styleInfos);
		// 保存的时候在持久化
		if (serverId != 0) {
			FeatureInfo featureInfo = new FeatureInfo();
			featureInfo.setServerId(serverId);
			StringBuilder sb = new StringBuilder();
			sb.append(envelope.getMinX()).append(";").append(envelope.getMinY()).append(";").append(envelope.getMaxX())
					.append(";").append(envelope.getMaxY());
			featureInfo.setNativeLatLon(sb.toString());
			if(isSameCoordinatesGroup) {
				StringBuilder sBuilder = new StringBuilder();
				sBuilder.append(guassEnvelope.getMinX()).append(";").append(guassEnvelope.getMinY()).append(";").append(guassEnvelope.getMaxX())
						.append(";").append(guassEnvelope.getMaxY());
				featureInfo.setResourceSrs(guassSrs);
				featureInfo.setResourceBbox(sBuilder.toString());
				try {
					featureInfo.setResourceCrsWkt(CRS.decode(guassSrs).toWKT());
				} catch (FactoryException e1) {
					log.error("MapServerServiceImpl constructGetMapRequest has error:" + ExceptionsUtil.stackTraceToStr(e1));
					throw new ResourceException(ExceptionsUtil.ERROR_500,
							ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, layerName + "图层坐标系"));
				}
			}else {
				featureInfo.setResourceBbox(sb.toString());
				featureInfo.setResourceSrs("EPSG:4326");
				featureInfo.setResourceCrsWkt(Constant.WGS_84_WKT);
			}
			featureInfo.setNativeSrs("EPSG:4326");
			featureInfo.setZoomLevel(getMaxLevel(list));
			featureInfo.setProjectionPolicy("REPROJECT_TO_DECLARED");
			if (layerGroupInfos != null) {
				layerGroupInfos.setFeatureInfo(featureInfo);
			}
			mapServer.deleteLayerGroupFeature(serverId);
			mapServer.saveFeatureData(featureInfo);
			try {
				mapServerService.getCataLogData().add(layerGroupInfo);
			} catch (Exception e) {
				mapServerService.getCataLogData().remove(layerGroupInfo);
				log.error("MapServerServiceImpl createLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,
						ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, "图层组信息"));
			}
			if (layerGroupInfos != null) {
				mapServerService.cacheLayerInfo(layerGroupInfos, reqIP);
			}
		} else {
			// 将图层组信息存储到redis中
			redisUtils.storeValue(reqIP, layerName + "-group", list);
		}
//		redisUtils.storeValue(reqIP, layerName + "-group", list);
	}

	/**
	 * 获取最大级别
	 *
	 * @param layerGroup
	 * @return
	 */
	private int getMaxLevel(List<LayerGroup> layerGroup) {
		int[] layerGroupArray = new int[layerGroup.size()];
		int index = 0;
		for (LayerGroup layerGroup2 : layerGroup) {
			layerGroupArray[index] = mapServerService.queryZoomLevel(layerGroup2.getLayerName());
			index++;
		}
		Arrays.sort(layerGroupArray);
		return layerGroupArray[layerGroup.size() - 1];
	}

	@Override
	@Transactional
	public void updateLayerGroup(DataServerEntity layerGroupInfos, String reqIP) {
		// 编辑基础信息
		DataServerEntity oldEntity = mapServer.queryServerInfoById(layerGroupInfos.getServerId());
		if (!layerGroupInfos.equals(layerGroupInfos)) {
			// 更新基础信息
			mapServer.editServerData(layerGroupInfos);
		}
		// 删除之前的图层组信息，保存当前的图层组信息
		mapServerService.getCataLogData()
				.remove(mapServerService.getCataLogData().getLayerGroupByName(oldEntity.getLayerName()));
		mapServer.deleteLayerGroupData(Arrays.asList(oldEntity.getServerId()));
		createLayerGroup(layerGroupInfos.getLayerName(), layerGroupInfos.getLayerGroups(), oldEntity.getServerId(),
				null, reqIP);
		mapServer.saveLayerGroups(layerGroupInfos.getLayerGroups());
	}

	@Override
	public Map<String, List<String>> queryLayerGroupNameByLayerInfo(List<String> layerNames) {
		Map<String, List<String>> result = new HashMap<>();
		List<String> tempRes = new ArrayList<>();
		String layerName = null;
		// 执行查询时,以单个服务为单位进行查询
		Iterator<String> iterator = layerNames.iterator();
		while (iterator.hasNext()) {
			layerName = iterator.next();
			tempRes.add(layerName);
			result.put(layerName, mapServer.queryLayerGroups(tempRes));
			tempRes.clear();
		}
		return result;
	}

	@Override
	@Transactional
	public void publishedLayerGroup(List<LayerGroup> layerGroup, String reqIP) {
		// 首先构造图层信息
		if (layerGroup == null || layerGroup.isEmpty()) {
			return;
		}
		List<String> layers = new ArrayList<String>();
		layerGroup.stream().forEach((layer) -> {
			layers.add(layer.getLayerName());
		});
		String layerName = String.join("-", layers);
		DataServerEntity dataServerEntity = new DataServerEntity(layerName, layerName, "");
		dataServerEntity.setLayerGroupFlag(1);
		dataServerEntity.setPublishor(UserUtil.getUserName());
		dataServerEntity.setPublisedProctol("WMS;WMTS;TMS");
		dataServerEntity.setIsPublic(1);
		dataServerEntity.setStatus(ServerStatusEnum.PUBLISHED.getCode());
		dataServerEntity.setFileType("");
		dataServerEntity.setPublishTime(new Date());
		dataServerEntity.setUpdateTime(new Date());
		dataServerEntity.setDataType(DataTypeEnum.GROUP.getFileCode());
		// 保存图层组信息
		mapServer.saveMapData(dataServerEntity);
		layerGroup.stream().forEach((felayer) -> felayer.setLayerGroupId(dataServerEntity.getServerId()));
		mapServer.saveLayerGroups(layerGroup);
		constructLayerGroup(layerName, dataServerEntity, layerGroup, reqIP);
	}

	/**
	 * 构造服务entity
	 *
	 * @param layerName        layerName
	 * @param dataServerEntity dataServerEntity
	 * @param layerGroups    layerGroups
	 * @param reqIP            reqIP
	 */
	private void constructLayerGroup(String layerName, DataServerEntity dataServerEntity,
			List<LayerGroup> layerGroups, String reqIP) {
		// 如果没有远程服务，则证明是当前我们服务，如果有远程url 则证明是远程服务
		LayerGroupInfoImpl layerGroupInfo = new LayerGroupInfoImpl();
		List<PublishedInfo> publishables = new ArrayList<>();
		List<org.geoserver.catalog.StyleInfo> styleInfos = new ArrayList<>();
		List<Envelope> envelopes = new ArrayList<>();
		for (LayerGroup layerGroup : layerGroups) {
			if (StringUtils.isBlank(layerGroup.getRemoteUrl())) {
				constructMapLayer(publishables, styleInfos, envelopes, layerGroup);
			} else {
				if ("wms".equalsIgnoreCase(layerGroup.getOgcProtocal())) {
					// 构造dataEntity
					DataServerEntity dataServer = createServerEntity(layerGroup);
					mapServerService.saveMapServerData(dataServer);
					String msg = mapServerService.publishServerData(dataServer, reqIP);
					if (!"OK".equals(msg)) {
						dataServer.setStatus(ServerStatusEnum.ABNORMAL.getCode());
						throw new StatusException(ExceptionsUtil.ERROR_500, ExceptionsUtil.STATUS_SERVER);
					}
					mapServerService.cacheLayerInfo(dataServer, reqIP);
					// 默认给栅格样式
					layerGroup.setStyleName("栅格");
					// 发布成功后调用
					constructMapLayer(publishables, styleInfos, envelopes, layerGroup);
				}
			}
		}
		// 求两个bbox的交集
		final ReferencedEnvelope envelope = new ReferencedEnvelope();
		if (!envelopes.isEmpty()) {
			envelopes.stream().forEach((envelope1) -> {
				envelope.expandToInclude(envelope1);
			});
		}
		// 存储到catalog中
		layerGroupInfo.setName(layerName);
		// bbox求多个图层的并集
		layerGroupInfo.setBounds(envelope);
		layerGroupInfo.setLayers(publishables);
		layerGroupInfo.setStyles(styleInfos);
		StringBuilder sb = new StringBuilder();
		sb.append(envelope.getMinX()).append(";").append(envelope.getMinY()).append(";").append(envelope.getMaxX())
				.append(";").append(envelope.getMaxY());
		dataServerEntity.getFeatureInfo().setNativeLatLon(sb.toString());
		// 默认将图层组的数据
		mapServerService.cacheLayerInfo(dataServerEntity, reqIP);
		// 保存的时候在持久化
		if (dataServerEntity.getServerId() != 0) {
			try {
				mapServerService.getCataLogData().add(layerGroupInfo);
			} catch (Exception e) {
				mapServerService.getCataLogData().remove(layerGroupInfo);
				log.error("MapServerServiceImpl createLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,
						ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, "图层组信息"));
			}
		}
	}

	private void constructMapLayer(List<PublishedInfo> publishables, List<org.geoserver.catalog.StyleInfo> styleInfos,
			List<Envelope> envelopes, LayerGroup layerGroup) {
		// 不需要发布，只需要构造当前的图层
		LayerInfo layerInfo = mapServerService.getCataLogData().getLayerByName(layerGroup.getLayerName());
		publishables.add(layerInfo);
		ResourceInfo resourceInfo = layerInfo.getResource();
		if (resourceInfo != null) {
			ReferencedEnvelope envelope = resourceInfo.getNativeBoundingBox();
			envelopes.add(envelope);
		}
		styleInfos.add(mapServerService.getCataLogData().getStyleByName(layerGroup.getStyleName()));
	}

	public void updateLayerGroupImg(List<String> layerGroups, List<LayerGroup> layerGroup, String reqIP) {
		for (int i = 0; i < layerGroups.size(); i++) {
			DataServerEntity dataServerEntity = mapServer.queryServerInfoByServerName(layerGroups.get(i));
			// 调用wms服务，获取渲染的对应的数据
			GetMapRequest getMapRequest = null;
			if (layerGroup != null && layerGroup.size() > 0 && DataTypeEnum.TILED.getFileCode() == dataServerEntity.getDataType()) {
				StringBuilder thumPath = new StringBuilder(MapServerUtils.getThumpath())
						.append(File.separator)
						.append(dataServerEntity.getServerName())
						.append(".png");
				WmsRequestParam wmsRequestParam = new WmsRequestParam();
				wmsRequestParam.setLayerName(dataServerEntity.getLayerName());
				wmsRequestParam.setSrs(dataServerEntity.getFeatureInfo().getResourceSrs());
				wmsRequestParam.setBbox(dataServerEntity.getFeatureInfo().getResourceBbox().replaceAll(";", ","));
				wmsRequestParam.setFormat("image/png");
				wmsRequestParam.setWidth(210);
				wmsRequestParam.setHeight(140);
				wmsRequestParam.setTransparent(true);
				wmsRequestParam.setOutputTransparentColor(dataServerEntity.getOutputTransparentColor());
				wmsRequestParam.setInputTransparentColor(dataServerEntity.getInputTransparentColor());
				byte[] wmsImageByte = wmsProtocolService.getWmsImageByte(wmsRequestParam, reqIP);
				JSONObject jsonObject = new JSONObject();
				try(BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(thumPath.toString()))) {
					outputStream.write(wmsImageByte);
					jsonObject.put("msg", MessageFormat.format("图层组{0}缩略图生成成功！", dataServerEntity.getServerName()));
					jsonObject.put("msgKey", "mapServer");
					webSocket.sendMessage(jsonObject.toJSONString());
				} catch (IOException e) {
					jsonObject.put("msg", MessageFormat.format("图层组{0}缩略图生成失败！", dataServerEntity.getServerName()));
					jsonObject.put("msgKey", "mapServer");
					webSocket.sendMessage(jsonObject.toJSONString());
					log.error(
							"MapServerServiceImpl publishedLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new StatusException(ExceptionsUtil.ERROR_500,
							ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_SEND_MESSAGE, "Socket"));
				}
			}
		}
	}

	public List<LayerGroup> createLayerGroup(List<String> layerGroups, String reqIP, List<String> delLayerNames) {
		List<LayerGroup> layerGroup = null;
		File temp;
		for (String layerName : layerGroups) {
			temp = new File(new StringBuilder(MapServerUtils.getThumpath()).append(File.separator).append(layerName)
					.append(".png").toString());
			if (temp.exists()) {
				temp.delete();
			}
			LayerGroupInfo layerGroupInfo = mapServerService.getCataLogData().getLayerGroupByName(layerName);
			if (layerGroupInfo != null) {
				// 先删除原来图层组数据，在创建新
				mapServerService.getCataLogData().remove(layerGroupInfo);
				// 获取当前图层组的服务信息
				int severId = -1;
				DataServerEntity dataServer = mapServer.queryServerInfoByServerName(layerName);
				// 删除图层组中的单个图层时，需要清除原来图层组缓存信息
				try {
					renderTileService.flushTileTask(dataServer.getServerName(), false, true);
					mapServerService.delTempFile(dataServer.getServerName());
				} catch (Exception e) {
					if (ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
						systemLogService
								.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR,
										ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
												dataServer.getServerName() + "服务缓存清理"),
										UserUtil.getUserName(), "Pytc服务", reqIP));
					}
					log.error("MapServerServiceImpl createLayerGroup has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new StatusException(ExceptionsUtil.ERROR_500,
							ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC, dataServer.getServerName() + "服务"));
				}
				if (dataServer != null) {
					layerGroup = dataServer.getLayerGroups();
					severId = dataServer.getServerId();
				}
				if (layerGroup != null && !layerGroup.isEmpty()) {
					createLayerGroup(layerName, layerGroup, severId, dataServer, reqIP);
				}
			}
		}
		return layerGroup;
	}

	/**
	 * 批量发布的编组任务进行编组、切图(如有)
	 *
	 * @param reqIP      reqIP
	 * @param list       list
	 * @param infoEntity infoEntity
	 */
	public void batchPublishAndEditGroup(String reqIP, List<DataServerEntity> list,
			MapServerBatchInfoEntity infoEntity) {
		// 对批量发布的编组任务进行编组
		String groupLayerName = infoEntity.getGroupLayerName();
		int openTiled = infoEntity.getOpenTiled();
		String describe = StringUtils.isBlank(infoEntity.getDescribe()) ? "" : infoEntity.getDescribe();
		DataServerEntity dataServerEntity = new DataServerEntity(new Date(), UserUtil.getUserName(), openTiled, "GBK");
		dataServerEntity.setDescription(describe);
		dataServerEntity.setIsPublic(1);
		dataServerEntity.setLayerGroupFlag(1);
		dataServerEntity.setPublisedProctol("WMS;WMTS;TMS");
		dataServerEntity.setServerName(groupLayerName);
		dataServerEntity.setLayerName(groupLayerName);
		RequestQueryCondition queryCondition = new RequestQueryCondition();
		List<org.pygisserver.web.entity.StyleInfo> styleList = styleInfoService
				.queryStyleInfoByCondition(queryCondition);
		List<LayerGroup> layerGroups = new ArrayList<>();
		list.forEach(doc -> {
			LayerGroup layerGroup = new LayerGroup();
			String serverName = doc.getServerName();
			layerGroup.setLayerName(serverName);
			String styleName = "";
			for (org.pygisserver.web.entity.StyleInfo style : styleList) {
				String layerName = style.getLayerName();
				if (StringUtils.isNotBlank(layerName)) {
					List<String> layerNameList = Arrays.asList(layerName.split(","));
					if (layerNameList.contains(serverName)) {
						styleName = style.getSldName();
						break;
					}
				}
			}

			// 设置默认样式及显示级别
			layerGroup.setStyleName(StringUtils.isBlank(styleName) ? "栅格" : styleName);
			layerGroup.setMinLevel(0);
			layerGroup.setMaxlevel(doc.getFeatureInfo().getZoomLevel());
			layerGroups.add(layerGroup);
		});
		dataServerEntity.setLayerGroups(layerGroups);
		dataServerEntity.setServerShow(0);

		// 进行编组
		publishedLayerGroup(dataServerEntity, reqIP);

		// 进行渲染
		mapServerService.dealThumbal(reqIP, dataServerEntity);
	}

	/**
	 * 构造图层组对应的数据信息
	 *
	 * @param dataServer  dataServer
	 * @param format      format
	 * @param imageWidth  imageWidth
	 * @param imageHeight imageHeight
	 * @return GetMapRequest
	 */
	public GetMapRequest constructGetMapRequest(DataServerEntity dataServer, String format, int imageWidth,
			int imageHeight) {
		FeatureInfo featureInfo = dataServer.getFeatureInfo();
		GetMapRequest getMapRequest = new GetMapRequest();
		getMapRequest.setFormat(format);
		getMapRequest.setHeight(imageHeight);
		getMapRequest.setWidth(imageWidth);
		getMapRequest.setRequest("GetMap");
		getMapRequest.setRequestCharset("UTF-8");
		getMapRequest.setBaseUrl("");
		getMapRequest.setSRS(featureInfo.getResourceSrs());
		getMapRequest.setTransparent(true);
		try {
			getMapRequest.setCrs(CRS.decode(featureInfo.getResourceSrs(), true));
		} catch (FactoryException e1) {
			log.error("MapServerServiceImpl constructGetMapRequest has error:" + ExceptionsUtil.stackTraceToStr(e1));
			throw new ResourceException(ExceptionsUtil.ERROR_500,
					ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, dataServer.getServerName() + "图层坐标系"));
		}
		final List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
		final List<Style> styles = new ArrayList<Style>();
		ReferencedEnvelope envelope = null;
		if (dataServer.getLayerGroupFlag() == 1 || dataServer.getLayerGroupFlag() == 2) {
			// 获取数据的bbox和图层
			List<LayerGroup> layerGroups = dataServer.getLayerGroups();
			layerGroups.stream().forEach((layerGroup) -> {
				LayerInfo layerInfo = mapServerService.getCataLogData().getLayerByName(layerGroup.getLayerName());
				if (layerInfo != null) {
					layers.add(new MapLayerInfo(layerInfo));
				} else {
					LayerInfo layer = new LayerInfoImpl();
					layer.setType(PublishedType.ONLINE);
					TiledInfoImpl tiledInfo = new TiledInfoImpl();
					tiledInfo.setName(layerGroup.getLayerName());
					layer.setResource(tiledInfo);
					layer.setDefaultStyle(mapServerService.getCataLogData().getStyleByName("栅格"));
					layers.add(new MapLayerInfo(layer));
				}
				try {
					styles.add(mapServerService.getCataLogData().getStyleByName(layerGroup.getStyleName()).getStyle());
				} catch (IOException e) {
					log.error("MapServerServiceImpl constructGetMapRequest has error:"
							+ ExceptionsUtil.stackTraceToStr(e));
					throw new ResourceException(ExceptionsUtil.ERROR_500, ExceptionsUtil
							.formatInfo(ExceptionsUtil.RESOURCE_GET, dataServer.getServerName() + "图层样式信息"));
				}
			});
		} else {
			layers.add(new MapLayerInfo(mapServerService.getCataLogData().getLayerByName(dataServer.getLayerName())));
			try {
//				if (dataServer.getStyleInfo().getStyleId().equalsIgnoreCase(Constant.DEM_STYLEID)) {
//					styles.add(catalog.getStyleByName("栅格").getStyle());
//				}else {
				styles.add(mapServerService.getCataLogData().getStyleByName(dataServer.getStyleInfo().getSldName())
						.getStyle());
//				}
			} catch (IOException e) {
				log.error("MapServerServiceImpl constructGetMapRequest has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,
						ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, dataServer.getServerName() + "图层样式信息"));
			}
		}
		String bbox = featureInfo.getResourceBbox();
		String[] bboxArray = bbox.split(";");
		ReferencedEnvelope resourceEnvelope;
		try {
			resourceEnvelope = new ReferencedEnvelope(Double.parseDouble(bboxArray[0]),
					Double.parseDouble(bboxArray[2]), Double.parseDouble(bboxArray[1]),
					Double.parseDouble(bboxArray[3]), CRS.decode(featureInfo.getResourceSrs(), true));
		} catch (MismatchedDimensionException | NumberFormatException | FactoryException e) {
			log.error("MapServerServiceImpl constructGetMapRequest has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,
					ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, dataServer.getServerName() + "图层坐标系"));
		}
		getMapRequest.setBbox(resourceEnvelope);
		envelope = resourceEnvelope;
		getMapRequest.setStyles(styles);
		getMapRequest.setLayers(layers);
		// 获取bbox
		StringBuilder sb = new StringBuilder();
		sb.append(envelope.getMinX()).append(",").append(envelope.getMinY()).append(",").append(envelope.getMaxX())
				.append(",").append(envelope.getMaxY());
		getMapRequest.setRawKvp(getRawKvp(dataServer.getLayerName(), sb.toString(), imageWidth, imageHeight,
				featureInfo.getResourceSrs(), format));
		return getMapRequest;
	}

	private Map<String, String> getRawKvp(String layerName, String bbox, int imageWidth, int imageHeight, String srs,
			String format) {
		Map<String, String> rawKvp = new HashMap<>();
		rawKvp.put("REQUEST", "GetMap");
		rawKvp.put("SRS", srs);
		rawKvp.put("FORMAT", format);
		rawKvp.put("BBOX", bbox);
		rawKvp.put("version", "1.1.0");
		rawKvp.put("STYLES", "");
		rawKvp.put("SERVICE", "WMS");
		rawKvp.put("WIDTH", imageWidth + "");
		rawKvp.put("HEIGHT", imageHeight + "");
		rawKvp.put("LAYERS", layerName);
		rawKvp.put("TRANSPARENT", "true");
		rawKvp.put("thumbal", "true");
		return rawKvp;
	}

	/**
	 * 创建地图服务
	 * 
	 * @param feLayerGroup
	 * @return
	 */
	private DataServerEntity createServerEntity(LayerGroup feLayerGroup) {
		DataServerEntity dataServerEntity = new DataServerEntity(feLayerGroup.getLayerName(),
				feLayerGroup.getLayerName(), "");
		dataServerEntity.setDataType(DataTypeEnum.WMS.getFileCode());
		dataServerEntity.setPublisedProctol("WMS;WMTS;TMS");
		dataServerEntity.setPublishor(UserUtil.getUserName());
		dataServerEntity.setPublishTime(new Date());
		dataServerEntity.setUpdateTime(new Date());
		dataServerEntity.setStatus(ServerStatusEnum.PUBLISHED.getCode());
		dataServerEntity.setStyleInfo(null);
		dataServerEntity.setIsPublic(0);
		// 构造文件类型 远程服务
		dataServerEntity.setFileType("wmsremote");
		if (feLayerGroup.getRemoteUrl().startsWith("http://")) {
			dataServerEntity.setDataUrl(feLayerGroup.getRemoteUrl());
		} else {
			dataServerEntity.setDataUrl("http://" + feLayerGroup.getRemoteUrl());
		}
		return dataServerEntity;
	}
	
	private BoundingBox convertBoundingBox(String resourcebbox, String srs) {
		try {
			String[] nativeBboxArray = resourcebbox.split(";");
			return new ReferencedEnvelope(Double.parseDouble(nativeBboxArray[0]),
					Double.parseDouble(nativeBboxArray[2]), Double.parseDouble(nativeBboxArray[1]),
					Double.parseDouble(nativeBboxArray[3]), CRS.decode(srs, true));
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("MapServerServiceImpl convertBoundingBox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,
					ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系Bbox信息"));
		}
	}
}
