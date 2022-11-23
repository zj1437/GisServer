package org.pygisserver.common.enumm;

/**
 * 服务状态枚举
 * @author
 * 
 * @since 2018-08-20
 *
 */
public enum ServerStatusEnum {

	/**
	 * 暂停中
	 */
	SUSPENDED(0),
	
    /**
     * 已发布
     */
	PUBLISHED(1),
	
	/**
	 * 异常
	 */
	ABNORMAL(2),
	
	/**
	 * 处理中
	 */
	PROCESSING(3),
	
	/**
	 * 切片中
	 */
	TILEING(4),

	/**
	 * 金字塔生成中
	 */
	OVERVIEWS(5);
	
	private int code;
	
	ServerStatusEnum(int infoCode){
		this.code = infoCode;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
}
