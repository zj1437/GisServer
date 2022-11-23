package org.pygisserver.web.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.ModelEntity;
import org.pygisserver.web.entity.ModelProduct;
import org.pygisserver.web.entity.OnlineModelEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description: 模型服务接口 
 * @author:  
 * @date: 2018-11-03
 */
public interface ModelServerService {

	boolean isPublished(String serverName);
	
	/**
	 * 获取模型列表数量
	 * @return
	 */
	int queryModelServerCount(RequestQueryCondition condition);
	
	
	/**
	 * 获取倾斜射影数量
	 * @return
	 */
	int queryTiltServerCount(RequestQueryCondition condition);
	
	/**
	 * 获取模型服务列表数据
	 * @return
	 */
	List<ModelEntity> queryModelServerList(RequestQueryCondition condition);
	
	/**
	 * 获取模型服务列表数据
	 * @return
	 */
	List<OnlineModelEntity> queryOnlineModels(RequestQueryCondition condition,String url);
	
	/**
	 * 获取模型服务列表数据
	 * @return
	 */
	List<OnlineModelEntity> getOnlineQXSY(RequestQueryCondition condition,String url);
	
	/**
	 * 获取倾斜射影列表数据
	 * @return
	 */
	List<ModelEntity> queryTiltServerList(RequestQueryCondition condition);
	
	
	/**
	 * 批量处理模型数据的发布(主要针对服务器上的数据)
	 * @param models
	 */
	Map<String,Object> publishModel(ModelEntity entity,String reqIP);
	
	/**
	 * 编辑模型服务
	 * @param entity
	 */
	Map<String,String> editModelServer(ModelEntity entity,String reqIP);
	
	/**
	 * 删除模型列表
	 * 
	 * @param ids
	 */
	void deleteModelServer(List<Integer> ids);
	
	/**
	 * 更新模型服务状态
	 * @param id
	 */
	void updateModelServerStatus(int id);
	
	/**
	 * 根据服务名和产品名称获取文件url
	 * 
	 * @param serverName
	 * @param productName
	 * @return
	 */
	String queryFileUrl(String serverName,String productName);
	
	
	/**
	 * 获取对应的文件位置
	 * @param fileurl  文件url
	 * @return
	 */
	String queryFilePosition(String fileurl,String user);
	
	/**
	 * 获取模型数据流
	 * @param url 文件url
	 * @param type 文件类型
	 * @return
	 */
	byte[] getModelFileStream (HttpServletRequest request,String url,String modelName,boolean isModel)  throws FileNotFoundException;
	
	/**
	 * 获取当前服务状态
	 * 
	 * @param serverName
	 * @return
	 */
	int queryCurrentServerStatus(String serverName);
	
	/**
	 * 根据产品id获取产品信息
	 * @param id
	 * @return
	 */
	ModelProduct getProductInfo(int id);
	
	/**
	 * 
	 * @param serverId
	 * @return
	 */
	List<String> getTiltServerUrl(int serverId,String reqIP);
	
	/**
	 * 获取当前服务的状态结果
	 * @param serverName
	 * @return
	 */
	double getCurrentDealResult(String serverName);

	/**
	 * online倾斜摄影偏移高度
	 *
	 * @param id 服务ID
	 * @param offsetHeight 偏移高度
	 * @return int
	 */
	int updateOffsetHeight(int id, double offsetHeight);

    void onlineQxsyData(HttpServletRequest request, HttpServletResponse response, String serverName, String productName) throws IOException;

	byte[] getGltfFile(HttpServletRequest request, String serverName);
}
