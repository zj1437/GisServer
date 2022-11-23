package org.pygisserver;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.redis.util.JedisConfig;
//import org.pygisserver.common.tdb.util.TdbUtils;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.common.util.SpringContextUtil;
import org.pygisserver.web.dao.SystemConfigMapper;
import org.pygisserver.render.tiled.TiledInfoReadAbstract;
import org.pygisserver.render.util.TdbUtils;
import org.pygisserver.web.entity.ServerCacheConfigEntity;
import org.geoserver.filters.FlushSafeFilter;
import org.geoserver.filters.ThreadLocalsCleanupFilter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.sun.management.OperatingSystemMXBean;

import redis.clients.jedis.Jedis;

@EnableDubbo
@SpringBootApplication
@ServletComponentScan(basePackages="org.pygisserver.web.listener")
@ImportResource(locations = { "classpath:spring/external-bean.xml" })
@MapperScan({ "org.pygisserver.*.mapper", "org.pygisserver.*.dao" })
@EnableAsync
@PropertySource(value = {"classpath:license-config.properties"})
public class MapServerApplocation extends SpringBootServletInitializer {

	public static void main(String[] args) {
//		initConfigFilePath();
		ConfigurableApplicationContext applicationContext = SpringApplication.run(MapServerApplocation.class, args);
		SpringContextUtil.setApplicationContext(applicationContext);
//		updateServerCacheMaxConfig();
	}
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		//保证数据强制以X方向启动
		if (System.getProperty("org.geotools.referencing.forceXY") == null) {
			System.setProperty("org.geotools.referencing.forceXY", "true");
		}
		//tomcat部署环境下
		initConfigFilePath();
		return builder.sources(MapServerApplocation.class);
	}
	
	@Override
	protected WebApplicationContext run(SpringApplication application) {
		WebApplicationContext context = (WebApplicationContext) application.run();
		SpringContextUtil.setApplicationContext(context);
		updateServerCacheMaxConfig();
		return context;
	}
	
	
	//服务内存以及磁盘检测项
	private static void updateServerCacheMaxConfig() {
		//1.获取当前机器的最大物理内存
		OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		int maxMemorySize = (int)Math.ceil((osmxb.getTotalPhysicalMemorySize()/(1024.0 * 1024.0))/4);
		//3.获取访问缓存的存储路径，先从Redis中获取，如果有则将其存入环境变量，否则就读取默认路径
		SystemConfigMapper systemConfigMapper = (SystemConfigMapper)SpringContextUtil.getBean("systemConfigMapper");
		ServerCacheConfigEntity entity = systemConfigMapper.queryCacheConfig();
		String defaltCachePath = entity.getCachePath();
		if(StringUtils.isBlank(defaltCachePath)) {
			//如果数据库中为空，则获取默认路径
			defaltCachePath = Constant.TILE_CACHE_TMP;
		}
		//2.获取服务所在目录下的磁盘空间
		File file = new File(defaltCachePath);
		int maxDiskSize = (int)Math.ceil(file.getFreeSpace()/(1024.0 * 1024.0 * 1024.0));
		int actualDisk = entity.getActualDisk() == entity.getMaxDisk() ? maxDiskSize : entity.getActualDisk();
		if (actualDisk <= entity.getMinDisk()) actualDisk = maxDiskSize;
		systemConfigMapper.updateMaxCacheConfig(maxMemorySize, maxDiskSize,actualDisk,defaltCachePath);
		//如果实际设置的访问缓存在合法范围内，则使用该值，否则使用当前磁盘的最大可用空间值
		long maxSize = 1L * actualDisk * 1024 * 1024 * 1024;
//		TileServiceController.setRedisStatus(entity.getUseRedisCache());
		String parentPath = defaltCachePath;
		parentPath = parentPath.endsWith("PYGISServerCache") ? parentPath : parentPath + File.separator + "PYGISServerCache";
		System.setProperty(TdbUtils.USER_VISIT_CACHE_DIR,parentPath);
		TiledInfoReadAbstract.maxCacheSize = maxSize;
		//将当前目录的磁盘使用情况注入环境
		TiledInfoReadAbstract.curDirSize = FileUtil.getFileLength(parentPath);
		
		String stylePath = System.getProperty("SYSTEM_STYLE_PATH");
		String dbPath = systemConfigMapper.querySysStylePath();
		if (!stylePath.equalsIgnoreCase(dbPath)) {
			systemConfigMapper.updateSysStylePath(stylePath);
		}
	}
	
	private static void initConfigFilePath() {
		//加载配置文件，查看是否有配置pygisserver.data.location，如果配置，则以配置为准，没有配置，则以默认为主
		String dataDir = MapServerApplocation.class.getClassLoader().getResource("data").getPath();
		String dataCache = null;
		if (StringUtils.isNotBlank(dataDir)) {
			File file = new File(FileUtil.decodeFilePath(dataDir));
			File yamFile = new File(file.getParentFile(),"application.yml");
			dataCache = loadYmlFile(FileUtil.decodeFilePath(yamFile.getAbsolutePath()));
		}
		/**
		 * jar 包加载
		 */
//		String dataDir = MapServerApplocation.class.getClassLoader().getResource("data").getPath();
//		String dataCache = null;
//		if (dataDir.contains("mapserver.jar!")) {
//			dataDir = dataDir.substring(5, dataDir.indexOf("mapserver.jar!"));
//			dataCache = loadYmlFile(dataDir + File.separator + "config" + File.separator + "application.yml");
//		}else {
//			dataCache = loadYmlFile("src/main/resources/application.yml");
//		}
//		File dataFile = null;
//		if (StringUtils.isNotBlank(dataCache)) {
//			dataFile = new File(dataCache);
//		}else {
//			if (dataDir.contains("mapserver.jar!")) {
//				dataDir = dataDir.substring(5, dataDir.indexOf("mapserver.jar!"));
//				dataFile = new File(FileUtil.decodeFilePath(dataDir),"data");
//			} else {
//				dataFile = new File(FileUtil.decodeFilePath(dataDir));
//			}
//		}
		/**
		 * war包加载
		 */
		File dataFile = null;
		if (StringUtils.isNotBlank(dataCache)) {
			dataFile = new File(FileUtil.decodeFilePath(dataCache));
		}else {
			dataFile = new File(FileUtil.decodeFilePath(dataDir));
		}
		
		System.setProperty("dataDir", dataFile.getAbsolutePath());
		StringBuilder sb = new StringBuilder(dataFile.getAbsolutePath());
		System.setProperty("SYSTEM_COLOR_PATH", sb.append(File.separator).append("color").toString());
		StringBuilder sb1 = new StringBuilder(dataFile.getAbsolutePath());
		System.setProperty("SYSTEM_THUMBAL_PATH", sb1.append(File.separator).append("thumbal").toString());
		StringBuilder sb2 = new StringBuilder(dataFile.getAbsolutePath());
		String stylePath = sb2.append(File.separator).append("styles").toString();
		System.setProperty("SYSTEM_STYLE_PATH", stylePath);
		saveStylePathToRedis(stylePath);
		StringBuilder sb3 = new StringBuilder(dataFile.getAbsolutePath());
		System.setProperty("SYSTEM_SAMPLES_PATH", sb3.append(File.separator).append("legendsamples").toString());
	}
	
	private static void saveStylePathToRedis(String stylePath) {
		Jedis jedis = null;
    	try {
    		JdkSerializationRedisSerializer jdkSerialization = new JdkSerializationRedisSerializer();
    		StringRedisSerializer keySerializer = new StringRedisSerializer(Charset.forName("UTF-8"));
    		jedis = JedisConfig.getInstance().getJedis();
    		byte[] key = keySerializer.serialize("SYSTEM_STYLE_PATH");
    		byte[] value = jdkSerialization.serialize(stylePath);
    		jedis.set(key, value);
		} 
    	catch (Exception e) 
    	{
    		e.getStackTrace();
		} 
    	finally
        {
            JedisConfig.getInstance().returnResource(jedis);
        }
	}
	
	
	private static String loadYmlFile(String ymlPath) {
		YamlPropertiesFactoryBean yamlBean = new YamlPropertiesFactoryBean();
		yamlBean.setResources(new FileSystemResource(ymlPath));
		if (yamlBean.getObject().get("pygisserver.data.location") != null) {
			return yamlBean.getObject().get("pygisserver.data.location").toString();
		}
		return null;
	}
	
	
