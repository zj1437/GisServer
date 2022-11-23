package org.pygisserver.pytc.thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

public class SeedTaskThreadPool extends ThreadPoolExecutor {

	private static final ThreadFactory tf = new CustomizableThreadFactory("Seeder Thread-");
	
	public SeedTaskThreadPool(int corePoolSize, int maximumPoolSize) {
		super(corePoolSize, 
			  maximumPoolSize, 
			  60,
              TimeUnit.SECONDS,
              new LinkedBlockingQueue<Runnable>(),
              tf);
	}
}
