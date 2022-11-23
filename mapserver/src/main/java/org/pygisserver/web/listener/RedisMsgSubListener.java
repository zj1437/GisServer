package org.pygisserver.web.listener;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.web.application.SpringUtil;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: redis消息的订阅监听
 * @author: 
 * @date: 2019-01-25
 */
@Slf4j
public class RedisMsgSubListener implements MessageListener {

	private Catalog catalog = SpringUtil.getBean("rawCatalog");

	private RedisTemplate<String, Object> redisTemplate;
	
	private static final String ADD_OPERATE = "add";
	
	private static final String REMOVE_OPERATE = "remove";
	
	private String add = ADD_OPERATE;
	
	private String remove = REMOVE_OPERATE;
	
	@Override
	public void onMessage(Message message, byte[] pattern) {

		byte[] body = message.getBody();
		LayerInfo layerInfo = (LayerInfo) redisTemplate.getValueSerializer().deserialize(body);
		byte[] channel = message.getChannel();
		String msgChannel = (String) redisTemplate.getValueSerializer().deserialize(channel);
		String[] channelArray = msgChannel.split(":");
		String address;
		try {
			address = InetAddress.getLocalHost().getHostAddress();
			if (add.equals(channelArray[0]) && !address.equals(channelArray[2])) {
				// 保存图层，保存资源信息
				catalog.add(layerInfo.getResource());
				catalog.add(layerInfo);
			}
			if (remove.equals(channelArray[0]) && !address.equals(channelArray[2])) {
				catalog.remove(layerInfo.getResource());
				catalog.remove(layerInfo);
			}
		} catch (UnknownHostException e) {
			log.error("RedisMsgSubListener onMessage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "本地IP"));
		}
	}

	/**
	 * @return the redisTemplate
	 */
	public RedisTemplate<String, Object> getRedisTemplate() {
		return redisTemplate;
	}

	/**
	 * @param redisTemplate the redisTemplate to set
	 */
	public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
}
