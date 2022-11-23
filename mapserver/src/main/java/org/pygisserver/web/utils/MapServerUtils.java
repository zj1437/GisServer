package org.pygisserver.web.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ServerProtocolInfo;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.web.service.impl.SystemServiceImpl;
import org.pygisserver.render.util.TdbUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * 服务发布工具类
 * @author admin
 *
 */
@Slf4j
public class MapServerUtils {

	public static List<String> cacheLayers = new CopyOnWriteArrayList<>();

	private static final String PATTERN_STR = "\\d+$";

	/**
	 * 出现重名时，动态构造新名称
	 * @param fileName
	 * @return
	 */
	public static  String getNewServerNameByMaxName(String fileName) {
		String newFileName = "";
		if (StringUtils.isNotBlank(fileName)) {
			Pattern pattern = Pattern.compile(PATTERN_STR);
			Matcher matcher = pattern.matcher(fileName);
			if (matcher.find()) {

				String str = matcher.group(0);
				int strLen = str.length();
				int fileSuffix = Integer.parseInt(matcher.group(0)) + 1;
				int newStrLen = (fileSuffix + "").length();
				if (fileName.length() < 21) {
					newFileName = fileName.substring(0, fileName.length() - strLen) + fileSuffix;
				} else {
					if (strLen == newStrLen) {
						newFileName = fileName.substring(0, fileName.length() - strLen) + fileSuffix;
					} else if (strLen < newStrLen) {
						newFileName =  fileName.substring(0, fileName.length() - newStrLen) + fileSuffix;
					}
				}
			}else {
				if (fileName.length() < 21) {
					newFileName =  fileName + 1;
				} else {
					newFileName =  fileName.substring(0,fileName.length() -1) + 1;
				}
				
			}
		}
		return newFileName;
	}

	public static String getMaxFileName(List<String> fileNames,String fileName) {
		if (fileNames == null || fileNames.isEmpty()) {
			return null;
		}
		List<String> names = new ArrayList<>(fileNames.size());
		fileNames.stream().filter((name) -> getDuplicateServer(name,fileName)).forEach((file) -> names.add(file));
		names.sort((name1,name2) -> matcherNum(name1) - matcherNum(name2));
		return names.get(names.size() - 1);
	}
	
	private static boolean getDuplicateServer(String serverName,String fileName) {
		if (StringUtils.isNotBlank(fileName) && StringUtils.isNotBlank(serverName)) {
			if (serverName.equals(fileName)) {
				return true;
			}
			Pattern pattern = Pattern.compile(PATTERN_STR);
			Matcher serverMatcher = pattern.matcher(serverName);
			int serverNumLen = 0;
			if (serverMatcher.find()) {
				serverNumLen = serverMatcher.group(0).length();
			}
			if (serverNumLen == 0) {
				Pattern pattern1 = Pattern.compile(PATTERN_STR);
				Matcher fileMatcher = pattern1.matcher(fileName);
				if (fileMatcher.find()) {
					String temName = fileName.substring(0,fileName.length() - fileMatcher.group(0).length());
					if (serverName.startsWith(temName)) {
						return true;
					}
				} else {
					return serverName.equals(fileName);
				} 
			} else {
				Pattern pattern1 = Pattern.compile(PATTERN_STR);
				Matcher fileMatcher = pattern1.matcher(fileName);
				int fileNumLen = 0;
				if (fileMatcher.find()) {
					fileNumLen = fileMatcher.group(0).length();
					String temSerName = serverName.substring(0,serverName.length() - serverNumLen);
					String temFileName = fileName.substring(0,fileName.length() - fileNumLen);
					if ((temSerName.length() - temFileName.length() >=2) || (temSerName.length() - temFileName.length() <= -2)) {
						return false;
					}
					if (serverNumLen > fileNumLen) {
						if (temFileName.startsWith(temSerName)) {
							return true;
						}
					} else if (serverNumLen < fileNumLen) {
						if (temSerName.startsWith(temFileName)) {
							return true;
						}
					} else if (serverNumLen == fileNumLen) {
						return temSerName.equals(temFileName);
					}
				} else {
					String temSerName = serverName.substring(0,serverName.length() - serverNumLen);
					if ((temSerName.length() - fileName.length() >=2) || (temSerName.length() - fileName.length() <= -2)) {
						return false;
					}
					if (fileName.startsWith(temSerName)) {
						return true;
					}
				} 
			}
		}
		return false;
	}
	
