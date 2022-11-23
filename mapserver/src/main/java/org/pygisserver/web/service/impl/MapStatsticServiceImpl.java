package org.pygisserver.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.exception.StatusException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.util.DateUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.dao.MapStatsticMapper;
import org.pygisserver.web.dao.ModelServerMapper;
import org.pygisserver.web.entity.*;
import org.pygisserver.tileserver.service.RenderTileService;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.websocket.WebSocketManager;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.ProgressCallback;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 地图服务service实现类
 * 
 * @author 
 * @since 2018-08-17
 */
@Service(value="mapStatsticService")
@Slf4j
public class MapStatsticServiceImpl implements MapStatsticService{

	@Resource
    private MapStatsticMapper mapStatsticMapper;
	
	@Resource
	private MapServerMapper mapServerMapper;
	
	@Resource
	private ModelServerMapper modelServerMapper;

	@Autowired
	private WebSocketManager webSocket;

    @Autowired
    private RedisUtils redisUtils;
	
	//最大插入数量
	private static final int MAX_INSERT_NUM = 2000;

	@DubboReference(version="3.0.0")
	private RenderTileService renderTileService;

	@Lazy
	@Autowired
	private MapServerServiceImpl mapServerService;

	private static ExecutorService excutorService = Executors.newFixedThreadPool(60);


	/**
	 * 保存当前请求信息的参数
	 * @param reqEntity 请求参数集合
	 */
	@Override
	public void saveServerRequest(List<ServerRequestInfo> reqEntity) {
//		DataServerEntity server = null;
//		for(Iterator<ServerRequestInfo> it = reqEntity.iterator() ; it.hasNext();) {
//			ServerRequestInfo info = it.next();
//			/*如果该服务在请求统计插入数据库之前已经删除，则该请求统计量应该剔除掉，否则会出现脏数据*/
//			server = mapServerMapper.queryServerInfoByServerName(info.getServerName());
//			if(server == null && info.getServerType() == 0) {
//				it.remove();
//			}
//		}
		if (reqEntity == null || reqEntity.isEmpty()) {
			return;
		}
		saveSubListServerRequest(0, MAX_INSERT_NUM, reqEntity);
	}
	
	private void saveSubListServerRequest(long start, long limit, List<ServerRequestInfo> reqEntity){
		if (start >= reqEntity.size()) {
			return;
		}
		List<ServerRequestInfo> serverRequests = reqEntity.parallelStream().skip(start).limit(limit).collect(Collectors.toList());
		insertIntoServerRequest(serverRequests);
		saveSubListServerRequest(start + MAX_INSERT_NUM, MAX_INSERT_NUM, reqEntity);
	}
	
	/**
	 * 批量插叙
	 */
	public void insertIntoServerRequest(List<ServerRequestInfo> reqEntity) {
		try {
			mapStatsticMapper.saveServerRequest(reqEntity);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl saveServerRequest has error:" + ExceptionsUtil.stackTraceToStr(e));
		}
		
	}
	

