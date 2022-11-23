package org.pygisserver.common.exception;

import lombok.Data;

/**
 * @Description: 全局异常类主要处理运行时的异常，对于其他异常则直接抛出
 * @author: 
 * @date: 2019-11-28
 */
@Data
public class ResourceException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private int errorCode;
	
	private String errorMsg;
	
	public ResourceException() {
		super();
	}
	
	public ResourceException(String errorMsg) {
		super(errorMsg);
	}
	
	public ResourceException(int errorCode,String errorMsg) {
		super(errorMsg);
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}
	
	public ResourceException(Throwable th) {
		super(th);
	}
	
	public ResourceException(String errorMsg,Throwable th) {
		super(errorMsg,th);
		this.errorMsg = errorMsg;
	}
	
	public ResourceException(int errorCode,Throwable th) {
		super(th);
		this.errorCode = errorCode;
	}
	
	public ResourceException(int errorCode,String errorMsg,Throwable th) {
		super(errorMsg,th);
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}
}
