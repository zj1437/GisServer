package org.pygisserver.web.application;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 手动加载spring bean  
 * 
 * 
 * @author 
 *
 */
@Component
public class SpringUtil implements ApplicationContextAware
{
	
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException
    {
        this.applicationContext = context;
    }
    
    public static <T>T getBean(Class<T> clazz,String beanName)
    {
        return applicationContext.getBean(beanName, clazz);
    }
    
    public static <T>T getBean(String beanName)
    {
        return (T) applicationContext.getBean(beanName);
    }
}
