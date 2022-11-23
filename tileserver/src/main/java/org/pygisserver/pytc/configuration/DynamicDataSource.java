package org.pygisserver.pytc.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @Description: 获取当前的数据源
 * @author:  
 * @date: 2019-12-09
 */
public class DynamicDataSource extends AbstractRoutingDataSource{

	private static volatile DynamicDataSource dynamicInstance;
	
	private static byte[] lock = new byte[0];
	
	private static Map<Object,Object> dataSourceMap = new HashMap<Object,Object>();
	
	@Override
	public void setTargetDataSources(Map<Object, Object> targetDataSources) {
		// TODO Auto-generated method stub
		super.setTargetDataSources(targetDataSources);
		dataSourceMap.putAll(targetDataSources);
		super.afterPropertiesSet();
	}
	
	
	@Override
	protected Object determineCurrentLookupKey() {
		String sourceKey = DataSourceContextHolder.getDataSource();
		if (StringUtils.isBlank(sourceKey)) {
			sourceKey = "default";
		}
		return sourceKey;
	}
	
	public Map<Object,Object> getDataSouceMap(){
		return dataSourceMap;
	}
	
	public static DynamicDataSource getInstance() {
		if (dynamicInstance == null) {
			synchronized (lock) {
				if (dynamicInstance == null) {
					dynamicInstance = new DynamicDataSource();
				}
			}
		}
		return dynamicInstance;
	}
}
