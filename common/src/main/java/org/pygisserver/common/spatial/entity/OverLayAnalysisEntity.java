package org.pygisserver.common.spatial.entity;

import lombok.Data;

@Data
public class OverLayAnalysisEntity {
	
	/**
	 * 输入图层名称
	 */
	private String inputLayerName;
	
	/**
	 * 输入文件的url，查询的时候传递给前端、
	 * 前端在调用接口的时候传递回来
	 */
	private String inputFileUrl;
	
	/**
	 * 叠加图层名称
	 */
	private String overLayLayerName;
	
	/**
	 * 叠加文件的url
	 */
	private String overlayFileUrl;
	
	/**
	 * 默认安装目录下tomcat下的temp下
	 */
	private String outPutPath;
	
	/**
	 * 是否自动发布（1  发布  0  不发布）
	 */
	private int autoPublish;
	
	/**
	 * 图层编码格式
	 */
	private String charsetName;
	
	/**
	 * 聚合字段名称
	 */
	private String aggregateName;
}
