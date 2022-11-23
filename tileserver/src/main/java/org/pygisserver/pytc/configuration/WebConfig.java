package org.pygisserver.pytc.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
public class WebConfig extends WebMvcConfigurationSupport {
 
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		AntPathMatcher matcher = new AntPathMatcher();
		matcher.setCaseSensitive(false);
		configurer.setPathMatcher(matcher);
	}
	
	
	@Override
	protected void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedOrigins("*").allowedHeaders("*")
		.allowedMethods("GET","POST","DELETE","OPTION","PUT").maxAge(3600);
	}
}
