package org.pygisserver.web.aop;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.MapServerBatchInfoEntity;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @Description: 地图服务service切片类
 * @author:  
 * @date: 2018-11-22
 */
@Component
@Aspect
public class MapServerAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.MapServerController.publishServerData(..)) "
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.publishedServerDataList(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.publishLayerGroup(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.updateLayerGroup(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.publishedLayerGroup(..))"
			+ "|| execution(* org.pygisserver.web.controller.ConfigController.checkPostGisConnect(..))"
			+ "|| execution(* org.pygisserver.web.controller.ConfigController.editPostGis(..))"
			+ "|| execution(* org.pygisserver.web.controller.ConfigController.deletePostgisInfo(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.deleteServer*(..)) "
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.editServerData(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.updateServerStatus(..))")
	private void mapServerService() {};
	
	@Pointcut(value="execution(* org.pygisserver.web.controller.MapServerController.deleteServer*(..))")
	private void delServerService() {};
	
	@Pointcut(value="execution(* org.pygisserver.web.controller.ConfigController.deletePostgisInfo(..))")
	private void delPostGISService() {};
	
	private ThreadLocal<String> delLayerName = new ThreadLocal<>();
	
	@Autowired
	private MapStatsticService mapStatsticService;
	
	@Autowired
	private MapServerMapper mapServerMapper;
	
	@Before("delServerService()")
	public void beforeDeleteServer(JoinPoint joinPoint) {
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		String methodName = signature.getName();
		List<Integer> serverIds = new ArrayList<Integer>();
		switch (methodName) {
			case "deleteServerData":
				int id = (int) param[1];
				serverIds.add(id);
				break;
			case "deleteServerDatas":
				List<Integer> ids = (List<Integer>) param[1];
				serverIds.addAll(ids);
				break;
		}
		List<String> serverNames = mapServerMapper.queryLayerNamesByServerIds(serverIds);
		String layerName = Arrays.toString(serverNames.toArray());
		//批量删除太多，导致操作日志内容字段超长
		if (layerName.length() > 100) {
			layerName = layerName.substring(0, 100);
			layerName = new StringBuilder(layerName).append("...").toString();
		}
		delLayerName.set(Arrays.toString(serverNames.toArray()));
	}
	
	@Before("delPostGISService()")
	public void beforeDeletePostGIS(JoinPoint joinPoint) {
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		String methodName = signature.getName();
		List<Integer> serverIds = new ArrayList<Integer>();
		switch (methodName) {
			case "deletePostgisInfo":
				List<Integer> ids = (List<Integer>) param[1];
				serverIds.addAll(ids);
				break;
		}
		List<String> serverNames = mapServerMapper.queryPostGISNamesById(serverIds);
		delLayerName.set(Arrays.toString(serverNames.toArray()));
	}
	
	/**
	 * 正常返回时，记录当前的操作日志
	 * TODO 需要处理返回值的问题
	 * @param joinPoint
	 */
	@AfterReturning("mapServerService()")
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
		case "publishServerData":
			DataServerEntity dataServer = (DataServerEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},成功发布了服务名为{1}的服务", userName,dataServer.getServerName()));
			break;
		case "publishedServerDataList":
			MapServerBatchInfoEntity infoEntity = (MapServerBatchInfoEntity) param[1];
			if (infoEntity.isEditGroup()) {
				operationLog.setContent(MessageFormat.format("用户{0},成功将{1}等服务发布为图层组", userName,infoEntity.getDataServer().get(0).getServerName()));
			} else {
				operationLog.setContent(MessageFormat.format("用户{0},成功批量发布了{1}等服务", userName,infoEntity.getDataServer().get(0).getServerName()));
			}
			break;
		case "publishLayerGroup":
			DataServerEntity groupEntity1 = (DataServerEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},成功发布了服务名为{1}的图层组服务", userName,groupEntity1.getLayerName()));
			break;
		case "updateLayerGroup":
			DataServerEntity groupEntity2 = (DataServerEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},编辑了服务名为{1}的图层组服务", userName,groupEntity2.getLayerName()));
			break;
		case "publishedLayerGroup":
			List<LayerGroup> layerGroups1 = (List<LayerGroup>) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},成功发布了{1}等远程图层聚合服务", userName,layerGroups1.get(0).getLayerName()));
			break;
		case "checkPostGisConnect":
			PostGisConnectEntity connectEntity = (PostGisConnectEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},创建了名称为{1}的PostGIS数据库连接", userName,connectEntity.getDataSourceName()));
			break;
		case "editPostGis":
			PostGisConnectEntity connectEntity1 = (PostGisConnectEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},编辑了名称为{1}的PostGIS数据库连接", userName,connectEntity1.getDataSourceName()));
			break;
		case "deletePostgisInfo":
			operationLog.setContent(MessageFormat.format("用户{0},删除了{1}等PostGIS数据库连接", userName,delLayerName.get()));
			delLayerName.remove();
			break;
		case "deleteServerData":
		case "deleteServerDatas":
			operationLog.setContent(MessageFormat.format("用户{0},删除了服务名为{1}的服务", userName,delLayerName.get()));
			delLayerName.remove();
			break;
		case "editServerData":
			DataServerEntity dataServer1 = (DataServerEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},编辑了服务名为{1}的服务", userName,dataServer1.getServerName()));
			break;
		case "updateServerStatus":
			int serverId = (int) param[1];
			DataServerEntity dataServer2 = mapServerMapper.queryServerInfoById(serverId);
			String operateFlag = dataServer2.getStatus() == 0 ? "暂停" : "开启";
			operationLog.setContent(MessageFormat.format("用户{0},{1}服务名为{2}的服务", userName,operateFlag,dataServer2.getServerName()));
			break;
		default:
			break;
		}
		mapStatsticService.saveOperationLog(operationLog);
	}
}
