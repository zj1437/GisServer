package org.pygisserver.web.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.pygisserver.common.entity.*;
import org.pygisserver.common.enumm.StoreType;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.entity.*;
import org.pygisserver.web.service.SystemService;
import org.pygisserver.web.service.TileCacheService;
import org.pygisserver.web.service.UserService;
import org.pygisserver.web.service.impl.SystemServiceImpl;
import org.pygisserver.web.entity.*;
import org.pygisserver.web.utils.MapServerUtils;
import org.pygisserver.web.utils.PostGisStoreUtils;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Config controller.
 *
 * @Description: 首页加载controller ，主要用于首页的加载
 * @author:
 * @date: 2018 -10-22
 */
@Api(tags="公共接口")
@RestController
@Slf4j
public class ConfigController {
	
	@Autowired
	private SystemService systemService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private TileCacheService tileCacheService;
	
	private boolean versionFlag = false;
	
	/**
	 * 
	 * 服务发布文件列表获取
	 * @param fileUrl
	 * @param fileType
	 * @return
	 */
	@ApiOperation(value="文件列表",notes="地图服务文件列表获取")
	@ApiImplicitParams({
		@ApiImplicitParam(name="fileUrl",value="文件url",required=false,paramType="query"),
		@ApiImplicitParam(name="fileType",value="文件类型",required=false,paramType="query")
	})
	@RequestMapping(value="/queryFileList",method=RequestMethod.GET)
	@ResponseBody
	public Map<String, Integer> queryFileListByCurrentSelect(@RequestParam(value="fileUrl",required=false) String fileUrl,
															 @RequestParam(value="fileType",required=false) String fileType){
		//如果参数为空，则获取当前服务器的根目录
		Subject subject = SecurityUtils.getSubject();
		String userName = subject.getPrincipal().toString();
		//如果未登录，直接返回空
		if (StringUtils.isBlank(userName)) {
			return null;
		}else {
			return systemService.getManagerFileList(fileUrl,fileType);
		}
	}

	/**
	 * 模型文件列表获取
	 * @param fileUrl
	 * @return
	 */
	@ApiOperation(value="文件列表",notes="模型服务文件列表获取")
	@ApiImplicitParams({
		@ApiImplicitParam(name="fileUrl",value="文件url",required=false,paramType="query"),
		@ApiImplicitParam(name="flag",value="是否只显示文件夹",required=false,paramType="query")
	})
	@RequestMapping(value="/queryModelFileList",method=RequestMethod.GET)
	@ResponseBody
	public Map<String, Integer> queryModelFileList(@RequestParam(value="fileUrl",required=false) String fileUrl,
												 @RequestParam(value="flag",required=false,defaultValue="false") boolean flag,
												 @RequestParam(value="fileType",required=false) String fileType){
		//如果参数为空，则获取当前服务器的根目录
		Subject subject = SecurityUtils.getSubject();
		String userName = subject.getPrincipal().toString();
		//如果未登录，直接返回空
		if (StringUtils.isBlank(userName)) {
			return null;
		}else {
			return systemService.getModelFileList(fileUrl,flag);
		}
	}
	
	/**
	 * 获取目标路径下文件
	 * @param fileUrl
	 * @return
	 */
	@ApiOperation(value="显示目标目录下文件或目录",notes="显示目标目录下文件或目录")
	@ApiImplicitParam(name="fileUrl",value="文件url",required=true,paramType="query")
	@GetMapping("/listfile")
	@ResponseBody
	public ReponseEntity<List<UploadFile>> getCurrentFiles(@RequestParam(name="fileUrl") String fileUrl){
		List<UploadFile> list = new ArrayList<UploadFile>();
		File directoryFile = new File(fileUrl);
		if (directoryFile.exists() && directoryFile.isDirectory()) {
			for (File file : directoryFile.listFiles()) {
				UploadFile uploadFile = new UploadFile();
				uploadFile.setFileName(file.getName());
				uploadFile.setFileType(file.isDirectory() ? 1 : 0);
				if (file.isDirectory()) {
					uploadFile.setFileUrl(file.getAbsolutePath());
				}else {
					uploadFile.setFileSize(file.length() / 1024);
				}
				list.add(uploadFile);
			}
		}
		return new ReponseEntity<List<UploadFile>>(ReponseStatus.SUCCESS.getCode(),
												   ReponseStatus.SUCCESS.getResult(),
												   list);
	}
	
	
	/**
	 * 保存系统默认密码
	 * @param passWord 默认密码
	 * @return
	 */
	@ApiOperation(value="保存系统用户密码",notes="保存系统用户密码")
	@ApiImplicitParam(name="passWord",value="密码",required=true,paramType="path")
	@RequestMapping(value="/savepassword/{passWord}",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<String> saveSystemPassword(@PathVariable String passWord){
		systemService.updateSystemPass(passWord);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "保存成功!");
	}
	
