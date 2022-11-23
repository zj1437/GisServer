package org.pygisserver.common.logback;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.UserLogbackInfo;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.redis.util.JedisConfig;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.FileUtil;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

@Component
@Slf4j
public class LogBackXmlConfigHelper {
	
	//系统中logback配置文件路径
	public static final String logbackPath = "/config/logback-spring.xml";
	
	
	private static final String fSepartor = File.separator + File.separator;
	
	public static final String mapserverLogBack = "mapserverLogBack";
	
	public static final String mapserverLogBackFile = "mapserver-logback-spring.xml";
	
	public static final String tileserverLogBack = "tileserverLogBack";
	
	public static final String tileserverLogBackFile = "tileserver-logback-spring.xml";
	
	//logback临时文件存储路径
	public static String getTmpLogFilePath(String keyWord) {
		String path = System.getenv("PYGISServer_HOME");
		if (StringUtils.isBlank(path)) {
			path = LogBackXmlConfigHelper.class.getResource(logbackPath).getPath();
			if (path.contains("target")) {
				path = path.substring(0,path.indexOf("target"));
			}
			if (path.contains("webapps")) {
				path = path.substring(0,path.indexOf("webapps") + 7);
			}
		}
		
		File file = new File(path);
		path = file.getAbsolutePath()  + File.separator + "logback" + File.separator;
		if (keyWord.equals(mapserverLogBack)) {
			path = path + mapserverLogBackFile;
		} else if (keyWord.equals(tileserverLogBack)) {
			path = path + tileserverLogBackFile;
		}
		return path;
	}
	
	/**
	 * 用户自定义日志配置入口方法
	 * @param newPath
	 */
	public void resetLogbackInfo(UserLogbackInfo logInfo, String keyWord) {
		String newPath = logInfo.getCurLogPath();
        //将用户自定义日志存储路径的单路径分隔符替换为双路径分隔符，防止该路径解析失败
        String path = null;
        //windows:\\ linux:/
        if(newPath.contains("\\")){
        	path = newPath.replace("\\", fSepartor);
        } else if(newPath.contains("/")) {
        	path = newPath;
        }
        logInfo.setCurLogPath(path);
        log.info("自定义日志path："+path);
        //对logback编辑并保存，如果isReload为false则说明logback配置文件无需编辑，系统默认的日志配置也无需刷新
        boolean isReload = editDocument(logInfo,keyWord);
		if(isReload) {
        	load(keyWord);
        }
	}
	
