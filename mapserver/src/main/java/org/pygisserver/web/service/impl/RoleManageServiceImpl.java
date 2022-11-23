package org.pygisserver.web.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.web.dao.RoleMapper;
import org.pygisserver.web.dao.UserMapper;
import org.pygisserver.web.entity.RoleInfo;
import org.pygisserver.web.entity.RolePermissionInfo;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.entity.UserRoleInfo;
import org.pygisserver.web.service.RoleManageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RoleManageServiceImpl implements RoleManageService {

	@Resource
	private RoleMapper roleMapper;
	
	@Resource
	private UserMapper userMapper;
	
	private static List<String> roleNameList = new ArrayList<String>();
	
	
	@Override
	public boolean isExistsRoleName(String roleName) {
		//如果为空，则查询
		if (roleNameList.isEmpty()) {
			roleNameList = roleMapper.queryExistsRoleName();
		}
		//查询
		if (roleNameList.isEmpty()) {
			return false;
		}
		return roleNameList.contains(roleName);
	}
	
	/**
	 * 保存当前的角色信息，包括处理角色权限以及用户角色表
	 * @param role  角色信息
	 */
	@Override
	@Transactional
	public void saveRoleInfo(RoleInfo role) {
		try {
			//设置更新时间
			role.setUpdateTime(new Date());
			//保存角色信息
			roleMapper.addRoleInfo(role);
			//保存角色权限信息
			saveRolePermissioninfo(role);
			//保存用户角色信息
			saveUserRoleInfo(role);
			roleNameList.add(role.getRoleName());
		} catch (SqlException e) {
			throw new SqlException(e.getErrorCode(),e.getMessage());
		} catch (Exception e) {
			log.error("RoleManageServiceImpl saveRoleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "用户角色"));
		}
	}

	private void saveUserRoleInfo(RoleInfo role) {
		//处理用户角色数据
		List<Integer> userIds = role.getUserIds();
		if (userIds == null || userIds.isEmpty()) {
			return;
		}
		List<UserRoleInfo> userRoles = new ArrayList<UserRoleInfo>();
		for (Integer userId : userIds) {
			UserRoleInfo userRole = new UserRoleInfo();
			userRole.setRoleId(role.getRoleId());
			userRole.setUserId(userId);
			userRoles.add(userRole);
		}
		try {
			userMapper.saveUserRoleInfo(userRoles);
		} catch (Exception e) {
			log.error("RoleManageServiceImpl saveRoleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "用户角色"));
		}
	}

	private void saveRolePermissioninfo(RoleInfo role) {
		//处理角色权限信息
		List<Integer> authIds = role.getAuthIds();
		
		List<RolePermissionInfo> roleAuths = new ArrayList<RolePermissionInfo>();
		for (Integer authId : authIds) {
			RolePermissionInfo rolePermission = new RolePermissionInfo();
			rolePermission.setRoleId(role.getRoleId());
			rolePermission.setAuthId(authId);
			roleAuths.add(rolePermission);
		}
		//保存角色权限信息
		try {
			roleMapper.saveRoleAuthinfo(roleAuths);
		} catch (Exception e) {
			log.error("RoleManageServiceImpl saveRolePermissioninfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "用户权限"));
		}
	}

	@Override
	@Transactional
	public String editRoleInfo(RoleInfo role) {
		try {
			// 判断修改操作是否移除系统默认账号的管理员角色
			if (role.getRoleId() == 1 && !role.getUserIds().contains(1)) {
				log.warn("RoleManageServiceImpl editRoleInfo has warn: 默认账号系统管理员角色不可删除!");
				return "默认账号系统管理员角色不可删除!";
			}
			//设置更新时间
			//role.setUpdateTime(new Date());
			//更新角色信息
			roleMapper.editRoleInfo(role);
			//更新角色权限信息，删除之前的角色权限信息，然后保存新的角色权限新
			List<Integer> roleIds = new ArrayList<Integer>();
			roleIds.add(role.getRoleId());
			//删除角色权限信息
			roleMapper.deleteRolePermission(roleIds);
			//删除该角色之前绑定的用户信息
			userMapper.deleteUserRoleByRoleId(roleIds);
			//保存角色权限信息
			saveRolePermissioninfo(role);
			//保存用户角色信息
			saveUserRoleInfo(role);
			//清空原有的缓存
			roleNameList.clear();
		} catch (SqlException e) {
			throw new SqlException(e.getErrorCode(),e.getMessage());
		} catch (Exception e) {
			log.error("RoleManageServiceImpl editRoleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_UPDATE, "用户角色"));
		}
		return "编辑成功!";
	}

	/**
	 * 删除指定角色,并且删除该角色对应的角色权限信息以及用户角色信息
	 * 当发现删除完用户角色信息后，
	 * 该用户当前无绑定角色，则默认绑定游客角色
	 * @param ids 角色id
	 */
	@Override
	@Transactional
	public void deleteRoleInfos(List<Integer> ids) {
		try {
			//首先删除指定角色
			roleMapper.deleteRoleInfos(ids);
			//删除角色权限信息
			roleMapper.deleteRolePermission(ids);
			//删除用户角色信息
			userMapper.deleteUserRoleByRoleId(ids);
			//清空原有的缓存
			roleNameList.clear();
		} catch (Exception e) {
			log.error("RoleManageServiceImpl deleteRoleInfos has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, "用户权限"));
		}
	}

	/**
	 * 获取当前的角色列表信息
	 * @return  返回角色列表
	 */
	@Override
	public List<RoleInfo> queryRoleInfo() {
		try {
			List<RoleInfo> roleInfos = roleMapper.queryRoleInfo();
			//处理其中的空用户
			for (RoleInfo roleInfo : roleInfos) {
				List<UserInfo> userInfos = roleInfo.getUserInfos();
				roleInfo.setUserInfos(getLegalUser(userInfos));
			}
			return roleInfos;
		} catch (Exception e) {
			log.error("RoleManageServiceImpl queryRoleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户角色"));
		}
	}

	/**
	 * 获取合乎规定的用户
	 * @param userInfos  用户信息
	 * @return
	 */
	private List<UserInfo> getLegalUser(List<UserInfo> userInfos){
		List<UserInfo> list = new ArrayList<UserInfo>();
		for (UserInfo userInfo : userInfos) {
			if (userInfo.getUserId() != 0) {
				list.add(userInfo);
			}
		}
		return list;
	}
	
	
	@Override
	public List<RoleInfo> queryRoleInfo(int userId) {
		try {
			return roleMapper.queryRoleInfoById(userId);
		} catch (Exception e) {
			log.error("RoleManageServiceImpl queryRoleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户角色"));
		}
	}

	@Override
	public int queryRoleNum() {
		try {
			return roleMapper.queryRoleNum();
		} catch (Exception e) {
			log.error("RoleManageServiceImpl queryRoleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户数量"));
		}
	}
}
