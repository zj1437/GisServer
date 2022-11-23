package org.pygisserver.web.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.BaseException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 
 * @author:  
 * @date: 2018-11-30
 */
@Slf4j
public class ZipCompress3ds implements Runnable{

	private String fileUrl;
	
	public ZipCompress3ds(String url) {
		this.fileUrl = url;
	}
	
	@Override
	public void run() {
		String batPath = this.getClass().getClassLoader().getResource("/bat/zip.bat").getPath();
		runbat(batPath.replaceFirst("/", ""),fileUrl);
	}

	/**
	 * @param batCommand bat命令所在位置
	 * @param param
	 * @throws  
	 */
	private static void runbat(String batCommand,String fileUrl) {
		String cmd = "cmd /c " + batCommand;
		File file = new File(fileUrl);
	    cmd = String.format(cmd+" %s %s %s", file.getParent(),fileUrl,file.getName().split("\\.")[0]);
	    InputStream is = null;
	    Process ps = null;
	    try {
			ps = Runtime.getRuntime().exec(cmd);
			is = ps.getInputStream();
			byte[] buffer = new byte[1024];
			while(is.read(buffer) != -1) {
				System.out.println(new String(buffer,"utf-8"));
			}
			is.close();
			ps.waitFor();
		} catch (InterruptedException | IOException e) {
			log.error("ZipCompress3ds runbat has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.BASE_ERROR);
		} finally {
	    	ps.destroy();
		}
	}
}
