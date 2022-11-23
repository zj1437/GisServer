package org.pygisserver.web.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.entity.UploadFile;
import org.pygisserver.common.entity.UserLogbackInfo;
import org.pygisserver.web.entity.BatchPublishEntity;
import org.pygisserver.web.entity.CityInfo;
import org.pygisserver.common.entity.FunctionPageEntity;
import org.pygisserver.web.entity.ProvinceInfo;
import org.pygisserver.web.entity.ServerAutoConfig;
import org.pygisserver.web.entity.ServerCacheConfigEntity;
import org.pygisserver.web.entity.ServerProvider;

/**
 * @Description: 系统相关操作service
 * @author:  
 * @date: 2018-11-19
 */
public interface SystemService {

	/**
	 * 修改系统默认密码
	 * @param password
	 */
	void updateSystemPass(String password);
	
	/**
	 * 重置系统用户密码
	 */
	void updateUserPassWord();
	
	/**
	 * 重置系统用户名和密码
	 */
	void updateUsernameAndPassWord();
	
	/**
	 * 
	 * @param serverProvider
	 */
	void saveServerProvider(ServerProvider serverProvider);
	
	/**
	 * 获取当前系统缓存路径
	 * @return
	 */
	String querySysTileCachePath();
	
	/**
	 * 获取当前服务提供商
	 * @return
	 */
	ServerProvider queryServerProvider();
	
	
	List<ProvinceInfo> queryChinaProvince();
	
	/**
	 * 根据省份code获取城市信息
	 * @return
	 */
	List<CityInfo> queryCityInfoByProvinceCode(String provinceCode);
	
	/**
	 * 文件上传
	 * @param uploadFiles
	 */
	void uploadFile(List<UploadFile> uploadFiles);
	
	/**
	 * 更新文件大小
	 * @param uploadFile
	 */
	void updateFileSize(UploadFile uploadFile);
	
	/**
	 * 获取上传文件的信息
	 * @return
	 */
	LinkedList<UploadFile> getUploadFile(String uploador);
	
	/**
	 * 
	 * @param uploador
	 * @return
	 */
	int getUploadFileCount(String uploador);
	
	/**
	 * 删除对应的文件
	 * @param fileUrl 文件url
	 * @param fileName 删除文件的名称
	 */
	void deleteFile(String fileUrl,String fileName);
	
	/**
	 * 获取管理员的文件list
	 * @param fileUrl 文件url
	 * @param type 筛选文件类型
	 * @return
	 */
	Map<String, Integer> getManagerFileList(String fileUrl, String type);
	
	/**
	 * 获取普通人员的文件list
	 * @param fileUrl 文件url
	 * @param type 筛选文件类型
	 * @return
	 */
	Map<String, Integer> getOrdinaryFileList(String fileUrl,String userName,String type);
	
	/**
	 * 获取模型文件list
	 * @param fileUrl 文件url
	 * @param type 筛选文件类型
	 * @return
	 */
	Map<String, Integer> getModelFileList(String fileUrl,boolean flag);
	
	/**
	 * 查询能够批量发布的服务数据
	 * @param fileUrl
	 * @return
	 */
	List<BatchPublishEntity> getServerFileList(String fileUrl,boolean isDir,String serverType);
	
	/**
	 * 保存服务自动配置信息
	 * @param autoConfig
	 */
	void saveServerAutoConfig(ServerAutoConfig autoConfig);
	
	
	ServerAutoConfig queryAutoConfig();
	
	/**
	 * 更新系统路径
	 * @param fileServer
	 * @param autoConfig
	 */
	void updateSystemConfig(String fileServer,String autoPath);
	
	
	void updateSystemFileServer(String fileUrl,String userName,String password);
	
	
	String queryFileServerPath();
	
	/**
	 * 获取系统时间
	 * @return
	 */
	String checkSysTime(String reqIP);
	
	/**
	 * 更新系统时间
	 */
	void updateSysTime();
	
	/**
	 * 激活当前的系统
	 * @param activeCode
	 */
	String activeSysTime(String activeCode);
	
	/**
	 * 获取系统的mac机器码
	 * @return
	 */
	String querySysMac();
	
	/**
	 * 获取系统资源信息
	 * @return
	 */
	String querySysResourcePath(String resourceName);
	
	
	/**
	 * 查询系统剩余使用天数
	 * @return
	 */
	Object[] queryRemainingDays();
	
	void resetLogbackInfo(UserLogbackInfo logInfo,String reqIP);
	
	UserLogbackInfo queryLogbackInfo();
	
	void saveSysLog(SystemLogEntity sysLog);
	
	ServerCacheConfigEntity queryCacheConfig(String reqIP);
	
	Map<String,Integer> queryCacheDirSize(String newPath,String reqIP);
	
	void updateCacheConfig(ServerCacheConfigEntity configEntity,String reqIP,boolean isMoveCacheFile);

	/**
	 * 检查数据是否可以正常连接
	 * @param connectEntity
	 * @return
	 */
	Map<String, Object> checkPostGisConnect(PostGisConnectEntity connectEntity);

	/**
	 * 编辑PostgisInfo
	 * @param connectEntity
	 */
	String editPostGis(PostGisConnectEntity connectEntity);

	void updatePostGisStatus(int id,int status);

	/**
	 * 查询PostgisInfo
	 * @param id
	 */
	List<PostGisConnectEntity> queryPostGisById(int id);

	/**
	 * 查询PostgisInfo
	 * @param sourceName
	 */
	PostGisConnectEntity queryPostGis(String sourceName);

	/**
	 * 查询所有PostgisInfo
	 */
	Map<String, String> queryPostGisInfos(String sourceName);

	/**
	 * 查询所有PostgisInfo
	 */
	List<PostGisConnectEntity> queryPostGisInfos();

	/**
	 * 删除PostgisInfo
	 * @param ids
	 */
	void deletePostgisInfo(List<Integer> ids,String reqIP);

	/**
	 * 查询图形组名称
	 * @param sourceNames
	 */
	Map<String,List<String>> queryServerNameBySourceName(List<String> sourceNames);
	
	/**
	 * 查询功能页签
	 * 
	 */
	List<FunctionPageEntity> queryFunctionPageEntity();
}
