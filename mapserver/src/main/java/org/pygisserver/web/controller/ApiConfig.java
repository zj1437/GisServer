package org.pygisserver.web.controller;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;

/**
 * 使用swagger 实现controller层的接口文档的实现 
 * @Description: 
 * @author:  
 * @date: 2019-01-11
 */
@Configuration
@EnableSwagger2
@EnableWebMvc
public class ApiConfig {
	@Autowired
	private TypeResolver typeResolver;
	
	@Bean
	public Docket api() {
		Docket docket = new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.build()
				.apiInfo(apiInfo());
		docket.alternateTypeRules(
				//Map<String,int>
				AlternateTypeRules.newMapRule(String.class, Integer.class),
				//Map<string,List<string>>
				AlternateTypeRules.newRule(
				typeResolver.resolve(Map.class,String.class,typeResolver.resolve(List.class,String.class)),
				typeResolver.resolve(Map.class,String.class, WildcardType.class), Ordered.HIGHEST_PRECEDENCE)
		);
		return docket;
	}
	
	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
				.title("PYGISServer接口文档使用")
				.description("PYGISServer所有接口描述")
				.contact(new Contact("普祐信创院", "www.pykjcl.com", ""))
				.licenseUrl("")
				.version("1.2.0")
				.build();
	}
}
