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
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: 地图服务配置相关切片类
 * @author:  
 * @date: 2018-11-22
 */
@Component
@Aspect
public class PYGISServerConfigAdvices {
	
	/**
	 * 该AOP中只处理了自动发布配置、路径配置、默认提供商、服务授权四个编辑。
	 * 由于切图设置是在TileCacheController接口中，因此在其对应的AOP中做了处理。数据库配置的处理也是基于相同原因
	 */
	@Pointcut(value="execution(* org.pygisserver.web.controller.ConfigController.saveServerAutoConfig(..)) "
			+ "|| execution(* org.pygisserver.web.controller.ConfigController.updateSystemConfig(..))"
			+ "|| execution(* org.pygisserver.web.controller.ConfigController.saveServerProvider(..))"
			+ "|| execution(* org.pygisserver.web.controller.ConfigController.activeSysTime(..))")
	private void pyGISServerConfigService() {};
	
	@Autowired
	private MapStatsticService mapStatsticService;
	
	/**
	 * 正常返回时，记录当前的操作日志
	 * TODO 需要处理返回值的问题
	 * @param joinPoint
	 */
	@AfterReturning("pyGISServerConfigService()")
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
		case "saveServerAutoConfig":
			operationLog.setContent(MessageFormat.format("用户{0},编辑了系统自动服务发布设置", userName));
			break;
		case "updateSystemConfig":
			operationLog.setContent(MessageFormat.format("用户{0},修改服务文件系统配置", userName));
			break;
		case "saveServerProvider":
			operationLog.setContent(MessageFormat.format("用户{0},编辑了系统服务提供商信息", userName));
			break;
		case "activeSysTime":
			operationLog.setContent(MessageFormat.format("用户{0},激活了当前服务系统", userName));
			break;
		default:
			break;
		}
		mapStatsticService.saveOperationLog(operationLog);
	}
}
