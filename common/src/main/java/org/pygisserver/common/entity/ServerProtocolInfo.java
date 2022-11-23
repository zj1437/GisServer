package org.pygisserver.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @Description: 服务支持的协议
 * @author: 
 * @date: 2018-10-31
 */
@Data
public class ServerProtocolInfo implements Serializable {

	private static final long serialVersionUID = -2189403588018568924L;

	/**
	 * 版本id
	 */
	private int protocolId;
	
	/**
	 * OGC服务名称
	 */
	private String protocolName;
	
	/**
	 * 版本信息（以分号隔开）
	 */
	private String versionList;
	
	/**
	 * 切片策略（以分号隔开）
	 */
	private String tileGridSet;
	
	/**
	 * 支持格式/切片格式（以分号隔开）
	 */
	private String supportFormat;
}
