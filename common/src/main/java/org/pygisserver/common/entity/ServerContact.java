package org.pygisserver.common.entity;

import lombok.Data;

/**
 * @Description: 默认服务提供商 
 * @author: 
 * @date: 2019-12-20
 */
@Data
public class ServerContact {

	private int id;

	private String titleName;

	private String abstractName;

	private String keywords;

	private String onLineResource;

	private String organization;

	private String webSite;

	private String contactPerson;

	private String primary;

	private String telephone;

	private String fax;

	private String address;

	private String city;

	private String province;

	private String country;

	private String postcode;

	private String email;
}
