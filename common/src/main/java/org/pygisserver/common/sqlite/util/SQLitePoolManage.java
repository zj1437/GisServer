package org.pygisserver.common.sqlite.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.SqlException;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * SQLite数据库连接池管理
 * 主要做两件事：
 * 1、维护一个数据库名称和连接池的Map，如果传入的数据库没有初始化，则进行数据库连接池的初始化，如果初始化了，则直接调用
 * 2、
 * @author Administrator
 *
 */
@Slf4j
public class SQLitePoolManage {

    static ConcurrentHashMap<String, SQLiteConnectionPool> connectionPoolMap = new ConcurrentHashMap<String, SQLiteConnectionPool>();
    
//    private static ThreadLocal<SQLiteConnectionPool> connectionPoolThread = new ThreadLocal<SQLiteConnectionPool>();
    
    static ConcurrentHashMap<String, DruidDataSource> DruidDataSourceMap = new ConcurrentHashMap<String, DruidDataSource>();

	private static final String SQL = "select max(zoom_level) from tiles";
	
	private static final String ZERO_LEVEL_NUM = "select count(tile_data) from tiles where zoom_level = 0";
	
	private static final String ONE_LEVEL_NUM = "select count(tile_data) from tiles where zoom_level = 1";
	
	
	
    /**
	 * 记录访问的数据库的次数
	 */
	public static Set<SQLiteConnectionPool> recentUsed = new HashSet<SQLiteConnectionPool>();
	
    private SQLitePoolManage() 
    {
    	
    }
    
    public static Map<String, SQLiteConnectionPool> getSqliteConnectionMap(){
    	return connectionPoolMap;
    }
    
    public static void releaseSqliteConnectionMap(){
    	connectionPoolMap.clear();
    }
    
    
    public static void releaseSqliteConnectionMap(SQLiteConnectionPool connectionPool){
    	Iterator<Map.Entry<String, SQLiteConnectionPool>> it = connectionPoolMap.entrySet().iterator();
    	while(it.hasNext()) {
    		Map.Entry<String, SQLiteConnectionPool> entry = it.next();
    		SQLiteConnectionPool pool = entry.getValue();
			if (pool.equals(connectionPool)) {
				it.remove();
			}
    	}
    }
    
    public static Set<SQLiteConnectionPool> getUsedConnectionUsed(){
    	return recentUsed;
    }
    
    public static void releaseConnection() {
    	recentUsed.clear();
    } 
    
    /**
     * 对外提供获取实例的方法
     * @return
     */
    public static SQLitePoolManage getPoolManageInstance()
    {
    	return SingletonHolder.instance;
    }
    
    /**
     * 私有静态内部类获取线程池管理类
     * @Description: 
     * @author:
     * @date: 2019-06-27
     */
    private static class SingletonHolder
    {
    	private static SQLitePoolManage instance = new SQLitePoolManage();
    }
    
    
    /**
     * 初始化当前的数据库连接池
     */
    public static void createConnectionPool(String dbPath)
    {
    	SQLiteConnectionPool connectionPool = null;
        //如果当前的dbpath已经实例化过线程池对象，则无需再实例化，否则先初始化线程池
        if (!connectionPoolMap.containsKey(dbPath))
        {
        	synchronized (SQLitePoolManage.class) {
        		if (!connectionPoolMap.containsKey(dbPath)) {
					connectionPool = new SQLiteConnectionPool(dbPath);
	        		//创建完之后，将获取到的连接池信息保存到Map中，方便下次直接获取
	        		connectionPoolMap.put(dbPath, connectionPool);
				}
			}
        }
    }
    
    
    public static byte[] getMbTilesImage(String sql,String dbPath)
    {
    	byte[] buffer = null;
    	//获取使用
    	SQLiteConnectionPool connectionPool = connectionPoolMap.get(dbPath);
        Connection connection = connectionPool.getAvailableConn();
        recentUsed.add(connectionPool);
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
		    statement = connection.prepareStatement(sql);
			rs = statement.executeQuery();
			while(rs.next())
			{
				buffer = rs.getBytes("tile_data");
				return buffer;
			}
		} catch (SQLException e) {
			log.error("SQLitePoolManage getMbTilesImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "SQLite"));
		}finally {
			try {
				rs.close();
			} catch (SQLException e) {
				log.error("SQLitePoolManage getMbTilesImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				statement.close();
			} catch (SQLException e) {
				log.error("SQLitePoolManage getMbTilesImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			connectionPool.close(connection);
		}
        return null;
    }
    
/*    
 * 没有通用性，并不是所有的数据都是全球数据，局部数据就不符合
 * public static String getCrsAccordtoTilesNum(String dbPath) {
    	int tilesNum = getTilesNum(ZERO_LEVEL_NUM,dbPath);
    	if (tilesNum != 0) {
    		if (tilesNum == 1) {
    			return "EPSG:3857";
    		}else {
    			return "EPSG:4326";
    		}
    	}else {
    		tilesNum = getTilesNum(ONE_LEVEL_NUM,dbPath);
    		if (tilesNum == 4) {
    			return "EPSG:3857";
    		}else {
    			return "EPSG:4326";
    		}
    	}
    }*/
    
    
    
    /**
     * 根据瓦片数，来识别当前的投影
     * @param sql
     * @param dbPath
     * @return
     */
    private static int getTilesNum(String sql,String dbPath)
    {
    	//获取使用
    	SQLiteConnectionPool connectionPool = connectionPoolMap.get(dbPath);
        Connection connection = connectionPool.getAvailableConn();
        recentUsed.add(connectionPool);
        PreparedStatement statement = null;
        ResultSet rs = null;
        int tileNum = 0;
        try {
		    statement = connection.prepareStatement(sql);
			rs = statement.executeQuery();
			while(rs.next())
			{
				tileNum = rs.getInt(1);
			}
		} catch (SQLException e) {
			log.error("SQLitePoolManage getMbTilesImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "SQLite"));
		}finally {
			try {
				rs.close();
			} catch (SQLException e) {
				log.error("SQLitePoolManage getMbTilesImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				statement.close();
			} catch (SQLException e) {
				log.error("SQLitePoolManage getMbTilesImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			connectionPool.close(connection);
		}
        return tileNum;
    }
    
    
    
    public static int getMaxLevel(String dbPath)
    {
    	int maxlevel = 0;
    	SQLiteConnectionPool connectionPool = connectionPoolMap.get(dbPath);
        Connection connection = connectionPool.getAvailableConn();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
		    statement = connection.prepareStatement(SQL);
			rs = statement.executeQuery();
			while(rs.next())
			{
				maxlevel = rs.getInt(1);
			}
		} catch (SQLException e) {
			log.error("SQLitePoolManage getMaxLevel has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "SQLite"));
		}finally {
			try {
				rs.close();
			} catch (SQLException e) {
				log.error("SQLitePoolManage getMaxLevel has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			try {
				statement.close();
			} catch (SQLException e) {
				log.error("SQLitePoolManage getMaxLevel has error:" + ExceptionsUtil.stackTraceToStr(e));
			}
			//最后关闭connection
			connectionPool.close(connection);
		}
        return maxlevel;
    }

}
