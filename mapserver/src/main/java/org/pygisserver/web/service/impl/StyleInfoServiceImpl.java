package org.pygisserver.web.service.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.dao.StyleInfoMapper;
import org.pygisserver.render.style.StyleInfoParseAndGenerate;
import org.pygisserver.tileserver.service.RenderTileService;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.RasterColorMap;
import org.pygisserver.web.entity.StyleInfo;
import org.pygisserver.web.entity.StyleInfo.SldType;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.service.FileExtensionFilter;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.service.ReadDataInfo;
import org.pygisserver.web.service.StyleInfoService;
import org.pygisserver.web.service.SystemService;
import org.pygisserver.web.startegy.DataInfoStrategy;
import org.pygisserver.web.startegy.FileDataInfoFactory;
import org.pygisserver.web.utils.PostGisStoreUtils;
import org.pygisserver.web.utils.UserUtil;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.styling.Style;
import org.geotools.util.Version;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StyleInfoServiceImpl implements StyleInfoService {

	@Autowired
	private StyleInfoMapper styleInfoMapper;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;
	
	@Autowired
	private MapStatsticService mapStatsticService;
	
	@Autowired
	private RedisUtils redisUtil;
	
	@Autowired
	private MapServerMapper mapServerMapper;
	
	@Autowired
	private MapServerService mapServerService;

	@Autowired
	private RedisUtils redisUtils;

	@Autowired
	private SystemService systemService;
	
	// 注入catalog
	@Autowired
	private Catalog catalog;
	
	@Autowired
	private StyleInfoParseAndGenerate styleHelper;
	
	private int typeId = 0;
	
	private static ConcurrentHashMap<Integer, String> cacheStyleId = new ConcurrentHashMap<Integer, String>();
	
	private static List<String> stleNames = new CopyOnWriteArrayList<String>();
	
	private static final MultipartFileFilter SLD_FILE_FILTER = new MultipartFileFilter("sld","zip");
	
	@DubboReference(version="3.0.0")
	private RenderTileService renderTileService;
	
	private String styleParentPath = null;
	private String thumbalParentPath = null;
	
	@Override
	public List<StyleInfo> queryStyleInfoByCondition(RequestQueryCondition queryCondition) {
		List<StyleInfo> styleInfos = styleInfoMapper.queryStyleInfo(queryCondition);
		return styleInfos; 
	}
	
	public org.geoserver.catalog.StyleInfo convertStyleInfoImpl(StyleInfo style) {
		StyleInfoImpl styleInfo = new StyleInfoImpl(catalog);
		styleInfo.setFilename(style.getFileName());
		String format = style.getFileName().substring(style.getFileName().indexOf(".") + 1);
		styleInfo.setFormat(format);
		styleInfo.setId(style.getStyleId());
		styleInfo.setFormatVersion(new Version("1.0.0"));
		styleInfo.setName(style.getSldName());
		return styleInfo;
	}
	

	@Override
	public int queryStyleCount(RequestQueryCondition queryCondition) {
		try {
			return styleInfoMapper.queryStyleInfoCount(queryCondition);
		} catch (Exception e) {
			log.error("StyleInfoServiceImpl queryStyleCount has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "样式数量"));
		}
	}
	
	/**
	 * 校验当前的sld文件是否合法，主要校验文件结构和语法是否错误 文件中是否有应用外部文件，而未导入
	 * 
	 * @param
	 * @return 返回校验结果
	 */
	@Override
	public String validateSldIllegel(HttpServletRequest req, MultipartFile multipartFile, String targetPath) {
		if (stleNames.isEmpty()) {
			stleNames = styleInfoMapper.queryCurrentSldName();
		}
		String reqIP = UserUtil.getRequestIp(req);
		String fileName = multipartFile.getOriginalFilename();
		String styleName = fileName.split("\\.")[0];
		if (fileName.endsWith(".sld") && stleNames.contains(styleName)) {
			log.warn("服务器已存在同名文件，请修改名称后再上传");
			// 记录到数据库
			SysOperationLog sysOperation = new SysOperationLog();
			sysOperation.setOperatorIP(UserUtil.getRequestIp(req));
			sysOperation.setOperateTime(new Date());
			sysOperation.setOperator(UserUtil.getUserName());
			sysOperation.setContent(MessageFormat.format("服务器已存在同名的sld文件{0}",multipartFile.getOriginalFilename()));
			mapStatsticService.saveOperationLog(sysOperation);
			return "文件重名";
		}
		// 校验后缀名以sld结尾的文件
		if (!SLD_FILE_FILTER.accept(multipartFile)) {
			log.error("文件{0}结尾不是以sld或zip结尾！", multipartFile.getOriginalFilename());
			return MessageFormat.format("文件{0}结尾不是以.sld或zip结尾", multipartFile.getOriginalFilename());
		}
		try {
			/**
			 * 文件以sld结尾只需要验证其语法的合法性 以zip结尾需要解压后查看其资源文件是否存在
			 */
			if (multipartFile.getOriginalFilename().endsWith(".zip")) {
				// 解压过程中处理当前的sld文件内容
				if (!analysisZipFile(multipartFile.getInputStream(), "",reqIP)) {
					log.error("压缩文件{}中的sld文件缺失,请检查后再重新上传！", multipartFile.getOriginalFilename());
					SysOperationLog sysOperation = new SysOperationLog();
					sysOperation.setOperatorIP(UserUtil.getRequestIp(req));
					sysOperation.setOperateTime(new Date());
					sysOperation.setOperator(UserUtil.getUserName());
					sysOperation.setContent(MessageFormat.format("压缩文件{0}中的sld文件缺失，影响正常使用",multipartFile.getOriginalFilename()));
					mapStatsticService.saveOperationLog(sysOperation);
					return MessageFormat.format("压缩文件{0}中的sld文件缺失,请检查后再重新上传！",
							multipartFile.getOriginalFilename());
				}
			}
			return "文件格式正确";
		} catch (IOException e) {
			log.error("MapServerServiceImpl validateSldIllegel has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "样式文件解析异常"));
		}
	}
	
	/**
	 * 获取新的文件名称
	 * @param fileName 文件名称
	 * @return
	 */
	private String getNewFileName(String fileName) {
		if (StringUtils.isNotBlank(fileName)) {
			Pattern pattern = Pattern.compile("\\(\\d\\)");
			Matcher matcher = pattern.matcher(fileName);
			if (matcher.find()) {
				String str = matcher.group(0);
				Pattern pattern1 = Pattern.compile("\\d{1,}");
				Matcher matcher1 = pattern1.matcher(str);
				if (matcher1.find()) {
					String newNO = matcher1.replaceAll(Integer.parseInt(matcher1.group(0)) + 1 + "");
					return matcher.replaceAll(newNO);
				}
			}else {
				if (fileName.indexOf(".") > -1) {
					return fileName.split("\\.")[0] +"(1)." + fileName.split("\\.")[1];
				}else {
					if (fileName.length() > 16) {
						return fileName.substring(0,17)+"(1)";
					} else {
						return fileName+"(1)";
					}
				}
			}
		}
		return "";
	}
	
	
	private String getMaxNameNum(String destPath,String fileName) {
		File destFile = new File(destPath);
		File[] fileArray = destFile.listFiles((file,name) -> getBaseName(name).equals(fileName));
		if (fileArray != null && fileArray.length > 1) {
			Arrays.asList(fileArray).sort((o1,o2) -> matcherNum(o2.getName()) - matcherNum(o1.getName()));
		}
		return (fileArray == null || fileArray.length == 0) ? "" : fileArray[0].getName();
	}

	private String getBaseName(String fileName) {
		return fileName.replaceAll("\\(\\d{1,5}\\)", "");
	}
	
	private int matcherNum(String str) {
		Pattern pattern = Pattern.compile("\\d{1,}");
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(0));
		}
		return 0;
	}
	
	/**
	 * 获取当前数据流的document对象
	 * 
	 * @param is 输入流
	 * @return 返回当前文件的dom对象
	 * @throws DocumentException
	 */
	private String analysisXmlFile(InputStream is) {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(is);
			Element element = document.getRootElement();
			return getResourceFileName(element.elements());
		} catch (DocumentException e) {
			log.error("MapServerServiceImpl analysisXmlFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "XML文件"));
		}
		
	}

	/**
	 * 校验当前上传的文件是否包括依赖的资源文件，如果不包括，则提示用户
	 * 
	 * @param elments
	 * @return
	 */
	private String getResourceFileName(List<Element> elments) {
		String externalResource = null;
		for (Element element : elments) {
			if (element.getName().contains("OnlineResource")) {
				// 获取到对应的值，然后查看当上传的文件中是否有对应的文件，如果没有，则提示用户
				externalResource = element.attribute("href").getText();
				if (StringUtils.isNoneBlank(externalResource)) {
					break;
				}
			} else {
				getResourceFileName(element.elements());
			}
		}
		return externalResource;
	}

	public static class MultipartFileFilter implements FileExtensionFilter<MultipartFile> {
		private Set<String> extensions;

		public MultipartFileFilter(String... extension) {
			this.extensions = new HashSet<String>(Arrays.asList(extension));
		}

		@Override
		public boolean accept(MultipartFile t) {
			return extensions.contains(t.getOriginalFilename().substring(t.getOriginalFilename().indexOf(".") + 1));
		}
	}

	/**
	 * 解压zip文件，将sld文件解析，并将对应的业务数据处理
	 * 
	 * @param targetPath
	 * @param zipName
	 * @param reqIP
	 * @return
	 * @throws DocumentException
	 */
	private String unZipFile(String targetPath, String zipName,String reqIP) {
		boolean isContainDir = false;
		String containedDirName = null;
		String failStyles = new String();
		BufferedOutputStream bos = null;
		ZipFile zipFile = null;
		List<ZipEntry> zipEntryList = new ArrayList<ZipEntry>();
		final List<String> exterResourceNames = new ArrayList<String>();
		try {
			zipFile = new ZipFile(targetPath + File.separator + zipName,Charset.forName("gbk"));
			int len = 0;
			byte[] BUFFER_SIZE = new byte[2048];
			Enumeration<ZipEntry> zipEntries = (Enumeration<ZipEntry>) zipFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry zipEntry = zipEntries.nextElement();
				if (zipEntry.isDirectory()) {
					File file = new File(targetPath, zipEntry.getName());
					file.mkdirs();
				} else {
					if (!zipEntry.getName().endsWith(".sld")) {
						exterResourceNames.add(zipEntry.getName());
					}
					zipEntryList.add(zipEntry);
					InputStream is = zipFile.getInputStream(zipEntry);
					bos = new BufferedOutputStream(new FileOutputStream(new File(targetPath, zipEntry.getName())));
					while ((len = is.read(BUFFER_SIZE)) != -1) {
						bos.write(BUFFER_SIZE, 0, len);
					}
					bos.flush();
					is.close();
					bos.close();
				}
			}
			
			for (ZipEntry zipEntry : zipEntryList) {
				String fileName = zipEntry.getName();
				if (fileName.endsWith(".sld")) {
					String sldName = fileName.substring(0, fileName.indexOf("."));
					if (fileName.contains("/")) sldName = sldName.split("/")[1];
					if (fileName.contains("/") && !isContainDir) {
						isContainDir = true;
						containedDirName = fileName.split("/")[0];
						File f = new File(targetPath,fileName.split("/")[0]);
						File[] fs = f.listFiles();
						for (File ff : fs) {
							FileUtil.transferResToDest(ff.getAbsolutePath(), (new File(targetPath,ff.getName())).getAbsolutePath());
						}
					}
					if (!stleNames.contains(sldName)) {
						if (fileName.contains("/")) {
							File sldFile = new File(targetPath,fileName.split("/")[1]);
							saveStyleInfo(new FileInputStream(sldFile),fileName.split("/")[1],targetPath,exterResourceNames,reqIP);
						} else {
							saveStyleInfo(zipFile.getInputStream(zipEntry),zipEntry.getName(),targetPath,exterResourceNames,reqIP);
						}
						stleNames.add(sldName);
					} else {
						failStyles =failStyles +  "," +  sldName;
					}
				}
			}
			if (isContainDir) {
				File f = new File(targetPath,containedDirName);
				FileUtil.deleteFile(f.getAbsolutePath());
				File zip = new File(targetPath,zipName);
				if (zip.exists()) {
					zip.delete();
				}
			}
			if(failStyles.length() > 1) {
				failStyles = failStyles.substring(1);
			}
		}catch (ResourceException e) {
			return MessageFormat.format("文件{0}解析出错", zipName);
		} catch (IOException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECOMPRESS,
					zipName + "样式文件压缩包"), "System","文件管理", reqIP));
			log.error(MessageFormat.format("StyleInfoServiceImpl unZipFile has error : 文件{}解析出错 {}", zipName,ExceptionsUtil.stackTraceToStr(e)));
			return MessageFormat.format("文件{0}解析出错", zipName);
		} finally {
			FileUtil.closeStream(bos,zipFile);
		}
		return failStyles;
	}

	/**
	 * 如果是zip，则需要解压处理解析过程，如果是sld，则直接处理
	 * 
	 * @param file
	 */
	@Override
	@Transactional
	public String importCustomSld(HttpServletRequest req, File file, String targetPath, String fileName) {
		String reqIP = UserUtil.getRequestIp(req);
		String failStyles = null;
		if (cacheStyleId.isEmpty()) {
			// 获取当前每一个类型styleId的最大值
			queryMaxStyleIdOfTypeId();
		}
		//经过确认，需求中对压缩文件的定义是，一个样式一个压缩文件。虽然当前支持一个压缩文件中可以包含多个样式文件，但是不属于需求范围。
		if (fileName.endsWith(".zip")) {
			failStyles =  unZipFile(targetPath, fileName,reqIP);
		} else {
			try {
				String sldName = fileName.substring(0, fileName.indexOf("."));
				if(!stleNames.contains(sldName)) {
					saveStyleInfo(new FileInputStream(file),fileName,targetPath,null,reqIP);
					stleNames.add(fileName.substring(0, fileName.indexOf(".")));
				} else {
					failStyles = sldName;
				}
			} catch (ResourceException e) {
				return MessageFormat.format("文件{0}解析出错", file.getName());
			} catch (IOException e) {
				log.error(MessageFormat.format("文件{}解析出错 {} ", file.getName()),ExceptionsUtil.stackTraceToStr(e));
				return MessageFormat.format("文件{0}解析出错", file.getName());
			}
		}
		if(StringUtils.isNoneBlank(failStyles) && !failStyles.contains("解析出错")) {
			failStyles = MessageFormat.format("样式列表中已存在{0}同名样式", failStyles);
		}
		return failStyles;
	}

	private void saveStyleInfo(InputStream is,
							  String fileName,
							  String targetPath,
							  List<String> exterResourceNames,
							  String reqIP){
		StyleInfo styleInfo = new StyleInfo();
		String sldName = fileName.substring(0, fileName.indexOf("."));
		styleInfo.setSldName(sldName);
		styleInfo.setFileName(fileName);
		styleInfo.setIsDefault(1);
		styleInfo.setUrl(String.format(Constant.COMMON_STYLE, fileName.substring(0, fileName.indexOf("."))));
		setSldTypeAndStyleId(styleInfo, is);
		if (exterResourceNames != null && styleInfo.getTypeId() == StyleInfo.RASTER_dem) {
			styleInfo.setIsColor(1);
			// 将对应的色带图存储到color文件夹下
			File destFileDir = new File(targetPath);
			// 找到对应的文件内容
			final String exterName = exterResourceNames.stream()
					.filter((name) -> name.split("\\.")[0].equals(styleInfo.getSldName())).findFirst().get();
			StringBuilder sb = new StringBuilder();
			String destPath = sb.append(destFileDir.getParent()).append(File.separator).append("color").append(exterName)
					.toString();
			FileUtil.transferResToDest(targetPath + File.separator + exterName, destPath);
			RasterColorMap colorMap = new RasterColorMap(fileName.substring(0, fileName.indexOf(".")),
					"/dataServer/legend/" + fileName);
			styleInfoMapper.saveRasterColor(colorMap);
		}
		org.geoserver.catalog.StyleInfo geoStyle = convertStyleInfoImpl(styleInfo);
		if (catalog.getStyleByName(sldName) != null) {
			//剔除非法的
			catalog.remove(catalog.getStyleByName(sldName));
		}
		catalog.add(geoStyle);
		org.geoserver.catalog.StyleInfo styleInfoTemp = (org.geoserver.catalog.StyleInfo)redisUtil.getValue(geoStyle.getName());
		if (styleInfoTemp == null) {
			redisUtil.storeValue(reqIP,geoStyle.getName(), geoStyle);
		}
		styleInfo.setUploadTime(new Date());
		styleInfo.setUploader(UserUtil.getUserName());
		styleInfoMapper.saveStyleInfo(styleInfo);
		cacheStyleId.put(styleInfo.getTypeId(), styleInfo.getStyleId());
	}

	/**
	 * setSldTypeAndStyleId
	 *
	 * @param styleInfo
	 * @param is
	 * @throws IOException
	 * @throws DocumentException
	 */
	private void setSldTypeAndStyleId(StyleInfo styleInfo, InputStream is) {
		try {
			// 解析当前数据，定位数据类型
			SAXReader reader = new SAXReader();
			Document document = reader.read(is);
			analysisSldType(document.getRootElement());
			styleInfo.setTypeId(typeId);
			setMaxStyleId(styleInfo, typeId);
		} catch (DocumentException e) {
			log.error("StyleInfoServiceImpl setSldTypeAndStyleId has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "图层样式信息"));
		}
	}

	private void setMaxStyleId(StyleInfo styleInfo, int typeId) {
		String styleId = cacheStyleId.get(typeId);
		String endNum = "";
		int index = 0;
		for (int i = styleId.length() - 1; i > 0; i--) {
			if (48 <= styleId.charAt(i) && styleId.charAt(i) <= 57) {
				endNum = styleId.charAt(i) + endNum;
				index++;
			} else {
				break;
			}
		}
		int curNum = Integer.parseInt(endNum);
		curNum++;
		styleInfo.setStyleId(styleId.substring(0, styleId.length() - index) + curNum);
	}

	/**
	 * 解析sld的数据类型
	 * 
	 * @param element
	 */
	private void analysisSldType(Element element) {
		if (element.getName().endsWith("Symbolizer")) {
			String type = element.getName().replace("Symbolizer", "");
			if ("text".equalsIgnoreCase(type)) {
				return;
			}
			if (type.equalsIgnoreCase("Raster")) {
				if (isDemRaster(element)) {
					typeId = SldType.DEM.getTypeId();
				} else {
					typeId = SldType.RASTER.getTypeId();
				}
			} else {
				typeId = SldType.getTypeIdByType(type.toUpperCase());
			}
		}
		List<Element> elements = element.elements();
		if (elements != null && !elements.isEmpty()) {
			for (Element object : elements) {
				analysisSldType(object);
			}
		}
	}

	private boolean isDemRaster(Element element) {
		List<Element> elememts = element.elements();
		for (Element ele : elememts) {
			if (ele.getName().equalsIgnoreCase("ColorMap")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void queryMaxStyleIdOfTypeId() {
		List<Map<String, Object>> list = styleInfoMapper.queryMaxStyleIdOfTypeId();
		if (list != null && !list.isEmpty()) {
			for (Map<String, Object> map : list) {
				int key = (int) map.get("type_id");
				String value = map.get("id").toString();
				cacheStyleId.put(key, value);
			}
		}
	}
	
	
	/**
	 * 分析当前的zip文件中的内容，如果合法，则解压到指定文件，并返回true,如果不合法，则返回false
	 * 
	 * @param in
	 * @param targetPath
	 * @throws DocumentException
	 */
	private boolean analysisZipFile(InputStream in, String targetPath,String reqIP) {
		ZipInputStream zipInputStream = null;
		ZipEntry zipEntry = null;
		FileOutputStream fos = null;
		boolean flag = false;  //是否有sld文件
		List<String> zipEntryNames = new ArrayList<String>();
		try {
			zipInputStream = new ZipInputStream(in, Charset.forName("gbk"));
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				// 如果是文件夹
				if (zipEntry.isDirectory()) {
					// File file = new File(targetPath, zipEntry.getName());
					// file.mkdirs();
				} else {
					//经过确认,在对压缩文件校验时,不对其他类型的文件做处理,只要包含sld文件就OK
					String sldName = zipEntry.getName();
					if (sldName.endsWith(".sld")) {
						zipEntryNames.add(sldName);
						// 则查看当前解压的其他文件是否不是有
						flag = true;
					}
				}
			}
			if (!flag) {
				throw new ResourceException(ExceptionsUtil.ERROR_400,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "当前上传的zip文件中sld文件"));
			}
			if (!zipEntryNames.isEmpty() && isIllegalSldFile(zipEntryNames)) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECOMPRESS,
					zipEntry.getName() + "样式文件压缩包"), "System","文件管理", reqIP));
			log.error("StyleInfoServiceImpl analysisZipFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "样式压缩包"));
		} finally {
			FileUtil.closeStream(zipInputStream, fos);
		}
	}

	private boolean isIllegalSldFile(List<String> sldNames) {
//		long extenalFileNum = sldNames.stream()
//				.filter((fileName) -> (fileName.endsWith(".png") || fileName.endsWith(".jpeg") || fileName.endsWith(".bmp"))).count();
		long sldFileNum = sldNames.stream().filter((fileName) -> fileName.endsWith(".sld")).count();
		return sldFileNum > 0; // && (extenalFileNum == sldFileNum);
	}
	
	@Override
	public List<RasterColorMap> queryColorMap() {
		try {
			return styleInfoMapper.queryRasterColor();
		} catch (Exception e) {
			log.error("StyleInfoServiceImpl queryColorMap has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "染色图"));
		}
	}
	
	@Override
	public List<StyleInfo> queryStyleInfoList(String fileType,String fileUrl,String reqIP) {
		RequestQueryCondition queryCondition = new RequestQueryCondition();
		ReadDataInfo dataInfo = null;
		DataStore store = null;
		if(SystemServiceImpl.isPostGIS(fileUrl)) {
			store = PostGisStoreUtils.getPostGisFromCache(fileUrl.split(";")[0]);
			if(store == null) {
				PostGisConnectEntity entity = systemService.queryPostGis(fileUrl.split(";")[0]);
				entity.setPassword(PostGisStoreUtils.getDecodePass(entity.getPassword()));
				store = PostGisStoreUtils.getPostGiDataStore(entity);
			}
		}
		try {
			dataInfo = FileDataInfoFactory.getDataInfo(fileUrl, fileType,store);
		} catch (BaseException e) {
			if(e.getErrorMsg().equals("Sqlite error")) {
		        File file = new File(fileUrl);
		        String fileName = file.exists() ? file.getName() : "";
		        systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.SQLITE_DB_INIT_FAIL,fileName), 
						"System","Sqlite服务", reqIP));
			} else {
				throw new ResourceException(e.getErrorCode(),e.getErrorMsg());
			}
		} catch (ResourceException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, e.getErrorMsg(), 
					"System","Sqlite服务", reqIP));
			throw e;
		}
		DataInfoStrategy strategy = new DataInfoStrategy(dataInfo);
		queryCondition.setStyleType(strategy.getDataType());
		return styleInfoMapper.queryStyleInfo(queryCondition);
	}
	
	/**
	 * 判断shape数据是点、线、还是面
	 * 
	 * @param fileUrl
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public int getFeatureTypeByFileUrl(String fileUrl) {
		//区别对待shape文件和postgis的类型获取
		GeometryDescriptor gd = null;
		try {
			if (SystemServiceImpl.isPostGIS(fileUrl)) {
				DataStore store = PostGisStoreUtils.getPostGisFromCache(fileUrl.split(";")[0]);
				FeatureSource<SimpleFeatureType, SimpleFeature> source = store.getFeatureSource(fileUrl.split(";")[1]);
				gd = source.getSchema().getGeometryDescriptor();
			} else {
				ShapefileDataStoreFactory shapeFactory = new ShapefileDataStoreFactory();
				ShapefileDataStore dataStore = (ShapefileDataStore) shapeFactory
						.createDataStore(new File(fileUrl).toURI().toURL());
				SimpleFeatureSource featureSource = dataStore.getFeatureSource();
				gd = featureSource.getSchema().getGeometryDescriptor();
			}
			//因要返回属性表暂时放开
			if (gd == null) {
//				return -1;
				return StyleInfo.DEFAULT;
			}
			Class gtype = gd.getType().getBinding();
			if (Point.class.isAssignableFrom(gtype) || MultiPoint.class.isAssignableFrom(gtype)) {
				return StyleInfo.POINT;
			} else if (LineString.class.isAssignableFrom(gtype) || MultiLineString.class.isAssignableFrom(gtype)) {
				return StyleInfo.LINE;
			} else if (Polygon.class.isAssignableFrom(gtype) || MultiPolygon.class.isAssignableFrom(gtype)) {
				return StyleInfo.POLYGON;
			}
			return StyleInfo.DEFAULT;
		} catch (IOException e) {
			log.error("StyleInfoServiceImpl getFeatureTypeByFileUrl has error:" + ExceptionsUtil.stackTraceToStr(e));
			return -1;
//			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "图层FeatureType"));
		}
	}

	@Override
	public String warnBeforedelete(HttpServletRequest req,List<Integer> ids) {
		try {
			List<String> relationLayers = styleInfoMapper.queryStyleRelationServer(ids);
			if (relationLayers != null && !relationLayers.isEmpty()) {
				log.warn("用户{}打算删除了关联着服务的样式文件{}","","");
				return Arrays.toString(relationLayers.toArray());
			}
		} catch (Exception e) {
			log.error("StyleInfoServiceImpl warnBeforedelete has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "样式名称"));
		}
		return "可以删除";
	}
	
	@Override
	@Transactional
	public void deleteStyleInfo(HttpServletRequest req,List<Integer> ids) {
		String reqIP = UserUtil.getRequestIp(req);
		//删除缓存中的样式名称
		List<String> names = styleInfoMapper.querySldNames(ids);
		List<Integer> serverIds = styleInfoMapper.queryServerIdsByStyleIds(ids);

		List<DataServerEntity> dataServers = null;
		if (serverIds != null && !serverIds.isEmpty()) {
			dataServers = mapServerMapper.queryServerDataByIds(serverIds);
			for (DataServerEntity dataServerEntity : dataServers) {
				try {
					String fileType = dataServerEntity.getDataUrl().endsWith(".shp") ? "Shape" : null;
					if (fileType != null) {
						List<StyleInfo> styles = queryStyleInfoList("Shape",dataServerEntity.getDataUrl(),reqIP);
						for (StyleInfo styleInfo : styles) {
							if (styleInfo.getSldName().startsWith("默认")) {
								dataServerEntity.setStyleInfo(styleInfo);
								break;
							}
						}
					}
					renderTileService.flushTileTask(dataServerEntity.getLayerName(),false,true);
				} catch (Exception e) {
					if(ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
						systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
								dataServerEntity.getServerName() + "服务缓存切片清除"), UserUtil.getUserName(),"Pytc服务", reqIP));
					}
					log.error("MapServerServiceImpl dealServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
				}
				
			}
		}

		//如果样式文件应用到服务上，对应的缩略图需要修改
		if (dataServers != null) {
			dataServers.forEach((server) -> {
				mapServerService.editServerData(server, reqIP);
			});
		}

		//将对应的服务信息还原
		styleInfoMapper.updateServerStyleId(ids);

		styleInfoMapper.deleteStyleInfo(ids);
		stleNames.removeAll(names);
		//删除对应的样式资源
		for (String name : names) {
			org.geoserver.catalog.StyleInfo style = catalog.getStyleByName(name);
			if (style != null) catalog.remove(style);
			org.geoserver.catalog.StyleInfo styleInfoTemp = (org.geoserver.catalog.StyleInfo)redisUtil.getValue(name);
			if (styleInfoTemp != null) {
				redisUtil.deleteKey(reqIP,name);
			}
		}
		//删除自定义样式时 同步删除缓存的样式图片
		String samplePath = System.getProperty("SYSTEM_SAMPLES_PATH");
		File legendFile = null;
		for (String name : names) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append(samplePath).append(File.separator).append(name).append(".png");
			legendFile = new File(sBuilder.toString());
			if (legendFile.exists()) {
				legendFile.delete();
			}
		}
	}

	@Override
	public List<String> querySldName(List<Integer> ids) {
		try {
			return styleInfoMapper.querySldNames(ids);
		} catch (Exception e) {
			log.error("StyleInfoServiceImpl querySldName has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "样式文件名称"));
		}
	}


	@Override
	public StyleInfo queryStyleInfoById(String styleId) {
		try {
			return styleInfoMapper.queryStyleInfoById(styleId);
		} catch (Exception e) {
			log.error("StyleInfoServiceImpl querySldName has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "样式"));
		}
	}

	@Override
	public Style parserStyleByStyleName(String styleName) {
		return parseStyleInfo(styleName,false);
	}

	@Override
	public Style parserStyleBySldFileName(String sldFileName) {
		return parseStyleInfo(sldFileName,false);
	}

	@Override
	public Style parserStyleBySldFilePath(String sldFilePath) {
		return parseStyleInfo(sldFilePath,true);
	}

	@Override
	public byte[] getStyleImageByStyleName(String styleName) {
		return getStyleThumbal(styleName,false);
	}
	
	@Override
	public byte[] getStyleImageBySldFileName(String sldFileName) {
		return getStyleThumbal(sldFileName,false);
	}

	@Override
	public byte[] getStyleImageBySldFilePath(String sldFilePath) {
		return getStyleThumbal(sldFilePath,true);
	}
	
	private Style parseStyleInfo(String info,boolean isSldPath) {
		checkStyleQueryInfo(info,isSldPath);
		String styleName = getStyleInfos(info,isSldPath)[0];
		String sldFilePath = getStyleInfos(info,isSldPath)[1];
		Style sldStyle = (Style)redisUtil.getValue(styleName);
		sldStyle = sldStyle == null ? styleHelper.getStyle(sldFilePath) : sldStyle;
		return sldStyle;
	}
	
    private byte[] getStyleThumbal(String info,boolean isSldPath){
    	checkStyleQueryInfo(info,isSldPath);
		String styleName = getStyleInfos(info,isSldPath)[0];
		String sldFilePath = getStyleInfos(info,isSldPath)[1];
		String thumbalPath = null;
		if(this.thumbalParentPath == null) {
			thumbalPath = System.getProperty("SYSTEM_SAMPLES_PATH");
			this.thumbalParentPath = thumbalPath;
		}
		thumbalPath = this.thumbalParentPath + File.separator + styleName + ".png";
		RequestQueryCondition condition = new RequestQueryCondition();
		condition.setStyleName(styleName);
		StyleInfo styleInfo = queryStyleInfoByCondition(condition).get(0);
		return styleHelper.getSldImage(thumbalPath, styleName, sldFilePath, styleInfo.getUrl());
    	
	}
    
    private String[] getStyleInfos(String info,boolean isSldPath) {
    	String sldFilePath = null;
		String styleName = null;
		String sldFileName = null;
		if(isSldPath) {
			styleName = info.substring(info.lastIndexOf(File.separator), info.lastIndexOf("."));
			sldFilePath = info;
		} else {
			styleName = info.contains(".sld") ? info.substring(0, info.indexOf(".")) : info;
			sldFileName = info.contains(".sld") ? info : info + ".sld";
			sldFilePath = this.styleParentPath + File.separator + sldFileName;
		}
		String[] strs = new String[] {styleName,sldFilePath};
		return strs;
    }
	
    /**
          * 对样式相关请求进行必要的校验
     * @param info
     * @param isSldPath
     */
	private void checkStyleQueryInfo(String info,boolean isSldPath) {
		//1.为空校验
		if (StringUtils.isBlank(info)) {
			if(isSldPath) {
				throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, "样式文件路径"));
			} else {
				throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, "样式文件名"));
			}
		}
		//2.校验是否存在，目前默认只处理styles路径下的样式文件
		String sldPath = System.getProperty("SYSTEM_STYLE_PATH");
		if(!StringUtils.isBlank(sldPath)) this.styleParentPath = sldPath;
		File sldDir = null;
		if(isSldPath) {
			if(!info.startsWith(sldPath)) {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, info));     //异常相关需要重新规划
			}
			sldDir = new File(info);
			if(!sldDir.exists()) {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_EXIST, info));
			}
		} else {
			sldDir = new File(sldPath);
			String sldFileName = info.contains(".sld") ? info : info + ".sld";
			File[] sldFiles = sldDir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if(name.equals(sldFileName)) {
						return true;
					}
					return false;
				}
			});
			if(sldFiles.length == 0) {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_EXIST, sldFileName));
			}
		}
	}

	public org.geoserver.catalog.StyleInfo getDefaultStyle(String dataType, StyleInfo feStyleInfo, String reqIP) {
		org.geoserver.catalog.StyleInfo styleInfo;
		styleInfo = catalog.getStyleByName(feStyleInfo.getSldName());
		if(styleInfo == null) {
			styleInfo = (org.geoserver.catalog.StyleInfo)redisUtils.getValue(feStyleInfo.getSldName());
			if (styleInfo != null) {
				catalog.add(styleInfo);
			} else {
				styleInfo = convertStyleInfoImpl(feStyleInfo);
				catalog.add(styleInfo);
				redisUtils.storeValue(reqIP,styleInfo.getName(), styleInfo);
			}
		}
		return styleInfo;
	}

	public StyleInfo getDataDefaultStyle(List<StyleInfo> styleInfos, String dataType, String sldName) {
		if (styleInfos.size() == 1) {
			return styleInfos.get(0);
		}
		if (StringUtils.isNotBlank(sldName)) {
			for (StyleInfo styleInfo : styleInfos) {
				if (styleInfo.getSldName().equals(sldName)) {
					return styleInfo;
				}
			}
		}
		return styleInfos.get(0);
	}

	@Override
	public List<DataServerEntity> queryServerByStyle(int typeId, String reqIP){
		List<DataServerEntity> serverNameList = styleInfoMapper.queryServerByStyle(typeId);
		return serverNameList;
	}
}
