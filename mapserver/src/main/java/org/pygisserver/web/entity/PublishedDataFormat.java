package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: 数据发布格式 
 * @author:
 * @date: 2020-01-02
 */
@Data
public class PublishedDataFormat {

	private String dataFormat;
	
	/**
	 * 服务类型，各服务间以;隔开
	 */
	private String serverFormat;
	
	/**
	 * 是否开启切片
	 */
	private int openTiled;
}
