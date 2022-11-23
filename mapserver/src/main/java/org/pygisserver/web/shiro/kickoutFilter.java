package org.pygisserver.web.shiro;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.AccountsException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 控制登录成后的跳转页面
 * @author: 
 * @date: 2018-10-30
 */
@Slf4j
public class kickoutFilter extends FormAuthenticationFilter {
	@Override
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
		boolean flag = super.isAccessAllowed(request, response, mappedValue);
		// 首先获取session
		if (flag) {
			Subject subject = getSubject(request, response);
			Session session = subject.getSession();
			if (session == null) {
				try {
					saveRequestAndRedirectToLogin(request, response);
				} catch (IOException e) {
					log.error("kickoutFilter isAccessAllowed has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new AccountsException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.LOGIN_ERROR, ""));
				}
			}
			long lastTime = session.getLastAccessTime().getTime();
			long current = System.currentTimeMillis();
			// 上一次操作与当前的时间差超过session的设置时间，则进行跳转
			if (current - lastTime >= 120000) {
				try {
					saveRequestAndRedirectToLogin(request, response);
				} catch (IOException e) {
					log.error("kickoutFilter isAccessAllowed has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new AccountsException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.LOGIN_ERROR, ""));
				}
			}
		}
		return flag;
	}

	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		if (isLoginRequest(request, response)) {
			if (isLoginSubmission(request, response)) {
				if (log.isTraceEnabled()) {
					log.trace("Login submission detected.  Attempting to execute login.");
				}
				return executeLogin(request, response);
			} else {
				if (log.isTraceEnabled()) {
					log.trace("Login page view.");
				}
				return true;
			}
		} else {
			if (log.isTraceEnabled()) {
				log.trace("Attempting to access a path which requires authentication.  Forwarding to the "
						+ "Authentication url [" + getLoginUrl() + "]");
			}
			saveRequestAndRedirectToLogin(request, response);
			return false;
		}
	}
}
