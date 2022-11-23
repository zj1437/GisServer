package org.pygisserver.pytc.configuration;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.pytc.entity.DataSourceEntity;
import org.pygisserver.pytc.dao.CommonMapper;
import org.pygisserver.pytc.util.SeedTaskUtil;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description:数据源的工具类 
 * @author:  
 * @date: 2019-12-09
 */
@Slf4j
public class DataSourceUtil {

	
	/**
	 * 添加数据源
	 * @param key
	 * @param dataSource
	 */
	public static void addDataSource(String key,DataSource dataSource) {
		DynamicDataSource myDynamicDataSource = DynamicDataSource.getInstance();
		Map<Object,Object> map = myDynamicDataSource.getDataSouceMap();
		map.put(key, dataSource);
        myDynamicDataSource.setTargetDataSources(map);
        myDynamicDataSource.afterPropertiesSet();
	}
	
	/**
	 * 删除数据源
	 * @param key
	 */
	public static void removeDataSource(String key) {
		DynamicDataSource myDynamicDataSource = DynamicDataSource.getInstance();
		Map<Object,Object> map = myDynamicDataSource.getDataSouceMap();
		map.remove(key);
        myDynamicDataSource.setTargetDataSources(map);
        myDynamicDataSource.afterPropertiesSet();
	}
	
	/**
	 * 初始化其它的数据源信息
	 * 查询默认数据库获取数据源的信息
	 * @throws CloneNotSupportedException 
	 */
	public static void initOthersDataSource(Map<Object,Object> map,DruidDataSource defaultDataSource) {
		
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		String dbType = null;
		bean.setDataSource(defaultDataSource);
		try {
			bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/CommonMapper.xml"));
			bean.setTypeAliasesPackage("org.pygisserver.common.pytc.entity");
			SqlSessionFactory sqlsessonFactory = bean.getObject();
			SqlSession sqlsession = sqlsessonFactory.openSession(true);
			CommonMapper commonMapper = sqlsession.getMapper(CommonMapper.class);
			List<DataSourceEntity> dataSources = commonMapper.queryDataSourceInfo();
			if (dataSources != null && !dataSources.isEmpty()) {
				for (DataSourceEntity dataSourceEntity : dataSources) {
					DruidDataSource dataSource = (DruidDataSource) defaultDataSource.clone();
					dataSource.setUrl(dataSourceEntity.getJdbcUrl());
					dataSource.setName(dataSourceEntity.getUserName());
					dataSource.setPassword(dataSourceEntity.getPassword());
					dataSource.setDriverClassName(dataSourceEntity.getDriverClass());
					//如果是其他数据库，则修改对应的驱动类  默认是
					dataSource.setDriver(SeedTaskUtil.createDriver(dataSourceEntity.getDriverClass()));
					map.put(dataSourceEntity.getSourceKey(), dataSource);
				}
			}
		} catch (Exception e) {
			log.error("DataSourceUtil initOthersDataSource has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.BASE_SETUP, "数据库"));
		}
	}
	
	
}
