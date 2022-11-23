package org.pygisserver.web.controller;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.web.service.UserService;
import org.pygisserver.web.entity.LoginErrorInfo;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @Description: 登录控制类，包括判断当前服务
 * @author:  
 * @date: 2018-10-22
 */
@Api(tags="用户登录接口")
@RestController
	public class LoginController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;
	
	/**
	 * 存储登录用户信息  键值对 <userName，sessionId> 
	 */
	private static ConcurrentHashMap<String,Object> loginUserMap = new ConcurrentHashMap<String,Object>();
	
	
	private static String SYSTEMUSER;
	
	/**
	 * 系统登录
	 * @param userName
	 * @param passWord
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception 
	 */
	@ApiOperation(value="用户登录",notes="用户登录接口")
	@ApiImplicitParams({
		@ApiImplicitParam(name="userName",value="用户名",required=true,paramType="query"),
		@ApiImplicitParam(name="passWord",value="密码",required=true,paramType="query"),
		@ApiImplicitParam(name="rememberme",value="是否自动登录",required=true,paramType="query")
	})
	@RequestMapping(value="/dologin",method= RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<UserInfo> doLogin(String userName,
										   String passWord,
										   boolean rememberme,
										   HttpServletRequest request) {
		UsernamePasswordToken token = new UsernamePasswordToken(userName, passWord,rememberme);
		Subject subject = SecurityUtils.getSubject();
		try {
			//认证前查看当前用户账号是否被锁定
			int isLock = userService.queryUserStatus(userName);
			if (isLock == 0) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, 
						ExceptionsUtil.formatInfo(ExceptionsUtil.SYS_LOGIN_LOCK,userName), 
						userName,"系统安全", UserUtil.getRequestIp(request)));
				return new ReponseEntity<UserInfo>(ReponseStatus.ERROR.getCode(), "用户被锁定"); 
			}
			//登录认证
			subject.login(token);
			Session session = subject.getSession();
			Serializable sessionId = session.getId();
			/**
			 * 防止同一用户在不同客户端登录
			 */
			if (loginUserMap.containsKey(userName) && !loginUserMap.containsValue(sessionId)) {
				Serializable lastSessionId = (Serializable) loginUserMap.get(userName);
				Subject lastSessionSubject = new Subject.Builder().sessionId(lastSessionId).buildSubject();
				lastSessionSubject.logout();
				//清除当前用户的数据
				removeUserNameSessionId(userName);
			}
			if (!loginUserMap.containsKey(userName)) {
				loginUserMap.put(userName, sessionId);
			}
			
			//成功登录后将当天的记录清空
			userService.deleteErrorLoginInfo(userName);
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_INFO, 
					ExceptionsUtil.formatInfo(ExceptionsUtil.SYS_LOGIN,userName), 
					userName,"系统安全", UserUtil.getRequestIp(request)));
			return new ReponseEntity<UserInfo>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult());
		} catch (UnknownAccountException e) {
			//查询当前用户已经登录错误的次数
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, 
					ExceptionsUtil.formatInfo(ExceptionsUtil.SYS_ILLEGAL_LOGIN_FAIL,userName), 
					userName,"系统安全", UserUtil.getRequestIp(request)));
			return new ReponseEntity<UserInfo>(ReponseStatus.ERROR.getCode(), "用户名不存在!");
		} catch (SqlException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, 
					ExceptionsUtil.formatInfo(ExceptionsUtil.SYS_ILLEGAL_LOGIN_FAIL,userName), 
					userName,"系统安全", UserUtil.getRequestIp(request)));
			throw e;
		} catch (IncorrectCredentialsException e)  {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, 
					ExceptionsUtil.formatInfo(ExceptionsUtil.SYS_LOGIN_FAIL,userName), 
					userName,"系统安全", UserUtil.getRequestIp(request)));
			//用户登录失败后记录错误次数，如果是系统用户则不需要处理
			if (StringUtils.isBlank(SYSTEMUSER)) {
				SYSTEMUSER = userService.querySystemUser();
			}
			UserInfo userInfo = null;
			if (!userName.equals(SYSTEMUSER)) {
				LoginErrorInfo errorInfo = new LoginErrorInfo(userName,new Date());
				userService.saveErrorLoginInfo(errorInfo);
				int limitCount = userService.queryUserLoginCount(userName);
				userInfo = new UserInfo(5 - limitCount);
				return new ReponseEntity<UserInfo>(ReponseStatus.ERROR.getCode(), "密码不正确!", userInfo);
			}
			return new ReponseEntity<UserInfo>(ReponseStatus.ERROR.getCode(), "系统用户", userInfo);
		}
	}
	
	/**
	 * 系统退出
	 */
	@ApiOperation(value="用户退出",notes="用户退出操作")
	@RequestMapping(value = "/logout", method= RequestMethod.GET)
	public void doLogOut(HttpServletRequest request) {
		Subject subject = SecurityUtils.getSubject();
		String userName = subject.getPrincipals().toString();
		subject.logout();
		systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_INFO, 
				ExceptionsUtil.formatInfo(ExceptionsUtil.SYS_LOGOUT,userName), 
				userName,"系统安全", UserUtil.getRequestIp(request)));
	}
	
	/**
	 * @return
	 */
	@ApiOperation(value="用户session状态",notes="获取当前用户session状态")
	@GetMapping("/sessionStatus")
	@ResponseBody
	public ReponseEntity<String> querySessionStatus(HttpServletRequest request){
		Subject subject = SecurityUtils.getSubject();
		Session session = subject.getSession();
		if (session == null) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(), "session超时");
		}
		String userName = (String) subject.getPrincipal();
		if (StringUtils.isBlank(userName)) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(), "session超时");
		}
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "session正常");
	}
	
	
	private void removeUserNameSessionId(String userName) {
		Iterator<Entry<String,Object>> it = loginUserMap.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String,Object> entry = it.next();
			if (userName.equals(entry.getKey())) {
				it.remove();
			}
		}
	}
}
