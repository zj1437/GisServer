package org.pygisserver.web.aop;

import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.web.dao.ModelServerMapper;
import org.pygisserver.web.entity.AbnormalServerInfo;
import org.pygisserver.web.entity.ModelEntity;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: 模型服务service切片类
 * @author: 
 * @date: 2018-11-22
 */
@Component
@Aspect
public class ModelServerAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.ModelServerController.publishModel(..)) "
			+ "|| execution(* org.pygisserver.web.controller.ModelServerController.editModel(..)) "
			+ "|| execution(* org.pygisserver.web.controller.ModelServerController.deleteModel*(..))"
			+ "|| execution(* org.pygisserver.web.controller.ModelServerController.updateServerStatus(..))")
	private void modelServerService() {};
	
	@Pointcut(value="execution(* org.pygisserver.web.controller.ModelServerController.getModelDataReponse(..))")
	private void modelRequest() {};
	
	@Pointcut(value="execution(* org.pygisserver.web.controller.ModelServerController.deleteModel*(..))")
	private void delModelServer() {};
	
	private ThreadLocal<String> delServerName = new ThreadLocal<String>();
	
	@Autowired
	private MapStatsticService mapStatsticService;
	
	@Resource
	private ModelServerMapper modelServerMapper;

	@Before("delModelServer()")
	public void beforeDeleteServer(JoinPoint joinPoint) {
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		String methodName = signature.getName();
		switch (methodName) {
			case "deleteModelServer":
				int serverId = (int) param[1];
				ModelEntity modelEntity = modelServerMapper.queryModelEntityById(serverId);
				delServerName.set(modelEntity.getServerName());
				break;
			case "deleteModelServers":
				List<Integer> serverIds = (List<Integer>) param[1];
				List<ModelEntity> modelEntitys = modelServerMapper.queryModelEntityByIds(serverIds);
				List<String> serverNames = new ArrayList<String>();
				for (ModelEntity Entity : modelEntitys) {
					serverNames.add(Entity.getServerName());
				}
				delServerName.set(Arrays.toString(serverNames.toArray()));
				break;
		}

	}
	
	
	/**
	 * 正常返回时，记录当前的操作日志
	 * TODO 需要处理返回值的问题
	 * @param joinPoint
	 */
	@AfterReturning("modelServerService()")
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
		String userName = UserUtil.getUserName();
		operationLog.setOperator(userName);
		String methodName = signature.getName();
		switch (methodName) {
		case "publishModel":
			ModelEntity dataServer = (ModelEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},成功发布了服务名为{1}的服务", userName,dataServer.getServerName()));
			break;
		case "editModel":
			ModelEntity modelServer = (ModelEntity) param[1];
			operationLog.setContent(MessageFormat.format("用户{0},编辑了服务名为{1}的服务", userName,modelServer.getServerName()));
			break;
		case "deleteModelServer":
			operationLog.setContent(MessageFormat.format("用户{0},删除了服务名为{1}的服务", userName,delServerName.get()));
			break;
		case "deleteModelServers":
			operationLog.setContent(MessageFormat.format("用户{0},删除了服务名为{1}的服务", userName,delServerName.get()));
			break;
		case "updateServerStatus":
			int id = (int) param[1];
			ModelEntity entity = modelServerMapper.queryModelEntityById(id);
			String operateFlag = entity.getStatus() == 0 ? "暂停" : "开启";
			operationLog.setContent(MessageFormat.format("用户{0},{1}服务名为{2}的服务", userName,operateFlag,entity.getServerName()));
			break;
		default:
			break;
		}
		mapStatsticService.saveOperationLog(operationLog);
	}
	
	/**
	 * 异常返回时，记录异常状态
	 * @param joinPoint
	 */
	@AfterThrowing(throwing="ex",pointcut="modelRequest()")
	public void modelReqAfterThrowing(JoinPoint joinPoint,Throwable ex) {
		Object[] params = joinPoint.getArgs();
		//服务访问完毕后，处理访问请求记录
		AbnormalServerInfo abnormalInfo = new AbnormalServerInfo();
		abnormalInfo.setServerName(params[1].toString());
		abnormalInfo.setServerType(1);
		abnormalInfo.setAbnormalTime(new Date());
		String reson = "";
		if (ex instanceof FileNotFoundException) {
			reson = "该模型产品在当前路径找不到";
			abnormalInfo.setAbnormalReason(reson);
			mapStatsticService.saveAbnormalServerInfo(abnormalInfo);
			//记录严重的日志
			SysOperationLog operationLog = new SysOperationLog();
			HttpServletRequest req = (HttpServletRequest) params[0];
			operationLog.setOperatorIP(UserUtil.getRequestIp(req));
			operationLog.setOperateTime(new Date());
			operationLog.setOperator(UserUtil.getUserName());
			operationLog.setContent(MessageFormat.format("模型服务{0}对应的数据路径已经变化，当前服务可能不能正常使用", params[1].toString()));
			mapStatsticService.saveOperationLog(operationLog);
		}
	}
}
