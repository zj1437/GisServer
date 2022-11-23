package org.pygisserver.web.dao;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.entity.UploadFile;
import org.pygisserver.web.entity.CityInfo;
import org.pygisserver.common.entity.FunctionPageEntity;
import org.pygisserver.web.entity.ProvinceInfo;
import org.pygisserver.web.entity.PublishedDataFormat;
import org.pygisserver.web.entity.ServerAutoConfig;
import org.pygisserver.web.entity.ServerCacheConfigEntity;
import org.pygisserver.web.entity.ServerProvider;
import org.pygisserver.web.entity.SystemConfigInfo;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigMapper {

	/**
	 * 更新系统默认密码
	 * @param password
	 */
	void updateSyetemPassword(String password);
	
	
	/**
	 * 更新缓存路径
	 * @param tilePath
	 */
	void updateSyetemTilePath(String tilePath);
	
	/**
	 * 获取系统默认信息
	 * @return
	 */
	SystemConfigInfo querySystemConfigInfo();
	
	/**
	 * 获取系统的缓存路径
	 * @return
	 */
	String querySystemCachePath();
	
	/**
	 * 保存服务提供商
	 * @param providerInfo
	 */
	void saveServerProvider(ServerProvider providerInfo);
	
	/**
	 * 获取默认服务提供商
	 * @return
	 */
	ServerProvider queryServerProvider();
	
	/**
	 * 获取省份信息
	 * @return
	 */
	List<ProvinceInfo> queryProvinceInfo();
	
	/**
	 * 获取对应省份的城市信息
	 * @param provinceCode
	 * @return
	 */
	List<CityInfo> queryCityInfo(String provinceCode);
	
	/**
	 * 保存上传文件信息
	 * 
	 * @param uploadFiles
	 */
	void saveUploadFiles(List<UploadFile> uploadFiles);
	
	/**
	 * 获取上传文件的数量
	 * @param uploador 
	 * @return
	 */
	int getUploadFileCount(String uploador);
	
	/**
	 * 获取上传文件的信息
	 * @return
	 */
	LinkedList<UploadFile> getUploadFiles(String uploador);
	
	
	/**
	 * 删除文件
	 * @param fileName
	 */
	void deleteFileByFileName(String fileName);
	
	
	/**
	 * 更新文件大小
	 * @param uploadFile
	 */
	void updateFileSize(UploadFile uploadFile);
	
	
	/**
	 * 删除用户对应的文件信息
	 * @param userNames 用户名称
	 */
	void deleteFileByUserName(List<String> userNames);
	
	void saveOrUpdateServerConfig(ServerAutoConfig serverConfig);
	
	void saveDataFormat(List<PublishedDataFormat> dataFormat);
	
	/**
	 * 查询系统自动检测配置
	 * @return
	 */
	ServerAutoConfig queryServerAutoConfig();
	
	/**
	 * 
	 * @param fileServer 
	 * @param uploadPath
	 */
	void updateSystemConfig(String fileServer);
	
	
	/**
	 * 更新系统用户账户信息
	 * @param systemUsername
	 */
	void updateSystemUser(String systemUsername);
	
	/**
	 * 更新自动发布路径
	 * @param autoPath
	 */
	void updateServerAutoConfig(String autoPath);
	
	void deleteDataFormat();
	
	/**
	 * 获取文件服务器路径
	 * @return
	 */
	String getFileServerPath();
	
	/**
	 * 获取最近使用时间
	 * @return
	 */
	String querySysTime();
	
	/**
	 * 更新系统时间
	 */
	void updateSysTime(String sysRemainTime);
	
	
	List<Map<String,String>> querySysResource();
	
    ServerCacheConfigEntity queryCacheConfig();
	
	void updateCacheConfig(ServerCacheConfigEntity configEntity);
	
	void updateMaxCacheConfig(@Param("maxRam") int maxRam,
			                  @Param("maxDisk") int maxDisk,
			                  @Param("actualDisk") int actualDisk,
			                  @Param("cachePath") String cachePath);
	
	/**
	 * 获取系统配置信息
	 * @param serverId
	 * @return
	 */
	String querySysStylePath();
	
	/**
	 * 更新系统配置信息
	 * @param serverId
	 * @return
	 */
	void updateSysStylePath(String stylePath);
	
	/**
	 * 查询功能页签
	 * 
	 */
	List<FunctionPageEntity> queryFunctionPageEntity();
}