//	@Bean("jasyptStringEncryptor")
//	public StringEncryptor stringEncryptor() {
//		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
//		SimpleStringPBEConfig config = new SimpleStringPBEConfig();
//		config.setPassword("puyousoft");
//		config.setAlgorithm("PBEWithMD5AndDES");
//		config.setPoolSize("1");
//		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
//		config.setIvGeneratorClassName("org.jasypt.iv.NoIvGenerator");
//		config.setStringOutputType("base64");
//		encryptor.setConfig(config);
//		return encryptor;
//	}
	
	/**
	 * 注册原geoserver的过滤器
	 */
	@Bean
	public FilterRegistrationBean shiroFilterProxy() {
		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
		registrationBean.setFilter(new DelegatingFilterProxy("shiroFilter"));
		registrationBean.addInitParameter("targetFilterLifecycle", "true");
		registrationBean.setUrlPatterns(Arrays.asList("/*"));
		return registrationBean;
	}
	
	
	/**
	 * 设置跨域过滤 如果tomcat设置跨域则不需要设置 
	 * 由于用到了shiro 所以需要设置
	 * @return
	 */
//	@Bean
//	public FilterRegistrationBean crosFilterProxy() {
//		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
//		UrlBasedCorsConfigurationSource cors = new UrlBasedCorsConfigurationSource();
//		cors.registerCorsConfiguration("/**", buildCorsConfig());
//		registrationBean.setFilter(new CorsFilter(cors));
//		registrationBean.setOrder(0);
//		return registrationBean;
//	}
//	
//	
//	private CorsConfiguration buildCorsConfig() {
//		CorsConfiguration config = new CorsConfiguration();
//		config.addAllowedHeader("*");
//		config.addAllowedOriginPattern("*");
//		config.addAllowedMethod("*");
//		config.setAllowCredentials(true);
//		return config;
//	}
	
	/**
	 * 注册原geoserver的过滤器
	 */
	@Bean
	public FilterRegistrationBean flushSafeFilter() {
		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
		registrationBean.setFilter(new FlushSafeFilter());
		registrationBean.setUrlPatterns(Arrays.asList("/*"));
		return registrationBean;
	}
	
	/**
	 * 注册原geoserver的过滤器
	 */
	@Bean
	public FilterRegistrationBean threadLocalsCleanupFilter() {
		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
		registrationBean.setFilter(new ThreadLocalsCleanupFilter());
		registrationBean.setUrlPatterns(Arrays.asList("/*"));
		return registrationBean;
	}
}
