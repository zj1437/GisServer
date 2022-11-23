package org.pygisserver.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ScheduleConfigure {

	private static final int THREA_POOL_SIZE = 10;
	
	
	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(THREA_POOL_SIZE);
		return scheduler;
	}
	
}
