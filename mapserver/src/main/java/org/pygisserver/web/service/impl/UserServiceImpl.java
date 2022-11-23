package org.pygisserver.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.util.DateUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.dao.SystemConfigMapper;
import org.pygisserver.web.dao.UserMapper;
import org.pygisserver.web.entity.LoginErrorInfo;
import org.pygisserver.web.entity.RoleInfo;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.entity.UserRoleInfo;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * @Description: 用户信息操作service的实现类
 * @author:  
 * @date: 2018-10-23
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

	@Resource
	private UserMapper userMapper;
	
	@Resource
	private MapServerService mapServerService;
	
	private List<String> userNameList = new ArrayList<String>();
	
	@Resource
	private SystemConfigMapper systemConfigMapper;
	
	@Override
	public boolean isExistsUserName(String userName) {
		//如果为空，则查询
		if (userNameList.isEmpty() ) {
			userNameList = userMapper.queryExistsUserName();
		}
		//查询
		if (userNameList.isEmpty()) {
			return false;
		}
		return userNameList.contains(userName);
	}
	
	
	
	@Override
	public UserInfo getUserInfo(String userName) {
		try {
			return userMapper.queryUserInfo(userName);
		} catch (Exception e) {
			log.error("UserServiceImp getUserInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, userName + "用户"));
		}
	}

	/**
	 * 添加用户信息 
	 * @param user 用户信息
	 */
	@Override
	@Transactional
	public String addUser(UserInfo user) {
		List<UserInfo> userInfos = userMapper.queryUserInfoByUserName(user.getUserName());
		if (CollectionUtils.isNotEmpty(userInfos)) {
			log.warn("该用户已存在，请勿重复添加!");
			return "该用户已存在，请勿重复添加!";
		}
		try {
			userMapper.addUserInfo(user);
			saveUserRoleInfo(user);
			userNameList.add(user.getUserName());
		} catch (Exception e) {
			log.error("UserServiceImp addUser has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, user.getUserName() + "用户"));
		}
		return "用户添加完成!";
	}

	private void saveUserRoleInfo(UserInfo user) {
		//完善用户角色表
		List<Integer> roleIds = user.getRoleIds();
		if (roleIds == null || roleIds.isEmpty()) {
			//如果角色为空，则设置为游客
			roleIds.add(3);
		}
		List<UserRoleInfo> userRoleInfos = new ArrayList<UserRoleInfo>();
		for (Integer roleId : roleIds) {
			UserRoleInfo userRole = new UserRoleInfo();
			userRole.setRoleId(roleId);
			userRole.setUserId(user.getUserId());
			userRoleInfos.add(userRole);
		}
		userMapper.saveUserRoleInfo(userRoleInfos);
	}

	@Override
	@Transactional
	public void editUserInfo(UserInfo user) {
		try {
			//编辑当前用户
			userMapper.editUserInfo(user);
			//编辑用户角色表信息
			userMapper.deleteUserRoleByUserId(user.getUserId());
			//获取修改前的用户名称
			UserInfo oldUserInfo = userMapper.queryUserInfoByUserId(user.getUserId());
			if (!oldUserInfo.getUserName().equals(user.getUserName())) {
				//如果修改当前用户名称，则需要修改对应的文件夹成
				String commonUrl = SystemServiceImpl.cacheUrl.get("CACHE_FILE_URL");
				FileUtil.renameFile(commonUrl + File.separator + user.getUserName(), commonUrl + File.separator + oldUserInfo.getUserName());
			}
			//保存当前的用户角色信息
			saveUserRoleInfo(user);
			userNameList.clear();
		} catch (Exception e) {
			log.error("UserServiceImp editUserInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_UPDATE, user.getUserName() + "用户"));
		}
	}

	@Override
	@Transactional
	public void deleteUerById(List<Integer> ids,String reqIP) {
		//根据用户id获取用户名称
		List<String> userNames = userMapper.queryUserNameByIds(ids);
		userMapper.deleteUserInfo(ids);
		//删除用户时，删除对应的用户角色
		userMapper.deleteUserRoleByUserIds(ids);
		for (String userName : userNames) {
			FileUtil.deleteFile(SystemServiceImpl.cacheUrl.get("CACHE_FILE_URL") + File.separator + userName);
		}
		systemConfigMapper.deleteFileByUserName(userNames);
		//删除对应的服务信息
		List<Integer> serverIds = userMapper.queryServerIdsByUserNames(userNames);
		if (serverIds != null && !serverIds.isEmpty()) {
			mapServerService.deleteServerData(serverIds,reqIP);
		}
		//清空系统缓存
		userNameList.clear();
	}

	@Override
	public void lockUserByUserName(String userName) {
		try {
			userMapper.lockUser(userName);
		} catch (Exception e) {
			log.error("UserServiceImp lockUserByUserName has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_UPDATE, userName + "用户"));
		}
		
	}
	
	/**
	 * 解锁用户
	 */
	@Override
	public void unLockUserByUserName(String userName) {
		try {
			userMapper.unLockUser(userName);
		} catch (Exception e) {
			log.error("UserServiceImp unLockUserByUserName has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_UPDATE, userName + "用户"));
		}
		
	}

	/**
	 * 根据用户名查看当前用户是否已经被锁定
	 * @param userName 用户名称
	 * @return 返回用户状态信息
	 */
	@Override
	public int queryUserStatus(String userName) {
		try {
			return userMapper.queryUserStatus(userName);
		} catch (Exception e) {
			log.error("UserServiceImp queryUserStatus has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, userName + "用户状态"));
		}
	}

	@Override
	public List<UserInfo> queryUserInfo() {
		try {
			List<UserInfo> list = userMapper.queryUserInfoList();
			for (UserInfo userInfo : list) {
				List<RoleInfo> roles = userInfo.getRoles();
				for (Iterator iterator = roles.iterator(); iterator.hasNext();) {
					RoleInfo roleInfo = (RoleInfo) iterator.next();
					if (roleInfo.getRoleId() == 0) {
						iterator.remove();
					}
				}
			}
			return list;
		} catch (Exception e) {
			log.error("UserServiceImp queryUserInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户"));
		}
	}

	/**
	 * 根据查询条件快速获取当前用户列表
	 * @param param 用户名称
	 * 
	 */
	@Override
	public List<UserInfo> queryUerNameByPre(String param) {
		try {
			return userMapper.queryUserInfoByUserName(param);
		} catch (Exception e) {
			log.error("UserServiceImp queryUerNameByPre has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户"));
		}
	}

	@Override
	public UserInfo getUserInfoByUserId(int userId) {
		try {
			return userMapper.queryUserInfoByUserId(userId);
		} catch (Exception e) {
			log.error("UserServiceImp getUserInfoByUserId has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户"));
		}
	}

	/**
	 * 修改用户密码
	 * @param userId 用户id
	 * @param passWord 新密码
	 * 
	 */
	@Override
	public void modifyUserpassByUserId(String userName, String passWord) {
		try {
			userMapper.updateUserPass(userName, passWord);
		} catch (Exception e) {
			log.error("UserServiceImp getUserInfoByUserId has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "用户"));
		}
	}



	@Override
	public String queryPasswordByUserName(String userName) {
		try {
			return userMapper.queryUserPassword(userName);
		} catch (Exception e) {
			log.error("UserServiceImp getUserInfoByUserId has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, userName + "用户密码"));
		}
	}



	@Override
	public void saveErrorLoginInfo(LoginErrorInfo info) {
		try {
			userMapper.saveErrorLoginInfo(info);
		} catch (Exception e) {
			log.error("UserServiceImp saveErrorLoginInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, info.getUserName() + "用户登陆失败信息"));
		}
	}



	@Override
	public int queryUserLoginCount(String userName) {
		try {
			Map<String,Object> param = new HashMap<String,Object>();
			param.put("userName", userName);
			param.put("startTime", DateUtil.getFirstTimeOfDay());
			param.put("endTime", new Date());
			return userMapper.queryUserLoginCount(param);
		} catch (Exception e) {
			log.error("UserServiceImp saveErrorLoginInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, userName + "用户登陆失败"));
		}
	}



	@Override
	public void deleteErrorLoginInfo(String userName) {
		try {
			Map<String,Object> param = new HashMap<String,Object>();
			param.put("userName", userName);
			param.put("startTime", DateUtil.getFirstTimeOfDay());
			param.put("endTime", new Date());
			userMapper.deleteErrorLoginInfo(param);
		} catch (Exception e) {
			log.error("SystemServiceImpl deleteErrorLoginInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, userName + "用户登陆失败信息"));
		}
	}


	@Override
	public String querySystemUser() {
		try {
			return userMapper.querySystemUser();
		} catch (Exception e) {
			log.error("SystemServiceImpl querySystemUser has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "系统用户"));
		}
	}
}
