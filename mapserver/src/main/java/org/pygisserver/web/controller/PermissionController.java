package org.pygisserver.web.controller;

import java.util.List;

import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.web.service.PermissionService;
import org.pygisserver.web.entity.PermissionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;

/**
 * @Description: 权限信息控制类
 * @author: 
 * @date: 2018-11-02
 */
@Api(value="权限控制接口",tags="权限控制接口")
@RestController
@RequestMapping("/permission")
public class PermissionController {

	@Autowired
	private PermissionService permissionService;
	
	/**
	 * 获取系统默认权限
	 * @return
	 */
	@ApiOperation(value="默认权限列表",notes="获取系统默认的权限信息")
	@RequestMapping(value="/defaultPermission",method=RequestMethod.GET)
	public ReponseEntity<List<PermissionInfo>> queryDefaultPermission(){
		List<PermissionInfo> list = permissionService.queryDefaultPermission();
		return new ReponseEntity<List<PermissionInfo>>(ReponseStatus.SUCCESS.getCode(), 
													   ReponseStatus.SUCCESS.getResult(), 
													   list);
	}
	
	/**
	 * 根据用户id 获取用户权限
	 * @return
	 */
	@ApiOperation(value="获取用户权限列表",notes="通过用户id获取该用户的权限信息")
	@ApiImplicitParam(name="userId",value="用户id",required=true,dataType="Integer",paramType="path")
	@RequestMapping(value="/userPermission/{userId}",method=RequestMethod.GET)
	public ReponseEntity<List<PermissionInfo>> queryUserPermissionByUserId(@PathVariable int userId){
		List<PermissionInfo> list = permissionService.queryDefaultPermission();
		return new ReponseEntity<List<PermissionInfo>>(ReponseStatus.SUCCESS.getCode(), 
													   ReponseStatus.SUCCESS.getResult(), 
													   list);
	}
	
	/**
	 * 根据用户名获取用户权限
	 * @return
	 */
	@ApiOperation(value="获取用户权限信息",notes="通过用户名称获取用户权限信息")
	@ApiImplicitParam(name="userName",value="用户名称",required=true,paramType="query")
	@RequestMapping(value="/userPermission",method=RequestMethod.GET)
	public ReponseEntity<List<PermissionInfo>> queryUserPermissionByUserId(@RequestParam("userName") String userName){
		List<PermissionInfo> list = permissionService.queryDefaultPermission();
		return new ReponseEntity<List<PermissionInfo>>(ReponseStatus.SUCCESS.getCode(), 
													   ReponseStatus.SUCCESS.getResult(), 
													   list);
	}
	
	/**
	 * 通过角色id获取权限信息
	 * @param roleId 角色id
	 * @return
	 */
	@ApiOperation(value="获取用户权限信息",notes="通过用户名称获取用户权限信息")
	@ApiImplicitParam(name="roleId",value="角色id",required=true,dataType="Integer",paramType="query")
	@RequestMapping(value="/rolePermission",method=RequestMethod.GET)
	public ReponseEntity<List<PermissionInfo>> queryUserPermissionByRoleId(@RequestParam("roleId") int roleId){
		List<PermissionInfo> list = permissionService.queryPermissionRoleInfo(roleId);
		return new ReponseEntity<List<PermissionInfo>>(ReponseStatus.SUCCESS.getCode(), 
													   ReponseStatus.SUCCESS.getResult(), 
													   list);
	}
	
}
