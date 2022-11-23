package org.pygisserver.web.entity;

import lombok.Data;

import java.util.Date;

/**
 * @Description: 登录错误信息 
 * @author: 刘洋 
 * @date: 2018-12-04
 */
@Data
public class LoginErrorInfo {

	private int id;
	
	private String userName;
	
	private Date loginTime;

	public LoginErrorInfo(String userName, Date loginTime) {
		super();
		this.userName = userName;
		this.loginTime = loginTime;
	}
}
