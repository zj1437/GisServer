package org.pygisserver.web.aop;

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
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.web.dao.UserMapper;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.stereotype.Component;

/**
 * @Description: 用户service切片类
 * @author: 
 * @date: 2018-11-22
 */
@Component
@Aspect
public class UserServerAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.UserController.addUserInfo(..)) ||"
			+ "execution(* org.pygisserver.web.controller.UserController.deleteUser*(..)) ||"
			+ "execution(* org.pygisserver.web.controller.UserController.lockUserAccount(..)) || "
			+ "execution(* org.pygisserver.web.controller.UserController.modifyUserPassword(..)) || "
			+ "execution(* org.pygisserver.web.controller.UserController.editUserInfo(..)) || "
			+ "execution(* org.pygisserver.web.controller.UserController.unlockUserAccount(..)) ")
	private void userServerService() {};
	
	@Pointcut(value="execution(* org.pygisserver.web.controller.UserController.deleteUserInfo*(..))")
	private void delUserServer() {};
	
	private ThreadLocal<String> delUserName = new ThreadLocal<String>();
	
	@Resource
	private MapStatsticService mapStatsticService;
	
	@Resource
	private UserMapper userMapper;
	
	
	@Before("delUserServer()")
	public void beforeDeleteServer(JoinPoint joinPoint) {
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		String methodName = signature.getName();
		List<Integer> userIds = new ArrayList<Integer>();
		switch (methodName) {
			case "deleteUserInfo":
				int userId = (int) param[1];
				userIds.add(userId);
				break;
			case "deleteUserInfos":
				List<Integer> ids = (List<Integer>) param[1];
				userIds.addAll(ids);
				break;
		}
		List<String> names = userMapper.queryUserNameByIds(userIds);
		delUserName.set(Arrays.toString(names.toArray()));
	}
	
	/**
	 * 正常返回时，记录当前的操作日志
	 * TODO 需要处理返回值的问题
	 * @param joinPoint
	 */
	@AfterReturning("userServerService()")
	public void afterReturningAdvices(JoinPoint joinPoint) {
		//获取参数集合
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		//获取目标方法名称
		SysOperationLog operationLog = new SysOperationLog();
		HttpServletRequest req = (HttpServletRequest) param[0];
		operationLog.setOperatorIP(UserUtil.getRequestIp(req));
		operationLog.setOperateTime(new Date());
		operationLog.setOperator(UserUtil.getUserName());
		String methodName = signature.getName();
		switch (methodName) {
		case "addUserInfo":
			UserInfo userInfo = (UserInfo) param[1];
			operationLog.setContent(MessageFormat.format("新增用户{0}", userInfo.getUserName()));
			break;
		case "editUserInfo":
			UserInfo userInfo1 = (UserInfo) param[1];
			operationLog.setContent(MessageFormat.format("用户{0}编辑了{1}用户信息", UserUtil.getUserName(),userInfo1.getUserName()));
			break;
		case "deleteUserInfo":
			operationLog.setContent(MessageFormat.format("管理员{0}删除了用户{1}", UserUtil.getUserName(),delUserName.get()));
			break;
		case "deleteUserInfos":
			operationLog.setContent(MessageFormat.format("管理员{0}删除了用户{1}", UserUtil.getUserName(),delUserName.get()));
			break;
		case "modifyUserPassword":
			operationLog.setContent(MessageFormat.format("管理员{0}重置了用户{1}密码", UserUtil.getUserName(),delUserName.get()));
			break;
		case "lockUserAccount":
			String userName = (String) param[1];
			operationLog.setContent(MessageFormat.format("用户{0}被锁住，将无法登录",userName));
			break;
		case "unlockUserAccount":
			String name = (String) param[1];
			operationLog.setContent(MessageFormat.format("用户{0}被解锁",name));
			break;
		default:
			break;
		}
		mapStatsticService.saveOperationLog(operationLog);
	}
}
