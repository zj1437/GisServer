package org.pygisserver.render;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;

//@EnableDubbo
//@SpringBootApplication
//@ComponentScan({"org.pygisserver.render.*","org.pygisserver.common.redis.util",
//	"org.pygisserver.common.service","org.pygisserver.common.exception","org.pygisserver.common.tiled"})
//@MapperScan({"org.pygisserver.common.mapper" })
public class RenderApp {

	public static void main(String[] args) {
		SpringApplication.run(RenderApp.class);
		//保证数据强制以X方向启动
		if (System.getProperty("org.geotools.referencing.forceXY") == null) {
			System.setProperty("org.geotools.referencing.forceXY", "true");
		}
	}
}
