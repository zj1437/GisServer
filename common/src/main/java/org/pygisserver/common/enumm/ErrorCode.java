package org.pygisserver.common.enumm;

/**
 * @Description: 错误码
 * @author: 
 * @date: 2020-06-16
 */
public enum ErrorCode{
	
	FILE_PATH_TOO_LONG(300,"发布文件路径超过系统最大设置深度，建议文件迁移后重新发布！"),
	
	PUBLISHED_TIME_OUT(301,"发布超时，请检查服务状态是否正常"),

	NOT_FOUND(404,"资源未找到"),
	
	RUNTIME_EXCEPTION(500,"运行时异常");

	private int errorCode;
	
	private String errorMsg;
	
	ErrorCode(int errorCode,String errorMsg) {
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}

	/**
	 * @return the errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @return the errorMsg
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * @param errorMsg the errorMsg to set
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
}