	/**
	 * 查询当前服务的日志配置信息
	 * @return
	 */
	public UserLogbackInfo getLogbackInfo(String keyWord) {
		Jedis jedis =  null;
		try {
			jedis = JedisConfig.getInstance().getJedis();
			UserLogbackInfo entity = null;
			if (keyWord.equals(mapserverLogBack)) {
				entity = (UserLogbackInfo)CommonUtil.unserialize(jedis.get(mapserverLogBack.getBytes()));
			} else if (keyWord.equals(tileserverLogBack)) {
				entity = (UserLogbackInfo)CommonUtil.unserialize(jedis.get(tileserverLogBack.getBytes()));
			}else {
				entity = (UserLogbackInfo)CommonUtil.unserialize(jedis.get(mapserverLogBack.getBytes()));
				if(entity == null) {
					entity = (UserLogbackInfo)CommonUtil.unserialize(jedis.get(tileserverLogBack.getBytes()));
				}
			}
			if(entity != null) {
				return entity;
			} else {
				entity = new UserLogbackInfo();
				Document doc = getDocument(getTmpLogFilePath(keyWord),false);
				Element root = doc.getDocumentElement();
				NodeList nodeList = root.getElementsByTagName("property");
				String name = null;
				String value = null;
				for(int i = 0;i < nodeList.getLength();i++) {
					Node node = nodeList.item(i);
					name = node.getAttributes().getNamedItem("name").getTextContent();
					value = node.getAttributes().getNamedItem("value").getTextContent();
					if(name.equals("LOG_PATH")) {
						entity.setCurLogPath(value);
					}
					
					if(name.equals("LOG_FILE_SIZE")) {
						int size = Integer.valueOf(value);
						entity.setLogFileSize(size);
					}
					
					if(name.equals("LOG_MAX_SAVE")) {
						int size = Integer.valueOf(value);
						entity.setLogMaxSave(size);
					}
				}
				InputStream is = null;
				try {
					if (keyWord.equals(mapserverLogBack) || keyWord.equals(tileserverLogBack)) {
						File file = new File(getTmpLogFilePath(keyWord));
						if (file.exists()) {
							is = new FileInputStream(file);
						} else {
							is = LogBackXmlConfigHelper.class.getResourceAsStream(logbackPath);
						}
					} else {
						is = LogBackXmlConfigHelper.class.getResourceAsStream(logbackPath);
					}
				} catch (FileNotFoundException e) {
					log.error("LogBackConfigLoader getLogbackInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
				}
				entity.setLogbackDetail(ioStream2Bytes(is));
				if (keyWord.equals(mapserverLogBack)) {
					jedis.set(mapserverLogBack.getBytes(), CommonUtil.serialize(entity));
				} else if (keyWord.equals(tileserverLogBack)) {
					jedis.set(tileserverLogBack.getBytes(), CommonUtil.serialize(entity));
				}
				return entity;
			}
		} finally {
			jedis.close();
		}
		
	}
	
	/**
	 * 编辑logback配置文件
	 * @param logPath
	 * @param fileSize
	 * @param saveDays
	 * @return
	 * @throws Exception
	 */
	public boolean editDocument(UserLogbackInfo logInfo,String keyWord) {
		Jedis jedis =  null;
		try {
			jedis = JedisConfig.getInstance().getJedis();
			boolean isNeedReload = false;
			String logPath = logInfo.getCurLogPath();
			int fileSize = logInfo.getLogFileSize();
			int saveDays = logInfo.getLogMaxSave();
			//1.如果三个参数均无实际意义，则无需编辑logback文件
			if(StringUtils.isBlank(logPath) && fileSize == 0 && saveDays == 0) {
				return isNeedReload;
			}
			
			//2.从Redis中获取缓存，如果请求的参数和Redis中缓存的信息完全一致，则直接返回
			UserLogbackInfo entity = (UserLogbackInfo)CommonUtil.unserialize(jedis.get(keyWord.getBytes()));
			if (entity != null && fileSize == entity.getLogFileSize() && 
					saveDays == entity.getLogMaxSave() && logPath.equals(entity.getCurLogPath())) {
				return isNeedReload;
			}
			
			//3.只有当参数不一致时，再执行编辑和保存操作
			Document doc = getDocument(logbackPath,true);
			Element root = doc.getDocumentElement();
			NodeList nodeList = root.getElementsByTagName("property");
			String name = null;
			String value = null;
			for(int i = 0;i < nodeList.getLength();i++) {
				Node node = nodeList.item(i);
				name = node.getAttributes().getNamedItem("name").getTextContent();
				value = node.getAttributes().getNamedItem("value").getTextContent();
				if(name.equals("LOG_PATH")) {
					if(logPath.endsWith(value)) {
						if(logPath.equals(value)) {
							continue;
						} else {
							isNeedReload = true;
							node.getAttributes().getNamedItem("value").setTextContent(logPath);
						}
					} else {
						isNeedReload = true;
						node.getAttributes().getNamedItem("value").setTextContent(logPath);
					}
				}
				
				if(name.equals("LOG_FILE_SIZE")) {
					int size = Integer.valueOf(value);
					if(size == fileSize) {
						continue;
					} else {
						isNeedReload = true;
						node.getAttributes().getNamedItem("value").setTextContent(fileSize + "");
					}
				}
				
				if(name.equals("LOG_MAX_SAVE")) {
					int size = Integer.valueOf(value);
					if(size == fileSize) {
						continue;
					} else {
						isNeedReload = true;
						node.getAttributes().getNamedItem("value").setTextContent(saveDays + "");
					}
				}
			}
			//4.编辑结束后将其写入临时文件
			saveDocument(doc,keyWord);
			//5.以流的形式读入将其缓存进Redis中
			byte[] logIS = logbackFile2Byte(getTmpLogFilePath(keyWord));
			UserLogbackInfo userLogbackInfo = new UserLogbackInfo();
			userLogbackInfo.setCurLogPath(logPath);
			userLogbackInfo.setLogFileSize(fileSize);
			userLogbackInfo.setLogMaxSave(saveDays);
			userLogbackInfo.setLogbackDetail(logIS);
			UserLogbackInfo info = (UserLogbackInfo)CommonUtil.unserialize(jedis.get(keyWord.getBytes()));
			if(info == null) {
				jedis.set(keyWord.getBytes(), CommonUtil.serialize(userLogbackInfo));
			} else {
				jedis.set(keyWord.getBytes(), CommonUtil.serialize(userLogbackInfo));
			}
			return isNeedReload;
		} finally {
			jedis.close();
		}
		
	}
	
	//自定义日志存储信息核心方法，调用logback配置类进行更新
	public void load(String keyWord) {
		LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(lc);
        lc.reset();
        try {
			jc.doConfigure(bytes2IOStream(logbackFile2Byte(getTmpLogFilePath(keyWord))));
		} catch (JoranException e) {
			log.error("LogBackConfigLoader load has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,"logback日志配置文件重加载失败");
		}
	}
	
	/**
	 * 将读取的文件流转换为byte数组，以便存入Redis
	 * @param is
	 * @return
	 */
	public static byte[] ioStream2Bytes(InputStream is) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[10240];
			int i = 0;
			while ((i = is.read(buffer)) != -1) {
				baos.write(buffer,0,i);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			log.error("LogBackConfigLoader getDocument has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE,  "logback配置文件"));
		}
	}
	
	/**
	 * 将Redis中的logback配置文件byte数组转换为流，以便logback配置类调用
	 * @param buffer
	 * @return
	 */
	public static InputStream bytes2IOStream(byte[] buffer) {
		InputStream is = null;
		is = new ByteArrayInputStream(buffer);
		return is;
	}
	
	/**
	 * 先将文件转换为byte[]流，否则无法存储在Redis中
	 * @param path
	 * @return
	 */
	public static byte[] logbackFile2Byte(String path) {
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(new File(path)));
			byte[] buffer = new byte[10240];
			int length = 0;
			int i = 0;
			while ((length = bis.read()) != -1) {
				buffer[i] = (byte)length;
				i++;
			}
			byte[] buffer1 = new byte[i];
			System.arraycopy(buffer, 0, buffer1, 0, i);
			return buffer1;
		} catch (IOException e) {
			log.error("LogBackConfigLoader getDocument has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND,  "logback配置文件"));
		} finally {
			if (bis != null) FileUtil.closeStream(bis);
		}
	}
	
	/**
	 * 获取logback配置文件对应的Document
	 * @param path
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws Exception
	 */
	public static Document getDocument(String logPath,boolean isEdit) {
		
		Document doc = null;
		InputStream is = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			if (isEdit) {
				is = LogBackXmlConfigHelper.class.getResourceAsStream(logPath);
			} else {
				File file = new File(logPath);
				if (file.exists()) {
					is = new FileInputStream(file);
				} else {
					is = LogBackXmlConfigHelper.class.getResourceAsStream(logbackPath);
				}
			}
			doc = db.parse(is);
			return doc;
		} catch (ParserConfigurationException | SAXException |IOException e) {
			log.error("LogBackConfigLoader getDocument has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE,  "logback配置文件"));
		}
	}
	
	/**
	 * 将编辑后的logback配置文件写入临时文件
	 * @param doc
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws Exception
	 */
	public static void saveDocument(Document doc,String keyWord) {
		File file = new File(getTmpLogFilePath(keyWord));
		try {
			if(file.exists()) {
				file.delete();
			} else {
				File tmpFile = file.getParentFile();
				if(!tmpFile.exists()) tmpFile.mkdirs();
				file.createNewFile();
			}
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer tt = tf.newTransformer();
			tt.setOutputProperty("indent", "yes");
			tt.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			DOMSource ds = new DOMSource();
			ds.setNode(doc);
			StreamResult sr = new StreamResult();
			sr.setOutputStream(new FileOutputStream(file));
			tt.transform(ds, sr);
		} catch (IOException | TransformerException e) {
			log.error("LogBackConfigLoader saveDocument has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE,  "logback配置文件"));
		}
		
	}

}
