package org.pygisserver.web.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.springframework.stereotype.Repository;

/**
 * @Description: POI查询的mapper接口 
 * @author: 
 * @date: 2019-01-26
 */
@Repository
public interface PoiQueryMapper {

	/**
	 * 查询兴趣点总数
	 * @param queryInfo
	 * @return
	 */
	int queryBestPoiCount(PoiQueryInfo queryInfo);
	
	/**
	 * 查询兴趣点的信息
	 * @param queryInfo
	 * @return
	 */
	List<PoiQueryResult> queryBestPoiInfo(PoiQueryInfo queryInfo);
	
	void insertPostGisTable(@Param("values") Object[] values);
	
	
}
