package org.pygisserver.web.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description: 角色信息
 * @author: 
 * @date: 2018-10-23
 */
@Api("角色信息")
@Data
public class RoleInfo implements Serializable {



	/**
	 * 
	 */
	private static final long serialVersionUID = 2584682288831281152L;

	/**
	 * 角色id
	 */
	private int roleId;
	
	/**
	 * 角色名称
	 */
	@ApiModelProperty("角色名称")
	private String roleName;
	
	/**
	 * 角色创建人
	 */
	@ApiModelProperty("角色创建人")
	private String roleCreator;
	
	/**
	 * 创建时间
	 */
	@ApiModelProperty("创建时间")
	@JSONField(format="yyyy-MM-dd")
//	@JsonFormat(pattern="yyyy-MM-dd")
	private Date genTime;
	
	/**
	 * 更新时间
	 */
	@JSONField(format="yyyy-MM-dd")
//	@JsonFormat(pattern="yyyy-MM-dd")
	@ApiModelProperty("更新时间")
	private Date updateTime;
	
	/**
	 * 角色描述
	 */
	@ApiModelProperty("角色描述")
	private String description;
	
	/**
	 * 是否为系统默认  默认是1
	 */
	@ApiModelProperty("是否为系统默认角色")
	private int isSystem;
	
	/**
	 * 关联的用户id
	 */
	private List<Integer> userIds;
	
	/**
	 * 管理的权限id
	 */
	private List<Integer> authIds;
	
	/**
	 * 用户信息
	 */
	private List<UserInfo> userInfos;
	
	
	/**
	 * 角色权限信息
	 */
	private List<PermissionInfo> permissions;
	
	
}
