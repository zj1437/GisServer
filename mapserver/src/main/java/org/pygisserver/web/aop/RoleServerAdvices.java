package org.pygisserver.web.aop;

import java.text.MessageFormat;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.web.dao.UserMapper;
import org.pygisserver.web.entity.RoleInfo;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Description: 角色service切片类
 * @author:
 * @date: 2018-11-22
 */
@Component
@Aspect
public class RoleServerAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.RoleManageController.editRoleInfo(..))")
	private void roleServerService() {};
	
	@Pointcut(value="execution(* org.pygisserver.web.controller.RoleManageController.editRoleInfo(..))")
	private void editRoleServer() {};
	
	private ThreadLocal<String> delRoleName = new ThreadLocal<String>();
	
	@Resource
	private MapStatsticService mapStatsticService;
	
	@Resource
	private UserMapper userMapper;
	
	
	@Before("editRoleServer()")
	public void beforeEditServer(JoinPoint joinPoint) {
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		String methodName = signature.getName();
		RoleInfo roleInfo = null;
		if(methodName.equals("editRoleInfo")) {
			roleInfo = (RoleInfo)param[0];
		}
		delRoleName.set(roleInfo.getRoleName());
	}
	
	/**
	 * 正常返回时，记录当前的操作日志
	 * TODO 需要处理返回值的问题
	 * @param joinPoint
	 */
	@AfterReturning("roleServerService()")
	public void afterReturningAdvices(JoinPoint joinPoint) {
		//获取参数集合
		// Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		//获取目标方法名称
		SysOperationLog operationLog = new SysOperationLog();
		// HttpServletRequest req = (HttpServletRequest) param[0];
		HttpServletRequest request =((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
		operationLog.setOperatorIP(UserUtil.getRequestIp(request));
		operationLog.setOperateTime(new Date());
		operationLog.setOperator(UserUtil.getUserName());
		String methodName = signature.getName();
		if(methodName.equals("editRoleInfo")) {
			operationLog.setContent(MessageFormat.format("管理员{0}编辑了{1}角色信息", UserUtil.getUserName(),delRoleName.get()));
		}
		mapStatsticService.saveOperationLog(operationLog);
	}
}
