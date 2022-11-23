package org.pygisserver.web.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description: 用户信息实体类
 * @author:  
 * @date: 2018-10-22
 */
@ApiModel(value="用户信息")
@Data
public class UserInfo implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1954968175327189055L;

	/**
	 * 用户自增id
	 */
	private int userId;

	/**
	 * 用户名称
	 */
	@ApiModelProperty("用户名称")
	private String userName;

	/**
	 * 用户密码
	 */
	@ApiModelProperty("用户密码")
	private String passWord;

	/**
	 * 电话
	 */
	@ApiModelProperty("用户电话")
	private String phone;

	/**
	 * 邮箱
	 */
	@ApiModelProperty("用户邮箱")
	private String email;

	/**
	 * 创建日期
	 */
	@JSONField(format="yyyy-MM-dd")
//	@JsonFormat(pattern="yyyy-MM-dd")
	@ApiModelProperty("创建时间")
	private Date genTime;

	/**
	 * 用户描述
	 */
	@ApiModelProperty("用户描述")
	private String description;

	/**
	 * 登录日期
	 */
	@JSONField(format="yyyy-MM-dd")
//	@JsonFormat(pattern="yyyy-MM-dd")
	@ApiModelProperty("登录时间")
	private Date loginTime;

	/**
	 * 上次登录时间
	 */
	@JSONField(format="yyyy-MM-dd")
//	@JsonFormat(pattern="yyyy-MM-dd")
	@ApiModelProperty("上次登录时间")
	private Date lastLoginTime;

	/**
	 * 是否为系统配置用户 0 不是  1是
	 */
	@ApiModelProperty("是否为系统配置用户")
	private int isSysConfig;

	/**
	 * 用户是否锁定
	 */
	@ApiModelProperty("是否锁定")
	private int isLock = 1;

	/**
	 * 用户权限信息
	 */
	private List<PermissionInfo> userPermisson;

	/**
	 * 用户剩余次数（每天最多只有5次输错机会）
	 */
	@ApiModelProperty("剩余登录次数")
	private int limitCount;

	/**
	 * 角色id集合
	 */
	private List<RoleInfo> roles;

	/**
	 * 角色ids
	 */
	private List<Integer> roleIds;

	/**
	 * 用户分配磁盘大小
	 */
	private double freeMemorySize;


	public UserInfo(String userName, String passWord, Date genTime) {
		super();
		this.userName = userName;
		this.passWord = passWord;
		this.genTime = genTime;
	}

	public UserInfo(int limitCount) {
		this.limitCount = limitCount;
	}
}
