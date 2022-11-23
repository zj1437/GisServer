package org.pygisserver.pytc.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @Description: 多数据源的配置，主要用于切图数据的保存和查询
 * 在不同数据库和同一数据库不同库之间的自动切换
 * @author:  
 * @date: 2019-12-09
 */
@Configuration
public class DataSourceConfig {

	@Value("${spring.datasource.url}")
	private String jdbcUrl;

	@Value("${spring.datasource.driver-class-name}")
	private String driverClass;
	
	@Value("${spring.datasource.username}")
	private String username;
	
	@Value("${spring.datasource.password}")
	private String password;
	
	@Value("${spring.datasource.druid.initial-size}")
	private int initSize;
	
	@Value("${spring.datasource.druid.max-active}")
	private int maxActive;
	
	@Value("${spring.datasource.druid.max-wait}")
	private int maxWaitTime;
	
	@Value("${spring.datasource.druid.time-between-eviction-runs-millis}")
	private int betweenEvictionTime;
	
	@Value("${spring.datasource.druid.validation-query}")
	private String validateQuery;
	
	@Value("${spring.datasource.druid.test-on-borrow}")
	private boolean testOnBorrow;
	
	@Value("${spring.datasource.druid.test-while-idle}")
	private boolean testWhileIdle;
	
	@Value("${spring.datasource.druid.test-on-return}")
	private boolean testOnReturn;
	
	@Value("${spring.datasource.druid.min-evictable-idle-time-millis}")
	private int minEvictableIdleRunMillis;
	
	/**
	 * 获取默认的datasource数据源
	 * @return
	 */
	@Bean
	public DynamicDataSource dynamicDataSource() {
		DynamicDataSource dynamicDataSource = DynamicDataSource.getInstance();
		DruidDataSource defaultDataSource = new DruidDataSource();
		defaultDataSource.setUrl(jdbcUrl);
		defaultDataSource.setDriverClassName(driverClass);
		defaultDataSource.setUsername(username);
		defaultDataSource.setPassword(password);
		defaultDataSource.setValidationQuery(validateQuery);
		defaultDataSource.setTestOnBorrow(testOnBorrow);
		defaultDataSource.setTestWhileIdle(testWhileIdle);
		defaultDataSource.setTestOnReturn(testOnReturn);
		defaultDataSource.setMaxActive(maxActive);
		defaultDataSource.setInitialSize(initSize);
		defaultDataSource.setMaxWait(maxWaitTime);
		defaultDataSource.setTimeBetweenEvictionRunsMillis(betweenEvictionTime);
		defaultDataSource.setMinEvictableIdleTimeMillis(minEvictableIdleRunMillis);
		Map<Object,Object> map = new HashMap<Object,Object>();
		map.put("default", defaultDataSource);
		//除了默认配置的数据源外，还需要加载动态新增的数据源
		DataSourceUtil.initOthersDataSource(map,defaultDataSource);
		dynamicDataSource.setTargetDataSources(map);
		dynamicDataSource.setDefaultTargetDataSource(defaultDataSource);
		return dynamicDataSource;
	}
	
	
	@Bean
	public SqlSessionFactory sqlSessionFactory(@Qualifier("dynamicDataSource") DataSource dynamicDataSource) throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		bean.setDataSource(dynamicDataSource);
		bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
		bean.setTypeAliasesPackage("org.pygisserver.common.pytc.entity,org.pygisserver.common.entity");
		return bean.getObject();
	}
	
	@Bean(name="sqlSessionTemplate")
	public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
		return new SqlSessionTemplate(sqlSessionFactory);
	}
}
