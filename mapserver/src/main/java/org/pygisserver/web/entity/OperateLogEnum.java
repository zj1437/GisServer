package org.pygisserver.web.entity;

/**
 * @Description: 日志操作枚举 
 * @author:
 * @date: 2018-11-26
 */
public enum OperateLogEnum {

	PUBLISH_MAP_SERVER(1,"发布地图服务"),
	
	EDIT_MAP_SERVER(2,"编辑地图服务"),
	
	DELE_MAP_SERVER(3,"删除地图服务"),

	PUBLISH_MODEL_SERVER(4,"发布模型服务"),
	
	EDIT_MODEL_SERVER(5,"编辑模型服务"),
	
	DELE_MODEL_SERVER(6,"删除模型服务");
	
	private int id;
	
	private String msg;
	
    OperateLogEnum(int id,String msg) {
		this.id = id;
		this.msg= msg;
	}

	public int getId() {
		return id;
	}

	public String getMsg() {
		return msg;
	}
}
