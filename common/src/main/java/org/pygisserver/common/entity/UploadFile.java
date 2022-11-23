package org.pygisserver.common.entity;

import java.util.Date;

import lombok.Data;

/**
 * @Description: 文件上传实体类 
 * @author:
 * @date: 2019-05-14
 */
@Data
public class UploadFile implements Cloneable {

	private int uploadId;
	
	private String fileName;
	
	private Date uploadTime;
	
	private Date updateTime;
	
	private String uploador;
	
	private double fileSize;
	
	/**
	 * 文件url，主要针对文件夹
	 */
	private String fileUrl;
	
	/**
	 * 文件类型  0是文件1是文件夹
	 */
	private int fileType;
	
	public UploadFile(Date uploadTime, Date updateTime, String uploador) {
		super();
		this.uploadTime = uploadTime;
		this.updateTime = updateTime;
		this.uploador = uploador;
	}
	
	@Override
	public UploadFile clone() throws CloneNotSupportedException {
		return (UploadFile) super.clone();
	}

	public UploadFile(String fileName, String fileUrl, int fileType) {
		super();
		this.fileName = fileName;
		this.fileUrl = fileUrl;
		this.fileType = fileType;
	}

	public UploadFile() {
		super();
	}
	
}
