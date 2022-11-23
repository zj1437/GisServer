package org.pygisserver.web.dao;

import java.util.List;

import org.pygisserver.web.entity.PermissionInfo;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionMapper {

	/**
	 * 获取某一个角色的权限信息，主要用于角色的权限编辑
	 * @param roleId
	 * @return
	 */
	List<PermissionInfo> queryPermissionByRoleId(int roleId);
	
	/**
	 * 获取系统默认权限,用于加载权限树
	 * @return
	 */
	List<PermissionInfo> queryDefaultPermission();
	
	/**
	 * 获取用户权限信息
	 * @param userName
	 * @return
	 */
	List<PermissionInfo> queryUserPermissionByUserName(String userName);
	
	/**
	 * 
	 * 
	 * @param userid
	 * @return
	 */
	List<PermissionInfo> queryUserPermissionByuserId(int userid);
	
}
