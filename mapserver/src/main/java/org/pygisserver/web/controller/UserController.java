package org.pygisserver.web.controller;

import io.swagger.annotations.*;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.service.UserService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

//import org.pygisserver.web.websocket.SingleLoginWebsocket;

/**
 * @Description: 用户管控制类 
 * @author: 
 * @date: 2018-10-24
 */
@Api(tags="用户管理接口")
@RestController
@RequestMapping("/user")
public class UserController {

	/**
	 * 用户信息
	 */
	@Autowired
	private UserService userService;
	
//	@Autowired
//	DefaultWebSessionManager sessionManager;
	/**
	 * 判断用户名是否可用
	 * @param userName
	 * @return
	 */
	@ApiOperation(value="用户名称唯一性校验",notes="判断当前用户名称是否存在")
	@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="path")
	@RequestMapping(value="/existsUserName/{userName}",method=RequestMethod.GET)
	public ReponseEntity<String> isExistsUserInfo(@PathVariable String userName) {
		boolean flag = userService.isExistsUserName(userName);
		if (flag) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
									 		  "用户名已经存在");
		}else {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
					   				 		  "用户名可用");
		}
	}
	
	/**
	 * 添加用户
	 * @param userInfo
	 * @return
	 */
	@ApiOperation(value="新建用户",notes="添加一个新的用户")
	@ApiImplicitParam(name="userInfo",value="用户信息",required=true,paramType="body")
	@RequestMapping(value="/addUser",method=RequestMethod.POST)
	public ReponseEntity<String> addUserInfo(HttpServletRequest req,@RequestBody(required=true) UserInfo userInfo) {
		//对密码进行加密处理,这样在登录认证时需要对密码进行处理
		String password = new SimpleHash("MD5", userInfo.getPassWord(), userInfo.getUserName(), 2).toHex();
		if (!UserUtil.checkUserName(userInfo.getUserName())) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(),
					"用户名不能包含特殊字符");
		}
		userInfo.setPassWord(password);
		String result = userService.addUser(userInfo);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), result);
	}
	
	/**
	 * 编辑用户
	 * @param userInfo
	 * @return
	 */
	@ApiOperation(value="编辑用户",notes="编辑当前存在用户")
	@RequestMapping(value="/editUser",method=RequestMethod.POST)
	public ReponseEntity<String> editUserInfo(HttpServletRequest req,@RequestBody @ApiParam("用户信息") UserInfo userInfo) {
		userService.editUserInfo(userInfo);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
								 		 ReponseStatus.SUCCESS.getResult());
	}
	
	/**
	 * 删除用户，单个用户
	 * @param userInfo
	 * @return
	 */
	@ApiOperation(value="删除用户",notes="删除当前某一个用户")
	@ApiImplicitParam(name="userId",value="用户id",required=true,paramType="path")
	@RequestMapping(value="/delete/{userId}",method=RequestMethod.GET)
	public ReponseEntity<String> deleteUserInfo(HttpServletRequest req,@PathVariable int userId) {
		String reqIP = UserUtil.getRequestIp(req);
		List<Integer> ids = new ArrayList<Integer>();
		ids.add(userId);
		userService.deleteUerById(ids,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "用户删除成功！");
	}
	
	/**
	 * 删除用户，批量
	 * @param userInfo
	 * @return
	 */
	@ApiOperation(value="删除用户",notes="批量删除用户")
	@ApiImplicitParam(name="ids",value="用户id集合",required=true,paramType="body")
	@RequestMapping(value="/deleteUsers",method=RequestMethod.POST)
	public ReponseEntity<String> deleteUserInfos(HttpServletRequest req,@RequestBody List<Integer> ids) {
		String reqIP = UserUtil.getRequestIp(req);
		userService.deleteUerById(ids,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
								 		  ReponseStatus.SUCCESS.getResult());
	}
	
	/**
	  * 根据用户名获取用户信息，
	  * 能从此拿到用户数据 代表用户已经可以从前台登录，
	  * 要求清除之前登录用户保持单地登录效果
	 * @param userName 用户名称
	 * @return
	 */
	@ApiOperation(value="获取用户信息",notes="根据用户名称获取用户信息")
	@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="path")
	@RequestMapping(value="/queryUserInfo/{userName}",method=RequestMethod.GET)
	public ReponseEntity<UserInfo> queryUserInfo(@PathVariable String userName) {
		UserInfo userInfo = userService.getUserInfo(userName);
		//处理session
       // DefaultWebSecurityManager securityManager = (DefaultWebSecurityManager) SecurityUtils.getSecurityManager();
        //DefaultWebSessionManager sessionManager = (DefaultWebSessionManager)securityManager.getSessionManager();
        //获取当前已登录的用户session列表
		/*Subject subject = SecurityUtils.getSubject();
        Collection<Session> sessions = sessionManager.getSessionDAO().getActiveSessions();
        System.out.println("all session"+sessions.toString());
        for(Session session:sessions){
            //清除该用户以前登录时保存的session
        	
            if(userName.equals(String.valueOf(session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY)))
            		&&!subject.getSession().getId().equals( session.getId())) {
               // sessionManager.getSessionDAO().delete(session);
            	System.out.println(subject.getSession().getId().toString());
            	Serializable lastSessionId = (Serializable) session.getId();
				Subject lastSessionSubject = new Subject.Builder().sessionId(lastSessionId).buildSubject();
				lastSessionSubject.logout();
				System.out.println("afterlogout"+sessionManager.getSessionDAO().getActiveSessions().toString());
            }
        }*/
		//告诉所有{userName} 的已登录账户，退出登录
//		SingleLoginWebsocket.sendMessageTo("logout",userName);
		
        return new ReponseEntity<UserInfo>(ReponseStatus.SUCCESS.getCode(), 
				   ReponseStatus.SUCCESS.getResult(), 
				   userInfo);
	}
	
	
	/**
	 * 根据用户id 获取用户信息
	 * @param userId
	 * @return
	 */
	@ApiOperation(value="获取用户信息",notes="根据用户id获取用户信息")
	@ApiImplicitParam(name="userId",value="用户id",required=true,dataType="Integer",paramType="path")
	@RequestMapping(value="/queryUser/{userId}",method=RequestMethod.GET)
	public ReponseEntity<UserInfo> queryUserInfo(@PathVariable int userId) {
		UserInfo userInfo = userService.getUserInfoByUserId(userId);
		return new ReponseEntity<UserInfo>(ReponseStatus.SUCCESS.getCode(), 
										   ReponseStatus.SUCCESS.getResult(), 
										   userInfo);
	}
	
	
	/**
	 * 锁定当前用户
	 * @param userName
	 * @return
	 */
	@ApiOperation(value="锁定当前用户",notes="根据用户名称锁定当前用户")
	@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="path")
	@RequestMapping(value="/lockUser/{userName}",method=RequestMethod.GET)
	public ReponseEntity<String> lockUserAccount(HttpServletRequest req,@PathVariable String userName) {
		userService.lockUserByUserName(userName);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
										 "锁定成功"); 
	}
	
	/**
	 * 解锁当前用户
	 * @param userName
	 * @return
	 */
	@ApiOperation(value="解锁当前用户",notes="根据用户名称解锁当前用户")
	@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="path")
	@RequestMapping(value="/unlockUser/{userName}",method=RequestMethod.GET)
	public ReponseEntity<String> unlockUserAccount(HttpServletRequest req,@PathVariable String userName) {
		userService.unLockUserByUserName(userName);
		userService.deleteErrorLoginInfo(userName);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
										 "解锁成功"); 
	}
	
	/**
	 * 获取当前用户状态
	 * @param userName
	 * @return
	 */
	@ApiOperation(value="获取用户状态",notes="根据用户名称获取当前用户状态")
	@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="path")
	@RequestMapping(value="/queryUserStatus/{userName}",method=RequestMethod.GET)
	public ReponseEntity<Integer> queryUserStatus(@PathVariable String userName) {
		int status = userService.queryUserStatus(userName);
		return new ReponseEntity<Integer>(ReponseStatus.SUCCESS.getCode(), 
				   						  ReponseStatus.SUCCESS.getResult(),
				   						  status); 
	}
	
	/**
	 * 修改用户密码
	 * @param userName 用户名称
	 * @param newPassWord 新密码
	 * @return
	 */
	@ApiOperation(value="修改密码",notes="修改当前用户的密码")
	@ApiImplicitParams({
		@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="path"),
		@ApiImplicitParam(name="newPassWord",value="新密码",required=true,paramType="path")
	})
	@RequestMapping(value="/modifypassword/{userName}/{newPassWord}",method=RequestMethod.GET)
	public ReponseEntity<String> modifyUserPassword(HttpServletRequest req,
													@PathVariable String userName,
											        @PathVariable String newPassWord){
		String password = new SimpleHash("MD5", newPassWord, userName, 2).toHex();
		userService.modifyUserpassByUserId(userName, password);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
								   		 "修改成功!");
	}
	
	/**
	 * 模糊查询用户
	 * @param preName 输入前缀
	 * @return
	 */
	@ApiOperation(value="获取用户列表",notes="获取用户列表")
	@ApiImplicitParam(name="preName",value="用户前缀",required=true,paramType="query")
	@RequestMapping(value="/queryUserList",method=RequestMethod.GET)
	public ReponseEntity<List<UserInfo>> queryUserListByPre(@RequestParam("preName") String preName){
		List<UserInfo> list= userService.queryUerNameByPre(preName);
		return new ReponseEntity<List<UserInfo>>(ReponseStatus.SUCCESS.getCode(), 
										 ReponseStatus.SUCCESS.getResult(),
										 list);
	}
	
	/**
	 * 获取对应的用户信息
	 * @return
	 */
	@ApiOperation(value="获取用户列表",notes="获取当前的用户列表信息")
	@RequestMapping(value="/queryUserInfo",method=RequestMethod.GET)
	public ReponseEntity<List<UserInfo>> queryUserList(){
		//首先对密码进行加密处理
		List<UserInfo> list= userService.queryUserInfo();
		return new ReponseEntity<List<UserInfo>>(ReponseStatus.SUCCESS.getCode(),
												 ReponseStatus.SUCCESS.getResult(),
												 list);
	}
	
	/**
	 * 校验当前用户的密码是否是正确的
	 * 
	 * @param userName 用户名
	 * @param password 密码
	 * @return
	 */
	@ApiOperation(value="密码校验",notes="密码校验")
	@ApiImplicitParams({
		@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="path"),
		@ApiImplicitParam(name="password",value="密码",required=true,paramType="path")
	})
	@RequestMapping(value="/check/{userName}/{password}",method=RequestMethod.GET)
	public ReponseEntity<Boolean> checkPasswordIsAccess(@PathVariable String userName,
	        										    @PathVariable String password){
		//首先对密码进行加密处理
		String oldPassWord = userService.queryPasswordByUserName(userName);
		//将输入密码加密后与原有密码进行比较
		String newPassWord = new SimpleHash("MD5", password, userName, 2).toHex();
		boolean flag = oldPassWord.equals(newPassWord);
		return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),
		 								  ReponseStatus.SUCCESS.getResult(),
		 								  flag);
			
	}
}
