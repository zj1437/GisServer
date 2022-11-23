package org.pygisserver.web.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.web.entity.LoginErrorInfo;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.entity.UserRoleInfo;
import org.springframework.stereotype.Repository;

/**
 * @Description: 用户信息操作Dao 
 * @author:
 * @date: 2018-10-23
 */
@Repository(value="UserMapper")
public interface UserMapper {
	
	
	List<String> queryExistsUserName();
	
	/**
	 * @param userName
	 * @param password
	 * @return
	 */
	UserInfo queryUserInfo(String userName);
	
	/**
	 * 
	 * @param userName
	 * @param password
	 * @return
	 */
	UserInfo queryUserInfoByUserId(int userId);
	
	/**
	 * 增加用户
	 * @param userInfo
	 */
	void addUserInfo(UserInfo userInfo);
	
	/**
	 * 编辑用户
	 * @param userInfo
	 */
	void editUserInfo(UserInfo userInfo);
	
	/**
	 * 删除用户
	 */
	void deleteUserInfo(List<Integer> ids);
	
	/**
	 * 锁定用户
	 * @param userName
	 */
	void lockUser(String userName);
	
	
	/**
	 * 解锁用户
	 * @param userName
	 */
	void unLockUser(String userName);
	
	/**
	 * 查询当前用户状态
	 * @param userName
	 * @return
	 */
	int queryUserStatus(String userName);
	
	/**
	 * 保存用户角色信息
	 * @param userRoleInfos
	 */
	void saveUserRoleInfo(List<UserRoleInfo> userRoleInfos);
	
	/**
	 * 删除用户角色id
	 * @param roleIds
	 */
	void deleteUserRoleByRoleId(List<Integer> roleIds);
	
	/**
	 * 删除某一个用户的角色信息表
	 * @param userId
	 */
	void deleteUserRoleByUserId(int userId);
	
	
	/**
	 * 删除某一个用户的角色信息表
	 * @param userId
	 */
	void deleteUserRoleByUserIds(List<Integer> userIds);
	
	/**
	 * 获取用户信息列表
	 * @return 返回用户信息列表
	 */
	List<UserInfo> queryUserInfoList();
	
	
	/**
	 * 根据用户名称获取用户列表信息
	 * @return 返回用户信息列表
	 */
	List<UserInfo> queryUserInfoByUserName(String param);
	
	/**
	 * 更新用户名
	 * @param userName
	 * @param pass
	 */
	void updateUserPass(@Param("userName") String userName,
						@Param("pass") String pass);
	/**
	 * 批量修改用户密码
	 * @param userName
	 * @param pass
	 */
	void updateUsersPass(@Param("userNames") List<String> userName,
						 @Param("pass") String pass);
	/**
	 * 获取用户密码
	 * @param userName
	 * @return
	 */
	String queryUserPassword(String userName);
	
	
	/**
	 * 保存错误登录信息
	 * @param info
	 */
	void saveErrorLoginInfo(LoginErrorInfo info);
	
	/**
	 * @param startTime  开始时间
	 * @param endTime  结束时间
	 * @return  查询当前用户输错的次数
	 */
	int queryUserLoginCount(Map<String, Object> param);
	
	/**
	 * 删除错误登录信息
	 * @param param
	 */
	void deleteErrorLoginInfo(Map<String, Object> param);
	
	/**
	 * 获取用户名称
	 * @param ids  用户ids
	 * @return
	 */
	List<String> queryUserNameByIds(List<Integer> ids);
	
	/**
	 * 根据用户id,查询用户对应的服务信息
	 * @param ids
	 * @return
	 */
	List<Integer> queryServerIdsByUserNames(List<String> userNames);

	String querySystemUser();
}
