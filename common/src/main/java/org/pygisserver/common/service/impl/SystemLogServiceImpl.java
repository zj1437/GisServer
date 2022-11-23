package org.pygisserver.common.service.impl;

import javax.annotation.Resource;

import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.mapper.SystemLogMapper;
import org.pygisserver.common.service.SystemLogService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 地图服务service实现类
 * 
 * @author 
 * @since 2018-08-17
 */
@Service
@Slf4j
public class SystemLogServiceImpl implements SystemLogService{

	@Resource
	private SystemLogMapper systemLogMapper;

	@Override
	public void saveSystemLog(SystemLogEntity sysLog) {
		try {
			systemLogMapper.saveSystemLog(sysLog);
		} catch (Exception e) {
			log.error("MapServerDataServiceImpl saveSystemLog has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "图层"));
		}
	}
}
