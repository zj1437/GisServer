package org.pygisserver.web.shiro;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.pygisserver.web.entity.PermissionInfo;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.service.PermissionService;

/**
 * @Description: 用户自定义realm 
 * @author: 
 * @date: 2018-10-23
 */
public class CustomRealm extends AuthorizingRealm{

	
	@Resource
	private PermissionService permissionService;
	
	/**
	 * 设置realm名称
	 */
	@Override
	public void setName(String name) {
		super.setName("myRealm");
	}
	
	/**
	 * 用户权限信息
	 */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		//获取身份
		UserInfo userInfo = (UserInfo) principals.getPrimaryPrincipal();
		//获取当前用户的权限信息
		List<PermissionInfo> userPermission = permissionService.queryUserPermissionByUserName(userInfo.getUserName());
		//遍历获取其中的权限信息
		List<String> permissionList = getPermission(userPermission);
		SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
		simpleAuthorizationInfo.addStringPermissions(permissionList);
		return simpleAuthorizationInfo;
	}

	private List<String> getPermission(List<PermissionInfo> permissionList) {
		
		List<String> list = null;
		if (permissionList != null && !permissionList.isEmpty()) {
			list = new ArrayList<String>(permissionList.size());
			for (PermissionInfo permission : permissionList) {
				//只获取权限不获取菜单
				String desc = permission.getDescription();
				if (desc.contains(":")) {
					list.add(permission.getDescription());
				}
			}
		}
		return list;
	}
	
	/**
	 * 用户身份认证
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
		//获取当前用户的userName
		String userName = (String)token.getPrincipal();
		//根据用户名获取用户信息
		UserInfo userInfo = permissionService.getUserInfo(userName);
		//查询不到则返回空
		if (userInfo == null) {
			return null;
		}
				
        
		//将用户权限设置到对应的用户信息中
//		List<PermissionInfo> permissions = permissionService.queryUserPermissionByUserName(userName);
//		userInfo.setUserPermisson(permissions);
		return new SimpleAuthenticationInfo(userName,userInfo.getPassWord(),ByteSource.Util.bytes(userInfo.getUserName()),this.getName());
	}

}
