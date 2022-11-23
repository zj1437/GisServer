package org.pygisserver.web.listener;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.web.dao.SystemConfigMapper;
import org.pygisserver.web.entity.PublishedDataFormat;
import org.pygisserver.web.entity.ServerAutoConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 文件监控 
 * @author:  
 * @date: 2020-01-03
 */
@Component
@Slf4j
public class FileMonitor {
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private SystemConfigMapper systemConfigMapper;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;

	private static final long TIME_INTERVAL = 5;

	private String monitorRootPath;
	
	private List<PublishedDataFormat> dataFormat;
	
	private FileAlterationMonitor monitor;
	
	private FileAlterationObserver observer;
	
	
	/**
	 * @return the monitorRootPath
	 */
	public String getMonitorRootPath() {
		return monitorRootPath;
	}

	/**
	 * @param monitorRootPath the monitorRootPath to set
	 */
	public void setMonitorRootPath(String monitorRootPath) {
		this.monitorRootPath = monitorRootPath;
	}

	/**
	 * @return the dataFormat
	 */
	public List<PublishedDataFormat> getDataFormat() {
		return dataFormat;
	}

	/**
	 * @param dataFormat the dataFormat to set
	 */
	public void setDataFormat(List<PublishedDataFormat> dataFormat) {
		this.dataFormat = dataFormat;
	}

	/**
	 * @return the observer
	 */
	public FileAlterationObserver getObserver() {
		return observer;
	}

	/**
	 * @param observer the observer to set
	 */
	public void setObserver(FileAlterationObserver observer) {
		this.observer = observer;
	}
	
	/**
	 * @return the monitor
	 */
	public FileAlterationMonitor getMonitor() {
		return monitor;
	}

	/**
	 * @param monitor the monitor to set
	 */
	public void setMonitor(FileAlterationMonitor monitor) {
		this.monitor = monitor;
	}

	@PostConstruct
	public void initFileMonitor() {
		ServerAutoConfig autoConfig = systemConfigMapper.queryServerAutoConfig();
		if (autoConfig == null) {
			return;
		}
		this.monitorRootPath = autoConfig.getMonitorPath();
		dataFormat = autoConfig.getDataFormat();
		constructFileObserver(getFileType());
		if (autoConfig.getIsOpen() == 1) {
			excuteFileMonitor();
		}
	}
	
	public void openFileMonitor(ServerAutoConfig autoConfig) {
		this.monitorRootPath = autoConfig.getMonitorPath();
		dataFormat = autoConfig.getDataFormat();
		constructFileObserver(getFileType());
		if (monitor == null) {
			excuteFileMonitor();
		}
	}
	
	
	private String[] getFileType() {
		String[] typeArray = new String[dataFormat.size()];
		int index = 0;
		for (PublishedDataFormat publishedDataFormat : dataFormat) {
			typeArray[index] = publishedDataFormat.getDataFormat();
			index++;
		}
		return typeArray;
	}
	
	public void constructFileObserver(String...fileType) {
		IOFileFilter[] fileFilter = new IOFileFilter[fileType.length + 1];
		int index = 0;
		for (String type : fileType) {
			//过滤tdb数据中的itdb文件创建监听
		    if ("tdb".equals(type)){
		        IOFileFilter filter = FileFilterUtils.and(
		                FileFilterUtils.fileFileFilter(),
                        FileFilterUtils.suffixFileFilter(".tdb"));
                fileFilter[index] = filter;
            }
		    else {
			IOFileFilter filter = FileFilterUtils.and(
						          FileFilterUtils.fileFileFilter(),
						          FileFilterUtils.suffixFileFilter(type));
                fileFilter[index] = filter;
		    }
			index++;
		}
		IOFileFilter filterDirectorFileFilter = FileFilterUtils.directoryFileFilter();
		fileFilter[fileFilter.length - 1] = filterDirectorFileFilter;
		IOFileFilter filter = FileFilterUtils.or(fileFilter);
		observer = new FileAlterationObserver(new File(monitorRootPath), filter);
	} 
	
	/**
	 * 开启监控程序
	 */
    public void excuteFileMonitor() {
        long interval = TimeUnit.SECONDS.toMillis(TIME_INTERVAL);
        observer.addListener(new FileListener(dataFormat,applicationContext));
        monitor = new FileAlterationMonitor(interval, observer);
        try {
            monitor.start();
        } catch (Exception e) {
        	systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
        			ExceptionsUtil.MONITOR_START, 
					"System","文件管理", ExceptionsUtil.getServerIp(null)));
        	log.error("FileMonitor excuteFileMonitor has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.BASE_START, "文件监听器"));
        }
    }
    
    /**
     * 关闭监控程序
     */
    public void stopFileMonitor() {
    	try {
			monitor.stop();
		} catch (Exception e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.MONITOR_STOP, 
					"System","文件管理", ExceptionsUtil.getServerIp(null)));
			log.error("FileMonitor stopFileMonitor has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.BASE_STOP, "文件监听器"));
		}
    }
}
