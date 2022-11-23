package org.pygisserver.web.service.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.pygisserver.common.cache.RamCacheAdapterRunnable;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.entity.UploadFile;
import org.pygisserver.common.entity.UserLogbackInfo;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.exception.SqlException;
import org.pygisserver.common.exception.StatusException;
import org.pygisserver.common.logback.LogBackXmlConfigHelper;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.CrypotUtil;
import org.pygisserver.common.util.DateUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.dao.SystemConfigMapper;
import org.pygisserver.web.dao.UserMapper;
import org.pygisserver.render.cache.VisitCacheClearRunnable;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.render.tiled.TiledInfoReadAbstract;
import org.pygisserver.render.util.TdbScheduledTask;
import org.pygisserver.render.util.TdbUtils;
import org.pygisserver.tileserver.service.RenderTileService;
import org.pygisserver.web.entity.BatchPublishEntity;
import org.pygisserver.web.entity.CityInfo;
import org.pygisserver.web.entity.FeatureInfo;
import org.pygisserver.common.entity.FunctionPageEntity;
import org.pygisserver.web.entity.ProvinceInfo;
import org.pygisserver.web.entity.PublishedDataFormat;
import org.pygisserver.web.entity.ServerAutoConfig;
import org.pygisserver.web.entity.ServerCacheConfigEntity;
import org.pygisserver.web.entity.ServerProvider;
import org.pygisserver.web.entity.SystemConfigInfo;
import org.pygisserver.web.entity.UserInfo;
import org.pygisserver.web.listener.FileListener;
import org.pygisserver.web.listener.FileMonitor;
import org.pygisserver.web.service.SystemService;
import org.pygisserver.web.service.UserService;
import org.pygisserver.web.startegy.CustomDataInfo;
import org.pygisserver.web.utils.MapServerUtils;
import org.pygisserver.web.utils.PostGisStoreUtils;
import org.pygisserver.web.websocket.WebSocketManager;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.jdbc.JDBCDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SystemServiceImpl implements SystemService {
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
	private LogBackXmlConfigHelper logBackXmlConfigHelper;

	@Resource
	private SystemConfigMapper systemConfigMapper;
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private RedisUtils redisUtil;
	
	@Autowired
	private SystemLogService systemLogService;
	
	@Autowired
	private MapServerServiceImpl mapServerService;
	
	@Resource
	private UserMapper userMapper;
	
	@Autowired
	private FileMonitor fileMonitor;
	
	@Autowired
	private WebSocketManager webSocket;
	
	@Resource
	private UserService userService;

	@Resource
	private MapServerMapper mapServer;

	@Autowired
	private StyleInfoServiceImpl styleInfoService;

	private static final Map<String, PostGisConnectEntity> postGisMap = new HashMap<>();
	
	private static Map<String,String> RESOURCE_MAP = new HashMap<String,String>();
	
	private static String parentPath;
	
	//文件存放容器,批量发布服务时,将符合发布条件的文件存入其中
	private static final List<BatchPublishEntity> entityList = new ArrayList<BatchPublishEntity>();
	
	private static String CURRENT_USE_DATE;
	
	private static Date EXPIRE_TIME;
	//服务当前支持的所有模型类型
	private static String[] modelTypes = new String[]{".osg",".osgb",".ive",".3ds",".obj",".flt",".fbx",".dae",".glb",".gltf"};
	private static List<String> supportModelTypes = Arrays.asList(modelTypes);
	//服务支持发布的地图数据类型
	private static String[] serverTypes = new String[]{".shp",".tif",".tiff",".db",".mbtiles",".img"};
	private static List<String> supportServerTypes = Arrays.asList(serverTypes);
	//服务支持的瓦片数据
	private static String[] tiledTypes = new String[]{"config.tdb","tms.xml","config.xml"};
	private static List<String> supportTiledTypes = Arrays.asList(tiledTypes);
	
	//服务支持的瓦片数据
	private static String[] allTypes = new String[]{".shp",".tif",".db",".mbtiles",".img",".tiff",".tdb",".xml"};
	private static List<String> supportAllTypes = Arrays.asList(allTypes);
	
	@DubboReference(version="3.0.0")
	private RenderTileService renderTileService;
	
	public static Map<String,String> cacheUrl = new HashMap<String, String>();
	
	@Override
	public void updateSystemPass(String password) {
		try {
			systemConfigMapper.updateSyetemPassword(password);
		} catch (Exception e) {
			log.error("SystemServiceImpl updateSystemPass has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.SQL_EXCUTION);
		}
	}

	/**
	 * 管理员重置默认密码
	 */
	@Override
	public void updateUserPassWord() {
		try {
			//获取系统默认密码，然后加密处理后再更新
			SystemConfigInfo systemConfigInfo = systemConfigMapper.querySystemConfigInfo();
			String sysPass = systemConfigInfo.getSystemPassword();
			String sysUser = systemConfigInfo.getSystemUsername();
			String password = new SimpleHash("MD5", sysPass, sysUser, 2).toHex();
			userMapper.updateUserPass(sysUser, password);
		} catch (Exception e) {
			log.error("SystemServiceImpl updateUserPassWord has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.SQL_EXCUTION);
		}
	}

	@Override
	public void saveServerProvider(ServerProvider serverProvider) {
		try {
			systemConfigMapper.saveServerProvider(serverProvider);
		} catch (Exception e) {
			log.error("SystemServiceImpl saveServerProvider has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "服务提供商信息"));
		}
	}

	@Override
	public String querySysTileCachePath() {
		try {
			String systemCachePath = System.getProperty("USER_VISIT_CACHE_DIR");
			File cacheFile = new File(new File(systemCachePath).getParentFile(),"spatial_analysis");
			if (!cacheFile.exists()) {
				cacheFile.mkdirs();
			}
			return cacheFile.getAbsolutePath();
		} catch (Exception e) {
			log.error("SystemServiceImpl querySysTileCachePath has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "系统缓存路径"));
		}
	}

	
	@Override
	public ServerProvider queryServerProvider() {
		try {
			return systemConfigMapper.queryServerProvider();
		} catch (Exception e) {
			log.error("SystemServiceImpl queryServerProvider has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "服务提供商"));
		}
	}

	@Override
	public List<ProvinceInfo> queryChinaProvince() {
		try {
			return systemConfigMapper.queryProvinceInfo();
		} catch (Exception e) {			
			log.error("SystemServiceImpl queryChianProvince has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "全国省份"));
		}
	}

	@Override
	public List<CityInfo> queryCityInfoByProvinceCode(String provinceCode) {
		try {
			return systemConfigMapper.queryCityInfo(provinceCode);
		} catch (Exception e) {
			log.error("SystemServiceImpl queryCityInfoByProvinceCode has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.SQL_EXCUTION);
		}
	}

	@Override
	public void uploadFile(List<UploadFile> uploadFiles) {
		try {
			systemConfigMapper.saveUploadFiles(uploadFiles);
		} catch (Exception e) {
			log.error("SystemServiceImpl uploadFile has error:{}",ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_SAVE, "上传文件信息"));
		}
	}

	@Override
	public LinkedList<UploadFile> getUploadFile(String uploador) {
		try {
			return systemConfigMapper.getUploadFiles(uploador);
		} catch (Exception e) {
			log.error("SystemServiceImpl getUploadFile has error:{}",ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "上传文件"));
		}
	}

	@Override
	public int getUploadFileCount(String uploador) {
		try {
			return systemConfigMapper.getUploadFileCount(uploador);
		} catch (Exception e) {
			log.error("SystemServiceImpl getUploadFileCount has error:{}",ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "上传文件总数"));
		}
	}
	
	@Override
	public void deleteFile(String fileUrl, String fileName) {
		try {
			//删除文件
			File tempFile = new File(fileUrl);
			fileName = FileUtil.formatFileUrl(fileName);
			if (fileName.indexOf(File.separator) > -1) {
				//更新当前文件夹对应的大小，如果当前文件夹下已经没有数据，则删除对应的数据库文件夹信息
				File file = new File(fileUrl,fileName.substring(0, fileName.indexOf(File.separator, 1)));
				UploadFile uploadFile = new UploadFile(new Date(), new Date(), tempFile.getParentFile().getName());
				uploadFile.setFileName(file.getName());
				long size = new File(tempFile, fileName).length();
				uploadFile.setFileSize(-size);
				systemConfigMapper.updateFileSize(uploadFile);
			}else {
				systemConfigMapper.deleteFileByFileName(fileName);
			}
			FileUtil.deleteFile(fileUrl + File.separator + fileName);
		} catch (Exception e) {
			log.error("SystemServiceImpl deleteFile has error {}",ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_DELETE, fileName));
		}
	}

	@Override
	public void updateFileSize(UploadFile uploadFile) {
		try {
			systemConfigMapper.updateFileSize(uploadFile);
		} catch (Exception e) {
			log.error("SystemServiceImpl updateFileSize has error {}",ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_UPDATE, "上传文件总数信息"));
		}
	}
	
	public Map<File,Integer> getCurrentUserList(String commonUrl,String userName){
		File file = new File(commonUrl);
		Map<File, Integer> map = new HashMap<File, Integer>();
		file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.equals(userName) || name.equals("示例数据")) {
					map.put(new File(dir,name),1);
					return true;
				}
				return false;
			}
		});
		return map;
	}
	
	/**
	 * 获取普通人员的文件list
	 * 
	 * @param fileUrl
	 * @param type
	 * @return
	 */
	@Override
	public Map<String, Integer> getOrdinaryFileList(String fileUrl,String userName,String type) {
		fileUrl = FileUtil.formatFileUrl(fileUrl);
		File file = new File(fileUrl);
		Map<String, Integer> map = new HashMap<String, Integer>();
		// 如果是文件夹就处理
		String commonUrl = cacheUrl.get("CACHE_FILE_URL");
		if (commonUrl.equals(file.getAbsolutePath())){
			file.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (name.equals("示例数据") || name.equals(userName)) {
						map.put((new File(dir,name)).getAbsolutePath(),1);
					}
					return true;
				}
			});
		}else if(commonUrl.substring(0, commonUrl.indexOf(":") + 2).equals(fileUrl)) {
			map.put((new File(commonUrl)).getAbsolutePath(), 1);
		}else {
			if (StringUtils.isNotBlank(type)) {
				dealFileFilter(type, file, map);
			}else {
				if (file.isDirectory()) {
					file.listFiles(new ModelFileSelect(map));
				}
			}
		}
		return map;
	}

	/**
	 * 获取管理员的文件list
	 * @param fileUrl
	 * @param type
	 * @return
	 */
	@Override
	public Map<String, Integer> getManagerFileList(String fileUrl, String type){
		if (StringUtils.isBlank(fileUrl)) {
			return getCurrentUserFile();
		}
		Map<String, Integer> map = new TreeMap<String, Integer>();
		fileUrl = FileUtil.formalFilePath(fileUrl);
		listMatchFiles(type, new File(fileUrl), map);
		return map;
	}
	
	private Map<String, Integer> getCurrentUserFile() {
		//第一次进来只能看见共享文件夹和用户自己的文件夹
		Map<String, Integer> roots = new TreeMap<String,Integer>();
		File[] fileArray = File.listRoots();
		for (File file : fileArray) {
			if (file.isDirectory()) {
				roots.put(file.getAbsolutePath(), 1);
			} else {
				roots.put(file.getAbsolutePath(), 0);
			}
		}
		return roots;
	}
	
	private void listMatchFiles(String type, File file, Map<String, Integer> map) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File file1 : files) {
				String name = file1.getName();
				String suffix = name.contains(".") ? name.substring(name.lastIndexOf(".")) : name;
				if(suffix.equalsIgnoreCase(".itdb") || suffix.equalsIgnoreCase(".etdb")) {
					File file2 = new File(file,"config.tdb");
					map.put(file2.getAbsolutePath(), 0);
					return;
				}
				if(file1.isDirectory()) {
					map.put(file1.getAbsolutePath(), 1);
				} else if (supportAllTypes.contains(suffix.toLowerCase())) {
					map.put(file1.getAbsolutePath(), 0);
				}
			}
		} else {
			map.put(file.getAbsolutePath(), 0);
		}
	}
	
	private void dealFileFilter(String type, File file, Map<String, Integer> map) {
		if (file.isDirectory() /*&& StringUtils.isNotBlank(type)*/) {
			file.listFiles(new FileSelect(type,map));
			/*if (!map.isEmpty()) {
				// 如果是文件夹，则全部返回，如果是文件则只返回第一个文件
				boolean flag = false;
				for (File file2 : map.keySet()) {
					if (file2.isDirectory()) {
						flag = true;
					}
				}
				if (!flag) {
					//文件名称
					File firstFile = map.keySet().iterator().next();
					map.clear();
					//0 表示当前文件类型为文件
					map.put(firstFile, 0);
				} 
			}*/
		} else {
			// 类型为空，不进行文件过滤
			file.listFiles((fiterFile)->{
				if (fiterFile.isDirectory()) {
					map.put(fiterFile.getAbsolutePath(), 1);
				} else {
					map.put(fiterFile.getAbsolutePath(), 0);
				}
				return true;
			});
		}
	}
	
	
	class FileSelect implements FilenameFilter {
		
		private String filterType;
		
		private Map<String,Integer> map;

		public FileSelect(String fileType,Map<String,Integer> map) {
			this.filterType = fileType;
			this.map = map;
		}

		@Override
		public boolean accept(File dir, String name) {
			File tempFile = new File(dir, name);
			if (!tempFile.isHidden()) {
				if(name.endsWith(".itdb") || name.endsWith(".etdb")) {
					File file2 = new File(dir,"config.tdb");
					map.put(file2.getAbsolutePath(), 0);
					return true;
				}
				if (tempFile.isDirectory()) {
					map.put(tempFile.getAbsolutePath(), 1);
				}else {
					String suffix = name.substring(name.lastIndexOf("."));
					if (supportAllTypes.contains(suffix.toLowerCase())) {
						map.put(tempFile.getAbsolutePath(), 0);
					}
				}
			}
			return false;
		}
	}
	
	
	
	/**
	 * 过滤模型选择文件
	 * 
	 * @param fileUrl
	 * @param flag
	 * @return
	 */
	@Override
	public Map<String, Integer> getModelFileList(String fileUrl, boolean flag) {
		if (StringUtils.isBlank(fileUrl)) {
			return getCurrentUserFile();
		}
		fileUrl = FileUtil.formalFilePath(fileUrl);
		File file = new File(fileUrl);
		Map<String, Integer> map = new TreeMap<String, Integer>();
		// 如果是文件夹就处理
		if (file.isDirectory()) {
			if (flag) {
				file.listFiles((file1) -> {if (file1.isDirectory()) {
					map.put(file1.getAbsolutePath(), 1);
					return true;
				}else{
					return false;
				}});
			}else {
				file.listFiles(new ModelFileSelect(map));
			}
		}
		return map;
	}

	class ModelFileSelect implements FilenameFilter {

		private Map<String, Integer> map;

		public ModelFileSelect(Map<String, Integer> map) {
			this.map = map;
		}

		@Override
		public boolean accept(File dir, String name) {
			File tempFile = new File(dir, name);
			if (!tempFile.isHidden()) {
				if (tempFile.isDirectory()) {
					// 1是文件夹
					map.put(tempFile.getAbsolutePath(), 1);
					return true;
				}
				//gltf和glb是为online适配的模型
				String suffix = name.contains(".") ? name.substring(name.lastIndexOf(".")) : name;
				if (suffix.equalsIgnoreCase(".zip") || suffix.equalsIgnoreCase(".json") || supportModelTypes.contains(suffix.toLowerCase())) {
					// 0是文件
					map.put(tempFile.getAbsolutePath(), 0);
					return true;
				}
			}
			return false;
		}
	}

	@Override
	@Transactional
	public void saveServerAutoConfig(ServerAutoConfig autoConfig) {
		ServerAutoConfig oldAutoConfig = systemConfigMapper.queryServerAutoConfig();
		systemConfigMapper.saveOrUpdateServerConfig(autoConfig);
		//每次保存前先将自动发布的数据类型表清空，然后再保存
		if (autoConfig.getIsOpen() == 0) {
			systemConfigMapper.deleteDataFormat();
		} else {
			systemConfigMapper.deleteDataFormat();
			systemConfigMapper.saveDataFormat(autoConfig.getDataFormat());
		}
		/**
		 * 如果改变了开启状态  从原来的开启到关闭，则直接关闭掉对应的监测
		 * 如果是从关闭开启，则重新构造参数，执行对应的监听
		 * 如果只是改变了对应的参数，则修改对应的监测参数即可
		 */
		if (autoConfig.getIsOpen() == 0) {
			if (fileMonitor.getMonitor() != null) {
				fileMonitor.stopFileMonitor();
			}
		}
		if (autoConfig.getIsOpen() == 1) {
			if (fileMonitor != null) {
				/**
				 * 监听器关闭异常可能是因为服务重启，服务重启后监听器不再生效但是它仍然存在，此时stop会抛异常，无需关注。
				 * 当服务处于运行状态下开启自动发布功能时，多次保存发布协议的同时需要先将之前保存的监听器关闭，防止多个监听器同时工作
				 */
				if (fileMonitor.getMonitor() != null) {
					try {
						fileMonitor.stopFileMonitor();
					} catch (Exception e) {
						log.error("文件监听器关闭异常",ExceptionsUtil.stackTraceToStr(e));
					}
				}
				fileMonitor.setMonitor(null);
			}
			fileMonitor.openFileMonitor(autoConfig);
			if (oldAutoConfig == null || !oldAutoConfig.getMonitorPath().equals(autoConfig.getMonitorPath())) {
				//首先异步查找当前的数据，并发布
				new Thread(new AutoPublishedData(autoConfig.getMonitorPath(), autoConfig.getDataFormat())).start();
			}
			if (oldAutoConfig != null && autoConfig.getMonitorPath().equals(oldAutoConfig.getMonitorPath())) {
				//监控路径不变，数据类型dataFormat变化时，要筛选增加的类型(删除类型不用管)，需要进行一次单独处理
				List<PublishedDataFormat> dif = new ArrayList<>();
				for (PublishedDataFormat dataFormat : autoConfig.getDataFormat()){
					if (!oldAutoConfig.getDataFormat().contains(dataFormat)){
						dif.add(dataFormat);
					}
				}
				if (!dif.isEmpty()) {
					new Thread(new AutoPublishedData(autoConfig.getMonitorPath(), dif)).start();
				}
			}
		}
	}

	@Override
	public ServerAutoConfig queryAutoConfig() {
		try {
			return systemConfigMapper.queryServerAutoConfig();
		} catch (Exception e) {
			log.error("SystemServiceImpl queryAutoConfig has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "自动发布配置"));
		}
	}

	@Override
	@Transactional
	public void updateSystemConfig(String fileServer,String autoPath) {
		ServerAutoConfig autoConfig = systemConfigMapper.queryServerAutoConfig();
		String oldMonitorPath = autoConfig.getMonitorPath();
		systemConfigMapper.updateSystemConfig(fileServer);
		systemConfigMapper.updateServerAutoConfig(autoPath);
		/**
		 * TODO 查看当前服务的监控路径，如果不相同，首先批量发布当前路径下的数据
		 */

		if (autoConfig != null) {

			if (!autoPath.equals(oldMonitorPath)) {
				//首先异步查找当前的数据，并发布
				new Thread(new AutoPublishedData(autoPath, autoConfig.getDataFormat())).start();
			}
		}
		fileMonitor.setMonitorRootPath(autoPath);
	}
	
	
	/**
	 * 自动检测并发布当前修改过的数据
	 *
	 * @Description: 
	 * @author: 刘洋 
	 * @date: 2020-01-13
	 */
	class AutoPublishedData implements Runnable{
		
		private String monitorPath;
		
		private List<PublishedDataFormat> dataFormat;
		
		public AutoPublishedData(String monitorPath,List<PublishedDataFormat> dataFormat) {
			this.monitorPath = monitorPath;
			this.dataFormat = dataFormat;
		}
		
		@Override
		public void run() {
			File file = new File(monitorPath);
			FileListener listener = new FileListener(dataFormat,applicationContext);
			publihedServerData(file,listener);
		}

		public void publihedServerData(File file,FileListener listener) {
			List<String> formats = new ArrayList<String>(dataFormat.size());
			dataFormat.forEach((format) -> formats.add(format.getDataFormat()));
			if (!file.isDirectory()) {
				return;
			}
			List<String> servers = new ArrayList<>();
			Arrays.asList(file.listFiles()).forEach((file1) -> {
				if ((!file1.isDirectory() && formats.contains(file1.getName().split("\\.")[1]))|| (formats.contains(file1.getName().split("\\.")[0])&&file1.getName().split("\\.")[1].equals("xml")))  {
					if (file1.toString().length() > 200) {
						servers.add(file1.getName().substring(0, 19));
					} else {
						listener.onFileCreate(file1);
					}
				}
				if (file1.isDirectory()) {
					publihedServerData(file1,listener);
				}
			});
			
			if (!servers.isEmpty()) {
				JSONObject jsonObject = new JSONObject();
				StringBuilder sBuilder = new StringBuilder();
				for (int i = 0; i < servers.size(); i++) {
					sBuilder.append(servers.get(i)).append(";");
				}
				String errMsg = sBuilder.toString().substring(0, sBuilder.toString().length() -1) + ":" + ErrorCode.FILE_PATH_TOO_LONG.getErrorMsg();
				jsonObject.put("status", 400);
				jsonObject.put("msg", errMsg);
				webSocket.sendMessage(jsonObject.toJSONString());
			}

		}
	}	

	@Override
	public String queryFileServerPath() {
		try {
			return systemConfigMapper.getFileServerPath();
		} catch (Exception e) {
			log.error("SystemServiceImpl queryFileServerPath has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new SqlException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.SQL_GET, "文件服务器路径"));
		}
	}

	@Override
	@Transactional
	public void updateSystemFileServer(String fileUrl,String userName,String password) {
		try {
			UserInfo userInfo = userService.getUserInfo(userName);
			if(userInfo != null) {
				return;
			}
			systemConfigMapper.updateSystemUser(userName);
			systemConfigMapper.updateSystemConfig(fileUrl);
			String newPassword = new SimpleHash("MD5", password, userName, 2).toHex();
			userInfo = new UserInfo(userName, newPassword, new Date());
			userInfo.setIsSysConfig(1);
			List<Integer> rolesIds = new ArrayList<Integer>();
			rolesIds.add(1);
			userInfo.setRoleIds(rolesIds);
			userService.addUser(userInfo);
			//保存当前安装时间到数据库，保证当前使用时间完整性
			updateSysTime();
		} catch (ResourceException e) {
			throw new ResourceException(e.getErrorCode(),e.getMessage());
		} catch (SqlException e) {
			throw new SqlException(e.getErrorCode(),e.getMessage());
		}catch (Exception e) {
			log.error("SystemServiceImpl updateSystemFileServer has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.BASE_ERROR);
		}
	}

	@Override
	public String checkSysTime(String reqIP) {
		//获取系统在当前机器的注册的到期时间
		if (EXPIRE_TIME == null) {
			EXPIRE_TIME = CommonUtil.getSystemExpireTime();
		}

		// 根据用户表判断时候为首次注册
		String userNames = userMapper.querySystemUser();
		boolean isFirst = StringUtils.isNotEmpty(userNames);
		// 如果还为空需要处理
		if (EXPIRE_TIME == null) {
			boolean isOk = CommonUtil.installExpireTime(EXPIRE_TIME, isFirst);
			if (!isOk) {
				log.error("系统文件受损，请联系技术服务");
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.CUS_CAN_USE_DATE_3,
						"System","服务试用", reqIP));
				return "系统文件受损，请联系管理员";
			}
		}
		
		log.debug("系统过期时间为："+ EXPIRE_TIME);
		
		/**
		 * 获取系统最近的使用时间,如果最近使用时间大于系统当前时间，则使用最近一次时间判断有效期
		 */
		Date systemDate = DateUtil.getFirstTimeOfDay();
		try {
			if (StringUtils.isBlank(CURRENT_USE_DATE)) {
				CURRENT_USE_DATE = systemConfigMapper.querySysTime();
				if (StringUtils.isBlank(CURRENT_USE_DATE)) {
					CURRENT_USE_DATE = CrypotUtil.encrypt(DateUtil.covertTimeToDayStr(systemDate));
				}
				log.debug("##############系统最近使用时间为："+CURRENT_USE_DATE);
			}
		} catch (ResourceException e) {
			log.error("SystemServiceImpl checkSysTime has error:" + e.getMessage());
		} catch (Exception e) {
			log.error("############转换时间出错!############## ",ExceptionsUtil.stackTraceToStr(e));
		}
		Date currentDate = DateUtil.covertStringToDate(CrypotUtil.decrypt(CURRENT_USE_DATE));
		log.debug("当前最近使用时间为：" + CrypotUtil.decrypt(CURRENT_USE_DATE));
		if (currentDate.after(systemDate)) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.CUS_CAN_USE_DATE_0, 
					"System","服务试用", reqIP));
			return "系统试用期已到，请联系管理员";
		}
		
		/**
		 * 如果当前日期加3天大于等于到期日期，提示用户期限已经不足
		 */
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(systemDate);
//		cal.add(Calendar.DATE, 3);
//		if (systemDate.before(EXPIRE_TIME) && (cal.getTime().equals(EXPIRE_TIME) || cal.getTime().after(EXPIRE_TIME))) {
//			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.CUS_CAN_USE_DATE_3,
//					"System","服务试用", reqIP));
//			log.info("当前系统时间为：" + systemDate);
//			return "系统试用期不足3天";
//		}
//
//		/**
//		 * 如果当前日期加3天大于等于到期日期，提示用户期限已经不足
//		 */
//		Calendar cale = Calendar.getInstance();
//		cale.setTime(systemDate);
//		cale.add(Calendar.DATE, 7);
//		if (systemDate.before(EXPIRE_TIME) && (cale.getTime().equals(EXPIRE_TIME) || cale.getTime().after(EXPIRE_TIME))) {
//			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.CUS_CAN_USE_DATE_7,
//					"System","服务试用", reqIP));
//			log.info("当前系统时间为：" + systemDate);
//			return "系统试用期不足7天";
//		}
//
//		/**
//		 * 如果当前日期大于等于到期日期，则提示用户已经过期
//		 */
//
//		if (systemDate.after(EXPIRE_TIME) || systemDate.equals(EXPIRE_TIME)) {
//			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_WARN, ExceptionsUtil.CUS_CAN_USE_DATE_3,
//					"System","服务试用", reqIP));
//			return "系统试用期已到，请联系管理员";
//		}
		return "系统可正常使用";
	}

	@Override
	public void updateSysTime() {
		Date curDate = DateUtil.getFirstTimeOfDay();
		String crypDate = CrypotUtil.encrypt(DateUtil.covertTimeToDayStr(curDate));
//		if (StringUtils.isBlank(CURRENT_USE_DATE)) {
//			CURRENT_USE_DATE = systemConfigMapper.querySysTime();
//			if (StringUtils.isBlank(CURRENT_USE_DATE)) {
//				//解密后直接保存
//				CURRENT_USE_DATE = crypDate;
//				systemConfigMapper.updateSysTime(crypDate);
//				return;
//			}
//		}
//		//解密最近使用时间
//		String unCrypDateStr = CrypotUtil.decrypt(CURRENT_USE_DATE);
//		Date currentDate = null;
//		try {
//			currentDate = DateUtil.covertStringToDate(unCrypDateStr);
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}
//		if (curDate.after(currentDate)) {
//			//保存加密后保存
			CURRENT_USE_DATE = crypDate;
			systemConfigMapper.updateSysTime(crypDate);
