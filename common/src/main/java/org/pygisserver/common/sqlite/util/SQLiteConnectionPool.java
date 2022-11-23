package org.pygisserver.common.sqlite.util;
/**
 * 自定义sqlite的数据库连接池，连接池主要用来做两件事
 * 1、创建初始容量的线程池
 * 2、提供当可用的连接
 * 
 * @author 
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Vector;

import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLiteConnectionPool {
	/**
	 * 数据库驱动名称
	 */
	private String jdbcDriverName = "org.sqlite.JDBC";

	/**
	 * 初始化容量
	 */
	private int initConnectionNum = 100;

	/**
	 * 最大连接数
	 */
	private int maxConnectionNum = 500;

	/**
	 * 连接池自增步调
	 */
	private int incrementalConnections = 100;

	/**
	 * 数据库连接
	 */
	private Vector<PoolConnection> connections = null;
	
	private String dbPath;

	/**
	 * 初始化创建sqlite的连接池
	 * 使用同步，
	 * 也就是同一个数据库只允许创建一次数据库连接池
	 *  
	 * 
	 * @param dbPath
	 */
	public SQLiteConnectionPool(String dbPath)
	{
		this.dbPath = dbPath;
		createSQLiteConnection();
	}
	
	
	private void createSQLiteConnection() {
		// 如果当前没有初始化，则进行初始化
		if (connections == null || connections.isEmpty()) 
		{
	        try 
	        {
				Class.forName(this.jdbcDriverName);
			} 
	        catch (ClassNotFoundException e) 
	        {
	        	log.error("SQLiteConnectionPool createSQLiteConnection has error:" + ExceptionsUtil.stackTraceToStr(e));
	        	throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "SQLite驱动类"));
			}
	        // 创建保存连接的向量 , 初始时有 0 个元素
	        connections = new Vector();
	        // 根据 initialConnections 中设置的值，创建连接。
	        createSQLiteConnection(this.initConnectionNum);
		}
	}

	/**
	 * 创建连接
	 * 
	 * @param num
	 */
	private void createSQLiteConnection(int num)
	{
		if (connections.size() + num > maxConnectionNum)
		{
			log.error("SQLiteConnectionPool createSQLiteConnection has error: SQLite连接数已经超过最大连接数");
		    return;
		}
		for (int i = 0; i < num;i++) {
			try {
				Connection conn = DriverManager.getConnection(dbPath);
				PoolConnection poolConnection = new PoolConnection(conn, false);
				//直接添加
				connections.addElement(poolConnection);
			} catch (SQLException e) {
				log.error("SQLiteConnectionPool createSQLiteConnection has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_CREATE, "SQLite连接"));
			}
			
		}
	}
	
	/**
	 * 获取一个可用的连接
	 * 
	 * @return
	 */
	public synchronized Connection getAvailableConn() {
		// 确保连接池己被创建，如果没有创建，则返回null
		if (connections == null) {
			return null;
		}
		Connection conn = getFreeConnection();
		// 如果目前没有可以使用的连接，即所有的连接都在使用中
		while (conn == null) {
			try 
			{
				Thread.sleep(300);
			} catch (InterruptedException e) {
				log.error("SQLiteConnectionPool getAvailableConn has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.BASE_ERROR);
			}
			conn = getFreeConnection();
		}
		return conn;
	}

	/**
	 * 获取当前可用连接
	 * 
	 * @return
	 * @throws SQLException
	 */
	private Connection getFreeConnection() {
		// 从连接池中获得一个可用的数据库连接
		Connection conn = findFreeConnection();
		if (conn == null) {
			// 如果目前连接池中没有可用的连接，创建一些连接
			createSQLiteConnection(incrementalConnections);
			// 重新从池中查找是否有可用连接
			conn = findFreeConnection();
			if (conn == null) {
				// 如果创建连接后仍获得不到可用的连接，则返回 null
				return null;
			}
		}
		return conn;
	}

	/**
	 * 查找当前可用的连接
	 * 
	 * @return
	 * @throws SQLException
	 */
	private Connection findFreeConnection() {
		Connection conn = null;
		PoolConnection pConn = null;
		// 获得连接池向量中所有的对象
		Enumeration enumerate = connections.elements();
		// 遍历所有的对象，看是否有可用的连接，如果找到，就直接跳出
		while (enumerate.hasMoreElements()) {
			pConn = (PoolConnection) enumerate.nextElement();
			if (!pConn.isBusy()) {
				// 如果此对象不忙，则获得它的数据库连接并把它设为忙
				conn = pConn.getConnection();
				pConn.setBusy(true);
				break;
			}
		}
		return conn;
	}
	
	/**
	 * 将当前的connection的状态变为空闲
	 * s
	 * @param connection
	 */
	public void close(Connection connection)
	{
        for (PoolConnection poolConnection : connections) {
			
        	if (poolConnection.getConnection().equals(connection))
        	{
        		poolConnection.setBusy(false);
        	}
		}
	}
}
