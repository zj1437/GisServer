package org.pygisserver.web.entity;

import lombok.Data;

/**
 * 批量发布查询实体包括服务和模型两种类型：
 * 1.服务包括fileName、filePath、isDemData、extension四个字段
 * 2.模型包括fileName、filePath、modelType三个字段
 * @author admin
 *
 */
@Data
public class BatchPublishEntity {
	/**
	 * 即将发布的服务名称
	 */
	private String fileName;
	/**
	 * 服务路径
	 */
	private String filePath;
	/**
	 * 即将发布的服务是否是高程数据，如果是true，则需要区别处理
	 */
	private boolean isDemData;
	
	/**
	 * 获取即将发布的服务文件类型，主要用于osgb和terrain文件
	 */
	private String extension;
	/**
	 * 模型类型，就是模型文件的后缀名
	 */
	private String modelType;

	/**
	 * 根据文件类型和打下判断是否可以生成金字塔缓存
	 */
	private boolean isOverView;

	public BatchPublishEntity(String fileName,String filePath,boolean isDemData,String extension,String modelType, boolean isOverView) {
		this.fileName = fileName;
		this.filePath = filePath;
		this.isDemData = isDemData;
		this.extension = extension;
		this.modelType = modelType;
		this.isOverView = isOverView;
	}
	
	public BatchPublishEntity() { }
}
