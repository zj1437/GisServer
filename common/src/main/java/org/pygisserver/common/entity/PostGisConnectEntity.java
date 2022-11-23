package org.pygisserver.common.entity;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "postgisEntity",description = "数据库连接模型")
public class PostGisConnectEntity implements Serializable {
	private static final long serialVersionUID = 5433538135989085101L;
	/**
	 * 数据库连接id
	 */
	@ApiModelProperty(value = "数据库连接id",required = false)
	private int dbId;
	
	/**
	 * 数据库名称，用户定义的名称，用以区别其他数据库
	 */
	@ApiModelProperty(value = "数据库名称",required = true)
	private String dataSourceName;

	/**
	 * 数据库IP
	 */
	@ApiModelProperty(value = "数据库Ip",required = true)
	private String databaseIp;
	
	/**
	 * 数据库端口
	 */
	@ApiModelProperty(value = "数据库端口",required = true)
	private int port;
	
	/**
	 *数据库库名
	 */
	@ApiModelProperty(value = "数据库库名",required = true)
	private String databaseName;
	
	/**
	 *用户名
	 */
	@ApiModelProperty(value = "用户名",required = true)
	private String userName;
	
	/**
	 * 密码
	 */
	@ApiModelProperty(value = "密码",required = true)
	private String password;
	
	/**
	 * 数据源类型
	 */
	@ApiModelProperty(value = "数据源类型",required = true)
	private String dbType;
	
	/**
	 * 数据库状态
	 */
	@ApiModelProperty(value = "数据库状态",required = true)
	private int dbStatus;
	
	/**
	 * 发布者
	 */
	@ApiModelProperty(value = "发布者",required = false)
	private String publisher;
	
	/**
	 * 发布时间
	 */
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "发布时间",required = true)
	private Date publishTime;
	
	/**
	 * 更新时间
	 */
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value = "更新时间",required = false)
	private Date updateTime;
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PostGisConnectEntity other = (PostGisConnectEntity) obj;
		if (!databaseName.equalsIgnoreCase(other.databaseName)) {
			return false;
		}
		String curIP = databaseIp.equals("localhost") || databaseIp.equals("127.0.0.1") ? "localhost" : databaseIp;
		String otherIP = other.databaseIp.equals("localhost") || other.databaseIp.equals("127.0.0.1") ? "localhost" : other.databaseIp;
		if (!curIP.equals(otherIP)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		if (!userName.equalsIgnoreCase(other.userName)) {
			return false;
		}
		if (!dbType.equalsIgnoreCase(other.dbType)) {
			return false;
		}
		return true;
	}
	
	public boolean isSamePostGIS(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PostGisConnectEntity other = (PostGisConnectEntity) obj;
		if (!dataSourceName.equalsIgnoreCase(other.dataSourceName)) {
			return false;
		}
		if (!databaseName.equalsIgnoreCase(other.databaseName)) {
			return false;
		}
		String curIP = databaseIp.equals("localhost") || databaseIp.equals("127.0.0.1") ? "localhost" : databaseIp;
		String otherIP = other.databaseIp.equals("localhost") || other.databaseIp.equals("127.0.0.1") ? "localhost" : other.databaseIp;
		if (!curIP.equals(otherIP)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		if (!userName.equalsIgnoreCase(other.userName)) {
			return false;
		}
		if (!dbType.equalsIgnoreCase(other.dbType)) {
			return false;
		}
		if (!password.equalsIgnoreCase(other.password)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + port;
		result = prime * result + ((databaseName == null) ? 0 : databaseName.hashCode());
		result = prime * result + ((databaseIp == null) ? 0 : databaseIp.hashCode());
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
		result = prime * result + ((dbType == null) ? 0 : dbType.hashCode());
		return result;
	}
	
}
