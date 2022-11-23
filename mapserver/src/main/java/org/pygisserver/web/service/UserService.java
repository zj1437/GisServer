package org.pygisserver.web.service;

import org.pygisserver.web.entity.LoginErrorInfo;
import org.pygisserver.web.entity.UserInfo;

import java.util.List;

/**
 * @Description: 用户信息操作service 
 * @author:
 * @date: 2018-10-23
 */
public interface UserService {

	/**
	 *
	 * @param userName 用户名
	 * @return
	 */
	boolean isExistsUserName(String userName);

	/**
	 * @param userName 用户名
	 * @return
	 */
	UserInfo getUserInfo(String userName);

	/**
	 * @param userId 用户id
	 * @return
	 */
	UserInfo getUserInfoByUserId(int userId);

	/**
	 * 增加用户
	 * @param user
	 * @return
	 */
	String addUser(UserInfo user);
	
	/**
	 * 编辑用户信息
	 * @param user
	 */
	void editUserInfo(UserInfo user);

	/**
	 * 删除用户信息
	 * @param id
	 */
	void deleteUerById(List<Integer> ids,String reqIP);

	/**
	 * 锁定用户
	 * @param userName
	 */
	void lockUserByUserName(String userName);


	/**
	 * 解锁用户
	 * @param userName
	 */
	void unLockUserByUserName(String userName);

	/**
	 * 查询用户状态信息
	 * @param userName
	 * @return
	 */
	int queryUserStatus(String userName);

	/**
	 * 查询用户信息列表
	 * @return
	 */
	List<UserInfo> queryUserInfo();

	/**
	 * @param preStr 查询参数
	 * @return
	 */
	List<UserInfo> queryUerNameByPre(String preStr);

	/**
	 * 修改当前用户的密码
	 * @param userName
	 * @param passWord
	 */
	void modifyUserpassByUserId(String userName,String passWord);

	/**
	 * 根据用户名获取当前密码
	 *
	 * @param userName
	 * @return
	 */
	String queryPasswordByUserName(String userName);

	/**
	 * 保存错误登录信息
	 * @param info
	 */
	void saveErrorLoginInfo(LoginErrorInfo info);


	void deleteErrorLoginInfo(String userName);

	/**
	 * @param startTime  开始时间
	 * @param endTime  结束时间
	 * @return  查询当前用户输错的次数
	 */
	int queryUserLoginCount(String userName);

	/**
	 * 获取系统用户
	 * @return
	 */
	String querySystemUser();
}
