package org.pygisserver.web.service;

import java.util.List;

import org.pygisserver.web.entity.PoiQueryInfo;
import org.pygisserver.web.entity.PoiQueryResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description: poi查询service接口
 * @author: 
 * @date: 2019-01-26
 */
public interface PoiQueryService {

	
	/**
	 * 获取最合适的兴趣点的数量
	 * @param queryParam  查询条件
	 * @return  返回数量
	 */
	public int queryBestPoiCount(PoiQueryInfo queryParam);
	
	
	/**
	 * 地点提示输入查询
	 * @param queryParam 查询条件
	 * @return 返回符合的poi信息
	 */
	public List<PoiQueryResult> queryBestPoiInfo(PoiQueryInfo queryParam);
	
	/**
	 * poi导入
	 *
	 */
	public String ExportTableToPostGis(String reqIp,MultipartFile[] files);
	
}
