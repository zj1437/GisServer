package org.pygisserver.web.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.ModelEntity;
import org.pygisserver.web.entity.ModelProduct;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelServerMapper {

	/**
	 * 查询已经存在的模型名称
	 * @return
	 */
	List<String> queryModelServerName();
	
	/**
	 * 获取模型服务数量
	 * @param queryCondition
	 * @return
	 */
	int queryModelCount(RequestQueryCondition queryCondition);
	
	/**
	 * 获取倾斜射影数量
	 * @param queryCondition
	 * @return
	 */
	int queryTiltCount(RequestQueryCondition queryCondition);
	
	/**
	 * 根据查询条件获取服务列表
	 * @param userName 当前登录用户
	 * @return
	 */
	List<ModelEntity> queryModellist(RequestQueryCondition queryCondition);
	
	/**
	 * 查询倾斜射影服务列表
	 * @param userName 当前登录用户
	 * @return
	 */
	List<ModelEntity> queryTiltlist(RequestQueryCondition queryCondition);
	
	/**
	 * 发布模型服务列表
	 * @param modelEntity
	 */
	void saveModelEntity(ModelEntity modelEntity);
	
	/**
	 * 保存模型产品列表
	 * @param products 产品列表
	 */
	void saveModelProducts(List<ModelProduct> products);
	
	/**
	 * 根据服务名和产品名获取当前服务的url
	 * @param serverName
	 * @param productName
	 * @return
	 */
	String queryServerUrl(Map<String, String> param);
	
	
	/**
	 * 根据服务id 获取当前服务下的产品
	 * @param modelServerid
	 * @return
	 */
	List<ModelProduct> queryProducts(int modelServerid);
	
	/**
	 * 编辑当前的模型服务
	 * @param modelEntity
	 */
	void editModelServer(ModelEntity modelEntity);
	
	/**
	 * 删除模型服务数据信息
	 * @param modelProducts
	 */
	void deleteModelProduct(List<ModelProduct> modelProducts);
	
	/**
	 * 删除模型服务
	 * @param ids
	 */
	void deleteModelServer(List<Integer> ids);
	
	/**
	 * 启动暂停某一个服务
	 * @param id
	 */
	void updateServerStatus(int id);
	
	/**
	 * 根据服务名称获取服务状态
	 * @return
	 */
	int queryServerStatus(String serverName);
	
	/**
	 * 根据模型产品id获取对应的产品信息
	 * @param productId
	 * @return
	 */
	ModelProduct queryProductInfo(int productId);
	
	/**
	 * 
	 * @param serverId
	 * @return
	 */
	String queryServerFileUrl(int serverId);
	
	/**
	 * 根据id 获取模型数据集合
	 * @param ids
	 * @return
	 */
	List<ModelEntity> queryModelEntityByIds(List<Integer> ids);
	
	/**
	 * 修改当前服务的状态
	 * @param serverName
	 */
	void updateServerStatusByName(@Param("name") String serverName,
							  	  @Param("status") int statusId);
	
	/**
	 * 根据服务id获取对应的模型服务信息
	 * @param serverId
	 * @return
	 */
	ModelEntity queryModelEntityById(int serverId);

	/**
	 * online倾斜摄影偏移高度
	 *
	 * @param id 服务ID
	 * @param offsetHeight 偏移高度
	 * @return int
	 */
	int updateOffsetHeight(@Param("id") int id, @Param("offsetHeight") double offsetHeight);
}
