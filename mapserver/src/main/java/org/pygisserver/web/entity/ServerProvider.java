package org.pygisserver.web.entity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description: 服务版本提供商 
 * @author:
 * @date: 2018-11-27
 */
@Api("服务提供商信息")
@Data
public class ServerProvider {
	
	private int id;
	
	@ApiModelProperty("标题名称")
	private String titleName;
	
	@ApiModelProperty("标题名称")
	private String abstractName;
	
	@ApiModelProperty("关键词")
	private String keywords;
	
	@ApiModelProperty("在线资源")
	private String onLineResource;
	
	/**
	 * 单位
	 */
	@ApiModelProperty("组织")
	private String organization;
	
	@ApiModelProperty("网站")
	private String webSite;
	
	@ApiModelProperty("联系人")
	private String contactPerson;
	
	/**
	 * 职位
	 */
	@ApiModelProperty("职位")
	private String primary;
	
	@ApiModelProperty("电话")
	private String telephone;
	
	/**
	 * 传真
	 */
	@ApiModelProperty("传真")
	private String fax;
	
	@ApiModelProperty("地址")
	private String address;
	
	@ApiModelProperty("城市")
	private String city;
	
	@ApiModelProperty("省")
	private String province;
	
	@ApiModelProperty("国家")
	private String country;
	
	@ApiModelProperty("邮编")
	private String postcode;
	
	@ApiModelProperty("邮箱")
	private String email;
}
