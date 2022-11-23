package org.pygisserver.web.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.pygisserver.web.interceptor.LicenseCheckInterceptor;
import org.pygisserver.web.interceptor.PreViewMapServerInterceptor;
import org.pygisserver.web.wmsprotocol.interceptor.WmsServiceInterceptor;
import org.pygisserver.web.wmtsprotocol.interceptor.WmtsServiceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class PyWebMvcConfigurer extends WebMvcConfigurationSupport {

	@Autowired
	private WmtsServiceInterceptor wmtsServiceInterceptor;
	@Autowired
	private WmsServiceInterceptor wmsServiceInterceptor;

	@Autowired
	private PreViewMapServerInterceptor preViewMapServerInterceptor;
	@Autowired
	private LicenseCheckInterceptor licenseCheckInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(licenseCheckInterceptor).addPathPatterns("/**").excludePathPatterns("/license/**",
				"/home/**","/index/**","/static/**","/activation","/licenseServer/**");
		registry.addInterceptor(wmtsServiceInterceptor).addPathPatterns("/pytc/service/wmts");
		registry.addInterceptor(wmtsServiceInterceptor).addPathPatterns("/pytc/service/rest");
		registry.addInterceptor(wmsServiceInterceptor).addPathPatterns("/wms");
		registry.addInterceptor(preViewMapServerInterceptor).addPathPatterns("/wms", "/pytc/service/wmts");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
		registry.addResourceHandler("webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
		registry.addResourceHandler("/index.html").addResourceLocations("classpath:/templates/");
		registry.addResourceHandler("/activation.html").addResourceLocations("classpath:/templates/");
		registry.addResourceHandler("/data/**").addResourceLocations("classpath:/data/");
		registry.addResourceHandler("/config/**").addResourceLocations("classpath:/config/");
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// registry.addViewController("/").setViewName("index");
		registry.addViewController("/main/**").setViewName("index");
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
		FastJsonConfig fastJsonConfig = new FastJsonConfig();
		fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat, SerializerFeature.WriteNullListAsEmpty,
				SerializerFeature.WriteEnumUsingName, SerializerFeature.WriteMapNullValue);
		fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
		fastJsonConfig.setSerializeFilters();
		fastJsonConfig.setCharset(StandardCharsets.UTF_8);
		List<MediaType> fastMediaTypes = new ArrayList<>();
		fastMediaTypes.add(MediaType.APPLICATION_JSON);
		fastJsonHttpMessageConverter.setSupportedMediaTypes(fastMediaTypes);
		fastJsonHttpMessageConverter.setFastJsonConfig(fastJsonConfig);
		converters.add(fastJsonHttpMessageConverter);
		addDefaultHttpMessageConverters(converters);
		// 修改StringHttpMessageConvert的默认格式，保证返回String的时候 不会乱码
		converters.add(2, new StringHttpMessageConverter(StandardCharsets.UTF_8));
	}

	/**
	 * 处理跨域问题
	 */
//	@Override
//	protected void addCorsMappings(CorsRegistry registry) {
//		registry.addMapping("/**").allowCredentials(true).allowedOriginPatterns("*")
//				.allowedMethods("GET", "POST", "DELETE", "OPTION", "PUT").allowedHeaders("*").exposedHeaders("*");
//	}
}
