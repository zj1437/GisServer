package org.pygisserver.common.sqlite.util;

import java.sql.Connection;

/**
 * 定义一个当前连接的对象，用来保存连接对象的属性和其属性
 * 
 * @author 
 *
 */
public class PoolConnection 
{
	/**
	 * 当前连接池
	 */
    private Connection connection;
    
    /**
     * 连接池是否繁忙,默认未使用
     */
    private boolean isBusy = false;

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public boolean isBusy() {
		return isBusy;
	}

	public void setBusy(boolean isBusy) {
		this.isBusy = isBusy;
	}

	public PoolConnection(Connection connection, boolean isBusy) {
		super();
		this.connection = connection;
		this.isBusy = isBusy;
	}
}
