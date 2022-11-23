package org.pygisserver.web.utils;

import java.io.File;

import org.pygisserver.common.util.FileUtil;

/**
 * @Description: zip文件压缩线程 
 * @author:  
 * @date: 2018-11-03
 */
public class ZipCompressThread implements Runnable{

	private File fileResource;
	
	public ZipCompressThread(File fileResource) {
		this.fileResource = fileResource;
	} 
	
	@Override
	public void run() {
		FileUtil.zipCompress(fileResource);
	}
}
