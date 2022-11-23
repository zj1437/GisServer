package org.pygisserver.common.ui.entity;

import lombok.Data;

/**
 * @Description: 批量发布服务信息 
 * @author: 
 * @date: 2020-01-06
 */
@Data
public class MapServerBatchInfo {

	private String serverName;
	
	private String fileUrl;
	
	private String serverProctol;
	
	private int openTiled;
	
	private String extension;
	
	private Boolean demServer;

	private int isPyamid;
}
