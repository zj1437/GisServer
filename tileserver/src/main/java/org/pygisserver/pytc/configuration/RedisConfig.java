package org.pygisserver.pytc.configuration;

import java.nio.charset.Charset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redisTemplate配置类
 * @Description: 
 * @author:  
 * @date: 2021-06-08
 */
@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer(Charset.forName("UTF-8")));
//		Jackson2JsonRedisSerializer jackson2JsonRedisSerialize = new Jackson2JsonRedisSerializer(Object.class);
//		ObjectMapper objectMapper = new ObjectMapper();
//		objectMapper.setVisibility(PropertyAccessor.ALL,JsonAutoDetect.Visibility.ANY);
//		objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//		jackson2JsonRedisSerialize.setObjectMapper(objectMapper);
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		return redisTemplate;
	}
}