	/**
	 * 保存提供商信息
	 * @param passWord 默认密码
	 * @return
	 */
	@ApiOperation(value="保存提供商信息",notes="保存提供商信息")
	@ApiImplicitParam(name="serverProvider",value="供应商信息",required=true,paramType="body")
	@RequestMapping(value="/saveserverprovider",method=RequestMethod.POST)
	@ResponseBody
	public ReponseEntity<String> saveServerProvider(HttpServletRequest req,@RequestBody ServerProvider serverProvider){
		systemService.saveServerProvider(serverProvider);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "保存成功!");
	}
	
	/**
	 * 查询提供商信息
	 * @param passWord 默认密码
	 * @return
	 */
	@ApiOperation(value="获取提供商信息",notes="获取提供商信息")
	@RequestMapping(value="/queryserverprovider",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<ServerProvider> queryServerProvider(){
		ServerProvider provider = systemService.queryServerProvider();
		return new ReponseEntity<ServerProvider>(ReponseStatus.SUCCESS.getCode(),
												 ReponseStatus.SUCCESS.getResult(),
												 provider);
	}
	
	/**
	 * 获取当前缓存位置信息
	 * @return
	 */
	@ApiOperation(value="获取缓存位置信息",notes="获取缓存位置信息")
	@RequestMapping(value="/analysis/output",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<String> querySpatialAnalysisPath(){
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
										 ReponseStatus.SUCCESS.getResult(),
										 systemService.querySysTileCachePath());
	}
	
	/**
	 * 更新用户默认密码
	 * @param userName
	 * @return
	 */
	@ApiOperation(value="更新用户默认用户名和密码",notes="更新用户默认用户名和密码")
	@RequestMapping(value="/updateUsernameAndPass",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<String> modifyUsernameAndPass(HttpServletRequest req){
		systemService.updateUsernameAndPassWord();
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),ReponseStatus.SUCCESS.getResult());
	}
	
	/**
	 * 更新用户默认密码
	 * @param userName
	 * @return
	 */
	@ApiOperation(value="更新用户默认密码",notes="更新用户默认密码")
	@RequestMapping(value="/updateUserPass",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<String> updateUserPassWord(){
		systemService.updateUserPassWord();
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult());
	}
	
	/**
	 * 获取所有省的信息
	 */
	@ApiOperation(value="获取全国各省信息",notes="获取全国各省信息")
	@RequestMapping(value="/queryProvince",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<List<ProvinceInfo>> queryProvinceInfo(){
		List<ProvinceInfo> provinces = systemService.queryChinaProvince();
		return new ReponseEntity<List<ProvinceInfo>>(ReponseStatus.SUCCESS.getCode(),
													 ReponseStatus.SUCCESS.getResult(),
													 provinces);
	}
	
	
	/**
	 * 根据省id获取市信息
	 */
	@ApiOperation(value="获取市信息",notes="根据省id获取市信息")
	@ApiImplicitParam(name="provinceCode",value="省id",required=true,paramType="path")
	@RequestMapping(value="/querycityinfo/{provinceCode}",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<List<CityInfo>> queryProvinceInfo(@PathVariable String provinceCode){
		List<CityInfo> cityInfos = systemService.queryCityInfoByProvinceCode(provinceCode);
		return new ReponseEntity<List<CityInfo>>(ReponseStatus.SUCCESS.getCode(),
												 ReponseStatus.SUCCESS.getResult(),
												 cityInfos);
	}
	
	/**
	 * 文件(包括文件夹)上传
	 *
	 * @param files 文件内容
	 * @param currentUrl 当前url
	 * @return
	 */
	@ApiOperation(value="上传文件",notes="上传文件")
	@ApiImplicitParams({
		@ApiImplicitParam(name="files",value="文件",required=true,paramType="query"),
		@ApiImplicitParam(name="currentUrl",value="文件上传路径",required=false,paramType="query")
	})
	@PostMapping("/upload")
	@ResponseBody
	@Deprecated
	public ReponseEntity<String> uploadFile(@RequestParam("file") MultipartFile[] files,
											@RequestParam(name="currentUrl",required=false) String currentUrl){
		//获取当前的用户的文件对象
		if (files == null || files.length == 0) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(), "上传文件为空！");
		}
 		String userName = UserUtil.getUserName();
		//获取当前所在的文件目录
		String destFilePath = SystemServiceImpl.cacheUrl.get("CACHE_FILE_URL") + File.separator + userName;
		boolean destPathIsDirectory = false;
		try {
			if (StringUtils.isNotBlank(currentUrl)
				&& !currentUrl.endsWith("ShareData")
				&& !currentUrl.startsWith(SystemServiceImpl.cacheUrl.get("CACHE_FILE_URL") + File.separator + "示例数据")) {
				currentUrl = URLDecoder.decode(currentUrl, "UTF-8");
				destFilePath = currentUrl;
				destPathIsDirectory = true;
			}
		} catch (UnsupportedEncodingException e) {
			log.error("ConfigController uploadFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(),currentUrl + "编码失败");	
		}
		List<UploadFile> uploadFiles = new ArrayList<UploadFile>();
		//标识当前长传的文件是文件夹还是文件
		boolean isFile = true;
		//如果用户当前选择在某个文件夹下上传，则当前上传的文件只更新当前文件夹对应的大小即可
		StringBuilder sb = new StringBuilder(destFilePath);
		long directorySize = 0;
		String tempPath = null;
		for (MultipartFile multipartFile : files) {
			//遍历每一个上传文件的路径，判断其是否为内套文件夹
			CommonsMultipartFile fileItem = (CommonsMultipartFile) multipartFile;
			String sourceName = fileItem.getFileItem().getName();
			UploadFile uploadFileBase = new UploadFile(new Date(),new Date(),userName);
			//使用"/"判断当前是否为文件夹
			if (sourceName.indexOf("/") > -1 && isFile) {
				isFile = false;
				//存储文件夹信息
				uploadFileBase.setFileName(sourceName.split("/")[0]);
				tempPath = sb.append(File.separator).toString();
				uploadFileBase.setFileUrl(sb.append(sourceName.split("/")[0]).toString());
				uploadFileBase.setFileType(1);
				uploadFiles.add(uploadFileBase);
			}
			String fileName = multipartFile.getOriginalFilename();
			if (isFile) {
				//如果是文件，如果上传的文件夹重复，则需要处理 TODO:
				String renameFile = getNewFileName(renameFile(destFilePath,fileName));
				fileName = StringUtils.isBlank(renameFile) ? fileName : renameFile;
			}
			File destFile = new File(StringUtils.isBlank(tempPath) ? destFilePath : tempPath, isFile ? fileName : sourceName);
			//如果文件夹不存在,则创建
			if (!destFile.getParentFile().exists()) {
				destFile.getParentFile().mkdirs();
			}
			try {
				directorySize += multipartFile.getSize();
				//如果当前是上传的是文件，并且目标路径不是文件夹，则需要记录其每一个上传文件的信息
				if (isFile && !destPathIsDirectory) {
					uploadFileBase.setFileName(fileName); 
					uploadFileBase.setFileSize(multipartFile.getSize());
					uploadFiles.add(uploadFileBase);
				}
				multipartFile.transferTo(destFile);
			} catch (IllegalStateException | IOException e) {
				log.error("ConfigController uploadFile has error:" + ExceptionsUtil.stackTraceToStr(e));
				return new ReponseEntity<String>(ExceptionsUtil.ERROR_400,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_UPLOAD, fileName));
			}
		}
		//如果目标路径是文件夹，则根性其数据库中的文件大小
		if (destPathIsDirectory) {
			UploadFile uploadFileBase = new UploadFile();
			uploadFileBase.setFileSize(directorySize);
			uploadFileBase.setFileName(new File(destFilePath).getName());
			systemService.updateFileSize(uploadFileBase);
		}else {
			if (!isFile) {
				uploadFiles.get(0).setFileSize(directorySize);
			}
			systemService.uploadFile(uploadFiles);
		}
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "上传成功！");
	}
		
	/**
	 * 查询系统剩余使用天数
	 * @return
	 */
	@ApiOperation(value="查询系统剩余使用天数",notes="查询系统剩余使用天数")
	@GetMapping(value="/remainDays")
	@ResponseBody
	public ReponseEntity<?> queryRemainingDays(){
		Object[] result = new Object [2];
		if (versionFlag) {
			result[0] = "永久使用";
		}else {
			result = systemService.queryRemainingDays();
		}
		return new ReponseEntity<Object[]>(ReponseStatus.SUCCESS.getCode(), 
													   ReponseStatus.SUCCESS.getResult(), 
													   result);
	}
	
	/**
	 * 如果当前上传的文件夹已有同名文件，则需要重命名文件
	 * 
	 * @param destPath  目标文件
	 * @param fileName  文件名称
	 */
	private String renameFile(String destPath,String fileName) {
		File destFile = new File(destPath);
		File[] fileArray = destFile.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (getBaseName(name).equals(fileName)) {
					return true;
				}else {
					return false;
				}
			}
		});
		
		if (fileArray != null && fileArray.length > 1) {
			Collections.sort(Arrays.asList(fileArray), new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return o2.getName().compareTo(o1.getName());
				}
			});
		}
		
		return (fileArray == null || fileArray.length == 0) ? "" : fileArray.length == 1 ? fileArray[0].getName() : fileArray[1].getName();
	}
	
	private String getBaseName(String fileName) {
		return fileName.replaceAll("\\(\\d{1,5}\\)", "");
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
				Pattern pattern1 = Pattern.compile("\\d");
				Matcher matcher1 = pattern1.matcher(str);
				if (matcher1.find()) {
					String newNO = matcher1.replaceAll(Integer.parseInt(matcher1.group(0)) + 1 + "");
					return matcher.replaceAll(newNO);
				}
			}else {
				if (fileName.indexOf(".") > -1) {
					return fileName.split("\\.")[0] +"(1)." + fileName.split("\\.")[1];
				}else {
					return fileName+"(1)";
				}
			}
		}
		return "";
	}
	
	
	/**
	 * 获取当前文件上传的进度信息
	 * @param req
	 * @return
	 */
	@ApiOperation(value="查询上传文件进度",notes="查询上传文件进度")
	@GetMapping("/progress")
	@ResponseBody
	public ReponseEntity<Progress> getProgressStatus(HttpServletRequest req){
		Progress progress = (Progress) req.getAttribute("progress");
		return new ReponseEntity<Progress>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),progress);
	}
	
	/**
	 * 删除资源文件
	 * @param fileName 文件名称
	 * @return
	 */
	@ApiOperation(value="删除资源文件",notes="删除资源文件")
	@ApiImplicitParam(name="fileName",value="文件名称",required=true,paramType="query")
	@GetMapping("/delete")
	@ResponseBody
	public ReponseEntity<String> deleteUserFile(@RequestParam(name="fileName") String fileName){
		Subject subject = SecurityUtils.getSubject();
		String userName = subject.getPrincipal().toString();
		String destFilePath = SystemServiceImpl.cacheUrl.get("CACHE_FILE_URL") + File.separator + userName;
		systemService.deleteFile(destFilePath, fileName);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "文件删除成功");
	}

	/**
	 * Get upload file info reponse entity.
	 *
	 * @return the reponse entity
	 */
	@ApiOperation(value="查询资源文件",notes="查询资源文件")
	@GetMapping("/resource")
	@ResponseBody
	@Deprecated
	public ReponseEntity<List<UploadFile>> getUploadFileInfo(){
		Subject subject = SecurityUtils.getSubject();
		String userName = subject.getPrincipal().toString();
		//首先查询数量
		int count = systemService.getUploadFileCount(userName);
		UploadFile uploadFile = new UploadFile("示例数据", SystemServiceImpl.cacheUrl.get("CACHE_FILE_URL") + File.separator + "示例数据", 1);
		if (count > 0) {
			LinkedList<UploadFile> list = systemService.getUploadFile(userName);
			list.addFirst(uploadFile);
			return new ReponseEntity<List<UploadFile>>(ReponseStatus.SUCCESS.getCode(),
													   ReponseStatus.SUCCESS.getResult(),
													   list,
													   count);
		}else {
			LinkedList<UploadFile> list = new LinkedList<UploadFile>();
			list.addFirst(uploadFile);
			return new ReponseEntity<List<UploadFile>>(ReponseStatus.SUCCESS.getCode(), 
													   ReponseStatus.SUCCESS.getResult(),
													   list);
		}
	}
	
	/**
	 * 保存自动发布配置信息
	 * @param autoConfig
	 * @return
	 */
	@ApiOperation(value="保存自动发布配置",notes="保存自动发布配置")
	@ApiImplicitParam(name="autoConfig",value="自动发布配置",required=true,paramType="body")
	@PostMapping("/save/auto/config")
	@ResponseBody
	public ReponseEntity<String> saveServerAutoConfig(HttpServletRequest req,@RequestBody ServerAutoConfig autoConfig){
		systemService.saveServerAutoConfig(autoConfig);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "配置信息更新成功！");
	}
	
	/**
	 * 获取自动发布配置
	 * @return
	 */
	@ApiOperation(value="查询自动发布配置",notes="查询自动发布配置")
	@GetMapping("/query/auto/config")
	@ResponseBody
	public ReponseEntity<ServerAutoConfig> queryServerAutoConfig(){
		return new ReponseEntity<ServerAutoConfig>(ReponseStatus.SUCCESS.getCode(), "获取成功！",systemService.queryAutoConfig());
	}
	
	/**
	 * 修改文件系统配置
	 * @param fileServer
	 * @return
	 */
	@ApiOperation(value="修改文件系统配置",notes="修改文件系统配置")
	@ApiImplicitParams({
		@ApiImplicitParam(name="fileServer",value="文件服务器",required=true,paramType="query"),
		@ApiImplicitParam(name="autoPath",value="发布服务路径",required=true,paramType="query")
	})
	@GetMapping("/update/system")
	@ResponseBody
	public ReponseEntity<String> updateSystemConfig(HttpServletRequest req,
													@RequestParam(value="fileServer") String fileServer,
													@RequestParam(value="autoPath") String autoPath){
		try {
			fileServer = URLDecoder.decode(fileServer, "UTF-8");
			autoPath = URLDecoder.decode(autoPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("ConfigController updateSystemConfig has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_400,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECODE, ""));
		}
		systemService.updateSystemConfig(fileServer,autoPath);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "更新成功！");
	}
	
	/**
	 * 查询文件系统路径
	 * @return
	 */
	@ApiOperation(value="查询文件系统路径",notes="查询文件系统路径")
	@GetMapping("/query/fileserver/path")
	@ResponseBody
	public ReponseEntity<String> querySystemConfig(){
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "获取系统路径成功！",systemService.queryFileServerPath());
	}
	
	/**
	 * 查询系统用户是否已经存在
	 * @return
	 */
	@ApiOperation(value="查询系统用户是否已经存在",notes="查询系统用户是否已经存在")
	@GetMapping("/query/system/user")
	@ResponseBody
	public ReponseEntity<Boolean> querySystemUser(){
		String systemUser = userService.querySystemUser();
		return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),"查询成功！",StringUtils.isNotBlank(systemUser));
	}
	
	/**
	 * 安装向导数据存储
	 * @return
	 */
	@ApiOperation(value="保存安装向导数据",notes="保存安装向导数据")
	@ApiImplicitParams({
		@ApiImplicitParam(name="fileServer",value="文件服务器",required=true,paramType="query"),
		@ApiImplicitParam(name="userName",value="系统用户名",required=true,paramType="path"),
		@ApiImplicitParam(name="password",value="系统用户密码",required=true,paramType="path")
	})
	@GetMapping("/save/init/{userName}/{password}")
	@ResponseBody
	public ReponseEntity<String> saveSystemConfig(@RequestParam("fileServer") String fileServer,
												  @PathVariable("userName") String userName,
												  @PathVariable("password") String password){
		if (!UserUtil.checkUserName(userName)) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(),"用户名不能包含特殊字符");
		}
		if (!UserUtil.checkUserPasswd(password)) {
			return new ReponseEntity<String>(ReponseStatus.ERROR.getCode(),"用户密码包含不合法特殊字符");
		}
		systemService.updateSystemFileServer(fileServer,userName,PostGisStoreUtils.getDecodePass(password));
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), "保存成功");
	}
	
	/**
	 * 文件路径校验
	 * @return
	 */
	@ApiOperation(value="校验文件路径",notes="校验文件路径")
	@ApiImplicitParam(name="filePath",value="文件路径",required=true,paramType="query")
	@GetMapping("/check/path")
	@ResponseBody
	public ReponseEntity<Boolean> checkFilePath(@RequestParam("filePath") String filePath){
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("window") > -1) {
			if ("localhost".equalsIgnoreCase(filePath)) {
				return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),ReponseStatus.SUCCESS.getResult(),true);
			}
			File file = new File(filePath);
			return new ReponseEntity<Boolean>(ReponseStatus.SUCCESS.getCode(),ReponseStatus.SUCCESS.getResult(),file.exists());
		} else {
			//TODO 处理linux的共享文件需要
			return null;
		}
	}
	
	/**
	 * 获取目录目录下的地图文件或模型文件信息
	 * @param dirpath
	 * @return
	 */
	@ApiOperation(value="查询目标目录下文件",notes="查询目标目录下文件")
	@ApiImplicitParam(name="dirpath",value="目录路径",required=true,paramType="query")
	@GetMapping("/list/batchPublishFiles")
	@ResponseBody
	public ReponseEntity<List<BatchPublishEntity>> listCurrentDirUsedFile(@RequestParam(value="dirpath") String dirpath, @RequestParam(value="serverType") String serverType){
		List<BatchPublishEntity> batchEntities = null;
		if(dirpath == null) {
			return null;
		}
		dirpath = MapServerUtils.getUTF8FilePath(dirpath);
		File file = new File(dirpath);
		if (file.isFile()) {
			batchEntities = systemService.getServerFileList(dirpath,false,serverType);
		} else {
			batchEntities = systemService.getServerFileList(dirpath,true,serverType);
		}
		return new ReponseEntity<List<BatchPublishEntity>>(ReponseStatus.SUCCESS.getCode(), "获取成功！",batchEntities);
	}
	
	/**
	 * 获取当前切片存储的空间剩余
	 * @return
	 */
	@ApiOperation(value="获取剩余磁盘空间",notes="获取剩余磁盘空间")
	@GetMapping("/pygis/totalspace")
	@ResponseBody
	public ReponseEntity<String> getCurrentFileSize(@RequestParam String tilePath){
		//如果路径不为空则获取该路径所在磁盘的可使用空间，否则执行原有业务逻辑
		if (StringUtils.isNoneBlank(tilePath)) {
			File file = new File(tilePath);
			long usableSpace = 0;
			if (file.exists()) {
				usableSpace = file.getFreeSpace()/1024/1024;
			} else {
				usableSpace = file.getParentFile().getFreeSpace()/1024/1024;
			}
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
					 ReponseStatus.SUCCESS.getResult(),
					 usableSpace + "MB");
		}
		String OS = System.getProperty("os.name").toLowerCase();
		String targetPath;
		SeedTaskParam seedTaskParam =  tileCacheService.queryGlobalSeedParam();
		String path = seedTaskParam.getStorePath();
		if (StringUtils.isNoneBlank(path) && seedTaskParam.getStoreType() != StoreType.TMS.getTypeId()) {
			targetPath = path;
		} else {
			targetPath = ConfigController.class.getClassLoader().getResource("").getPath();
			if (targetPath.contains("mapserver.jar!")) {
				targetPath = targetPath.substring(5, targetPath.indexOf("mapserver.jar!"));
			}
			if (targetPath.startsWith("/")) {
				targetPath = targetPath.replaceFirst("\\/", "");
			}
		}
		if (OS.indexOf("window") > -1) {
			targetPath.split("\\/");
			File file = new File(targetPath.split("\\/")[0]);
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
											 ReponseStatus.SUCCESS.getResult(),
											 file.getFreeSpace() / 1024 / 1024 + "MB");
		} else if (OS.indexOf("linux") > -1) {
			File rootPath = new File("/");
			long usableSpace = rootPath.getUsableSpace()/1024/1024;
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
					 ReponseStatus.SUCCESS.getResult(),
					 usableSpace + "MB");
		}
		
		return null;
	}
	
	/**
	 * 查看当前系统的使用期是否已到
	 * @return
	 */
	@ApiOperation(value="查看当前系统是否已经到期",notes="查询当前系统是否已经到期")
	@GetMapping("/check/systime")
	@ResponseBody
	public ReponseEntity<String> sysCurrentRemainTime(HttpServletRequest req){
		String reqIP = UserUtil.getRequestIp(req);
		//此处用于屏蔽系统授权功能，用于产品交付打包时，发版本状态下系统授权功能需要开启
		this.versionFlag = true;
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),"系统可正常使用");
//		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),systemService.checkSysTime(reqIP));
	}
	
	/**
	 * 激活当前的系统
	 * @param activecode 激活码
	 * @return
	 */
	@ApiOperation(value="激活当前系统",notes="激活当前系统")
	@ApiImplicitParam(name="activecode",value="系统激活码",required=true,paramType="path")
	@GetMapping("/active/systime/{activecode}")
	@ResponseBody
	public ReponseEntity<String> activeSysTime(HttpServletRequest req,@PathVariable("activecode") String activecode){
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),systemService.activeSysTime(activecode));
	}
	
	/**
	 * 查询当前系统Mac码信息
	 */
	@ApiOperation(value="查询当前系统Mac码信息",notes="查询当前系统Mac码信息")
	@GetMapping("/query/sysmac")
	@ResponseBody
	public ReponseEntity<String> querySysMac(){
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),systemService.querySysMac());
	}
	
	/**
	 * 下载某个资源文件
	 *
	 * @param resourceName  资源文件
	 * @param name 文件名称
	 */
	@ApiOperation(value="下载资源文件",notes="下载资源文件")
	@ApiImplicitParams({
		@ApiImplicitParam(name="resourceName",value="资源文件名",required=true,paramType="path"),
		@ApiImplicitParam(name="name",value="下载文件名",required=true,paramType="path")
	})
	@GetMapping("/resources/{resourceName}/{fileName}")
	public void downLoadSysResource(@PathVariable(name="resourceName") String resourceName,
													 @PathVariable(name="fileName") String name,
													 HttpServletRequest req,
													 HttpServletResponse rep) {
		//根据资源名称获取资源路径，然后再读取资源文件信息
		String path = null;
		switch (resourceName) {
		case "color":
			path = System.getProperty("SYSTEM_COLOR_PATH");
			break;
		case "thumbal":
			path = System.getProperty("SYSTEM_THUMBAL_PATH");
			break;
		case "style":
			path = System.getProperty("SYSTEM_STYLE_PATH");
			break;
		case "samples":
			path = System.getProperty("SYSTEM_SAMPLES_PATH");
			break;
		default:
			break;
		}
		String fileName = name + ".png";
		OutputStream os = null;
		try {
			os = rep.getOutputStream();
			//读取文件
			byte[] buffer = FileUtil.getFileInfo(path + File.separator + fileName);
			os.write(buffer);
			os.flush();
//			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult());
		} catch (IOException e) {
			log.error("{}下载失败，ConfigController downLoadSysResource has error: {}",name,ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, fileName));
		} finally {
			FileUtil.closeStream(os);
		}
	}
	
	@ApiImplicitParam(name="logPath",value="文件url",required=false,paramType="query")
	@RequestMapping(value="/resetLogbackInfo",method=RequestMethod.POST)
	@ResponseBody
	public ReponseEntity<String> resetLogbackInfo(@RequestBody UserLogbackInfo logInfo, HttpServletRequest req){
		//如果参数为空，则获取当前服务器的根目录
		String reqIP = UserUtil.getRequestIp(req);
		systemService.resetLogbackInfo(logInfo,reqIP);
		return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),ReponseStatus.SUCCESS.getResult(),"日志存储信息更新成功！");
	}
	
	@ApiOperation(value="文件列表",notes="地图服务文件列表获取")
	@RequestMapping(value="/queryLogbackInfo",method=RequestMethod.GET)
	@ResponseBody
	public ReponseEntity<UserLogbackInfo> queryLogbackInfo(){
		//将返回的logback信息的xml文件流置为null，防止信息流向前端
		UserLogbackInfo logInfo = systemService.queryLogbackInfo();
		File file = new File(logInfo.getCurLogPath());
		logInfo.setCurLogPath(file.getPath());
		logInfo.setLogbackDetail(null);
		return new ReponseEntity<UserLogbackInfo>(ReponseStatus.SUCCESS.getCode(),ReponseStatus.SUCCESS.getResult(),logInfo);
	}
	
	/**
	 * 服务缓存配置信息查询（内存缓存、访问缓存）
	 * @return
	 */
	@ApiOperation(value="查询缓存配置",notes="查询缓存配置")
	@GetMapping("/queryCacheConfig")
	@ResponseBody
	public ReponseEntity<ServerCacheConfigEntity> queryCacheConfig(HttpServletRequest req){
		String reqIP = UserUtil.getRequestIp(req);
		return new ReponseEntity<ServerCacheConfigEntity>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), systemService.queryCacheConfig(reqIP));
	}
	
	/**
	 * 查询新配置目录磁盘空间与原有目录使用磁盘空间大小
	 * @return
	 */
	@ApiOperation(value="查询新配置目录磁盘空间与原有目录使用磁盘空间大小",notes="查询新配置目录磁盘空间与原有目录使用磁盘空间大小")
	@GetMapping("/queryCacheDirSize")
	@ResponseBody
	public ReponseEntity<Map<String,Integer>> queryCacheDirSize(HttpServletRequest req,@RequestParam String newPath){
		String reqIP = UserUtil.getRequestIp(req);
		return new ReponseEntity<Map<String,Integer>>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), systemService.queryCacheDirSize(newPath,reqIP));
	}
	
	/**
	 * 用户更新服务缓存配置（内存缓存、访问缓存）
	 * @param configEntity
	 * @return
	 */
	@ApiOperation(value="用户更新服务缓存配置",notes="用户更新服务缓存配置")
	@PostMapping("/updateCacheConfig/{isMoveCacheFile}")
	@ResponseBody
	public ReponseEntity<String> updateCacheConfig(HttpServletRequest request,@RequestBody ServerCacheConfigEntity configEntity,@PathVariable boolean isMoveCacheFile){
		String reqIP = UserUtil.getRequestIp(request);
		systemService.updateCacheConfig(configEntity,reqIP,isMoveCacheFile);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), "服务缓存配置更新成功！");
	}

	/**
	 * PostGIS连接测试
	 *
	 * @param connectEntity
	 * @return
	 */
	@ApiOperation(value = "检测PostGIS是否可正常连接",notes="检测PostGIS是否可正常连接")
	@PostMapping("/dataServer/checkPostGis")
	public ReponseEntity<Map<String, Object>> checkPostGisConnect(
			HttpServletRequest request,
			@RequestBody @ApiParam(value="PostGIS连接信息",type="postGisConnectEntity",name="connectEntity") PostGisConnectEntity connectEntity){
		Map<String, Object> result = systemService.checkPostGisConnect(connectEntity);
		if ((boolean)result.get("isOk") == true) {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),result);
		} else if (result.containsKey("duplicateServer")) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "数据源重复，同一个数据源只能添加一次");
		} else if (result.containsKey("数据源名称已存在")) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "数据源名称已存在");
		} else {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "数据源连接失败，请检查参数是否正确");
		}
	}

	/**
	 * PostGIS编辑
	 *
	 * @param connectEntity
	 * @return
	 */
	@ApiOperation(value = "编辑PostGIS",notes="编辑PostGIS")
	@PostMapping("/dataServer/editPostGis")
	public ReponseEntity<String> editPostGis(HttpServletRequest request,
											 @RequestBody @ApiParam(value="PostGIS信息",type="postGisConnectEntity",name="connectEntity") PostGisConnectEntity connectEntity){
//		if (!UserUtil.checkUserName(connectEntity.getDataSourceName())) {
//			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(),"数据源名称不能包含特殊字符");
//		}
		String result = systemService.editPostGis(connectEntity);
		if(result.equals("编辑成功")) {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),result);
		} else {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), result);
		}

	}

	/**
	 * 根据数据源ID查询PostGIS信息
	 *
	 * @param entityId
	 * @return
	 */
	@ApiOperation(value = "根据数据源ID查询PostGIS信息",notes="根据数据源ID查询PostGIS信息")
	@GetMapping("/dataServer/querySourceEntityPostGis/{entityId}")
	public ReponseEntity<PostGisConnectEntity> queryPostGisById(@PathVariable int entityId){
		PostGisConnectEntity entity = systemService.queryPostGisById(entityId).get(0);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),entity);
	}

	/**
	 * 根据数据源名称查询其下可以发布的PostGIS表信息
	 *
	 * @param sourceName
	 * @return
	 */
	@ApiOperation(value = "根据数据源名称查询其下可以发布的PostGIS表信息",notes="根据数据源名称查询其下可以发布的PostGIS表信息")
	@GetMapping("/dataServer/queryPostGisTables/{sourceName}")
	public ReponseEntity<Map<String, String>> queryPostGisByName(@PathVariable String sourceName){
		Map<String, String> entity = systemService.queryPostGisInfos(sourceName);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),entity);
	}

	/**
	 * 查询所有PostGIS数据源信息
	 *
	 * @return
	 */
	@ApiOperation(value = "查询所有PostGIS数据源信息",notes="查询所有PostGIS数据源信息")
	@GetMapping("/dataServer/queryPostGis")
	public ReponseEntity<List<PostGisConnectEntity>> queryPostGis(){
		List<PostGisConnectEntity> entity = systemService.queryPostGisInfos();
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),entity);
	}

	/**
	 * 确认PostGIS数据源名称是否重复
	 *
	 * @param sourceName
	 * @return
	 */
	@ApiOperation(value = "确认PostGIS数据源名称是否重复",notes="确认PostGIS数据源名称是否重复")
	@GetMapping("/dataServer/checkPostGisSourceName/{sourceName}")
	public ReponseEntity<String> checkPostGisSourceName(@PathVariable String sourceName){
		PostGisConnectEntity entity = systemService.queryPostGis(sourceName);
		if (entity == null) {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),"不存在同名数据源");
		} else {
			return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),"已存在同名数据源");
		}
	}

	/**
	 * PostGIS信息删除，支持单个删除或批量删除
	 *
	 * @param ids
	 * @return
	 */
	@ApiOperation(value = "删除PostGIS信息",notes="删除PostGIS信息")
	@ApiImplicitParam(name="ids",value="PostGIS ids",required=true,paramType="body")
	@PostMapping("/dataServer/deletePostGis")
	public ReponseEntity<String> deletePostgisInfo(HttpServletRequest request,@RequestBody @ApiParam(value="PostGIS id",type="list",name="ids") List<Integer> ids){
		if (ids.size() == 0) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(),	"请求参数不能为空");
		}
		String reqIP = UserUtil.getRequestIp(request);
		systemService.deletePostgisInfo(ids,reqIP);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),"PostGis信息删除成功");
	}

	/**
	 * 根据postgis配置的数据源查询与之相对应的已经发布的服务名称
	 *
	 * @param sourceNames
	 * @return
	 */
	@ApiOperation(value = "查询postgis名称",notes="根据postgis配置的数据源查询与之相对应的已经发布的服务名称")
	@ApiImplicitParam(name="sourceNames",value="postgis名称",required=true,paramType="body")
	@PostMapping("/dataServer/queryPostGisSourceNames")
	public ReponseEntity<Map<String,List<String>>> queryServerNameBySourceName(@RequestBody List<String> sourceNames){
		if (sourceNames.size() == 0) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "请求参数不能为空");
		}
		Map<String,List<String>> serverNames = systemService.queryServerNameBySourceName(sourceNames);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),serverNames);
	}

	/**
	 * 查询文件大小
	 *
	 * @param fileUrl
	 * @param req
	 * @return
	 */
	@ApiOperation(value = "获取文件大小",notes="获取文件大小")
	@GetMapping("/dataServer/queryFileSize")
	public ReponseEntity<String> queryFileSize(@RequestParam(value="fileUrl") String fileUrl, HttpServletRequest req){
		if (StringUtils.isBlank(fileUrl)) {
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "请求参数不能为空");
		}
		File file = new File(fileUrl);
		if (!file.exists()){
			return new ReponseEntity<>(ReponseStatus.ERROR.getCode(), "文件路径指定文件不存在!");
		}
		String fileSize =String.format("%.2f", file.length() / 1024.0 / 1024.0 / 1024.0);
		return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(), fileSize);
	}
	
	/**
	 * 查看功能页面
	 *
	 * @param fileUrl
	 * @param req
	 * @return
	 */
	@ApiOperation(value = "查看功能页面",notes="查看功能页面")
	@GetMapping("/queryFunctionPage")
	public ReponseEntity<List<FunctionPageEntity>> queryFunctionPage(){
		return new ReponseEntity<List<FunctionPageEntity>>(ReponseStatus.SUCCESS.getCode(), 
				 ReponseStatus.SUCCESS.getResult(), 
				 systemService.queryFunctionPageEntity());
	}
}