	@Override
	public boolean isPublishedServer(String layer, String serviceName, String version) {
		Map<String, Object> param = new HashMap<String,Object>();
		param.put("layer", layer);
		param.put("serviceName", serviceName);
		param.put("version", version);
		try {
			return mapStatsticMapper.isPublishedVersion(param);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl isPublishedServer has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, layer + "地图服务协议和版本"));
		}
	}

	@Override
	public void saveAbnormalServerInfo(AbnormalServerInfo abnormalServerInfo) {
		try {
			String serverName = abnormalServerInfo.getServerName();
			DataServerEntity server = mapServerMapper.queryServerInfoByServerName(serverName);
			if(server != null && abnormalServerInfo.getServerType() == 0) {
				mapStatsticMapper.saveAbnormalServerInfo(abnormalServerInfo);
			}
			RequestQueryCondition condition = new RequestQueryCondition();
			condition.setServerName(serverName);
			List<ModelEntity> models = modelServerMapper.queryModellist(condition);
			List<ModelEntity> tilts = modelServerMapper.queryTiltlist(condition);
			if((models.size() > 0 || tilts.size() > 0) && abnormalServerInfo.getServerType() == 1) {
				mapStatsticMapper.saveAbnormalServerInfo(abnormalServerInfo);
			}
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl saveAbnormalServerInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "异常服务信息"));
		}
	}

	@Override
	public List<ServerStatisticinfo> queryServerVisitNum(String startTime, String endTime) {
		Date startDate = covertStrToDate(startTime,true);
		Date endDate = covertStrToDate(endTime,false);
		Map<String, Object> param = new HashMap<String,Object>();
		param.put("startTime", startDate);
		param.put("endTime", endDate);
		try {
			return mapStatsticMapper.queryServerVisitNum(param);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "地图服务访问统计"));
		}
	}

	/**
	 * 将当前的日期字符串转成日期格式
	 * @param dateStr 日期字符串
	 * @param isStart 是否为起始日期
	 * @return
	 */
	private Date covertStrToDate(String dateStr,boolean isStart) {
		if (StringUtils.isBlank(dateStr)) {
			if (isStart) {
				return DateUtil.getFirstDayOfMouthDate();
			}else {
				return new Date();
			}
		}else {
			return DateUtil.covertStringToDateHour(dateStr);
		}
	}
	
	
	@Override
	public List<ServerStatisticinfo> queryServerPerformance(String filed, String startTime, String endTime, String sort) {
		Map<String, Object> param = new HashMap<>();
		param.put("filed", filed);
		param.put("sort", "asc".equalsIgnoreCase(sort) ? "asc" : "desc");

		if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
			param.put("startTime", DateUtil.covertStringToDateHour(startTime));
			param.put("endTime", DateUtil.covertStringToDateHour(endTime));
		}
		try {
			return mapStatsticMapper.queryServerPerformance(param);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerPerformance has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "系统性能统计"));
		}
	}
	
	@Override
	public void saveSysResource(SysResourceRecord sysResource) {
		try {
			mapStatsticMapper.saveSysResourceInfo(sysResource);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl saveSysResource has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "系统资源统计信息"));
		}
	}

	@Override
	public List<SysResourceRecord> querySysResourceRecords() {
		String startTime = DateUtil.coverTimeToStart(new Date());
		Date startDate;
		startDate = DateUtil.covertStringToDateHour(startTime);
		try {
			return mapStatsticMapper.querySysResource(startDate, new Date());
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl querySysResourceRecords has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "系统资源统计"));
		}
	}

	@Override
	public List<AbnormalServerInfo> queryAbnormalServerInfo(String startTime, String endTime) {
		
		Map<String, Object> param = null;
		if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
			param = new HashMap<String,Object>();
			param.put("startTime", DateUtil.covertStringToDateHour(startTime));
			param.put("endTime", DateUtil.covertStringToDateHour(endTime));
		}
		try {
			return mapStatsticMapper.queryAbnormalServerInfo(param);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryAbnormalServerInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "异常服务统计"));
		}
	}

	@Override
	public void saveOperationLog(SysOperationLog sysOperation) {
		try {
			mapStatsticMapper.saveOperationLog(sysOperation);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl saveOperationLog has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "操作日志信息"));
		}
	}

	/**
	 * 服务状态统计
	 * 统计周期为当月的时间
	 * 
	 */
	@Override
	public List<ServerStatisticinfo> serverStatusStatstic() {
		try {
			return mapStatsticMapper.serverStatusStatstic();
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl serverStatusStatstic has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "服务状态访问统计"));
		}
	}

	/**
	 * 服务协议类型统计
	 * 统计周期为当月的时间
	 */
	@Override
	public List<ServerStatisticinfo> serverTypeStatstic() {
		try {
			return mapStatsticMapper.serverTypeStatstic(DateUtil.getFirstDayOfMouthDate(),new Date());
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl serverTypeStatstic has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "服务类型访问统计"));
		}
	}

	@Override
	public List<SysOperationLog> querySysOprationLog(RequestQueryCondition queryEntity) {
		try {
			return mapStatsticMapper.querySysOprationLog(queryEntity);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl querySysOprationLog has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "服务操作日志"));
		}
	}

	@Override
	public void deleteServerReqInfo(String serverName, String productName) {
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("serverName", serverName);
		param.put("productName", serverName);
		try {
			mapStatsticMapper.deleteServerReqInfoByServerName(param);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl deleteServerReqInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, "服务访问统计信息"));
		}
	}

	@Override
	public boolean isSupportTheFormat(String layer,String serviceType,String format) {
		try {
			return mapStatsticMapper.isSupportTheFormat(layer, serviceType, format);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl isSupportTheFormat has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, layer + "服务支持的格式"));
		}
	}

	@Override
	public void deleteServerReqInfoByName(List<String> serverNames) {
		try {
			mapStatsticMapper.deleteServerReqInfoByName(serverNames);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl deleteServerReqInfoByName has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, "服务访问统计信息"));
		}
		
	}

	@Override
	public void deleteServerReqInfoById(List<Integer> ids) {
		try {
			mapStatsticMapper.deleteServerReqInfoByServerIds(ids);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl deleteServerReqInfoById has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, "服务访问统计信息"));
		}
	}

	@Override
	public void deleteAbnormalInfo(List<Integer> ids) {
		try {
			mapStatsticMapper.deleteAbnormalInfo(ids);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl deleteAbnormalInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, "异常服务统计信息"));
		}
	}

	@Override
	public List<String> queryAllLayers() {
		try {
			return mapStatsticMapper.queryAllLayers();
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryAllLayers has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "当前统计表中所有图层"));
		}
	}

	@Override
	public DataServerEntity queryServerInfo(String layerName) {
		try {
			return mapServerMapper.queryServerInfoByServerName(layerName);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryAllLayers has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, layerName + "服务"));
		}
	}

	@Override
	public void saveSystemLog(SystemLogEntity sysLog) {
		try {
			mapStatsticMapper.saveSystemLog(sysLog);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "地图服务访问统计"));
		}
	}
	
	@Override
	public void saveStatisticServerRequest(List<ServerStatisticinfo> list) {
		try {
			mapStatsticMapper.saveOrUpdateServerStatistic(list);
		} catch (Exception e) {
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "服务性能统计"));
		}
	}

	@Override
	public int querySystemLogCount(RequestQueryCondition queryEntity) {
		try {
			return mapStatsticMapper.querySystemLogCount(queryEntity);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "地图服务访问统计"));
		}
	}

	@Override
	public List<SystemLogEntity> querySystemLogs(RequestQueryCondition queryEntity) {
		try {
			return mapStatsticMapper.querySystemLogs(queryEntity);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "地图服务访问统计"));
		}
	}

	@Override
	public void deleteSystemLog(List<Integer> sysLogIds) {
		try {
			mapStatsticMapper.deleteSystemLog(sysLogIds);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "地图服务访问统计"));
		}
	}

	@Override
	public List<SysOperationLog> queryAllSysOprationLog() {
		try {
			return mapStatsticMapper.queryAllSysOprationLog();
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryAllSysOprationLog has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "系统操作日志"));
		}
	}

	@Override
	public void deleteOprationLog(List<Integer> sysOpraIds) {
		try {
			mapStatsticMapper.deleteOprationLog(sysOpraIds);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl deleteOprationLog has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, "系统操作日志"));
		}
	}
	
	/**
	 * 	清理监控信息
	 */
	@Override
	public void deleteServerStatstic(List<String> layers) {
		try {
			mapStatsticMapper.deleteServerStatstic(layers);
		} catch (Exception e) {
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, "服务监控信息"));
		}
	}

	@Override
	public int queryOperationLogCount(RequestQueryCondition queryEntity) {
		try {
			return mapStatsticMapper.queryOprationLogCount(queryEntity);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "地图服务访问统计"));
		}
	}

	@Override
	public void updateRequestServerName(int serverId, String newServerName) {
		try {
			mapStatsticMapper.updateRequestServerName(serverId, newServerName);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_UPDATE, "地图服务访问统计"));
		}
	}

	@Override
	public void updateVisitServerName(int serverId, String newServerName) {
		try {
			mapStatsticMapper.updateVisitServerName(serverId, newServerName);
		} catch (Exception e) {
			log.error("MapStatsticServiceImpl queryServerVisitNum has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_UPDATE, "地图服务访问统计"));
		}
	}

	/**
	 * 构建金字塔缓存
	 * @param dataServerEntity
	 * @return
	 */
	@Override
	public Future buildOverviews(DataServerEntity dataServerEntity) {
		gdal.AllRegister();
		Dataset open = gdal.Open(dataServerEntity.getDataUrl(), gdalconstConstants.GA_ReadOnly);
		Thread.currentThread().setName("overViews" + dataServerEntity.getServerName());
		BuildOverviesCallBack buildOverviesCallBack = new BuildOverviesCallBack(dataServerEntity.getServerId(), dataServerEntity.getOpenTiled(), dataServerEntity.getServerName(), open, dataServerEntity.getDataUrl());
		open.BuildOverviews("average", new int[]{2,4,6,8,16}, buildOverviesCallBack);

		return new AsyncResult("Result");
	}

	private class BuildOverviesCallBack extends ProgressCallback {
		private int serverId;

		private int openTiled;

		private String serverName;

		private Dataset open;

		private String fileUrl;

		public BuildOverviesCallBack(int serverId, int openTiled, String serverName, Dataset open, String fileUrl){
			this.serverId = serverId;
			this.openTiled = openTiled;
			this.serverName = serverName;
			this.open = open;
			this.fileUrl = fileUrl;
		}
		@Override
		public int run(double dfComplete, String pszMessage) {
			if (Thread.currentThread().isInterrupted()) {
				Thread.currentThread().stop();
				return -1;
			}

			if (StringUtils.isNotBlank(pszMessage)) {
				log.info("overViews is ok!");
			}

			String format = String.format("%.2f", dfComplete * 100);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("status", 200);
			jsonObject.put("msgKey", "overViews");
			jsonObject.put("msg", serverId + "---" + format);
			webSocket.sendMessage(jsonObject.toJSONString());

			if (format.equals("100.00")) {
				// 金字塔缓存生成完成修改服务状态
				List<Integer> ids = new ArrayList<>();
				ids.add(serverId);
				mapServerMapper.updateServerStatusByIds(1, ids);

				jsonObject.put("status", 200);
				jsonObject.put("msgKey", "mapServer");
				jsonObject.put("msg", serverName + "服务金字塔缓存生成完成!");
				webSocket.sendMessage(jsonObject.toJSONString());
				mapServerService.getOverViews().remove(serverName);

                redisUtils.deleteKey("system", serverName);
				// 如果有切图任务,将切图任务从预等待队列添加到等待队列
				if (openTiled == 1) {
					renderTileService.addWaitTileTask(serverName);
				}
			}
			return 1;
		}
	}

	@Override
	public Future publishImageMosaic(DataServerEntity dataServer,String reqIP) {
		Thread.currentThread().setName("ImageMosaic" + dataServer.getServerName());
		// 默认读取原数据时进度为3%
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("status", 200);
		jsonObject.put("msgKey", "ImageMosaic");
		jsonObject.put("msg", dataServer.getServerId() + "---" + "3.00");
		webSocket.sendMessage(jsonObject.toJSONString());
		File file = new File(dataServer.getDataUrl());
		String[] fileList = file.list();
		int length = fileList.length;
		List<Integer> ids = new ArrayList<>();
		ids.add(dataServer.getServerId());
		CompletableFuture.supplyAsync(() -> {
			Thread.currentThread().setName("ImageMosaicRead" + dataServer.getServerName());
			BboxInfoEntity bboxInfoEntity = queryBboxForImageMosaic(dataServer.getDataUrl(), dataServer, reqIP);

			// 修改服务状态
			mapServerMapper.updateServerStatusByIds(1, ids);
			mapServerService.publishServerResult(dataServer, reqIP);

			// 更新状态后删除redis中的缓存，否则预览时服务状态为发布中，无法预览
			redisUtils.deleteKey("system", dataServer.getServerName());
			return "OK";
		}, excutorService).whenComplete((result,r) -> {
			if ("OK".equals(result)) {
				jsonObject.put("status", 200);
				jsonObject.put("msg", dataServer.getServerId() + "---" + "99.00");
			} else {
				jsonObject.put("status", 500);
				jsonObject.put("msg", dataServer.getServerName() + "发布失败!");
			}
			jsonObject.put("msgKey", "ImageMosaic");
			webSocket.sendMessage(jsonObject.toJSONString());
		});

		// 默认1秒读取3个3秒推送一次---每次推送进度时读取了9个原数据
		double schedule = 3.00d;
		DataServerEntity serverStatus = mapServerMapper.queryServerDataByIds(ids).get(0);
		while (serverStatus.getStatus() == 3) {
			if (Thread.currentThread().isInterrupted()) {
				Thread.currentThread().stop();
				return null;
			}
			// 默认读取过程占96%,读取完成后进度为99%
			schedule += 9.0 / length / 0.96 * 100;
			if (schedule > 99.00) {
				schedule = 99.00;
			}
			jsonObject.put("status", 200);
			jsonObject.put("msgKey", "ImageMosaic");
			jsonObject.put("msg", dataServer.getServerId() + "---" + String.format("%.2f", schedule));
			webSocket.sendMessage(jsonObject.toJSONString());
			try {
				Thread.sleep(3000);
				serverStatus = mapServerMapper.queryServerDataByIds(ids).get(0);
			} catch (InterruptedException e) {
				log.error("MapStatsticServiceImpl dealServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC, dataServer.getServerName() + "ImageMosaic数据"));
			}
		}
		jsonObject.put("status", 200);
		jsonObject.put("msgKey", "ImageMosaic");
		jsonObject.put("msg", dataServer.getServerName() + "发布完成!");
		webSocket.sendMessage(jsonObject.toJSONString());
		mapServerService.getOverViews().remove(dataServer.getLayerName());
		return new AsyncResult("Result");
	}

	/**
	 * 生成ImageMosaic遥感影像数据的footprint的shp文件并从shp文件中获取bbox
	 *
	 * @param url
	 * @param serverEntity
	 * @param reqIP
	 * @return
	 */
	private BboxInfoEntity queryBboxForImageMosaic(String url, DataServerEntity serverEntity, String reqIP) {
		File source = new File(url);
		try {
			ImageMosaicReader reader = new ImageMosaicReader(source, null);
			reader.harvest(null, source, null);
		} catch (IOException e) {
			log.error("生成footprint的shp文件异常!");
		}

		url = url + File.separator + source.getName() + ".shp";
		return mapServerService.queryServerBboxByFile(url, "Shape", serverEntity, reqIP);
	}

	@Override
	public void saveReqEntity(ServerRequestInfo requestInfo) {
		List<ServerRequestInfo> requestInfos = new ArrayList<>();
		requestInfos.add(requestInfo);
		saveServerRequest(requestInfos);
	}
}
