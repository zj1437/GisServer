package org.pygisserver.web.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.web.service.SystemService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @Description: 用户service切片类
 * @author: 
 * @date: 2018-11-22
 */
@Component
@Aspect
public class LicenseAdvices {

	@Pointcut(value="execution(* org.pygisserver.web.controller.LoginController.doLogin(..))"
			+ "|| execution(* org.pygisserver.web.controller.MapServerController.*(..))")
	private void licenseServer() {};

	@Resource
	private SystemService systemService;
	
	
	@Before("licenseServer()")
	public void beforeLicenseServer(JoinPoint joinPoint) {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
		String remoteAddr = request.getRemoteAddr();
		systemService.checkSysTime(remoteAddr);
	}

}
