package org.pygisserver.common.util;

import org.springframework.context.ApplicationContext;

/**
 * @Description: Spring的上下文内容加载 
 * @author:  
 * @date: 2019-12-09
 */
public class SpringContextUtil {

	private static ApplicationContext applicationContext;
	
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
 
    public static void setApplicationContext(ApplicationContext applicationContext) {
        SpringContextUtil.applicationContext = applicationContext;
    }
 
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }
 
    public static Object getBean(Class<?> clazz) {
        return applicationContext.getBean(clazz);
    }
}
