package org.pygisserver.web.thread;

import java.io.File;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.BaseException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 监控路径线程 
 * @author:  
 * @date: 2020-01-02
 */
@Slf4j
public class MonitorPathThread implements Runnable{

	private String storePath;
	
	private boolean openMonitor;
	
	public MonitorPathThread(String storePath,boolean openMonitor) {
		this.storePath = storePath;
		this.openMonitor = openMonitor;
		//计算当前的文件大小
	}
	
	/**
	 * @return the storePath
	 */
	public String getStorePath() {
		return storePath;
	}

	/**
	 * @param storePath the storePath to set
	 */
	public void setStorePath(String storePath) {
		this.storePath = storePath;
	}
	
	/**
	 * @return the openMonitor
	 */
	public boolean isOpenMonitor() {
		return openMonitor;
	}

	/**
	 * @param openMonitor the openMonitor to set
	 */
	public void setOpenMonitor(boolean openMonitor) {
		this.openMonitor = openMonitor;
	}

	@Override
	public void run() {
		//如果开启自动发布，则开始监测路径的变化，已有变化就通知对应的服务，然后调用发布接口
		while(openMonitor) {
			File file = new File(storePath);
			if (!file.isDirectory()) {
				Thread.interrupted();
				openMonitor = false;
			}else {
				
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				log.error("MonitorPathThread run has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.BASE_ERROR);
			}
		}
	}
}

