package org.pygisserver.common.redis.util;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Properties;

import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.util.CommonUtil;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 手动加载redis的初始化
 * 
 * @author 
 */
@Slf4j
public class JedisConfig
{
    private static JedisPool jedisPool = null;
    
    private static JedisConfig instance = null;
    
    /**
     * 初始化jedisPool
     * @throws IOException 
     */
    private void initJedisPool() throws IOException
    {
    	String operatorName = System.getProperty("os.name");
    	//widows和linux下需要对路径的前缀进行不同处理
    	String path;
    	if (operatorName.indexOf("Window") > -1) {
    		path = JedisConfig.class.getClassLoader().getResource("redis.properties").getPath().replaceFirst("/", "");
    	}else {
    		path = JedisConfig.class.getClassLoader().getResource("redis.properties").getPath();
    	}
    	if (path.contains("mapserver.jar!")) {
			path = path.substring(5, path.indexOf("mapserver.jar!")) + File.separator + "redis.properties";
		}
    	if (path.contains("pytc.jar!")) {
            if ("linux".equalsIgnoreCase(operatorName)) {
                path = "/opt/pygisserver/config/redis.properties";
            } else {
                path = path.substring(5, path.indexOf("pytc.jar!")) + File.separator + "redis.properties";
            }
		}
    	Properties pros = CommonUtil.loadPropertyFile(URLDecoder.decode(path,"UTF-8"));
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(Integer.parseInt(pros.get("redis.max_active").toString()));
        config.setMaxIdle(Integer.parseInt(pros.get("redis.max_idle").toString()));
        config.setMaxWaitMillis(Integer.parseInt(pros.get("redis.max_wait").toString()));
        config.setTestOnBorrow(true);
        jedisPool = new JedisPool(config, 
        						  pros.getProperty("redis.host").toString(), 
        						  Integer.parseInt(pros.get("redis.port").toString()), 
        						  Integer.parseInt(pros.get("redis.timeout").toString()),
        						  null,
        						  1);
    }
    
    public synchronized Jedis getJedis()
    {
        Jedis jedis = null;
        if (jedisPool != null)
        {
            jedis = jedisPool.getResource();
        }
        return jedis;
    }
    
    /**
     * 如果jedis为空，则进行初始化
     * @throws IOException 
     */
    private JedisConfig() throws IOException
    {
        if (jedisPool == null)
        {
            initJedisPool();
        }
    }
    
    /**
     * 释放jedis资源
     * @param jedis
     */
    public void returnResource(Jedis jedis)
    {
        if (jedis != null) 
        {
            jedis.close();
        }
    }
    
    
    /**
     * 
     * 单例获取JedisConfig实例
     * 
     * @return 返回当前的jedis实例
     */
    public static JedisConfig getInstance()
    {
        if (instance == null)
        {
            synchronized (JedisConfig.class)
            {
                if (instance == null)
                {
					try {
						instance = new JedisConfig();
					} catch (IOException e) {
						log.error("JedisConfig getInstance has error:" + ExceptionsUtil.stackTraceToStr(e));
						throw new BaseException(ExceptionsUtil.ERROR_500, ExceptionsUtil.formatInfo(ExceptionsUtil.BASE_SETUP, "Redis"));
					}
                }
            }
            
        }
        return instance;
    }
}
