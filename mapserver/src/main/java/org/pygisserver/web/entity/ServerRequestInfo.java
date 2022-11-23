package org.pygisserver.web.entity;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.Data;

/**
 * @Description: 服务请求信息
 * @author:
 * @date: 2018-11-22
 */
@Data
public class ServerRequestInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6889072335077948052L;

	/**
	 * 主键id
	 */
    private int id;

    private int serverId;

    /**
     * 服务名称
     */
    private String serverName;
    
    /**
     * 服务类型  0是地图服务，  1是模型服务
     */
    private int serverType;
    
    
    /**
     * 协议名称
     */
    private String protocolName;
    
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 响应时间 单位ms
     */
    private long reponseTime;

	public void setReponseTime(Date requetStartTime, Date requetEndTime) {
		this.reponseTime = requetEndTime.getTime() - requetStartTime.getTime();
	}

	/**
     * 请求开始时间
     */
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
//    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date requetStartTime;

    private String requetStartTimeStr;

	/**
     * 请求结束时间
     */
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
//    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Date requetEndTime;
    
    
    /**
     * 请求ip
     */
    private String reqIP;


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerRequestInfo other = (ServerRequestInfo) obj;
		if (id != other.id)
			return false;
		if (productName == null) {
			if (other.productName != null)
				return false;
		} else if (!productName.equals(other.productName))
			return false;
		if (protocolName == null) {
			if (other.protocolName != null)
				return false;
		} else if (!protocolName.equals(other.protocolName))
			return false;
		if (reponseTime != other.reponseTime)
			return false;
		if (reqIP == null) {
			if (other.reqIP != null)
				return false;
		} else if (!reqIP.equals(other.reqIP))
			return false;
		if (requetEndTime == null) {
			if (other.requetEndTime != null)
				return false;
		} else if (!requetEndTime.equals(other.requetEndTime))
			return false;
		if (requetStartTime == null) {
			if (other.requetStartTime != null)
				return false;
		} else if (!requetStartTime.equals(other.requetStartTime))
			return false;
		if (serverName == null) {
			if (other.serverName != null)
				return false;
		} else if (!serverName.equals(other.serverName))
			return false;
		if (serverType != other.serverType)
			return false;
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((productName == null) ? 0 : productName.hashCode());
		result = prime * result + ((protocolName == null) ? 0 : protocolName.hashCode());
		result = prime * result + (int) (reponseTime ^ (reponseTime >>> 32));
		result = prime * result + ((reqIP == null) ? 0 : reqIP.hashCode());
		result = prime * result + ((requetEndTime == null) ? 0 : requetEndTime.hashCode());
		result = prime * result + ((requetStartTime == null) ? 0 : requetStartTime.hashCode());
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + serverType;
		return result;
	}
    
    
}
