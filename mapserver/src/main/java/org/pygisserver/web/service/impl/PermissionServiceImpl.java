package org.pygisserver.web.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.web.dao.PermissionMapper;
import org.pygisserver.web.dao.UserMapper;
import org.pygisserver.web.entity.PermissionInfo;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.service.PermissionService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 权限service的实现类
 * @author:  
 * @date: 2018-10-24
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {
	
	@Resource
	private PermissionMapper permissionMapper;
	
	@Resource
	private UserMapper userMapper;
	
	@Override
	public List<PermissionInfo> queryPermissionRoleInfo(int roleId) {
		try {
			return permissionMapper.queryPermissionByRoleId(roleId);
		} catch (Exception e) {
			log.error("PermissionServiceImpl queryPermissionRoleInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户权限"));
		}
	}

	@Override
	public List<PermissionInfo> queryDefaultPermission() {
		try {
			return permissionMapper.queryDefaultPermission();
		} catch (Exception e) {
			log.error("PermissionServiceImpl queryDefaultPermission has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户默认权限"));
		}
	}

	@Override
	public List<PermissionInfo> queryUserPermissionByUserName(String userName) {
		try {
			return permissionMapper.queryUserPermissionByUserName(userName);
		} catch (Exception e) {
			log.error("PermissionServiceImpl queryDefaultPermission has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, userName +  "用户权限"));
		}
	}
	
	@Override
	public List<PermissionInfo> queryUserPermissionByUserId(int userId) {
		try {
			return permissionMapper.queryUserPermissionByuserId(userId);
		} catch (Exception e) {
			log.error("PermissionServiceImpl queryUserPermissionByUserId has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户权限"));
		}
	}

	@Override
	public UserInfo getUserInfo(String userName) {
		try {
			return userMapper.queryUserInfo(userName);
		} catch (Exception e) {
			log.error("PermissionServiceImpl queryUserPermissionByUserId has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, userName +  "用户权限"));
		}
	}

}
