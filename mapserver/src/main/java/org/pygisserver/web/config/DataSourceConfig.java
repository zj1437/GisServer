package org.pygisserver.web.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }




    public DruidDataSource getDataSource(String dataSourceName){
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(30);
        dataSource.setMaxActive(500);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(60000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(false);
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnReturn(false);
        return dataSource;
    }

}
