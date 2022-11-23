package org.pygisserver.web.shiro;

import java.io.Serializable;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.redis.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 使用redis实现对session的缓存
 * @author: 
 * @date: 2018-10-30
 */
@Component
@Slf4j
public class RedisSessionDao extends CachingSessionDAO {

	@Autowired
	private RedisUtils redisUtil;
	
	private static final String KEY_PREFIX = "shiro_redis_session";
	
	private String keyPrefix = KEY_PREFIX;
	
	private String getKey(String original) {
		return keyPrefix + original;
	}
	
	/**
	 * 更新session
	 * @param session
	 */
	@Override
	protected void doUpdate(Session session) {
		log.debug("更新session数据");
		redisUtil.storeValue(ExceptionsUtil.getServerIp(null),getKey(session.getId().toString()), session,1800);
	}

	/**
	 * 删除session
	 * @param session
	 */
	@Override
	protected void doDelete(Session session) {
		log.debug("删除对应的session数据");
		redisUtil.deleteKey(ExceptionsUtil.getServerIp(null),getKey(session.getId().toString()));
	}

	/**
	 * 创建sessionId
	 */
	@Override
	protected Serializable doCreate(Session session) {
		//生成当前的sessionId
		Serializable sessionId = generateSessionId(session);
		assignSessionId(session,sessionId);
		return sessionId;
	}

	/**
	 * 根据sessionId获取当前sessoion
	 * @param sessionId
	 * @return
	 */
	@Override
	protected Session doReadSession(Serializable sessionId) {
		return (Session) redisUtil.getValue(getKey(sessionId.toString()));
	}
}
