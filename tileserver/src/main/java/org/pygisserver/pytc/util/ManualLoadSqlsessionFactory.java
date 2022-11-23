package org.pygisserver.pytc.util;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @Description: 手动获取sqlsessionfactory 
 * @author:  
 * @date: 2020-05-28
 */
public class ManualLoadSqlsessionFactory {

	private static SqlSessionFactory sqlSessionFactory;
	
	private static SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
	
	private static final String TYPE_PACKAGE = "org.pygisserver.common.entity,org.pygisserver.common.pytc.entity";
	
	public static void builderSqlsessionFactory(DruidDataSource dataSource) throws Exception {
		sqlSessionFactoryBean.setDataSource(dataSource);
		sqlSessionFactoryBean.setTypeAliasesPackage(TYPE_PACKAGE);
		sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
		sqlSessionFactory = sqlSessionFactoryBean.getObject();
	}
	
	
	public static SqlSession getSqlseesion() {
		return sqlSessionFactory.openSession(true);
	}
	
	
}
