package org.pygisserver.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.AbnormalServerInfo;
import org.pygisserver.web.entity.ServerStatisticinfo;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.entity.SysResourceRecord;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 服务统计controller 
 * @author:  
 * @date: 2018-11-26
 */
@Api(tags="统计服务")
@RestController
@RequestMapping("/statstic")
public class ServerStatisticController {

	@Resource
	private MapStatsticService mapStatsticService;
	
	/**
	 * 访问量
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@ApiOperation(value="服务访问数量统计",notes="获取当前时间段内服务的访问数量")
	@ApiImplicitParams({
		@ApiImplicitParam(name="startTime",value="起始时间",required=false,paramType="query"),
		@ApiImplicitParam(name="endTime",value="结束时间",required=false,paramType="query")
	})
	@RequestMapping(value="/visitNum",method=RequestMethod.GET)
	public ReponseEntity<List<ServerStatisticinfo>> getServerVisitNum(@RequestParam(value="startTime",required=false) String startTime,
																	  @RequestParam(value="endTime",required=false) String endTime){
		List<ServerStatisticinfo> list = mapStatsticService.queryServerVisitNum(startTime, endTime);
		return new ReponseEntity<List<ServerStatisticinfo>>(ReponseStatus.SUCCESS.getCode(), 
															ReponseStatus.SUCCESS.getResult(), 
															list, 
															list.size());
	}
	
	/**
	 * 系统性能
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@ApiOperation(value="系统服务性能统计",notes="获取当前时间段内系统的响应性能")
	@ApiImplicitParams({
		@ApiImplicitParam(name="filed", value="图表字段", required=false, paramType="query"),
		@ApiImplicitParam(name="startTime", value="起始时间", required=false, paramType="query"),
		@ApiImplicitParam(name="endTime", value="结束时间", required=false, paramType="query"),
		@ApiImplicitParam(name="sort", value="排序方式", required=false, paramType="query")
	})
	@RequestMapping(value="/efficient",method=RequestMethod.GET)
	public ReponseEntity<List<ServerStatisticinfo>> getServerEfficient(@RequestParam(value="filed", required=false) String filed,
																	   @RequestParam(value="startTime", required=false) String startTime,
											  						   @RequestParam(value="endTime", required=false) String endTime,
																	   @RequestParam(value="sort", required=false) String sort) {
		if (!StringUtils.equalsAny(filed, "visitCount", "avgResponse", "concurrentCount", "ogc")) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "请求参数异常!");
		}
		List<ServerStatisticinfo> list = mapStatsticService.queryServerPerformance(filed, startTime, endTime, sort);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), list, list.size());
	}
	
	/**
	 * 系统资源获取
	 * @return
	 */
	@ApiOperation(value="系统资源访问统计",notes="获取系统资源访问记录")
	@RequestMapping(value="/resource",method=RequestMethod.GET)
	public ReponseEntity<List<SysResourceRecord>> getSysResource(){
		List<SysResourceRecord> list = mapStatsticService.querySysResourceRecords();
		return new ReponseEntity<List<SysResourceRecord>>(ReponseStatus.SUCCESS.getCode(), 
														  ReponseStatus.SUCCESS.getResult(), 
														  list, 
													   	  list.size());
	}
	
	/**
	 * 异常资源获取
	 * @param startTime 起始时间
	 * @param endTime 结束时间
	 * @return 返回服务异常信息
	 */
	@ApiOperation(value="服务异常信息统计",notes="获取当前服务的异常信息")
	@ApiImplicitParams({
		@ApiImplicitParam(name="startTime",value="起始时间",required=false,paramType="query"),
		@ApiImplicitParam(name="endTime",value="结束时间",required=false,paramType="query")
	})
	@RequestMapping(value="/abnormal",method=RequestMethod.GET)
	public ReponseEntity<List<AbnormalServerInfo>> getAbnormalInfo(@RequestParam(value="startTime",required=false) String startTime,
																   @RequestParam(value="endTime",required=false) String endTime){
		List<AbnormalServerInfo> list = mapStatsticService.queryAbnormalServerInfo(startTime,endTime);
		return new ReponseEntity<List<AbnormalServerInfo>>(ReponseStatus.SUCCESS.getCode(), 
														  ReponseStatus.SUCCESS.getResult(), 
														  list, 
													   	  list.size());
	}
	
