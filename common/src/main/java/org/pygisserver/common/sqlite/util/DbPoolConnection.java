package org.pygisserver.common.sqlite.util;

import java.sql.SQLException;
import java.util.Properties;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.StatusException;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.pool.DruidPooledConnection;

import lombok.extern.slf4j.Slf4j;

/**
 * 手动获取数据库连接池
 * @Description: 
 * @author: 
 * @date: 2019-06-27
 */
@Slf4j
public class DbPoolConnection {
	
	/**
	 * 获取druid连接池
	 * 
	 * @return
	 * @throws SQLException
	 */
	public DruidPooledConnection getConnection(DruidDataSource dataSource)
	{
		try 
		{
			return dataSource.getConnection();
		} 
		catch (SQLException e) 
		{
			log.error("DbPoolConnection getConnection has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_DATABASE, "获取数据库连接池失败"));
		}
	}
	
	public DruidDataSource getDataSource(Properties properties)
	{
		DruidDataSource dataSource = null;
	    try 
	    {
	    	dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(properties);
		} 
	    catch (Exception e) 
	    {
	    	log.error("DbPoolConnection getDataSource has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_DATABASE, "创建数据库连接失败"));
		}
	    return dataSource;
	}
}
