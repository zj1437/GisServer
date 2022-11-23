package org.pygisserver.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.web.service.RoleManageService;
import org.pygisserver.web.entity.RoleInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @Description: 角色管理接口 
 * @author: 
 * @date: 2019-01-14
 */
@Api(tags="角色管理接口")
@RequestMapping("/role")
@RestController
public class RoleManageController {

	@Autowired
	private RoleManageService roleManageService;
	
	/**
	 * 判断当前角色是否唯一
	 * @param roleName  角色名称
	 * @return
	 */
	@ApiOperation(value="角色名称唯一校验",notes="判断当前角色是否唯一")
	@ApiImplicitParam(name="roleName",value="角色名称",required=true,paramType="path")
	@RequestMapping(value="/exists/{roleName}",method=RequestMethod.GET)
	public ReponseEntity<String> isExistsRoleName(@PathVariable String roleName){
		boolean flag = roleManageService.isExistsRoleName(roleName);
		if (flag) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
											 "角色名已存在");
		}
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
										 "角色名可用");
	}
	
	/**
	 * 创建角色信息
	 * @param roleInfo 角色信息
	 * @return
	 */
	@ApiOperation(value="保存角色信息",notes="保存当前的角色信息")
	@RequestMapping(value="/create",method=RequestMethod.POST)
	public ReponseEntity<String> saveRoleInfo(@RequestBody @ApiParam("角色信息") RoleInfo roleInfo){
		roleManageService.saveRoleInfo(roleInfo);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
										 "创建成功!");
	}
	
	
	/**
	 * 编辑角色信息
	 * @param roleInfo 角色信息
	 * @return
	 */
	@ApiOperation(value="编辑角色",notes="编辑当前的角色信息")
	@RequestMapping(value="/edit",method=RequestMethod.POST)
	public ReponseEntity<String> editRoleInfo(@RequestBody @ApiParam("角色信息") RoleInfo roleInfo){
		String result = roleManageService.editRoleInfo(roleInfo);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), result);
	}
	
	/**
	 * 删除角色信息,单个删除
	 * @param id 角色id  
	 * @return
	 */
	@ApiOperation(value="删除角色",notes="删除某一个角色")
	@ApiImplicitParam(name="roleId",value="角色id",required=true,paramType="path")
	@RequestMapping(value="/delete/{roleId}",method=RequestMethod.GET)
	public ReponseEntity<String> deleteRoleInfo(@PathVariable int roleId){
//			List<Integer> ids = new ArrayList<Integer>();
//			ids.add(roleId);
		roleManageService.deleteRoleInfos(Arrays.asList(roleId));
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
										 "删除成功!");
	}
	
	/**
	 * 删除角色信息,批量删除
	 * @param roleInfo  
	 * @return
	 */
	@ApiOperation(value="删除角色",notes="批量删除角色")
	@ApiImplicitParam(name="ids",value="角色ids",required=true,paramType="body")
	@RequestMapping(value="/deleteRoles",method=RequestMethod.POST)
	public ReponseEntity<String> deleteRoleInfo(@RequestBody List<Integer> ids){
		roleManageService.deleteRoleInfos(ids);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
										 "删除成功!");
	}
	
	/**
	 * 获取角色列表
	 * @param roleInfo  
	 * @return
	 */
	@ApiOperation(value="获取角色信息",notes="获取角色信息列表")
	@RequestMapping(value="/querylist",method=RequestMethod.GET)
	public ReponseEntity<List<RoleInfo>> queryRoleInfo(){
		//获取数量
		int num = roleManageService.queryRoleNum();
		if (num > 0) {
			List<RoleInfo> list = roleManageService.queryRoleInfo();
			return new ReponseEntity<List<RoleInfo>>(ReponseStatus.SUCCESS.getCode(),
													 ReponseStatus.SUCCESS.getResult(),
													 list,
													 num);
		}else {
			return new ReponseEntity<List<RoleInfo>>(ReponseStatus.SUCCESS.getCode(),
													 ReponseStatus.SUCCESS.getResult(),
													 new ArrayList<RoleInfo>(),
													 num);
		}
	} 
	
	
	/**
	 * 根据用户id获取角色信息
	 * @param roleInfo  
	 * @return
	 */
	@ApiOperation(value="获取角色信息",notes="根据用户id获取用户的角色信息")
	@ApiImplicitParam(name="userId",value="用户id",required=true,dataType="Integer",paramType="path")
	@RequestMapping(value="/query/{userId}",method=RequestMethod.GET)
	public ReponseEntity<List<RoleInfo>> queryRole(@PathVariable int userId){
		//获取数量
		int num = roleManageService.queryRoleNum();
		if (num > 0) {
			List<RoleInfo> list = roleManageService.queryRoleInfo(userId);
			return new ReponseEntity<List<RoleInfo>>(ReponseStatus.SUCCESS.getCode(),
											    	ReponseStatus.SUCCESS.getResult(),
											    	list,
											    	num);
		}else {
			return new ReponseEntity<List<RoleInfo>>(ReponseStatus.SUCCESS.getCode(),
													 ReponseStatus.SUCCESS.getResult(),
													 new ArrayList<RoleInfo>(),
													 num);
		}
	}
}
