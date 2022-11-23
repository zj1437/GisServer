package org.pygisserver.web.aop;

import java.text.MessageFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.pygisserver.web.service.TileCacheService;

/**
 * @Description: 地图服务service切片类
 * @author: 
 * @date: 2018-11-22
 */
@Component
@Aspect
public class TileServerAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.TileCacheController.manualSeedTaskList(..)) "
			+ "|| execution(* org.pygisserver.web.controller.TileCacheController.saveSeedTileConfig(..)) "
			+ "|| execution(* org.pygisserver.web.controller.TileCacheController.flushTiledInfo(..))"
			+ "|| execution(* org.pygisserver.web.controller.TileCacheController.stopTileTask(..)) "
			+ "|| execution(* org.pygisserver.web.controller.TileCacheController.deleteTileTask(..))"
			+ "|| execution(* org.pygisserver.web.controller.TileCacheController.activeTileTask(..))")
	private void tileServerService() {};
	
	@Autowired
	private MapStatsticService mapStatsticService;
	
	@Autowired
	private TileCacheService tileCacheService;
	
	/**
	 * 正常返回时，记录当前的操作日志
	 * TODO 需要处理返回值的问题
	 * @param joinPoint
	 */
	@AfterReturning("tileServerService()")
	public void afterReturningAdvices(JoinPoint joinPoint) {
		//获取参数集合
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		//获取目标方法名称
		SysOperationLog operationLog = new SysOperationLog();
		HttpServletRequest req = (HttpServletRequest) param[0];
		operationLog.setOperatorIP(UserUtil.getRequestIp(req));
		operationLog.setOperateTime(new Date());
		//获取当前的操作人
		Subject subject = SecurityUtils.getSubject();
		String userName = (String) subject.getPrincipal();
		//表示未登录
		if (StringUtils.isBlank(userName)) {
			return;
		}
		operationLog.setOperator(userName);
		String methodName = signature.getName();
		switch (methodName) {
		case "manualSeedTaskList":
			SeedTaskInstance seedParam = (SeedTaskInstance) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},开启了服务名为{1}的切图服务", userName,seedParam.getLayerName()));
			break;
		case "saveSeedTileConfig":
			operationLog.setContent(MessageFormat.format("用户{0},编辑了系统默认切图配置信息", userName));
			break;
		case "flushTiledInfo":
			String layerName = (String) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},清除了服务名为{1}的服务的切图缓存", userName,layerName));
			break;
		case "stopTileTask":
			String layerName1 = tileCacheService.queryTaskInstance((String) param[1]).getLayerName();
			operationLog.setContent(MessageFormat.format("用户{0},暂停了服务名为{1}的服务的切图任务", userName,layerName1));
			break;
		case "deleteTileTask":
			String layerName2 = tileCacheService.queryTaskInstance((String) param[1]).getLayerName();
			operationLog.setContent(MessageFormat.format("用户{0},删除了切图任务名为{1}的服务的切图任务", userName,layerName2));
			break;
		case "activeTileTask":
			String layerName3 = tileCacheService.queryTaskInstance((String) param[1]).getLayerName();
			operationLog.setContent(MessageFormat.format("用户{0},激活了切图任务名为{1}的服务的切图任务", userName,layerName3));
			break;
		default:
			break;
		}
		mapStatsticService.saveOperationLog(operationLog);
	}
}