	/**
	 *  服务状态统计
	 * @return
	 */
	@ApiOperation(value="服务状态统计",notes="获取当前不同状态的服务数量")
	@RequestMapping(value="/serverStatus",method=RequestMethod.GET)
	public ReponseEntity<List<ServerStatisticinfo>> serverStatusStatstic(){
		List<ServerStatisticinfo> list = mapStatsticService.serverStatusStatstic();
		return new ReponseEntity<List<ServerStatisticinfo>>(ReponseStatus.SUCCESS.getCode(), 
															ReponseStatus.SUCCESS.getResult(), 
															list, 
															list.size());
	}
	
	/**
	 *  不同服务协议访问量统计
	 * @return
	 */
	@ApiOperation(value="服务协议访问量统计",notes="不同服务协议访问量统计")
	@RequestMapping(value="/serverType",method=RequestMethod.GET)
	public ReponseEntity<List<ServerStatisticinfo>> serverTypeStatstic(){
		List<ServerStatisticinfo> list = mapStatsticService.serverTypeStatstic();
		return new ReponseEntity<List<ServerStatisticinfo>>(ReponseStatus.SUCCESS.getCode(), 
															ReponseStatus.SUCCESS.getResult(), 
															list, 
															list.size());
	}
	
	/**
	 * 获取服务的操作日志
	 * @return
	 */
	@ApiOperation(value="服务操作日志获取",notes="获取当前服务的操作信息")
	@ApiImplicitParams({
		@ApiImplicitParam(name="startTime",value="起始时间",required=false,paramType="query"),
		@ApiImplicitParam(name="endTime",value="结束时间",required=false,paramType="query")
	})
	@RequestMapping(value="/operatelog",method=RequestMethod.POST)
	public ReponseEntity<List<SysOperationLog>> getServerOperateLogInfo(@RequestBody(required=false) RequestQueryCondition queryEntity){
		int num = mapStatsticService.queryOperationLogCount(queryEntity);
		if (num > 0) {
			List<SysOperationLog> list = mapStatsticService.querySysOprationLog(queryEntity);
			return new ReponseEntity<List<SysOperationLog>>(ReponseStatus.SUCCESS.getCode(), 
															ReponseStatus.SUCCESS.getResult(), 
															list, 
															num);
		}
		return new ReponseEntity<List<SysOperationLog>>(ReponseStatus.SUCCESS.getCode(), 
				 ReponseStatus.SUCCESS.getResult(), 
			   	 new ArrayList<SysOperationLog>(),
				 num);
		
	}
	
	@ApiOperation(value="查询系统日志",notes="查询系统日志")
	@ApiImplicitParam(name="queryCondition",value="查询系统日志条件",required=false,paramType="body")
	@RequestMapping(value="/querySystemLog",method=RequestMethod.POST)
	public ReponseEntity<List<SystemLogEntity>> querySystemLog(@RequestBody(required=false) RequestQueryCondition queryEntity) {
		int num = mapStatsticService.querySystemLogCount(queryEntity);
		if (num > 0) {
			List<SystemLogEntity> list = mapStatsticService.querySystemLogs(queryEntity);
			return new ReponseEntity<List<SystemLogEntity>>(ReponseStatus.SUCCESS.getCode(), 
															 ReponseStatus.SUCCESS.getResult(), 
															 list, 
															 num);
		}
		return new ReponseEntity<List<SystemLogEntity>>(ReponseStatus.SUCCESS.getCode(), 
														 ReponseStatus.SUCCESS.getResult(), 
													   	 new ArrayList<SystemLogEntity>(),
														 num);
	}
	
}
