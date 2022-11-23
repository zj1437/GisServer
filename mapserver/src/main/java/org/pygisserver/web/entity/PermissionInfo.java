package org.pygisserver.web.entity;

import java.io.Serializable;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description: 权限信息对象 
 * @author:
 * @date: 2018-10-24
 */
@Api("权限信息")
@Data
public class PermissionInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3531577968959555896L;

	private int id;
	
	/**
	 * 权限id
	 */
	@ApiModelProperty("权限id")
	private String authId;
	
	/**
	 * 父权限id
	 */
	@ApiModelProperty("父权限id")
	private String pAuthId;
	
	/**
	 * 权限名称
	 */
	@ApiModelProperty("权限名称")
	private String authName;
	
	/**
	 * 权限描述
	 */
	@ApiModelProperty("权限描述")
	private String description;
	
    /**
     * 是否被选中
     */
	@ApiModelProperty("是否被选中")
    private int isCheck;
}
