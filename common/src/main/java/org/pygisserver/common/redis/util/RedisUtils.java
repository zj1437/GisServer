package org.pygisserver.common.redis.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.StatusException;
import org.pygisserver.common.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


/**
 * @Description: 实现对redis的操作 
 * @author:  
 * @date: 2019-01-23
 */
@Component
@Slf4j
public class RedisUtils{
	/**
	 * 注册redisTemplate操作redis
	 */
	@Resource
	private RedisTemplate<String, Object> redisTemplate;
	
	/**
	 * 快速存储字符串
	 */
	@Resource
	private StringRedisTemplate StringRedisTemplate;
	
	@Autowired
	private SystemLogService systemLogService;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	/**
	 * 指定缓存的失效时间
	 * @param key
	 * @param time
	 * @return
	 */
	public boolean expire(String key,long time) {
		try {
			if (time > 0) {
				redisTemplate.expire(key, time, TimeUnit.SECONDS);
			}
			return true;
		} catch (Exception e) {
			log.error("RedisUtils expire has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key + "超时时间"));
		}
	}
	
	/**
	 * 执行key在指定的时间过期
	 * @param key
	 * @param date
	 */
	public boolean expireAtTime(String reqIP,String key,Date date) {
		try {
			redisTemplate.expireAt(key, date);
			return true;
		} catch (Exception e) {
			log.error("RedisUtils expireAtTime key {} has error {}",key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.STATUS_REDIS,
						"System","Redis服务", reqIP));
				
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_RELEASE, key));
			}
		}
	}
	
	
	/**
	 * 保存普通对象
	 * @param key
	 * @param object
	 */
	public void storeValue(String reqIP,String key,Object value) {
		try {
			redisTemplate.opsForValue().set(key, value);
		} catch (Exception e) {
			log.error("RedisUtils storeValue {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key));
			}
		}
	}
	
	
	/**
	 * 保存普通对象
	 * @param key
	 * @param object
	 * @param time
	 */
	public void storeValue(String reqIP,String key,Object value,long time) {
		try {
			redisTemplate.opsForValue().set(key, value,time,TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("RedisUtils storeValue {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key));
			}
		}
	}
	
	
	/**
	 * 保存普通对象
	 * @param key
	 * @param object
	 * @param time
	 */
	public void storeStrValue(String reqIP,String key,String value,long time) {
		try {
			StringRedisTemplate.opsForValue().set(key, value,time,TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("RedisUtils storeValue {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key));
			}
		}
	}
	
	
	/**
	 * 获取普通对象
	 * @param key
	 * @return
	 */
	public String getStrValue(String key) {
		try {
			return StringRedisTemplate.opsForValue().get(key);
		} catch (Exception e) {
			log.error("RedisUtils getValue has error {}",ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS,"System", "Redis服务", ExceptionsUtil.getServerIp(null)));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, key));
			}
		}
	}
	
	/**
	 * 获取普通对象
	 * @param key
	 * @return
	 */
	public Object getValue(String key) {
		try {
			if(StringUtils.isNotBlank(key)){
				return redisTemplate.opsForValue().get(key);
			}else {
				return null;
			}
		} catch (Exception e) {
			log.error("RedisUtils getValue has error {}",ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS,"System", "Redis服务", ExceptionsUtil.getServerIp(null)));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, key));
			}
		}
	}
	
	
	/**
	 * 将list存入缓存
	 * @param key
	 * @param value
	 * @param time
	 * @return
	 */
	public boolean storeList(String key,Object value,long time) {
		try {
			redisTemplate.opsForList().rightPush(key, value);
			if (time > 0) {
				expire(key,time);
			}
			return true;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error("RedisUtils store {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key));
		}
	}
	
	/**
	 * 将list存入缓存
	 * @param key
	 * @param value
	 * @param time
	 * @return
	 */
	public boolean storeLists(String key,List<Object> value,long time) {
		try {
			redisTemplate.opsForList().rightPushAll(key, value);
			if (time > 0) {
				expire(key,time);
			}
			return true;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error("RedisUtils storeLists {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key));
		}
	}
	
	
	/**
	 * 从链表中获取根据key弹出元素
	 * @param key
	 * @return
	 */
	public List<?> getList(String reqIP,String key){
		try {
			return redisTemplate.opsForList().range(key, 0, -1);
		} catch (Exception e) {
			log.error("RedisUtils hmset {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, key));
			}
		}
	}
	
	
	
	/**
	 * 从链表中获取根据key弹出元素
	 * @param key
	 * @return
	 */
	public void removeListValue(String reqIP,String key,int count,Object value){
		try {
			redisTemplate.opsForList().remove(key, count, value);
		} catch (Exception e) {
			log.error("RedisUtils hmset {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DELETE, key));
			}
		}
	}
	
	
	/**
	 * 修改链表某一个位置的数据
	 * @param key
	 * @param index
	 * @param value
	 */
	public void setList(String reqIP,String key,long index,Object value) {
		try {
			redisTemplate.opsForList().set(key, index, value);
		} catch (Exception e) {
			log.error("RedisUtils setList {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_EDIT, key));
			}
		}
	}
	
	/**
	 * 存入map类型
	 * @param key
	 * @param map
	 * @return
	 */
	public boolean storeHash(String reqIP,String key,Map<Object,Object> map) {
		try {
			redisTemplate.opsForHash().putAll(key, map);
			return true;
		} catch (Exception e) {
			log.error("RedisUtils hmset {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key));
			}
		}
	}
	
	/**
	 * 存入map类型
	 * @param key
	 * @param map
	 * @return
	 */
	public boolean storeHash(String key,Map<String,Object> map,long time) {
		try {
			redisTemplate.opsForHash().putAll(key, map);
			if (time > 0) {
				expire(key,time);
			}
			return true;
		} catch (ResourceException e) {
			throw new ResourceException(e.getErrorCode(),e.getMessage());
		} catch (Exception e) {
			log.error("RedisUtils storeHash {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, key));
		}
	}
	
	/**
	 * 获取hash的值
	 * @param key
	 * @param item
	 * @return
	 */
	public Object getHash(String reqIP,String key,String item) {
		try {
			return redisTemplate.opsForHash().get(key, item);
		} catch (Exception e) {
			log.error("RedisUtils getHash has error {}",ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, key));
			}
		}
	}
	
	
	/**
	 * 获取某一个key对象的hash对象
	 * @param key
	 * @return
	 */
	public Map<Object,Object> getHash(String reqIP,String key){
		try {
			return redisTemplate.opsForHash().entries(key);
		} catch (Exception e) {
			log.error("RedisUtils getHash has error {}",ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, key));
			}
		}
	}
	
	/**
	 * 判断key是否存在
	 * @param key  
	 * @return
	 */
	public boolean hasKey(String reqIP,String key) {
		try {
			return redisTemplate.hasKey(key);
		} catch (Exception e) {
			log.error("RedisUtils hasKey has error {}",ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			}
		}
	}

	/**
	 * 批量删除key
	 * @param key
	 * @return
	 */
	public void deleteKey(String reqIP,String...key) {
		try {
			if (key != null && key.length > 0) {
				if (key.length == 1) {
					redisTemplate.delete(key[0]);
				}else {
					redisTemplate.delete(Arrays.asList(key));
				}
			}
		} catch (Exception e) {
			log.error("RedisUtils deleteKey {} has error {}", key,ExceptionsUtil.stackTraceToStr(e));
			if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
						ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DELETE, key.toString()));
			}
		}
	}
	
    /**
     * redis发布消息
     * @param channel
     * @param message
     */
    public void publishMsg(String reqIP,String channel,String message) {
        try {
        	redisTemplate.convertAndSend(channel, message);
        } catch (Exception e) {
        	log.error("RedisUtils publishMsg has error {}", ExceptionsUtil.stackTraceToStr(e));
        	if(ExceptionsUtil.isRedisError(ExceptionsUtil.stackTraceToStr(e))) {
        		systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, 
        				ExceptionsUtil.STATUS_REDIS, "System","Redis服务", reqIP));
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.STATUS_REDIS);
			} else {
				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PUBLISH, "Redis"));
			}
        }
    }
}
