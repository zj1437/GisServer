package org.pygisserver.web.dao;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description: POI查询的mapper接口 
 * @author: 
 * @date: 2019-01-26
 */
@Repository
public interface BaseCalculateMapper {

	/**
	 *
	 * @param startPoint
	 * @param endPoint
	 * @param srid
	 * @param formSrid
	 * @return
	 */
	String projection(@Param("startPoint")String startPoint,@Param("endPoint")String endPoint,@Param("srid")String srid,@Param("formSrid")String formSrid);

	/**
	 *
	 * @param startPoint
	 * @param endPoint
	 * @param srid
	 * @param formSrid
	 * @return
	 */
	String projectionBak(String startPoint,String endPoint,String srid,String formSrid);
}
