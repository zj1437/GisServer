package org.pygisserver.pytc.configuration;

/**
 * @Description: 数据源上下文控制类 
 * @author:  
 * @date: 2019-12-09
 */
public class DataSourceContextHolder {

	/**
	 * 记录每一个线程对应的数据源的key
	 */
	private static final ThreadLocal<String> contextDataSource = new ThreadLocal<String>();
	
	/**
	 * 设置数据源
	 * @param dataSource
	 */
	public static void setDataSource(String sourceKey) {
		contextDataSource.set(sourceKey);
	}
	
    public static String getDataSource() {
        return contextDataSource.get();
    }
 
    public static void clearDataSource() {
    	contextDataSource.remove();
    }
}
