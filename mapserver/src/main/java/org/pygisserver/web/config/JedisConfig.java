package org.pygisserver.web.config;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Properties;

import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.util.CommonUtil;
import org.springframework.beans.factory.annotation.Value;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 手动加载redis的初始化
 * 
 * @author 
 */
public class JedisConfig
{
    private static JedisPool jedisPool = null;
    
    private static JedisConfig instance = null;
    
    
    @Value("${spring.redis.jedis.pool.max-active}")
	private String redisMaxActive;

	@Value("${spring.redis.jedis.pool.max-idle}")
	private String redisMaxIdle;
	
	@Value("${spring.redis.jedis.pool.max-wait}")
	private String redisMaxWait;
	
	@Value("${spring.redis.host}")
	private String redisHost;
	
	@Value("${spring.redis.port}")
	private String redisPort;
	
	@Value("${spring.redis.timeout}")
	private String redisTimeOut;
    
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
    public static JedisConfig getInstance() {
        if (instance == null) {
            synchronized (JedisConfig.class) {
                if (instance == null) {
					try {
						instance = new JedisConfig();
					} catch (IOException e) {
						throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(), "JedisConfig getInstance has error: 实例化jedisconfig出错："+e.getMessage(), e.getCause());
					}
                }
            }
            
        }
        return instance;
    }
}
