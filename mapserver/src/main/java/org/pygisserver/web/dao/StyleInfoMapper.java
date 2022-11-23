package org.pygisserver.web.dao;

import java.util.List;
import java.util.Map;

import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.RasterColorMap;
import org.pygisserver.web.entity.StyleInfo;
import org.springframework.stereotype.Repository;

/**
 * @Description: 
 * @author:
 * @date: 2019-12-19
 */
@Repository
public interface StyleInfoMapper {

	int queryStyleInfoCount(RequestQueryCondition queryCondition);
	
	List<StyleInfo> queryStyleInfo(RequestQueryCondition queryCondition);
	
	List<String> queryCurrentSldName();
	
	
	void saveRasterColor(RasterColorMap colorMap);
	
	/**
	 * 获取最大的styleId
	 * @return
	 */
	List<Map<String,Object>> queryMaxStyleIdOfTypeId();
	
	/**
	 * 获取栅格色带
	 * @return 返回色带集合
	 */
	List<RasterColorMap> queryRasterColor();
	
	/**
	 * 查询当前删除样式关联的服务名称
	 * @return
	 */
	List<String> queryStyleRelationServer(List<Integer> ids);
	
	List<Integer> queryServerIdsByStyleIds(List<Integer> ids);
	
	/**
	 * 删除样式文件
	 * @param ids
	 */
	void deleteStyleInfo(List<Integer> ids);
	
	
	/**
	 * 保存导入的sld文件
	 * @param styleInfo
	 */
	void saveStyleInfo(StyleInfo styleInfo);
	
	/**
	 * 删除的sld关联的样式id，置为默认样式
	 * @param styleId
	 */
	void updateServerStyleId(List<Integer> styleId);
	
	/**
	 * 根据样式id，获取样式名称
	 * @param styleId
	 * @return
	 */
	List<String> querySldNames(List<Integer> styleIds);
	
	/**
	 * 根据样式id，获取样式名称
	 * @param styleId
	 * @return
	 */
	List<String> queryStyleId(List<Integer> styleIds);
	
	/**
	 * 根据styleId查询样式信息
	 * @param styleId
	 * @return
	 */
	StyleInfo queryStyleInfoById(String styleId);

	List<DataServerEntity> queryServerByStyle(int typeId);
}
