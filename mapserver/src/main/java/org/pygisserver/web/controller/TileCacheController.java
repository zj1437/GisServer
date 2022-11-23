package org.pygisserver.web.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.SeedTaskParam;
import org.pygisserver.common.entity.TileTaskEntity;
import org.pygisserver.common.enumm.StoreType;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.service.TileCacheService;
import org.pygisserver.tileserver.service.RenderTileService;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(value="切图缓存管理接口",tags="切图缓存管理接口")
@RestController
@RequestMapping("/pytc/tile")
public class TileCacheController {

	@Autowired
	private MapServerService mapServerService;
	
	@Autowired
	private TileCacheService tileCacheService;
	
	@DubboReference(version="3.0.0")
	private RenderTileService renderTileService;
	
	@DubboReference(version="3.0.0")
	private org.pygisserver.tileserver.service.TileCacheService tileServerCacheService;
	
	@Autowired
	private WebSocketManager webSocketManager;
	
	/**
	 * 
	 * @return
	 */
	@ApiOperation(value = "计算切图大小",notes="根据相关参数计算切图缓存")
	@ApiImplicitParams({
		@ApiImplicitParam(name="layer",value="瓦片层级",required=true,paramType="path"),
		@ApiImplicitParam(name="gridSet",value="表格",required=true,paramType="path"),
		@ApiImplicitParam(name="startZoom",value="起始层级",required=true,paramType="path"),
		@ApiImplicitParam(name="format",value="格式",required=true,paramType="path"),
		@ApiImplicitParam(name="endZoom",value="结束层级",required=true,paramType="path")
	})
	@GetMapping("/calculate/size/{layer}/{gridSet}/{startZoom}/{endZoom}")
	public ReponseEntity<Double> getIncreaseTileSize(@PathVariable("layer") String layer,
													 @PathVariable("gridSet") String gridSet,
													 @PathVariable("startZoom") int startZoom,
													 @RequestParam("format") String format,
													 @PathVariable("endZoom") int endZoom,
													 HttpServletRequest req){
		String reqIP = UserUtil.getRequestIp(req);
		double tileSize = tileCacheService.calcuteTileSize(layer, gridSet, format, startZoom, endZoom,reqIP);
		return new ReponseEntity<Double>(ReponseStatus.SUCCESS.getCode(),
				 						 ReponseStatus.SUCCESS.getResult(),
				 						 tileSize);
	}
	
