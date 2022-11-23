package org.pygisserver.web.service;

import java.util.List;

import org.pygisserver.web.entity.PermissionInfo;
import org.pygisserver.web.entity.UserInfo;

/**
 * @Description: 权限信息
 * @author:  
 * @date: 2018-10-24
 */
public interface PermissionService {

	/**
	 * 获取某一个角色的权限信息，主要用于角色的权限编辑
	 * @param roleId
	 * @return
	 */
	List<PermissionInfo> queryPermissionRoleInfo(int roleId);
	
	/**
	 * 获取系统默认权限,用于加载权限树
	 * @return
	 */
	List<PermissionInfo> queryDefaultPermission();
	
	/**
	 * 获取用户权限信息
	 * @param userName  用户名称
	 * @return
	 */
	List<PermissionInfo> queryUserPermissionByUserName(String userName);
	
	
	List<PermissionInfo> queryUserPermissionByUserId(int userId);
	
	
	UserInfo getUserInfo(String userName);
}