	private static int matcherNum(String str) {
		Pattern pattern = Pattern.compile("\\d{1,}");
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(0));
		}
		return 0;
	}
	
	public static Map<String,ServerProtocolInfo> convertProtocol2Map(List<ServerProtocolInfo> protocols) {
		Map<String,ServerProtocolInfo> maps = new HashMap<String, ServerProtocolInfo>();
		protocols.forEach(item -> {
			maps.put(item.getProtocolName(), item);
		});
		return maps;
	}
	
	public static String getUTF8FilePath(String path) {
		try {
			return URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("MapServerUtils getUTF8FilePath has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECODE, path));
		}
	}
	
	/**
	 * 根据待发布服务文件获取服务类型
	 * @param filePath
	 * @return
	 */
	public static String getServerFileType(String filePath) {
		String fileType = null;
		if (SystemServiceImpl.isPostGIS(filePath)) {
			fileType = "PostGIS";
		} else {
			File file = new File(filePath);
			String fileNameLower = file.getName().toLowerCase();
			if (fileNameLower.endsWith(".tiff") || fileNameLower.endsWith(".tif")) {
				fileType = "GeoTIFF";
			}
			if (fileNameLower.endsWith(".img") ) {
				fileType = "ERDASImg";
			}
			if (fileNameLower.endsWith(".shp")) {
				fileType = "Shape";
			}
			if (fileNameLower.endsWith(".db") || fileNameLower.endsWith(".mbtiles")) {
				fileType = "MBTiles";
			}
			if (fileNameLower.endsWith(".tdb")) {
				fileType = "TDB";
			}
			if (fileNameLower.equals("config.xml")) {
				fileType = "FlatGis";
			}
			if (fileNameLower.equals("tms.xml")) {
				fileType = "TMS";
			}
		}
		return fileType;
	}

	public static Object getgeometryObject(int dataType, Coordinate...coordinates) {
		GeometryFactory geomtryFactory = JTSFactoryFinder.getGeometryFactory();
		Geometry geometry = null;
		if (dataType == 0) {
			geometry = geomtryFactory.createPoint(coordinates[0]);
		}else if (dataType == 1) {
			geometry = geomtryFactory.createMultiPointFromCoords(coordinates);
		}
		GeometryJSON json = new GeometryJSON(6);
		StringWriter writer = new StringWriter();
		try {
			json.write(geometry, writer);
		} catch (IOException e) {
			log.error("MapLayerAnalysisController convertGeojsonFromGeometry has error:" + e.getLocalizedMessage());
		}
		String geometryStr = writer.toString();
		return JSONObject.parse(geometryStr);
	}

	/**
	 * 获取系统默认的workspace
	 *
	 * @return WorkspaceInfo
	 */
	public static WorkspaceInfo getDefaultWorkspace() {
		WorkspaceInfoImpl workspaceInfo = new WorkspaceInfoImpl();
		workspaceInfo.setName("pygis");
		workspaceInfo.setId("WorkspaceInfoImpl--4f21b602:164a0ead067:-7fff");
		return workspaceInfo;
	}

	/**
	 * 获取一个随机名称
	 *
	 * @return String
	 */
	public static String createIdentiCode() {
		Random random = new Random();
		long t1 = 0x7FFFFFFF & System.currentTimeMillis();
		long uuid = t1 << 32 | Math.abs(random.nextInt());
		return Long.toString(uuid);
	}

	/**
	 * 修改服务名称时，同步修改缓存文件名称
	 *
	 * @param oName oName
	 * @param oName oName
	 */
	public static void renameLocalCacheFileNames(String oName,String nName) {
		//先重命名，然后再异步删除
		String cachePath = System.getProperty(TdbUtils.USER_VISIT_CACHE_DIR);
		if (StringUtils.isBlank(cachePath)) {
			return;
		}
		File parentFile = new File(cachePath);
		File[] files = parentFile.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				String name1 = name.substring(0, name.indexOf("_EPSG"));
				return name1.equals(oName);
			}
		});
		if (files == null || files.length == 0) {
			return;
		}
		for (File filePath : files) {
			String fileName = filePath.getName();
			String newName = fileName.replace(oName, nName);
			filePath.renameTo(new File(filePath.getParentFile(),newName));
		}
	}

	public static String getThumpath() {
		return System.getProperty("SYSTEM_THUMBAL_PATH");
	}
}
