package org.pygisserver.pytc.aop;

import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.pygisserver.pytc.configuration.DataSourceContextHolder;

/**
 * @Description: 瓦片缓存服务切片类
 * @author: 
 * @date: 2019-12-25
 */
//@Component
//@Aspect
public class TileCacheServiceAop {

	@Pointcut(value="execution(* org.pygisserver.pytc.controller.TileCacheController.*(..))"
			+ "|| execution(* org.pygisserver.pytc.controller.TileServiceController.*(..))")
	private void beforeSetDataSource() {};
	
	@Before("beforeSetDataSource()")
	public void setDataSurce() {
		DataSourceContextHolder.setDataSource("default");
	}
}
