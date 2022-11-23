package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: 角色权限信息对象
 * @author: 
 * @date: 2018-11-16
 */
@Data
public class RolePermissionInfo {

	/**
	 * 主键id
	 */
	private int id;
	
	/**
	 * 角色id
	 */
	private int roleId;
	
	/**
	 * 权限id
	 */
	private int authId;
	
	/**
	 * 权限状态
	 */
	private int authStatus;
}
