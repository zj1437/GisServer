package org.pygisserver.web.utils;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;

import lombok.extern.slf4j.Slf4j;

/**
 * PostGIS数据源连接管理类
 * @author admin
 *
 */
@Slf4j
public class PostGisStoreUtils {
	private static final ConcurrentHashMap<String, DataStore> DATA_STORE = new ConcurrentHashMap<>();
	
	/**
	 * 首先从缓存中查询是否存在该数据源，不存在则创建连接并存入缓存
	 * @return
	 */
	public static DataStore getPostGiDataStore(PostGisConnectEntity connectEntity){
		DataStore dataStore = null;
		try {
			String sourceName = connectEntity.getDataSourceName();
			if (isPostGisInCache(sourceName)) {
				return DATA_STORE.get(sourceName);
			}
			dataStore = DataStoreFinder.getDataStore(convertEntityToMap(connectEntity));
			DATA_STORE.put(sourceName, dataStore);
		} catch (IOException e) {
			log.error("PostGisStoreUtils getPostGiDataStore has error:" +  ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "PostGisDataStore"));
		}
		return dataStore;
	}
	
	public static boolean isEditPostGISName(PostGisConnectEntity oldEntity,PostGisConnectEntity newEntity) {
		String oldPasswd = PostGisStoreUtils.getDecodePass(oldEntity.getPassword());
		if (!oldEntity.getDataSourceName().equalsIgnoreCase(newEntity.getDataSourceName()) 
				&& oldEntity.getDatabaseName().equalsIgnoreCase(newEntity.getDatabaseName()) 
				&& oldEntity.getDatabaseIp().equalsIgnoreCase(newEntity.getDatabaseIp())
				&& oldEntity.getPort() == newEntity.getPort()
				&& oldEntity.getDbType().equalsIgnoreCase(newEntity.getDbType())
				&& oldPasswd.equalsIgnoreCase(newEntity.getPassword()) 
				&& oldEntity.getUserName().equalsIgnoreCase(newEntity.getUserName())) {
			return true;
		}
		return false;
	}
	
	/**
	 * 从缓存中查询是否存在该数据源
	 * @return
	 */
	public static boolean isPostGisInCache(String sourceName){
		if (DATA_STORE.containsKey(sourceName)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 从缓存中查询是否存在该数据源，有则返回，无则返回null
	 * @return
	 */
	public static DataStore getPostGisFromCache(String sourceName){
		if (isPostGisInCache(sourceName)) {
			return DATA_STORE.get(sourceName);
		}
		return null;
	}
	
	/**
	 * 删除缓存中数据源
	 * @return
	 */
	public static void deletePostGisFromCache(String sourceName){
		if (isPostGisInCache(sourceName)) {
			DATA_STORE.remove(sourceName);
		}
	}
	
	/**
	 * 缓存数据源
	 * @return
	 */
	public static void savePostGisToCache(String sourceName, DataStore store){
		if (store != null) {
			DATA_STORE.put(sourceName, store);
		}
	}
	
	/**
	 * 将请求参数转化为连接数据库要求的格式
	 * @param connectEntity
	 * @return
	 */
	private static Map<String,Object> convertEntityToMap(PostGisConnectEntity connectEntity) {
		Map<String,Object> connetParms = new HashMap<>();
		connetParms.put("dbtype", "postgis");
		connetParms.put("host", connectEntity.getDatabaseIp());
		connetParms.put("port", connectEntity.getPort());
		connetParms.put("database", connectEntity.getDatabaseName());
		connetParms.put("user", connectEntity.getUserName());
		connetParms.put("passwd", connectEntity.getPassword());
		return connetParms;
	}
	
	public static String getEncodePass(String passwd) {
		return Base64.getEncoder().encodeToString(passwd.getBytes());
	}
	
	public static String getDecodePass(String passwd) {
		return new String(Base64.getDecoder().decode(passwd));
	}
	
}
