package org.pygisserver.web.entity;

import lombok.Data;

@Data
public class ServerProtocolEntity {
	//服务id
	private int serverId;
	//服务支持的投影坐标
	private String protocol;
	//服务支持的投影坐标
	private String gridSets;
	//服务协议支持的格式
	private String formats;
}
