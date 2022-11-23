package org.pygisserver.pytc.configuration;

import org.pygisserver.pytc.thread.SeedTaskThreadPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: 处理带参数的bean的实例化 
 * @author:  
 * @date: 2021-04-01
 */
@Configuration
public class ConstructBeanConfig {

	/**
	 * 切图任务线程池
	 * @return
	 */
	@Bean
	public SeedTaskThreadPool seedTaskThreadPool() {
		return new SeedTaskThreadPool(32,64);
	}
}
