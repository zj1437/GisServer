package org.pygisserver.web.service;

import java.util.List;

import org.pygisserver.web.entity.RoleInfo;

/**
 * @Description: 角色管理service接口类
 * @author: 
 * @date: 2018-11-16
 */
public interface RoleManageService {


	boolean isExistsRoleName(String roleName);
	
	/**
	 * 保存角色
	 * 
	 * @param role
	 */
	void saveRoleInfo(RoleInfo role);
	
	/**
	 * 编辑角色
	 * @param role
	 */
	String editRoleInfo(RoleInfo role);
	
	/**
	 * 删除角色  系统指定角色不可以删除
	 * @param ids
	 */
	void deleteRoleInfos(List<Integer> ids);
	
	
	/**
	 * 获取当前所有角色
	 * @return
	 */
	List<RoleInfo> queryRoleInfo();
	
	/**
	 * 根据用户id 获取当前用户的角色列表
	 * @param userId
	 * @return
	 */
	List<RoleInfo> queryRoleInfo(int userId);
	
	
	int queryRoleNum();
}
