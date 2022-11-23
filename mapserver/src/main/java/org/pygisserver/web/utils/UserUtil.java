package org.pygisserver.web.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserUtil {
	public static boolean checkUserName(String name) {
		String regex = "(\\w{1,20})";
		if (name.matches(regex)) {
			return true;
		}
		return false;
	}

	public static boolean checkUserPasswd(String pass) {
		String userPass = PostGisStoreUtils.getDecodePass(pass);
		String regex = "([\\w!@#$%&*+-]{8,20})";
		if (userPass.matches(regex)) {
			return true;
		}
		return false;
	}
	
    /**
     * 获取当前登录的用户名
     * @return
     */
    public static String getUserName() {
    	try {
    		Subject subject = SecurityUtils.getSubject();
        	if (subject != null) {
        		if (subject.getPrincipal() != null) {
        			return subject.getPrincipal().toString();
        		}
        	}
    		return "游客";
		} catch (Exception e) {
			log.warn("UserUtil getUserName has error:" + ExceptionsUtil.stackTraceToStr(e));
		}
    	return "admin";
    }
    
    /**
	 * 根据请求信息获取当前客户端的IP
	 * @param req 请求体
	 * @return
	 */
	public static String getRequestIp(HttpServletRequest req) {
		String ip = req.getHeader("X-Forwarded-For");
		if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
			// 多次反向代理后会有多个ip值，第一个ip才是真实ip
			int index = ip.indexOf(",");
			if (index != -1) {
				return ip.substring(0, index);
			} else {
				return ip;
			}
		}
		ip = req.getHeader("X-Real-IP");
		if (StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)) {
			return ip;
		}
		if (!"0:0:0:0:0:0:0:1".equals(req.getRemoteAddr())) {
			return req.getRemoteAddr();
		}
		return getLocalIpArress();
	}
	
	/**
	 * 获取本地ip
	 * @return  返回本机ip信息
	 */
	public static String getLocalIpArress() {
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			log.error("UserUtil getLocalIpArress has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "本机IP"));
		}
		return address.getHostAddress();
	}

}
