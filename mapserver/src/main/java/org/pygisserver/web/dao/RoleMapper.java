package org.pygisserver.web.dao;

import java.util.List;

import org.pygisserver.web.entity.RoleInfo;
import org.pygisserver.web.entity.RolePermissionInfo;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleMapper {

	/**
	 * 根据用户id获取用户名
	 * @param userId
	 * @return
	 */
	List<RoleInfo> queryRoleInfoById(int userId);
	
	/**
	 * 保存角色
	 * 
	 * @param role
	 */
	void addRoleInfo(RoleInfo role);
	
	/**
	 * 保存角色权限信息
	 * @param rolePermissions
	 */
	void saveRoleAuthinfo(List<RolePermissionInfo> rolePermissions);
	
	/**
	 * 编辑角色
	 * @param role
	 */
	void editRoleInfo(RoleInfo role);
	
	
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
	 * 根据角色id删除角色权限信息
	 * 
	 * @param roleId
	 */
	void deleteRolePermission(List<Integer> roleIds);
	
	
	/**
	 * 查询目前已经存在的角色名称
	 * 
	 * @return
	 */
	List<String> queryExistsRoleName();
	
	
	int queryRoleNum();
}
