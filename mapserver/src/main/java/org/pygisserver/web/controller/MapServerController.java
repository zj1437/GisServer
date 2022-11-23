package org.pygisserver.web.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.entity.ServerProtocolInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.service.AnalysisLayerServerService;
import org.pygisserver.web.service.LayerGroupServerService;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.entity.BboxInfoEntity;
import org.pygisserver.web.entity.MapServerBatchInfoEntity;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.geoserver.catalog.LayerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 服务控制，主要包括服务发布，服务查询、编辑、删除的操作控制 
 * @author:  
 * @date: 2018-10-26
 */
@Api(tags="地图服务")
@RestController
@RequestMapping(value="/dataServer")
public class MapServerController {

	@Autowired
	private MapServerService mapServerService;
	
	@Resource
	private RedisTemplate<String, Object> redisTemplate;
	
	@Autowired
	private WebSocketManager webSocketManager;

	@Autowired
	private LayerGroupServerService layerGroupServerService;
	
	@Autowired
	private AnalysisLayerServerService analysisLayerServerService;
	
	/**
	 * 该服务是否已经发布
	 * 
	 * @param serverName 服务名称
	 */
	@RequestMapping(value="/ispublised/{serverName:.+}",method=RequestMethod.GET)
	@ApiOperation(value="唯一校验",notes="校验当前的服务名称是否唯一")
	@ApiImplicitParam(name="serverName",value="服务名称",required=true,paramType="path")
	public ReponseEntity<Boolean> isPublished(@PathVariable(value="serverName") String serverName){
		boolean flag = mapServerService.isPublished(serverName);
		return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),
				  ReponseStatus.SUCCESS.getResult(),
				  flag);
	}

	/**
	 * 该服务是否已经发布
	 *
	 * @param serverName 服务名称
	 */
	@RequestMapping(value="/ispublisedRest/{serverName:.+}",method=RequestMethod.GET)
	@ApiOperation(value="rest唯一校验",notes="校验当前的rest服务名称是否唯一")
	@ApiImplicitParam(name="serverName",value="rest服务名称",required=true,paramType="path")
	public ReponseEntity<Boolean> ispublisedRest(@PathVariable(value="serverName") String serverName){
		boolean flag = mapServerService.ispublisedRest(serverName);
		return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),
				ReponseStatus.SUCCESS.getResult(),
				flag);
	}
	
	/**
	 * 批量服务发布
	 * @param dataServer
	 * @return
	 */
	@ApiOperation(value="批量发布服务",notes="批量发布多个服务")
	@ApiImplicitParam(name="infoEntity",value="多个服务数据",required=true,paramType="body")
	@RequestMapping(value="/batch/publish",method=RequestMethod.POST)
	public ReponseEntity<String> publishedServerDataList(HttpServletRequest request, @RequestBody MapServerBatchInfoEntity infoEntity){
		String reqIP = UserUtil.getRequestIp(request);
		if (infoEntity.isEditGroup() && StringUtils.isBlank(infoEntity.getGroupLayerName())) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "发布失败，聚合图层组名不能为空");
		}
		String msg = mapServerService.batchPublishServerData(infoEntity, reqIP);
		if (msg.equals("发布成功") || msg.equals("发布部分成功")) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),msg);
		} else {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(),msg);
		}
	}

	/**
	 * 批量服务发布
	 * @param dataServer
	 * @return
	 */
	@ApiOperation(value="批量发布rest服务",notes="批量发布多个rest服务")
	@ApiImplicitParam(name="infoEntity",value="多个rest服务数据",required=true,paramType="body")
	@RequestMapping(value="/batch/publishRest",method=RequestMethod.POST)
	public ReponseEntity<String> publishedRestDataList(HttpServletRequest request, @RequestBody MapServerBatchInfoEntity infoEntity){
		String reqIP = UserUtil.getRequestIp(request);
		if (infoEntity.isEditGroup() && StringUtils.isBlank(infoEntity.getGroupLayerName())) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "发布失败，聚合图层组名不能为空");
		}
		String msg = mapServerService.batchPublishRestData(infoEntity, reqIP);
		if (msg.equals("发布成功") || msg.equals("发布部分成功")) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),msg);
		} else {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(),msg);
		}
	}
	
	/**
	 * 保存发布的数据信息
	 * @param dataServer
	 * @return
	 */
	@ApiOperation(value="服务发布",notes="发布当前的服务")
	@RequestMapping(value="/publishServerData",method=RequestMethod.POST)
	public ReponseEntity<String> publishServerData(HttpServletRequest request,@RequestBody @ApiParam(value="地图模型",type="dataServerEntity",name="dataServer") DataServerEntity dataServer) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.singlePublishServerData(dataServer,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),"发布成功");
	}

	/**
	 * 保存发布的rest数据信息
	 * @param dataServer
	 * @return
	 */
	@ApiOperation(value="服务发布",notes="发布当前的rest服务")
	@RequestMapping(value="/publishRestData",method=RequestMethod.POST)
	public ReponseEntity<String> publishRestData(HttpServletRequest request,@RequestBody @ApiParam(value="REST",type="dataServerEntity",name="dataServer") DataServerEntity dataServer) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.singlePublishRestData(dataServer,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),"发布成功");
	}
	
	/**
	 * 获取当前已经发布的数据
	 * TODO 暂时不预留分页，分页由前台处理,后期数据如果增多，考虑查询分页和检索
	 * @return
	 * @throws IOException 
	 */
	@ApiOperation(value="查询服务数据",notes="获取服务数据列表信息")
	@ApiImplicitParam(name="queryCondition",value="查询已发布服务条件",required=false,paramType="body")
	@RequestMapping(value="/queryServerData",method=RequestMethod.POST)
	public ReponseEntity<List<DataServerEntity>> queryServerData(@RequestBody(required=false) RequestQueryCondition queryCondition,
													HttpServletResponse rep,
													HttpServletRequest req) throws IOException{
		String reqIP = UserUtil.getRequestIp(req);
		int num = mapServerService.queryServerDataCount(queryCondition);
		if (num > 0) {
			List<DataServerEntity> list = mapServerService.queryServerDataList(queryCondition,reqIP);
			return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(), 
															 ReponseStatus.SUCCESS.getResult(), 
															 list, 
															 num);
		}
		return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(), 
														 ReponseStatus.SUCCESS.getResult(), 
													   	 new ArrayList<DataServerEntity>(),
														 num);
	}


	/**
	 * 获取当前已经发布的rest数据
	 * TODO 暂时不预留分页，分页由前台处理,后期数据如果增多，考虑查询分页和检索
	 * @return
	 * @throws IOException
	 */
	@ApiOperation(value="查询rest服务数据",notes="获取rest服务数据列表信息")
	@ApiImplicitParam(name="queryRestCondition",value="查询已发布rest服务条件",required=false,paramType="body")
	@RequestMapping(value="/queryRestData",method=RequestMethod.POST)
	public ReponseEntity<List<DataServerEntity>> queryRestData(@RequestBody(required=false) RequestQueryCondition queryCondition,
																 HttpServletResponse rep,
																 HttpServletRequest req) throws IOException{
		String reqIP = UserUtil.getRequestIp(req);
		int num = mapServerService.queryRestDataCount(queryCondition);
		if (num > 0) {
			List<DataServerEntity> list = mapServerService.queryRestDataList(queryCondition,reqIP);
			return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(),
					ReponseStatus.SUCCESS.getResult(),
					list,
					num);
		}
		return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(),
				ReponseStatus.SUCCESS.getResult(),
				new ArrayList<DataServerEntity>(),
				num);
	}
	
	/**
	 * 获取单个服务的信息
	 * 
	 * @return 返回服务信息
	 */
	@ApiOperation(value="查询单个服务数据",notes="查询单个服务数据")
	@ApiImplicitParam(name="serverId",value="服务id",required=true,paramType="path")
	@RequestMapping(value="/server/{serverId}",method=RequestMethod.GET)
	public ReponseEntity<DataServerEntity> queryServerById(@PathVariable int serverId){
		DataServerEntity data = mapServerService.queryServerInfoById(serverId);
		return new ReponseEntity<DataServerEntity>(ReponseStatus.SUCCESS.getCode(), 
												   ReponseStatus.SUCCESS.getResult(), 
												   data);
	}
	
	
	/**
	 * 编辑服务
	 * @param dataServer 当前的服务数据
	 * @return 返回编辑状态
	 */
	@ApiOperation(value="服务编辑",notes="编辑当前的地图服务")
	@ApiImplicitParam(name="dataServer",value="服务数据",required=true,paramType="body")
	@RequestMapping(value="/editServerData",method=RequestMethod.POST)
	public ReponseEntity<String> editServerData(HttpServletRequest request,
										@RequestBody DataServerEntity dataServer) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.editServerData(dataServer,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
								 ReponseStatus.SUCCESS.getResult());
	}

	/**
	 * 编辑rest服务
	 * @param dataServer 当前的服务数据
	 * @return 返回编辑状态
	 */
	@ApiOperation(value="rest服务编辑",notes="编辑当前的rest服务")
	@ApiImplicitParam(name="dataRestServer",value="服务数据",required=true,paramType="body")
	@RequestMapping(value="/editRestData",method=RequestMethod.POST)
	public ReponseEntity<String> editRestData(HttpServletRequest request,
												@RequestBody DataServerEntity dataServer) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.editRestData(dataServer,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
				ReponseStatus.SUCCESS.getResult());
	}
	
	/**
	 * 单个删除
	 * @param id 服务id
	 * @return 返回删除状态
	 */
	@ApiOperation(value="服务删除",notes="删除单个服务")
	@ApiImplicitParam(name="id",value="服务id",required=true,dataType="int",paramType="path")
	@RequestMapping(value="/deleteServerData/{id}",method=RequestMethod.GET)
	public ReponseEntity<String> deleteServerData(HttpServletRequest request,@PathVariable int id) {
		String reqIP = UserUtil.getRequestIp(request);
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(id);
		mapServerService.deleteServerData(ids,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功!");
	}
	/**
	 * 单个删除rest服务
	 * @param id 服务id
	 * @return 返回删除状态
	 */
	@ApiOperation(value="rest服务删除",notes="删除单个rest服务")
	@ApiImplicitParam(name="id",value="服务id",required=true,dataType="int",paramType="path")
	@RequestMapping(value="/deleteRestData/{id}",method=RequestMethod.GET)
	public ReponseEntity<String> deleteRestData(HttpServletRequest request,@PathVariable int id) {
		String reqIP = UserUtil.getRequestIp(request);
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(id);
		mapServerService.deleteRestData(ids,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功!");
	}

	
	
	/**
	 * 删除服务数据批量 
	 * @param ids 服务ids
	 * @return  返回删除状态
	 */
	@ApiOperation(value="服务删除",notes="批量删除多个服务")
	@RequestMapping(value="/deleteServerDatas",method=RequestMethod.POST)
	public ReponseEntity<String> deleteServerDatas(HttpServletRequest request,@RequestBody @ApiParam(value="id集合",type="int") List<Integer> ids) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.deleteServerData(ids,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功!");
	}

	/**
	 * 删除rest服务数据批量
	 * @param ids 服务ids
	 * @return  返回删除状态
	 */
	@ApiOperation(value="rest服务删除",notes="批量删除多个rest服务")
	@RequestMapping(value="/deleteRestDatas",method=RequestMethod.POST)
	public ReponseEntity<String> deleteRestDatas(HttpServletRequest request,@RequestBody @ApiParam(value="id集合",type="int") List<Integer> ids) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.deleteRestData(ids,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "删除成功!");
	}
	
	/**
	 * 获取当前服务id的状态
	 * @return 发挥
	 */
	@ApiOperation(value="查询服务状态",notes="查询服务状态")
	@ApiImplicitParam(name="ids",value="服务id",required=true,dataType="int",paramType="body")
	@PostMapping("/getServerStatus")
	public ReponseEntity<List<Integer>> getServerStatusByIds(@RequestBody List<Integer> ids){
		List<Integer> list = mapServerService.getServerStatusByIds(ids);
		return new ReponseEntity<List<Integer>>(ReponseStatus.SUCCESS.getCode(), 
											    ReponseStatus.SUCCESS.getResult(), 
											    list);
	}
	
	
	/**
	 * 启动、暂停服务
	 * @param serverId  服务id
	 * @return 返回更改服务状态
	 */
	@ApiOperation(value="服务启停",notes="服务的启动和暂停")
	@ApiImplicitParam(name="serverId",value="服务id",required=true,dataType="int",paramType="path")
	@RequestMapping(value="/updateServerStatus/{serverId}",method=RequestMethod.GET)
	public ReponseEntity<String> updateServerStatus(HttpServletRequest request,@PathVariable int serverId) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.updateServerStatus(serverId,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
								 		  "更新成功!");
	}

	/**
	 * 启动、暂停rest服务
	 * @param serverId  服务id
	 * @return 返回更改服务状态
	 */
	@ApiOperation(value="rest服务启停",notes="rest服务的启动和暂停")
	@ApiImplicitParam(name="serverId",value="rest服务id",required=true,dataType="int",paramType="path")
	@RequestMapping(value="/updateRestStatus/{serverId}",method=RequestMethod.GET)
	public ReponseEntity<String> updateRestStatus(HttpServletRequest request,@PathVariable int serverId) {
		String reqIP = UserUtil.getRequestIp(request);
		mapServerService.updateRestStatus(serverId,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
				"更新成功!");
	}
	
	/**
	 * 获取服务请求的基础url
	 * @param req 请求
	 * @return
	 */
	@ApiOperation(value="公共url获取",notes="获取服务的公共url")
	@RequestMapping(value="/queryServerUrl",method=RequestMethod.GET)
	public String queryServerUrl(HttpServletRequest req) {
		StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://").append(req.getServerName()).append(":").append(req.getServerPort())
        .append(req.getContextPath()).append("/");
        return sb.toString();
	}
	
	/**
	 * 获取服务协议信息
	 * @return
	 */
	@ApiOperation(value="获取服务协议信息",notes="获取服务协议信息")
	@RequestMapping(value="/queryServerProtocolInfo",method=RequestMethod.GET)
	public ReponseEntity<List<ServerProtocolInfo>> queryServerProtocolInfo(){
		List<ServerProtocolInfo> list = mapServerService.queryServerProtocolInfo();
		return new ReponseEntity<List<ServerProtocolInfo>>(ReponseStatus.SUCCESS.getCode(), 
													 ReponseStatus.SUCCESS.getResult(),
													 list);
	}
	
	/**
	 * 获取当前文件的bbox
	 * @param fileUrl
	 * @return
	 */
	@ApiOperation(value="获取数据bbox",notes="获取当前数据的bbox")
	@ApiImplicitParams({
		@ApiImplicitParam(name="fileUrl",value="文件url",required=true,paramType="query"),
		@ApiImplicitParam(name="fileType",value="文件类型",required=true,paramType="query")
	})
	@RequestMapping(value="/queryBbox",method=RequestMethod.GET)
	public BboxInfoEntity queryBboxValue(@RequestParam("fileUrl") String fileUrl,
								 @RequestParam("fileType") String fileType,
								 HttpServletRequest request) {
		String reqIP = UserUtil.getRequestIp(request);
		return mapServerService.queryBboxFromFile(fileUrl,fileType,null,reqIP);
	}

	/**
	 * 修改图层本地缓存
	 * @param layerInfo
	 * @return
	 */
	@ApiOperation(value="修改图层本地缓存",notes="修改图层本地缓存")
	@ApiImplicitParams({
		@ApiImplicitParam(name="layerInfoStr",value="图形信息",required=true,paramType="body"),
		@ApiImplicitParam(name="isAdd",value="是否加载",required=true,paramType="query")
	})
	@RequestMapping(value="/cacheInfo",method=RequestMethod.POST)
	public ReponseEntity<String> modifyCacheInfo(@RequestBody byte[] layerInfoStr,
												 @RequestParam("isAdd") boolean isAdd){
		//反序列化为layerInfo
		LayerInfo layerInfo = (LayerInfo) redisTemplate.getValueSerializer().deserialize(layerInfoStr);
		mapServerService.modifyCacheInfo(layerInfo, isAdd);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
	 									 ReponseStatus.SUCCESS.getResult());
	}
	
	
	/**
	 * 发送切片已完成消息
	 * @param layerInfo
	 * @return
	 */
	@ApiOperation(value="发送切片已完成消息",notes="发送切片已完成消息")
	@ApiImplicitParam(name="msg",value="消息",required=true,paramType="query")
	@RequestMapping(value="/notice/tile",method=RequestMethod.GET)
	public void noticeTiledInfo(@RequestParam("msg") String msg){
		Map<String,Object> msgMap = new HashMap<String,Object>();
		msgMap.put("msg", msg);
		msgMap.put("msgKey", "mapServer");
		JSONObject jsonObject = new JSONObject(msgMap);
		webSocketManager.sendMessage(jsonObject.toJSONString());
	}
	
	/**
	 * 查询所有服务总数
	 * @return
	 */
	@ApiOperation(value="查询服务数量",notes="查询服务数量")
	@GetMapping("/server")
	public ReponseEntity<List<DataServerEntity>> getServerEntityForWeb(HttpServletRequest request){
		String reqIP = UserUtil.getRequestIp(request);
		int num = mapServerService.queryServerDataCount(null);
		if (num > 0) {
			List<DataServerEntity> list = mapServerService.queryServerDataList(null,reqIP);
			sortDataServerByProctol(list);
			//根据规则排序
			list = list.stream().sorted((server1,server2) ->{
				return server1.getCurrentLevel() - server2.getCurrentLevel();
			}).collect(Collectors.toList());
			return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(), 
															 ReponseStatus.SUCCESS.getResult(), 
															 list, 
															 num);
		}
		return new ReponseEntity<List<DataServerEntity>>(ReponseStatus.SUCCESS.getCode(), 
														 ReponseStatus.SUCCESS.getResult(), 
													   	 new ArrayList<DataServerEntity>(),
														 num);
		
	}
	
	/**
	 * 设置最优级别和排序规则的顺序
	 * @param list
	 */
	private void sortDataServerByProctol(List<DataServerEntity> list) {
		list.stream().forEach((serverData) -> {
			String[] versions = serverData.getPublisedProctol().split(";");
			boolean hasWmts = false;
			boolean hasTms = false;
			boolean hasWms = false;
			boolean hasWfs = false;
			for (String version : versions) {
				//如果有wfs，则优先级组考最后
				if ("wmts".equalsIgnoreCase(version)) {
					hasWmts = true;
				} else if("tms".equalsIgnoreCase(version)) {
					hasTms = true;
				} else if("wms".equalsIgnoreCase(version)) {
					hasWms = true;
				} else if ("wfs".equalsIgnoreCase(version)) {
					hasWfs = true;
				}
			}
			if (hasWfs) {
				//设置最后
				serverData.setCurrentLevel(3);   //3表示排序的顺序  按照从小到大依次是 0 wmts 1 tms 2 wms 3 wfs 4 wcs
				if (hasWmts) {
					serverData.setOptimalServerName("wmts");
				}else if(hasTms) {
					serverData.setOptimalServerName("tms");
				}else if(hasWms) {
					serverData.setOptimalServerName("wms");
				} else if (hasWfs) {
					serverData.setOptimalServerName("wfs");
				} else {
					serverData.setOptimalServerName("wcs");
				}
			}else if(hasWmts) {
				//推荐协议
				serverData.setOptimalServerName("wmts");
				serverData.setCurrentLevel(0);
			} else if (hasTms) {
				serverData.setOptimalServerName("tms");
				serverData.setCurrentLevel(1);
			} else if (hasWms) {
				serverData.setOptimalServerName("wms");
				serverData.setCurrentLevel(2);
			} else {
				serverData.setOptimalServerName("wcs");
				serverData.setCurrentLevel(4);
			}
		});
	}
	
	/**
	 * 图层数据发布
	 * @param layerGroupInfos
	 * @return
	 */
	@PostMapping("/group")
	@ApiOperation(value="图层组发布",notes="发布图层组服务")
	public ReponseEntity<String> publishLayerGroup(HttpServletRequest request,@RequestBody @ApiParam(value="地图模型",type="dataServerEntity",name="dataServer") DataServerEntity layerGroupInfos){
		if (StringUtils.isBlank(layerGroupInfos.getServerName()) || StringUtils.isBlank(layerGroupInfos.getLayerName())) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(), ReponseStatus.ERROR.getResult(),"服务名称或图层名称不能为空！");
		}
		if (CollectionUtils.isEmpty(layerGroupInfos.getLayerGroups())) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), ReponseStatus.ERROR.getResult(),"请选择需要进行编组的图层！");
		}
		//获取缩略图的文件位置信息
		String reqIP = UserUtil.getRequestIp(request);
		layerGroupServerService.publishedLayerGroup(layerGroupInfos,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), MessageFormat.format("图层组{0}发布成功！", layerGroupInfos.getServerName()));
	}
	
	/**
	 * 图层数据更新
	 * @param layerGroupInfos
	 * @return
	 */
	@PutMapping("/group")
	@ApiOperation(value="图层组编辑",notes="编辑当前的图层组信息")
	public ReponseEntity<String> updateLayerGroup(HttpServletRequest request,@RequestBody @ApiParam(value="地图模型",type="dataServerEntity",name="dataServer") DataServerEntity layerGroupInfos){
		String reqIP = UserUtil.getRequestIp(request);
		layerGroupServerService.publishedLayerGroup(layerGroupInfos,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), MessageFormat.format("图层组{0}更新成功！", layerGroupInfos.getServerName()));
	}
	
	/**
	 * 预览图层组
	 * @param layerGroups 图层组信息
	 * @param layerName 图层名称
	 * @return
	 */
	@ApiOperation(value = "预览图层组",notes="预览图层组")
	@ApiImplicitParams({
		@ApiImplicitParam(name="layerGroups",value="图形组信息",required=true,paramType="body"),
		@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="path")
	})
	@PostMapping("/preview/{layerName}")
	public ReponseEntity<String> previewLayerGroup(HttpServletRequest request, @RequestBody List<LayerGroup> layerGroups,@PathVariable(value="layerName") String layerName){
		String reqIP = UserUtil.getRequestIp(request);
		layerGroupServerService.createLayerGroup(layerName, layerGroups, 0,null,reqIP);  //预览当前的服务id 0
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult());
	}
	
	/**
	 * 查询图层组名称 删除某个图层时，将图形组名称作为提示信息
	 * @param layerNames
	 * @return
	 */
	@ApiOperation(value = "查询图层组名称",notes="根据某个图层信息查询图层组名称")
	@ApiImplicitParam(name="layerNames",value="图层名称",required=true,paramType="body")
	@PostMapping("/group/queryLayerName")
	public ReponseEntity<Map<String,List<String>>> queryLayerGroupNameByLayerInfo(@RequestBody List<String> layerNames){
		Map<String,List<String>> groupsNames = layerGroupServerService.queryLayerGroupNameByLayerInfo(layerNames);
		return new ReponseEntity<Map<String,List<String>>>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),groupsNames);
	}
	
	/**
	 * 对外图层组发布
	 * @param layerGroups
	 * @return
	 */
	@ApiOperation(value = "远程图层聚合服务",notes="将多个远程服务聚合成图层组")
	@ApiImplicitParam(name="layerGroups",value="图层信息",required=true,paramType="body")
	@PostMapping("/exgroup")
	public ReponseEntity<String> publishedLayerGroup(HttpServletRequest request,@RequestBody @ApiParam(value="图层组信息",type="layerGroup",name="layerGroups") List<LayerGroup> layerGroups){
		String reqIP =UserUtil.getRequestIp(request);
		layerGroupServerService.publishedLayerGroup(layerGroups,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult());
	}
	
	@ApiOperation(value = "查询高程图层",notes="查询高程图层")
	@GetMapping("/layer/dem")
	public ReponseEntity<List<String>> queryLayerDem(){
		List<String> list = mapServerService.getLayerNameByDataType(2);
		return new ReponseEntity<List<String>>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),list); 
	}

	/**
	 * 停止生成金字塔缓存
	 *
	 * @param req req
	 * @param serverName 服务名称
	 * @return ReponseEntity
	 */
	@ApiOperation(value = "停止生成金字塔缓存", notes="停止生成金字塔缓存")
	@ApiImplicitParam(name="serverName",value="图层名称",required=true,paramType="path")
	@GetMapping("/stop/overview/{serverId}")
	public ReponseEntity<?> stopOverView(HttpServletRequest req, @PathVariable int serverId){
		String reqIP =UserUtil.getRequestIp(req);
		mapServerService.stopOverView(serverId, reqIP);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), "停止成功!", true);
	}
	
	
	/**
	 * 获取可分析的矢量图层
	 * @return
	 */
	@GetMapping("/vector/layer")
	public ReponseEntity<List<String>> queryVectorAnalysisLayer(){
		return new ReponseEntity<List<String>>(ReponseStatus.SUCCESS.getCode(), "查询成功!", analysisLayerServerService.queryAnalysisVectorLayer());
	}

	@ApiOperation(value="系统服务性能统计",notes="获取当前时间段内系统的响应性能")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "resourceSrs", value = "投影坐标", required = true, paramType = "query"),
			@ApiImplicitParam(name = "maxlevel", value = "服务最大级别", required = true, paramType = "query")
	})
	@RequestMapping(value="/preview/tileSize",method=RequestMethod.GET)
	public ReponseEntity<Map<String, Object>> queryPreviewParameter(@RequestParam(value="resourceSrs", required = true) String resourceSrs,
															 @RequestParam(value="maxlevel", required = true) int maxlevel){
		Map<String, Object> resultMap = mapServerService.queryPreviewParameter(resourceSrs, maxlevel);
		if (MapUtils.isNotEmpty(resultMap)) {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), "查询成功!", resultMap);
		} else {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "查询参数异常!", Collections.emptyMap());
		}
	}

	@ApiOperation(value = "获取OGC协议信息", notes="获取OGC协议信息")
	@ApiImplicitParams({
			@ApiImplicitParam(name="protocol",value="协议",required=true,paramType="path"),
			@ApiImplicitParam(name="serverName",value="服务名称",required=true,paramType="path")
	})
	@GetMapping("/ogcProtocol/{protocol}/{serverName}")
	public ReponseEntity<Map<String, Object>> queryOgcProtocolInfo(HttpServletRequest req, @PathVariable(value = "protocol", required = true) String protocol,
												 @PathVariable(value = "serverName", required = true) String serverName) {
		String reqIp =UserUtil.getRequestIp(req);
		Map<String, Object> resultMap = mapServerService.queryOgcProtocolInfo(reqIp, protocol, serverName);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), "查询成功!", resultMap);
	}
}