//		}
	}

	/**
	 * 首先base64解码
	 * 然后再解密
	 * 然后对比当前的mac地址是否一致,一致就将到期日期更改为指定的日期
	 */
	@Override
	public String activeSysTime(String activeCode) {
		if (StringUtils.isBlank(activeCode)) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, "激活码信息"));
		}
		String base64Code = new String(Base64.getDecoder().decode(activeCode));
		String code = CrypotUtil.decrypt(base64Code);
		//时间取后10位，mac取前面的
		String codeMac = code.substring(0, code.length() - 10);
		String currentMac = CommonUtil.getMacAddress();
		if (!codeMac.equalsIgnoreCase(currentMac)) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "MAC码信息"));
		}
		String codeDate = code.substring(code.length() - 10, code.length());
		//更新到期日期到注册表，并刷新缓存
		CommonUtil.updateSystemExpireTime(codeDate);
		EXPIRE_TIME = DateUtil.covertStringToDate(codeDate);
		return "激活成功";
	}

	@Override
	public String querySysMac() {
		return CommonUtil.getMacAddress();
	}

	/**
	 * 获取系统资源库
	 */
	@Override
	public String querySysResourcePath(String resourceName) {
		//根据资源名称获取资源路径，然后再读取资源文件信息
		if (RESOURCE_MAP.isEmpty()) {
			RESOURCE_MAP = getResourcePath();
		}
		if (!RESOURCE_MAP.containsKey(resourceName)) {
			RESOURCE_MAP.clear();
			RESOURCE_MAP = getResourcePath();
		}
		return RESOURCE_MAP.get(resourceName);
	}

	public Map<String, String> getResourcePath() {
		Map<String,String> resourceMap = new HashMap<String,String>();
		List<Map<String,String>> sysResourceMap = systemConfigMapper.querySysResource();
		for (Map<String, String> map : sysResourceMap) {
//			if (map.keySet().stream().forEach((key) -> if ()))
			resourceMap.put(map.get("resource_name"),map.get("resource_path"));
		}
		return resourceMap;
	}

	@Override
	public Object[] queryRemainingDays() {
		Date enDate = null;
		Date startDate = null;
		long remainDays;
		enDate = CommonUtil.getSystemExpireTime();
		startDate = DateUtil.covertStringToDate(DateUtil.covertTimeToDayStr(new Date()));
		remainDays = (enDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
		Object[] result = new Object[2];
		result[0] = enDate;
		result[1] = remainDays;
		return result;
	}

	@Override
	public void updateUsernameAndPassWord() {
		//1 查询系统用户信息
		String sysDefUser = "admin";
		SystemConfigInfo systemConfigInfo = systemConfigMapper.querySystemConfigInfo();
		String sysPass = systemConfigInfo.getSystemPassword();
		String sysUser = systemConfigInfo.getSystemUsername();
		//2 根据用户名查询用户信息 并删除已有记录
		UserInfo oldInfo = userService.getUserInfo(sysUser);
		List<Integer> ids = new ArrayList<>();
		if (oldInfo != null) {
			ids.add(oldInfo.getUserId());
			userService.deleteUerById(ids,ExceptionsUtil.getServerIp(null));
		}
		//3 更新系统用户表中原有的用户名
		systemConfigMapper.updateSystemUser(sysDefUser);
		//4 整理用户相关信息并保存入库
		String newPassword = new SimpleHash("MD5", sysPass, sysDefUser, 2).toHex();
		UserInfo newInfo = new UserInfo(sysDefUser, newPassword, new Date());
		newInfo.setIsSysConfig(1);
		List<Integer> rolesIds = new ArrayList<Integer>();
		rolesIds.add(1);
		newInfo.setRoleIds(rolesIds);
		userService.addUser(newInfo);
	}

	@Override
	public List<BatchPublishEntity> getServerFileList(String fileUrl,boolean isDir,String serverType) {
		try {
//			if (fileUrl.contains("/")) {
//				String tempDir = fileUrl;
//			}
			parentPath = fileUrl;
			List<BatchPublishEntity>  result = new ArrayList<BatchPublishEntity>();
			if (isDir) {
				getUsedFileFromDirPath((new File(fileUrl)).getAbsolutePath(),serverType);
				result.addAll(entityList);
			} else {
				result.add(getServerFileInfo(fileUrl,serverType));
			}
			if (result.size() > 1) {
				return clearDuplicateModels(result);
			}
			return result;
		} finally {
			entityList.clear();
		}
	}
	
	private List<BatchPublishEntity> clearDuplicateModels(List<BatchPublishEntity>  result) {
		Set<BatchPublishEntity> zips = new HashSet<BatchPublishEntity>();
		Set<BatchPublishEntity> zips1 = new HashSet<BatchPublishEntity>();
		List<BatchPublishEntity> unzips = new ArrayList<BatchPublishEntity>();
		for (BatchPublishEntity batchPublishEntity : result) {
			String fullPath = batchPublishEntity.getFilePath();
			if (fullPath.endsWith(".zip")) {
				zips.add(batchPublishEntity);
				zips1.add(batchPublishEntity);
			} else {
				unzips.add(batchPublishEntity);
			}
		}
		for (BatchPublishEntity unZIP : unzips) {
			String fullPath = unZIP.getFilePath();
			String suffix = fullPath.substring(fullPath.lastIndexOf("."));
			String path = fullPath.replace(suffix, "");
			for (BatchPublishEntity zip : zips) {
				String fullPath1 = zip.getFilePath();
				String suffix1 = fullPath1.substring(fullPath1.lastIndexOf("."));
				String path1 = fullPath1.replace(suffix1, "");
				if (unZIP.getModelType().equalsIgnoreCase(zip.getModelType()) && path.equalsIgnoreCase(path1)
						&& suffix1.equalsIgnoreCase(".zip")) {
					zips1.remove(zip);
				}
			}
		}
		unzips.addAll(zips1);
		return unzips;
	}
	
	private BatchPublishEntity getServerFileInfo(String filePath,String serverType) {
		BatchPublishEntity entity = null;
		File file = new File(filePath);
		String fileName = MapServerUtils.getUTF8FilePath(file.getName());
		switch (serverType) {
		case "mapServer":
			String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : fileName;
			//截取文件后缀后如果仍然包含"."则将其过滤掉
			String newName = fileName.replace(suffix, "");
			if (newName.contains(".")) {
				return null;
			}
			if (!file.isHidden() && (supportServerTypes.contains(suffix.toLowerCase()))) {
				entity = new BatchPublishEntity(fileName,MapServerUtils.getUTF8FilePath(file.getAbsolutePath()),false,null,null, isOverViews(file, suffix));
			}
			if (supportTiledTypes.contains(fileName.toLowerCase())) {
				String path = MapServerUtils.getUTF8FilePath(file.getAbsolutePath());
				entity = new BatchPublishEntity(fileName, path, mapServerService.isDemServer(path,null), getExtensionOfServer(path,null),null, isOverViews(file, suffix));
			}
			break;
        case "modelServer":
			if (fileName.endsWith(".zip")) {
				entity = getModelFileInfo(MapServerUtils.getUTF8FilePath(file.getAbsolutePath()));
			} else {
				String modelType = fileName.substring(fileName.lastIndexOf(".") + 1);
				entity = new BatchPublishEntity(fileName,MapServerUtils.getUTF8FilePath(file.getAbsolutePath()),false,null, modelType,false);
			}
			break;
		}
		return entity;
	}

	/**
	 * 批量发布时对文件大小及文件类型进行校验是否可以生成金字塔缓存
	 *
	 * @param file 源文件
	 * @param suffix 文件后缀
	 * @return boolean
	 */
	private boolean isOverViews(File file, String suffix) {
		// 批量发布时对文件大小及文件类型进行校验是否可以生成金字塔缓存
		boolean isOverView = Double.compare(file.length() / 1024.0 / 1024.0 / 1024.0, 5.00) < 0 ? false :true;
		boolean img = StringUtils.equalsAnyIgnoreCase(suffix, ".tif", ".tiff", "img");
		return isOverView && img;
	}
	/**
	 * 获取zip包中的模型信息，一个zip包只能包含一个模型
	 * @param zipPath
	 * @return
	 */
	private BatchPublishEntity getModelFileInfo(String zipPath) {
		int modelsNum = 0;
		BatchPublishEntity entity = null;
		try {
			ZipFile file = new ZipFile(zipPath,Charset.forName("gbk"));
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String fileName = entry.getName();
				String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : fileName;
				//glb和gltf不支持发布zip包的格式
				if (supportModelTypes.contains(suffix.toLowerCase()) && !(".glb".equalsIgnoreCase(suffix) || ".gltf".equalsIgnoreCase(suffix))) {
					entity = new BatchPublishEntity();
					entity.setFilePath(zipPath);
					File fil = new File(zipPath);
					entity.setFileName(fil.getName());
					entity.setModelType(suffix.replace(".", ""));
					modelsNum++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (modelsNum == 1) {
			return entity;
		} else {
			return null;
		}
	}
	
	private void getUsedFileFromDirPath(String dirPath,String serverType){
        /*if (dirPath.contains("/")) {
        	dirPath = dirPath.replace("/", "\\");
		}*/
        String newPath = null;
        if (dirPath.startsWith(parentPath) && (dirPath.length() > (parentPath.length() + 1))){
            newPath = dirPath.replace(parentPath + File.separator,"").replace(File.separator, ";");
            if (newPath.split(";").length >= 3){
                return;
            }
        }
		File[] files = (new File(MapServerUtils.getUTF8FilePath(dirPath))).listFiles();
		if (files == null || files.length == 0) {
			return;
		}
		List<File> subDirs = new ArrayList<>();
		String fileName = null;
		//先处理文件,将目录暂存在集合中,如果所有文件都处理完并没有返回再循环这些目录
		boolean isContainSptialFile = false;
		for (File subFile : files) {
			if (!subFile.isHidden() && subFile.isDirectory()) {
				subDirs.add(subFile);
				continue;
			}
			fileName = MapServerUtils.getUTF8FilePath(subFile.getName());
			if (serverType.equals("mapServer")) {
				String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : fileName;
				//截取文件后缀后如果仍然包含"."则将其过滤掉
				String newName = fileName.replace(suffix, "");
				if (newName.contains(".")) continue;
				if (!subFile.isHidden() && (supportServerTypes.contains(suffix.toLowerCase()))) {
					putFileInfoToMap(fileName, MapServerUtils.getUTF8FilePath(subFile.getAbsolutePath()),false,null,null, isOverViews(subFile, suffix));
				}
				if (supportTiledTypes.contains(fileName.toLowerCase())) {
					String path = MapServerUtils.getUTF8FilePath(subFile.getAbsolutePath());
					putFileInfoToMap(fileName, path, mapServerService.isDemServer(path,null), getExtensionOfServer(path,null),null, isOverViews(subFile, suffix));
					isContainSptialFile = true;
					subDirs.clear();
					return;
				}
			} else if (serverType.equals("modelServer")) {
				String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : fileName;
				if (suffix.toLowerCase().equals(".zip")) {
					BatchPublishEntity entity = getModelFileInfo(subFile.getAbsolutePath());
					if (entity != null) {
						putFileInfoToMap(entity.getFileName(), entity.getFilePath(),false,null,entity.getModelType(), false);
					}
				} else if (supportModelTypes.contains(suffix.toLowerCase())) {
					putFileInfoToMap(fileName, subFile.getAbsolutePath(),false,null,suffix.replace(".", ""), false);
				}
			}
		}
		if (subDirs.size() > 0 && !isContainSptialFile) {
			for (File dir : subDirs) {
				getUsedFileFromDirPath(dir.getAbsolutePath(),serverType);
			}
		}
	}
	
	/**
	 * 在map中存放文件信息时需要处理文件重名情况
	 * @param name
	 * @param path
	 */
	private void putFileInfoToMap(String name,String path,boolean isDemServer,String extension,String modelType, boolean isOverViews) {
		if(StringUtils.isNoneBlank(name) && StringUtils.isNoneBlank(path)) {
			BatchPublishEntity entity = new BatchPublishEntity(name,path,isDemServer,extension,modelType, isOverViews);
			entityList.add(entity);
		}
	}

	@Override
	public void resetLogbackInfo(UserLogbackInfo logInfo,String reqIP) {
		File file = new File(logInfo.getCurLogPath());
		if(!file.exists() || !file.isDirectory() || !file.canRead()) {
			throw new ResourceException(ExceptionsUtil.ERROR_301,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND,
					logInfo.getCurLogPath() + "日志路径"));
		}
        if (logInfo.getLogFileSize() < 0 && logInfo.getLogFileSize() >= 10) {
			throw new ArgumentException(ExceptionsUtil.ERROR_301,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "日志文件大小参数"));
		}
        if (logInfo.getLogMaxSave() < 0 && logInfo.getLogMaxSave() >= 90) {
			throw new ArgumentException(ExceptionsUtil.ERROR_301,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "日志文件最大保存天数参数"));
		}

		String curLogPath = Constant.TILE_CACHE_TMP.replace("temp", "logs");
		if (curLogPath.equals(logInfo.getCurLogPath())) {
			logInfo.setCurLogPath("logs");
		}
        //先重新配置mapserver的日志配置
        logBackXmlConfigHelper.resetLogbackInfo(logInfo, LogBackXmlConfigHelper.mapserverLogBack);
        //然后对tileserver进行配置
        try {
        	renderTileService.resetLogbackInfo(logInfo);
		} catch (Exception e) {
			if(ExceptionsUtil.isPytcError(ExceptionsUtil.stackTraceToStr(e))) {
				systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.formatInfo(ExceptionsUtil.PYTC_SERVER_ERROR,
					"logback自定义配置信息"), "System","Pytc服务", reqIP));
			}
			log.error("MapServerServiceImpl dealServerData has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_PYTC, "logback自定义配置信息"));
		}
	}
	
	@Override
	public UserLogbackInfo queryLogbackInfo() {
		UserLogbackInfo logbackInfo = logBackXmlConfigHelper.getLogbackInfo(LogBackXmlConfigHelper.mapserverLogBack);
		String curLogPath = logbackInfo.getCurLogPath();
		if ("logs".equals(curLogPath)) {
			curLogPath = Constant.TILE_CACHE_TMP.replace("temp", "logs");
			logbackInfo.setCurLogPath(curLogPath);
		}
		return logbackInfo;
	}

	@Override
	public void saveSysLog(SystemLogEntity sysLog) {
		if(sysLog != null) {
			systemLogService.saveSystemLog(sysLog);
		}
	}

	@Override
	public ServerCacheConfigEntity queryCacheConfig(String reqIP) {
		return systemConfigMapper.queryCacheConfig();
	}
	
	@Override
	public Map<String,Integer> queryCacheDirSize(String newPath,String reqIP) {
		 Map<String,Integer> map =new HashMap<String, Integer>();
		String oldPath = queryCacheConfig(null).getCachePath();
		oldPath = oldPath.endsWith("PYGISServerCache") ? oldPath : oldPath + File.separator + "PYGISServerCache";
		int cacheSize =(int)Math.ceil(FileUtil.getFileLength(oldPath)/(1024.0 * 1024.0 * 1024.0));
		File file = new File(newPath);
		int newPathSize = 0;
		if (!file.exists()) file = file.getParentFile();
		newPathSize = (int)Math.ceil(file.getFreeSpace()/(1024.0 * 1024.0 * 1024.0));
		map.put("cacheSize", cacheSize);
		map.put("newPathSize", newPathSize);
		return map;
	}

	@Override
	public void updateCacheConfig(ServerCacheConfigEntity configEntity,String reqIP,boolean isMoveCacheFile) {
		ServerCacheConfigEntity entity = systemConfigMapper.queryCacheConfig();
		//对用户下发的缓存参数进行合法性校验
		checkCacheConfig(configEntity,entity);
		//如果磁盘缓存路径为空则认为用户修改了redis缓存配置，否则就是修改了访问缓存配置
		if (StringUtils.isBlank(configEntity.getCachePath()) && configEntity.getActualDisk() == 0) {
			updateRamCacheConfig(configEntity,entity,reqIP);
		} else {
			updateVisitCacheConfig(configEntity,entity,reqIP,isMoveCacheFile);
		}
	}
	
	/**
	 * 更新Redis缓存配置
	 * @param newConfig
	 * @param oldConfig
	 */
	private void updateRamCacheConfig(ServerCacheConfigEntity newConfig,ServerCacheConfigEntity oldConfig,String reqIP) {
		boolean ram = newConfig.getActualRam() == oldConfig.getActualRam();
		boolean isUseCache = newConfig.getUseRedisCache() == oldConfig.getUseRedisCache();
		if(ram && isUseCache) {
			return;
		}
//		TileServiceController.setRedisStatus(newConfig.getUseRedisCache());
		newConfig.setActualDisk(0);
		newConfig.setCachePath("");
		updateRamCache(newConfig.getActualRam(),reqIP);
		systemConfigMapper.updateCacheConfig(newConfig);
	}
	
	/**
	 * 更新访问缓存配置
	 * @param newConfig
	 * @param oldConfig
	 */
    private void updateVisitCacheConfig(ServerCacheConfigEntity newConfig,ServerCacheConfigEntity oldConfig,String reqIP,boolean isMoveCacheFile) {
    	boolean disk = newConfig.getActualDisk() == oldConfig.getActualDisk();
		boolean path = newConfig.getCachePath().equals(oldConfig.getCachePath());
		newConfig.setUseRedisCache(-1);
		//1.disk和path都不变
		if(disk && path) {
			return;
		}
		//2.disk变，path不变
		if(!disk && path) {
			newConfig.setActualRam(0);
			newConfig.setCachePath("");
			updateDiskCache(oldConfig.getActualDisk(),newConfig.getActualDisk(),false,oldConfig.getCachePath(),reqIP);
			systemConfigMapper.updateCacheConfig(newConfig);
		}
		//3.path变，disk不变
		if(disk && !path) {
			newConfig.setActualRam(0);
			newConfig.setActualDisk(0);
			updateCachePath(oldConfig.getCachePath(),newConfig.getCachePath(),reqIP,isMoveCacheFile);
			systemConfigMapper.updateCacheConfig(newConfig);
		}
		//4.disk和path都变
		if(!disk && !path) {
			newConfig.setActualRam(0);
			updateCachePath(oldConfig.getCachePath(),newConfig.getCachePath(),reqIP,isMoveCacheFile);
			updateDiskCache(oldConfig.getActualDisk(),newConfig.getActualDisk(),true,null,reqIP);
			systemConfigMapper.updateCacheConfig(newConfig);
		}
	}
	
    /**
     * 对用户修改的配置参数进行校验
     * @param newConfig
     * @param oldConfig
     */
	private void checkCacheConfig(ServerCacheConfigEntity newConfig,ServerCacheConfigEntity oldConfig) {
		if (StringUtils.isBlank(newConfig.getCachePath())) {
			if(newConfig.getActualRam() < oldConfig.getMinRam() || newConfig.getActualRam() > oldConfig.getMaxRam()) {
				throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "内存缓存设置参数"));
			}
		} else {
			if(newConfig.getActualDisk() < oldConfig.getMinDisk() || newConfig.getActualDisk() > oldConfig.getMaxDisk()) {
				throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "访问缓存设置参数"));
			}
			String path = System.getProperty(TdbUtils.USER_VISIT_CACHE_DIR);
			File file = new File(path);
			int freeSize = (int)Math.ceil(file.getParentFile().getFreeSpace()/(1024.0 * 1024.0 * 1024.0));
			int cacheSize = (int)Math.ceil(FileUtil.getFileLength(path)/(1024.0 * 1024.0 * 1024.0));
			if((freeSize + cacheSize) < newConfig.getActualDisk()) {
				throw new ArgumentException(ExceptionsUtil.ERROR_500,"访问缓存设置参数超过当前磁盘所能使用的最大值");
			}
			if(StringUtils.isBlank(newConfig.getCachePath())) {
				throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_NULL, "访问缓存存储路径"));
			}
		}
	}
	
	//磁盘大小设置只是更新最大缓存，当不改变缓存路径，且更新后的缓存大于原有配置时均不处理
    private void updateDiskCache(int oldDisk,int newDisk,boolean isChangeDir,String curPath,String reqIP) {
    	long maxCache = 1L * newDisk * 1024 * 1024 * 1024;
    	TiledInfoReadAbstract.maxCacheSize = maxCache;
    	if(!isChangeDir) {
    		if(oldDisk > newDisk) {
    			Thread t = new Thread(new VisitCacheClearRunnable(curPath,null,false,TiledInfoReadAbstract.maxCacheSize,false));
    			t.start();
    		}
    	}
		
	}
    
    /**
     * 获取Redis的文件路径，key可能是redis的配置文件，也可能是启动redis的bat脚本
     * @param key
     * @return
     */
    private String getRedisPath(String key,String reqIP) {
    	boolean isInEnv = true;
    	boolean isInRedis = true;
    	String path = System.getProperty(key);
    	if(path == null) {
    		isInEnv = false;
    		path = (String)redisUtil.getValue(key);
    	}
    	//如果环境变量和Redis中都不存在该路径，则从服务安装目录下去找
    	if(path == null) {
    		isInRedis = false;
    		String parPath = System.getenv("PYGISServer_HOME");
    		if(parPath == null) {
    			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "PYGISServer服务环境变量PYGISServer_HOME"));
    		}
    		File file = new File(parPath);
    		if(!file.exists() || !file.isDirectory()) {
    			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "PYGISServer安装目录"));
    		}
    		parPath = file.getAbsolutePath();
    		if(key.equals(TdbUtils.REDIS_CONFIG_PATH)) {
    			path = parPath + File.separator + "Redis" + File.separator + "redis.windows-service.conf";
    		} else if (key.equals(TdbUtils.REDIS_START_BAT_PATH)) {
    			path = parPath + File.separator + "restartRedisBat.bat";
    		}
    	}
    	//无论环境变量或redis，只要有一个不存在，就将两个存储都更新，以防止数据不一致问题出现
    	if(isInEnv == false || isInRedis == false) {
    		System.setProperty(key, path);
    		redisUtil.deleteKey(reqIP,key);
    		redisUtil.storeValue(reqIP,key, path);
    	}
    	return path;
    }
	
    /**
     * 内存缓存更新功能实现方法
     * @param newRam
     */
    private void updateRamCache(int newRam,String reqIP) {
    	String redisConf = getRedisPath(TdbUtils.REDIS_CONFIG_PATH,reqIP);
    	String redisBat = getRedisPath(TdbUtils.REDIS_START_BAT_PATH,reqIP);
    	Thread t = new Thread(new RamCacheAdapterRunnable(redisConf,redisBat,newRam, systemLogService, exceptionsUtil,reqIP));
		t.start();
	}
	
    //如果修改缓存路径，则只需要将原有的整个目录删除，并将新的路径进行更新即可
	private void updateCachePath(String oldPath,String newPath,String reqIP,boolean isMoveCacheFile) {
		newPath = newPath + File.separator + "PYGISServerCache";
		Thread t = new Thread(new VisitCacheClearRunnable(oldPath,newPath, true, 0,false));
		t.start();
		//更新数据库磁盘大小
		ServerCacheConfigEntity entity = queryCacheConfig(reqIP);
		File file = new File(newPath);
		int maxDiskSize = (int)Math.ceil(file.getParentFile().getFreeSpace()/(1024.0 * 1024.0 * 1024.0));
		//如果实际磁盘大小与用户设置磁盘大小相等，说明用户没有设置访问缓存大小
		int actDisk = entity.getActualDisk() == entity.getMaxDisk() ? maxDiskSize : entity.getActualDisk();
		systemConfigMapper.updateMaxCacheConfig(entity.getMaxRam(), maxDiskSize,actDisk,entity.getCachePath());
		//清除之前所有的TDB缓存信息
		CustomDataLoadImpl.tdbFileStoreMaxCol.clear();
		TdbUtils.tdbMap.clear();
		TdbScheduledTask.POS_INFO_MAP.clear();
		System.setProperty(TdbUtils.USER_VISIT_CACHE_DIR, newPath);
		//更新访问缓存信息，最大使用
		TiledInfoReadAbstract.maxCacheSize = 1L * actDisk * 1024 * 1024 * 1024;
		TiledInfoReadAbstract.curDirSize = 0;
	}

	@Override
	public Map<String, Object> checkPostGisConnect(PostGisConnectEntity connectEntity) {
		boolean isOk = false;
		Map<String, Object> result = new HashMap<>();
		String sourceName = connectEntity.getDataSourceName();
		DataStore dataSource = null;
		try {
			if(checkPostGISIsDuplicate(connectEntity)) {
				result.put("isOk", isOk);
				result.put("duplicateServer", "duplicateServer");
				return result;
			}
			//测试数据源信息时只对数据源名称进行长度和特殊字符处理
			PostGisConnectEntity entity = queryPostGis(connectEntity.getDataSourceName());
			if (entity != null) {
				result.put("isOk", isOk);
				result.put("数据源名称已存在", "数据源名称已存在");
				return result;
			}
			result.put("isOk", isOk);
			result.put(sourceName, null);
			connectEntity.setDataSourceName(sourceName);
			dataSource = PostGisStoreUtils.getPostGisFromCache(sourceName);
			if (dataSource == null) {
				dataSource = PostGisStoreUtils.getPostGiDataStore(connectEntity);
			}
			if (dataSource != null) {
				isOk = true;
			}
			String[] layers = dataSource.getTypeNames();
			result.put(sourceName, Arrays.asList(layers));
			result.put("isOk", isOk);
			if (isOk == true) {
				//将密码加密后再存入数据库--使用base64进行简单的加密
				connectEntity.setDbStatus(1);
				connectEntity.setPublishTime(new Date());
				connectEntity.setPassword(PostGisStoreUtils.getEncodePass(connectEntity.getPassword()));
				mapServer.savePostgisInfo(connectEntity);
				if(!postGisMap.containsKey(sourceName)) {
					postGisMap.put(sourceName, connectEntity);
				}
			}
		} catch (Exception e) {
			result.put("isOk", false);
			PostGisStoreUtils.deletePostGisFromCache(sourceName);
			log.error("MapServerServiceImpl checkPostGisConnect has error:" + ExceptionsUtil.stackTraceToStr(e));
			String msg = "PostGis数据库连接失败，请检查PostGIS配置参数是否正确";
			throw new ResourceException(ExceptionsUtil.ERROR_500,msg);
		}
		return result;
	}

	private boolean checkPostGISIsDuplicate(PostGisConnectEntity connectEntity) {
		List<PostGisConnectEntity> giss = queryPostGisInfos();
		for(PostGisConnectEntity entity: giss) {
			if(entity.equals(connectEntity)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 移除postgis配置时，先检查并删除已经发布的服务，然后清除redis中的缓存，第三，清除中间表和数据库连接表信息
	 */
	@Override
	public void deletePostgisInfo(List<Integer> ids,String reqIP) {
		for (int i = 0; i < ids.size(); i++) {
			List<Integer> servers = mapServer.queryServerIdBySourceId(ids.get(i));
			if (servers.size() > 0) {
				mapServerService.deleteServerData(servers,reqIP);
			}
			PostGisConnectEntity entity = mapServer.queryPostGis(ids.get(i)).get(0);
			if(postGisMap.containsKey(entity.getDataSourceName())) {
				postGisMap.remove(entity.getDataSourceName());
			}
			PostGisStoreUtils.deletePostGisFromCache(entity.getDataSourceName());
		}
		mapServer.deletePostgisInfo(ids);
	}

	/**
	 * 先同步修改缓存中存储的数据库连接信息，再修改数据库记录
	 */
	@Override
	public String editPostGis(PostGisConnectEntity connectEntity) {
		PostGisConnectEntity oldEntity = queryPostGisById(connectEntity.getDbId()).get(0);
		if(oldEntity.isSamePostGIS(connectEntity)) {
			return "编辑成功";
		}
		List<PostGisConnectEntity> conns = queryPostGisInfos();
		for (PostGisConnectEntity entity : conns) {
			if (connectEntity.getDbId() == entity.getDbId()) {
				continue;
			}
			if (connectEntity.equals(entity)) {
				return "数据库重复";
			}
		}

		DataStore store = null;
		//测试数据源信息时只对数据源名称进行长度和特殊字符处理
		String sourceName = connectEntity.getDataSourceName().replace(Constant.SPECIAL_CHARACTER, "");
		if (sourceName.length() > 20) {
			sourceName = sourceName.substring(0,19);
		}
		if(postGisMap.size() == 0) {
			queryPostGisInfos();
		}
		if (postGisMap.containsKey(connectEntity.getDataSourceName())) {
			if (connectEntity.getDbId() == oldEntity.getDbId() && !connectEntity.getDataSourceName().equals(oldEntity.getDataSourceName())) {
				return "数据源名称重复";
			}
		}
		connectEntity.setDataSourceName(sourceName);
		if (PostGisStoreUtils.isPostGisInCache(oldEntity.getDataSourceName())) {
			store = PostGisStoreUtils.getPostGisFromCache(oldEntity.getDataSourceName());
			PostGisStoreUtils.deletePostGisFromCache(oldEntity.getDataSourceName());
		} else {
			if (store == null) {
				oldEntity.setPassword(PostGisStoreUtils.getDecodePass(oldEntity.getPassword()));
				store = PostGisStoreUtils.getPostGiDataStore(oldEntity);
				PostGisStoreUtils.deletePostGisFromCache(oldEntity.getDataSourceName());
			}
		}
		/**
		 * 当前修改包括如下几种情况，分别是  数据源名称改变其他不变，或者其他
		 * 第一种情况只对原有的数据信息进行编辑，第二种情况修改数据库记录，重新创建数据库连接并更新到缓存中去
		 */
		if (postGisMap.containsKey(oldEntity.getDataSourceName())) {
			postGisMap.remove(oldEntity.getDataSourceName());
		}
		PostGisConnectEntity newEntity = new PostGisConnectEntity();
		newEntity.setDbId(oldEntity.getDbId());
		newEntity.setUpdateTime(new Date());
		if (PostGisStoreUtils.isEditPostGISName(oldEntity, connectEntity)) {
			newEntity.setDataSourceName(connectEntity.getDataSourceName());
		} else {
			newEntity.setDataSourceName(connectEntity.getDataSourceName());
			newEntity.setDatabaseName(connectEntity.getDatabaseName());
			newEntity.setDatabaseIp(connectEntity.getDatabaseIp());
			newEntity.setPort(connectEntity.getPort());
			newEntity.setUserName(connectEntity.getUserName());
			newEntity.setPassword(PostGisStoreUtils.getEncodePass(connectEntity.getPassword()));
			newEntity.setDbType(connectEntity.getDbType());
			store = PostGisStoreUtils.getPostGiDataStore(connectEntity);
		}
		try {
			String[] layers = store.getTypeNames();
		} catch (Exception e) {
			postGisMap.remove(newEntity.getDataSourceName());
			PostGisStoreUtils.deletePostGisFromCache(newEntity.getDataSourceName());
			return "PostGIS数据源连接失败，请确保配置参数正确";
		}
		postGisMap.put(newEntity.getDataSourceName(), newEntity);
		mapServer.editPostGis(newEntity);
		PostGisStoreUtils.savePostGisToCache(connectEntity.getDataSourceName(), store);
		return "编辑成功";

	}

	@Override
	public List<PostGisConnectEntity> queryPostGisById(int id) {
		return mapServer.queryPostGis(id);
	}

	@Override
	public Map<String, List<String>> queryServerNameBySourceName(List<String> sourceNames) {
		Map<String, List<String>> result = new HashMap<>();
		String sourceName = null;
		for (String str:sourceNames) {
			sourceName = str;
			result.put(sourceName, mapServer.queryServerNameBySourceName(sourceName));
		}
		return result;
	}


	@Override
	public Map<String, String> queryPostGisInfos(String sourceName) {
		Map<String, String> map = new TreeMap<>();
		try {
			DataStore store = PostGisStoreUtils.getPostGisFromCache(sourceName);
			if (store == null) {
				store = PostGisStoreUtils.getPostGiDataStore(queryPostGis(sourceName));
			}
			// 查出数据库设置中指定database中的表
			//String[] layers = store.getTypeNames();
			ResultSet resultSet = ((JDBCDataStore) store).getDataSource().getConnection().createStatement().executeQuery(Constant.QUERY_TABLE_NAMES);
			List<String> layers = new ArrayList<>();
			while (resultSet.next()) {
				layers.add(resultSet.getString("TABLE_NAME"));
			}
			for (String str : layers) {
				SimpleFeatureSource source = store.getFeatureSource(str);
				if(source != null) {
					int dataType = styleInfoService.getFeatureTypeByFileUrl(sourceName + ";" + str);
					if (dataType != -1) {
						map.put(str, dataType + "");
					}
				}
			}
		} catch (IOException | SQLException e) {
			PostGisStoreUtils.deletePostGisFromCache(sourceName);
			log.error("MapServerServiceImpl queryPostGisInfos has error:" +  ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "PostGis图层"));
		}
		return map;
	}

	@Override
	public PostGisConnectEntity queryPostGis(String sourceName) {
		PostGisConnectEntity entity = mapServer.queryPostGisBySourceName(sourceName);
		if (entity != null) {
			entity.setPassword(PostGisStoreUtils.getDecodePass(entity.getPassword()));
		}
		return entity;
	}

	@Override
	public List<PostGisConnectEntity> queryPostGisInfos() {
		List<PostGisConnectEntity> results = mapServer.queryPostGis(-1);
		results.stream().forEach((postgis) -> {
			if(!postGisMap.containsKey(postgis.getDataSourceName())) {
				postGisMap.put(postgis.getDataSourceName(), postgis);
			}
		});
		return results;
	}

	@Override
	public void updatePostGisStatus(int id, int status) {
		PostGisConnectEntity entity = new PostGisConnectEntity();
		entity.setDbId(id);
		entity.setDbStatus(status);
		entity.setUpdateTime(new Date());
		mapServer.editPostGis(entity);
	}

	/**
	 * 只有当服务的路径中包含分号，且分号前边的字段已经存储在postgis缓存中时，才返回true
	 * @param postGisRul
	 * @return
	 */
	public static boolean isPostGIS(String postGisRul) {
		if(postGisRul.contains(";")) {
			String datasoueName = postGisRul.split(";")[0];
			if(postGisMap.containsKey(datasoueName)) {
				return true;
			}
		}
		return false;
	}

	public void checkPostGISList() {
		if (postGisMap.isEmpty()) {
			List<PostGisConnectEntity> connects = mapServer.queryPostGis(-1);
			if (connects == null || connects.size() == 0) {
				return;
			}
			connects.forEach((entity) -> {
                entity.setPassword(PostGisStoreUtils.getDecodePass(entity.getPassword()));
				PostGisStoreUtils.getPostGiDataStore(entity);
				postGisMap.put(entity.getDataSourceName(), entity);
			});
		}
	}

	private String getExtensionOfServer(String url, FeatureInfo featureInfo) {
		String extension = null;
		File tempFile = null;
		//如果FeatureInfo不为null，就从该对象中获取文件后缀，否则从文件中解析
		if(featureInfo != null) {
			return featureInfo.getTileExtension();
		}

		File file = new File(url);
		if(file.isDirectory()) {
			file = new File(file,"config.xml");
		}
		CustomDataInfo customDataInfo = new CustomDataInfo(file.getAbsolutePath(),null);
		if(file.exists() && (file.getAbsolutePath().endsWith("config.xml") || file.getAbsolutePath().endsWith("tms.xml"))) {
			extension = customDataInfo.parseXml(file.getAbsolutePath()).getExtension();
		}
		if(extension == null) {
			extension = mapServerService.getTDBFileExtension(url);
		}
		if(extension == null) {
			String terrianPath = file.getParent() + File.separator + "0" + File.separator + "0" + File.separator + "0.terrain";
			tempFile = new File(terrianPath);
			if(tempFile.exists() && tempFile.isFile()) {
				extension = "terrain";
			}
		}
		return extension;
	}

	@Override
	public List<FunctionPageEntity> queryFunctionPageEntity() {
		return systemConfigMapper.queryFunctionPageEntity();
	}
}
