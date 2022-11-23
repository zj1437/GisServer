package org.pygisserver.web.aop;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.service.StyleInfoService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @Description: 样式文件service切片类
 * @author:  
 * @date: 2018-11-22
 */
@Component
@Aspect
public class StyleInfoServiceAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.StyleInfoController.deleteStyle*(..))"
			+ "|| execution(* org.pygisserver.web.controller.StyleInfoController.importCustomSld(..))")
	private void styleInfoService() {};
	
	@Pointcut(value="execution(* org.pygisserver.web.controller.StyleInfoController.deleteStyle*(..))"
			+ "|| execution(* org.pygisserver.web.controller.StyleInfoController.importCustomSld(..))")
	private void delStyleServer() {};
	
	private ThreadLocal<String> delStyleName = new ThreadLocal<String>();
	
	@Autowired
	private MapStatsticService mapStatsticService;
	
	@Autowired
	private StyleInfoService styleInfoService;
	
	
	@Before("delStyleServer()")
	public void beforeDeleteServer(JoinPoint joinPoint) {
		Object[] param = joinPoint.getArgs();
		Signature signature = joinPoint.getSignature();
		String methodName = signature.getName();
		List<Integer> serverIds = new ArrayList<Integer>();
		List<String> styleNames = new ArrayList<>();
		switch (methodName) {
			case "deleteStyleInfoByIds":
				@SuppressWarnings("unchecked") List<Integer> ids = (List<Integer>) param[1];
				serverIds.addAll(ids);
				break;
			case "deleteStyleInfoById":
				int id = (int) param[1];
				serverIds.add(id);
				break;
			case "importCustomSld":
				MultipartFile[] styles = (MultipartFile[]) param[1];
				for (int i = 0; i < styles.length; i++) {
					styleNames.add(styles[i].getOriginalFilename());
				}
				break;
		}
		List<String> names = CollectionUtils.isEmpty(styleNames) ? styleInfoService.querySldName(serverIds) : styleNames;
		delStyleName.set(Arrays.toString(names.toArray()));
	}
	
	
	
	/**
	 * 正常返回时，记录当前的操作日志
	 * TODO 需要处理返回值的问题
	 * @param joinPoint
	 */
	@AfterReturning("styleInfoService()")
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
		case "deleteStyleInfoById":
			operationLog.setContent(MessageFormat.format("用户{0},删除了样式{1}", userName,delStyleName.get()));
			break;
		case "deleteStyleInfoByIds":
			operationLog.setContent(MessageFormat.format("用户{0},删除了样式{1}", userName,delStyleName.get()));
			break;
		case "exportCustomSld":
			MultipartFile[] file =  (MultipartFile[]) param[1];
			for (MultipartFile multipartFile : file) {
				operationLog.setContent(MessageFormat.format("用户{0}成功导入{1}sld文件", userName,multipartFile.getOriginalFilename()));
			}
			break;
		case "importCustomSld":
			operationLog.setContent(MessageFormat.format("用户{0},上传了样式{1}", userName,delStyleName.get()));
			break;
		default:
			break;
		}
		mapStatsticService.saveOperationLog(operationLog);
	}
	
}
