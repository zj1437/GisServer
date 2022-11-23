package org.pygisserver.common.exception;

import lombok.Data;

/**
 * @Description: 全局异常类主要处理运行时的异常，对于其他异常则直接抛出
 * @author:  
 * @date: 2019-11-28
 */
@Data
public class ArgumentException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private int errorCode;
	
	private String errorMsg;
	
	public ArgumentException() {
		super();
	}
	
	public ArgumentException(String errorMsg) {
		super(errorMsg);
	}
	
	public ArgumentException(int errorCode,String errorMsg) {
		super(errorMsg);
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}
	
	public ArgumentException(Throwable th) {
		super(th);
	}
	
	public ArgumentException(String errorMsg,Throwable th) {
		super(errorMsg,th);
		this.errorMsg = errorMsg;
	}
	
	public ArgumentException(int errorCode,Throwable th) {
		super(th);
		this.errorCode = errorCode;
	}
	
	public ArgumentException(int errorCode,String errorMsg,Throwable th) {
		super(errorMsg,th);
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}
}
