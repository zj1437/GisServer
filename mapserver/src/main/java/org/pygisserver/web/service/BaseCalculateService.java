package org.pygisserver.web.service;

import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description: poi查询service接口
 * @author: 
 * @date: 2019-01-26
 */
public interface BaseCalculateService {


	String projection(String startPoint,String endPoint,String srid,String fromSrid);
	
}
