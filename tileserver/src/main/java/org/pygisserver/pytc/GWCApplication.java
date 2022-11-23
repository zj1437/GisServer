package org.pygisserver.pytc;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.pygisserver.common.util.SpringContextUtil;
import org.pygisserver.pytc.listener.PyGisServerInitStartupListener;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.converter.HttpMessageConverter;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

@EnableDubbo
@SpringBootApplication
@ComponentScan({ "org.pygisserver.pytc", "org.pygisserver.common","org.pygisserver.render" })
@ImportResource(locations = {"classpath*:geowebcache-core-context.xml"})
//@MapperScan({"org.pygisserver.pytc.dao", "org.pygisserver.common.mapper" , "org.pygisserver.render.dao", "org.pygisserver.render.mapper" })
@MapperScan({ "org.pygisserver.*.mapper", "org.pygisserver.*.dao" })
//@EnableEncryptableProperties
public class GWCApplication {

	public static void main(String[] args) {
	    if (args.length != 0){
            String type = args[0];
            if ("stop".equals(type)) {
                //pytc.jar适配windows服务需要处理服务关闭
                System.exit(0);
            }else{
                ConfigurableApplicationContext applicationContext = SpringApplication.run(GWCApplication.class, args);
                SpringContextUtil.setApplicationContext(applicationContext);
                //保证数据强制以X方向启动
                if (System.getProperty("org.geotools.referencing.forceXY") == null) {
                    System.setProperty("org.geotools.referencing.forceXY", "true");
                }
            }
        }else {
	        //开发环境时，启动参数空指针问题
            ConfigurableApplicationContext applicationContext = SpringApplication.run(GWCApplication.class, args);
            SpringContextUtil.setApplicationContext(applicationContext);
            //保证数据强制以X方向启动
            if (System.getProperty("org.geotools.referencing.forceXY") == null) {
                System.setProperty("org.geotools.referencing.forceXY", "true");
            }
        }
	}
	
	@Bean
	public HttpMessageConverters fastJsonHttpMessageConverters(){
        //1.需要定义一个Convert转换消息的对象
        FastJsonHttpMessageConverter fastConverter=new FastJsonHttpMessageConverter();
        //2.添加fastjson的配置信息，比如是否要格式化返回的json数据
        FastJsonConfig fastJsonConfig=new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat,SerializerFeature.WriteNullStringAsEmpty);
        //格式化日期
//        fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
        //3.在convert中添加配置信息
        fastConverter.setFastJsonConfig(fastJsonConfig);
        HttpMessageConverter<?> converter = fastConverter;
        return new HttpMessageConverters(converter);
    }
	
	
	 @Bean
	 public ServletListenerRegistrationBean geoserverInitStartupListener(){
		 ServletListenerRegistrationBean<PyGisServerInitStartupListener> registrationBean = new ServletListenerRegistrationBean<>(new PyGisServerInitStartupListener());
		 return registrationBean;
	 }
	 
	 
//	@Bean("jasyptStringEncryptor")
//	public StringEncryptor stringEncryptor() {
//		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
//		SimpleStringPBEConfig config = new SimpleStringPBEConfig();
//		config.setPassword("pygissoft");
//		config.setAlgorithm("PBEWithMD5AndDES");
//		config.setPoolSize("1");
//		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
//		config.setIvGeneratorClassName("org.jasypt.iv.NoIvGenerator");
//		config.setStringOutputType("base64");
//		encryptor.setConfig(config);
//		return encryptor;
//	}
}