	/**
	 * 手动切图/导出TMS标准格式数据数据
	 * @param taskInstance
	 * @param req
	 * @return
	 */
	@ApiOperation(value = "开启手动切图",notes="手动切图")
	@ApiImplicitParam(name="seedParam",value="切图参数",required=true,paramType="body")
	@PostMapping("/manual/seed/task")
	public ReponseEntity<String> manualSeedTaskList(HttpServletRequest req,@RequestBody SeedTaskInstance taskInstance) {
		String reqIP = UserUtil.getRequestIp(req);
		LayerBaseInfo layerBaseInfo = mapServerService.getLayerBaseInfo(taskInstance.getLayerName(),reqIP);
		taskInstance.setStyleName(layerBaseInfo.getStyleName());
		//导出TMS数据格式的存储类型为5
		if (taskInstance.getStoreType() == StoreType.TMS.getTypeId()) {
			//导出前先判断导出路径是否改变，如果有改变则修改缓存和数据库
			String exportPath = taskInstance.getStorePath();
			if (!exportPath.equals(System.getProperty(Constant.EXPORT_RUN_ENV))) {
				System.setProperty(Constant.EXPORT_RUN_ENV, exportPath);
				tileCacheService.updateExportPath(exportPath);
			}
			if(renderTileService.submitTaskIsSeeding(taskInstance,true)) {
				String result = MessageFormat.format("已存在与{0}服务相同投影、瓦片格式、瓦片大小的切图导出任务!", taskInstance.getLayerName());
				return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
						 ReponseStatus.SUCCESS.getResult(),result);
			}
			//是否已导出只针对同样是导出且已完成的导出任务
			boolean isNeedExportNew = renderTileService.exportServerData(taskInstance);
			if (!isNeedExportNew) {
				noticeWebSocketTileFinshed(MessageFormat.format("导出任务{0}已经完成！", taskInstance.getLayerName()));
				return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),ReponseStatus.SUCCESS.getResult());
			}
			if (isNeedExportNew) {
				renderTileService.seedTaskByParam(taskInstance, 0);
			}
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
					 						 ReponseStatus.SUCCESS.getResult());
		} else {
			if(renderTileService.submitTaskIsSeeding(taskInstance,false)) {
				String result = MessageFormat.format("已存在与{0}服务相同投影、瓦片格式、瓦片大小的切图任务!", taskInstance.getLayerName());
				return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
						 ReponseStatus.SUCCESS.getResult(),result);
			}
			renderTileService.seedTaskByParam(taskInstance, 0);
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
					 						 ReponseStatus.SUCCESS.getResult());
		}
	}
	
	/**
	 * 切图完成推送消息
	 * @param message
	 */
	public void noticeWebSocketTileFinshed(String message) {
		Map<String,Object> msgMap = new HashMap<String,Object>();
		msgMap.put("msg", message);
		msgMap.put("msgKey", "tileServer");
		JSONObject jsonObject = new JSONObject(msgMap);
		webSocketManager.sendMessage(jsonObject.toJSONString());
	}
	
	@ApiOperation(value = "查询Tms数据导出路径",notes="查询Tms数据导出路径")
	@GetMapping("/getExportPath")
	public ReponseEntity<String> getExportPath(HttpServletRequest req) {
		String exportPath = System.getProperty(Constant.EXPORT_RUN_ENV);
		if (StringUtils.isBlank(exportPath)) {
			exportPath = tileCacheService.queryGlobalSeedParam().getExportPath();
			if (StringUtils.isBlank(exportPath)) {
				exportPath = Constant.TILE_CACHE_TMP;
				System.setProperty(Constant.EXPORT_RUN_ENV, exportPath);
				tileCacheService.updateExportPath(exportPath);
			}
		}
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
				 						 ReponseStatus.SUCCESS.getResult(),
				 						exportPath);
	}
	
	@ApiOperation(value = "查询Tms数据导出路径",notes="查询Tms数据导出路径")
	@GetMapping("/isCanDownloadTmsFile/{taskName}")
	public ReponseEntity<Boolean> isCanDownloadTmsFile(HttpServletRequest req,@PathVariable String taskName) {
		SeedTaskInstance taskInstance = tileCacheService.queryTaskInstance(taskName);
		if (taskInstance == null) {
			return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),
					 ReponseStatus.SUCCESS.getResult(),
					false);
		}
		return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),
				 						 ReponseStatus.SUCCESS.getResult(),
				 						renderTileService.isCanDownloadTmsFile(taskInstance));
	}
	
	@ApiOperation(value = "下载Tms标准数据",notes="下载Tms标准数据")
	@ApiImplicitParam(name="downloadServerData",value="下载Tms标准数据参数",required=true,paramType="body")
	@GetMapping("/downloadServerData/{taskName}")
	public ResponseEntity<String> downloadServerData(HttpServletRequest req, HttpServletResponse resp,@PathVariable String taskName) {
		if (StringUtils.isBlank(taskName)) return null;
		SeedTaskInstance taskInstance = tileCacheService.queryTaskInstance(taskName);
		if (taskInstance == null) return null;
		File file = new File(taskInstance.getStorePath(),getExportDataName(taskInstance) + ".zip");
		resp.setHeader("content-type", "image/png");
		resp.setContentType("application/octet-stream");
		resp.setHeader("Content-Disposition", "attachment;filename=" + file.getName());
		byte[] buff = new byte[1024];
		BufferedInputStream bis = null;
		OutputStream os = null;
		try {
			os = resp.getOutputStream();
			bis = new BufferedInputStream(new FileInputStream(file));
			int read;
			while ((read = bis.read(buff)) != -1) {
				os.write(buff, 0, buff.length);
				os.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bis != null) FileUtil.closeStream(bis);
			if (os != null) FileUtil.closeStream(os);
		}
		return null;
	}
	
	/**
	 * 获取请求格式的缩写
	 * @param tileInfo
	 * @return
	 */
	public static String simpleMineType(String mineType) {
		if (mineType.indexOf(";") > -1) {
			if (mineType.contains("type=mapbox-vector")) {
				mineType = "protobuf";
			}else if(mineType.contains("type=geojson")){
				mineType = "geojson";
			} else if (mineType.contains("type=topojson")) {
				mineType = "topojson";
			}
		}
		if (mineType.indexOf("/") > -1) {
			mineType = mineType.split("/")[1];
		}
		return mineType;
	}
	
	/**
	 * 创建导出任务压缩包的名称,格式为：layername_srs_format_width_maxlevel
	 * @param tileInfo 请求信息
	 * @return
	 */
	public static String getExportDataName(SeedTaskInstance seedTaskInstance) {
		String grid = seedTaskInstance.getSrs().contains(":") ? seedTaskInstance.getSrs().split(":")[1] : seedTaskInstance.getSrs();
		String mimeType = simpleMineType(seedTaskInstance.getTileFormat());
		String dbName = new StringBuilder(seedTaskInstance.getLayerName()).append("_").append(grid)
				.append("_").append(mimeType).append("_").append(seedTaskInstance.getTileWidth()).append("_").append(seedTaskInstance.getZoomEnd()).toString();
		return dbName;
	}
	
	/**
	 * 获取当前切图任务信息
	 * @return
	 */
	@ApiOperation(value = "获取当前切图任务信息",notes="获取当前切图任务信息")
	@PostMapping(value= {"/query/tasks"})
	public ReponseEntity<List<TileTaskEntity>> queryTileTaskInfo(@RequestBody(required=false) RequestQueryCondition queryCondition){
		int taskNum = renderTileService.queryTileTaskNum(queryCondition);
		if (taskNum > 0) {
			List<TileTaskEntity> list = renderTileService.queryTileTaskInfo(queryCondition);
			return new ReponseEntity<List<TileTaskEntity>>(ReponseStatus.SUCCESS.getCode(),
											 			   ReponseStatus.SUCCESS.getResult(),
											 			   list,
											 			  taskNum);
		}
		return new ReponseEntity<List<TileTaskEntity>>(ReponseStatus.SUCCESS.getCode(),
										 			   ReponseStatus.SUCCESS.getResult(),
										 			   null);
	} 
	
	/**
	 * 保存切片缓存配置
	 * @param seedParam
	 * @return
	 */
	@ApiOperation(value = "保存切片缓存配置",notes="保存切片缓存配置")
	@ApiImplicitParam(name="seedParam",value="切图参数",required=true,paramType="body")
	@PostMapping("/save/config")
	public ReponseEntity<String> saveSeedTileConfig(HttpServletRequest req,@RequestBody SeedTaskParam seedParam){
		SeedTaskParam globalSeedParam = tileCacheService.queryGlobalSeedParam();
		if (globalSeedParam.getStoreType() != seedParam.getStoreType()) {
			tileServerCacheService.cleanStoreTypeCache();
		}
		tileCacheService.saveSeedParam(seedParam);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
										ReponseStatus.SUCCESS.getResult(),
										"保存成功");
	}
	
	/**
	 * 获取切片配置信息
	 * @return
	 */
	@ApiOperation(value = "获取切片配置信息",notes="获取切片配置信息")
	@GetMapping("/query/Config")
	public ReponseEntity<SeedTaskParam> queryTileConfig(){
		SeedTaskParam globalSeedParam = tileCacheService.queryGlobalSeedParam();
		return new ReponseEntity<SeedTaskParam>(ReponseStatus.SUCCESS.getCode(),
											   	  ReponseStatus.SUCCESS.getResult(),
												  globalSeedParam);
	}
	
	/**
	 * 获取切图任务详细信息
	 * @return
	 */
	@ApiOperation(value = "获取切图任务详细信息",notes="获取切图任务详细信息置")
	@ApiImplicitParam(name="layerName",value="切图名称",required=true,paramType="path")
	@GetMapping("/query/task/detail/{taskName}")
	public ReponseEntity<SeedTaskInstance> queryTileTaskDetailInfo(@PathVariable String taskName){
		return new ReponseEntity<SeedTaskInstance>(ReponseStatus.SUCCESS.getCode(),
		  									   	   ReponseStatus.SUCCESS.getResult(),
		  									   	   tileCacheService.queryTaskInstance(taskName));
	}
	
	/**
	 * 清空当前已经切好的瓦片
	 * @param layerName
	 * @return
	 */
	@ApiOperation(value = "删除已经切好的瓦片",notes="删除某个已经切好的瓦片")
	@ApiImplicitParam(name="layerName",value="切图名称",required=true,paramType="path")
	@GetMapping("/flush/{layer}")
	public ReponseEntity<String> flushTiledInfo(HttpServletRequest req,@PathVariable(value="layer") String layerName){
		renderTileService.flushTileTask(layerName,true,true);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),layerName + "服务缓存数据清除成功！");
	}
	
	/**
	 * 切图图层列表
	 * @param queryCondition
	 * @return
	 */
	@ApiOperation(value = "查询图层信息",notes="查询图层信息")
	@ApiImplicitParam(name="queryCondition",value="图层查询信息",required=true,paramType="body")
	@PostMapping("/query/layer")
	public ReponseEntity<List<DataServerEntity>> queryTileLayerInfo(@RequestBody(required=false) RequestQueryCondition queryCondition){
		int num = tileCacheService.querySeedTaskNum(queryCondition);
		if (num > 0) {
			List<DataServerEntity> list = tileCacheService.querySeedTaskInfo(queryCondition);
			return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(), 
															 "查询成功！", 
															 list,
															 num);
		} else {
			return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(), "查询成功！");
		}
	}
	
	/**
	 * 暂停当前切图任务
	 * @param taskName 任务名称
	 * @return
	 */
	@ApiOperation(value = "暂停当前切图任务",notes="暂停当前切图任务")
	@ApiImplicitParam(name="layer",value="图层名称",required=true,paramType="path")
	@GetMapping("/stop/{taskName}")
	public ReponseEntity<String> stopTileTask(HttpServletRequest req,@PathVariable String taskName){
		renderTileService.stopSeedTask(taskName);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "暂停成功！");
	}
	
	/**
	 * 删除当前的切图任务
	 * @return
	 */
	@ApiOperation(value = "删除当前切图任务",notes="删除当前切图任务")
	@ApiImplicitParam(name="layer",value="图层名称",required=true,paramType="path")
	@GetMapping("/delete/{taskName}/{isDelExportData}")
	public ReponseEntity<String> deleteTileTask(HttpServletRequest req,@PathVariable String taskName,@PathVariable boolean isDelExportData){
		List<String> taskIds = new ArrayList<String>();
		taskIds.add(taskName);
		renderTileService.deleteTileTask(taskIds,isDelExportData);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功！");
	}
	
	
	/**
	 * 批量删除当前的切图任务
	 * @return
	 */
	@ApiOperation(value = "删除当前切图任务",notes="删除当前切图任务")
	@ApiImplicitParam(name="layer",value="图层名称",required=true,paramType="path")
	@PostMapping("/delete/{isDelExportData}")
	public ReponseEntity<String> deleteTileTask(HttpServletRequest req,@RequestBody List<String> taskNames,@PathVariable boolean isDelExportData){
		renderTileService.deleteTileTask(taskNames,isDelExportData);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功！");
	}
	
	/**
	 * 激活当前切图任务
	 * @return
	 */
	@ApiOperation(value = "激活当前切图任务",notes="激活当前切图任务")
	@ApiImplicitParam(name="layer",value="图层名称",required=true,paramType="path")
	@GetMapping("/active/{taskName}")
	public ReponseEntity<String> activeTileTask(HttpServletRequest req,@PathVariable String taskName){
		renderTileService.activeSeedTask(taskName);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "激活成功！");
	}
	
	@ApiOperation(value = "获取当前的切图级别",notes="获取当前的切图级别")
	@ApiImplicitParams({
		@ApiImplicitParam(name="currentLevel",value="默认级别",required=true,paramType="path"),
		@ApiImplicitParam(name="tilewidth",value="瓦片宽度",required=true,paramType="path")
	})
	@GetMapping("/level/{currentlevel}/{tilewidth}/{srsCode}")
	public ReponseEntity<Integer> getCurrentLevel(@PathVariable("currentlevel") int currentLevel,@PathVariable("tilewidth") int tileWidth
												  ,@PathVariable("srsCode") int code){
		int level = mapServerService.calcuCurrentLevel(currentLevel, tileWidth,"EPSG:" + code);
		return new ReponseEntity<Integer>(ReponseStatus.SUCCESS.getCode(), 
										  ReponseStatus.SUCCESS.getResult(), 
										  level);
	}
	
	/**
	 * 将当前的图层的bbox进行转化
	 * @param layerName
	 * @return
	 */
	@ApiOperation(value = "转化当前的图层的bbox",notes="转化当前的图层的bbox")
	@GetMapping("/{layer}/{epsgCode}")
	public ReponseEntity<String> getCurrentLevel(@PathVariable("layer") String layerName,
												 @PathVariable("epsgCode") int epsgCode){
		String transformBbox = tileCacheService.transformCordinateBbox(layerName, epsgCode);
		if (StringUtils.isBlank(transformBbox)) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, layerName + "图层Bbox信息"));
		}
		
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
										 ReponseStatus.SUCCESS.getResult(), 
										 transformBbox);
		
		
	}
}
