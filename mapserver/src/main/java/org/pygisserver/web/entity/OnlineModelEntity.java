package org.pygisserver.web.entity;

import lombok.Data;

/**
 * 服务与online模型联动实体类
 * @author admin
 *
 */
@Data
public class OnlineModelEntity {
	/**
	 * 模型名称
	 */
	private String modelName;
	/**
	 * 模型/倾斜摄影获取路径
	 */
	private String modelPath;
	/**
	 * 模型缩略图获取路径
	 */
	private String thumbalPath;

	/**
	 * id
	 */
	private int id;

	/**
	 * 偏移高度
	 */
	private double offsetHeight;
	
	public OnlineModelEntity(String modelName,String modelPath,String thumbalPath) {
		this.modelName = modelName;
		this.modelPath = modelPath;
		this.thumbalPath = thumbalPath;
	}
}
