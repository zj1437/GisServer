package org.pygisserver.web.entity;

import lombok.Data;

/**
 * @Description: 进度上传模型类
 * @author:
 * @date: 2019-05-14
 */
@Data
public class Progress {

	/**
	 * 当前上传大小
	 */
	private long byteRead;
	
	/**
	 * 文件总大小
	 */
	private long contentLength;
	
	/**
	 * 当前第几个文件
	 */
	private int item;
	
	/**
	 * 当前文件名称
	 */
	private String fileName;

	public Progress(long byteRead, long contentLength, String fileName) {
		super();
		this.byteRead = byteRead;
		this.contentLength = contentLength;
		this.fileName = fileName;
	}

	public Progress(long byteRead, long contentLength, int item) {
		super();
		this.byteRead = byteRead;
		this.contentLength = contentLength;
		this.item = item;
	}

	public Progress() {
		super();
	}
}
