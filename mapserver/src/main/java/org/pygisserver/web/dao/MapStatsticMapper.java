package org.pygisserver.web.dao;

import org.apache.ibatis.annotations.Param;
import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.*;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 地图服务Dao
 * 
 * @author 
 *
 */
@Repository
public interface MapStatsticMapper {

	
	/**
	 * 保存服务请求信息
	 * @param reqEntity
	 */
	void saveServerRequest(List<ServerRequestInfo> reqEntity);
	
	/**
	 * 保存或者更新服务访问统计数据
	 * @param mapParam
	 */
	void saveOrUpdateServerStatistic(List<ServerStatisticinfo> serverStatistic);
	
	/**
	 * 
	 * @param mapParam
	 * @return
	 */
	List<ServerStatisticinfo> queryServerStatisticById(Map<String,Integer> mapParam);
	
	/**
	 * 获取图层是否可以访问
	 * @param layer
	 * @return
	 */
	boolean isAccess(String layer);
	
	/**
	 * 查看当前服务是否发布了该版本号的服务协议
	 * @param param
	 * @return
	 */
	boolean isPublishedVersion(Map<String, Object> param);
	
	/**
	 * 保存异常服务信息
	 * @param abnormalServerInfo 异常服务信息
	 */
	void saveAbnormalServerInfo(AbnormalServerInfo abnormalServerInfo);
	
	/**
	 * 保存系统操作日志
	 * @param sysOperation
	 */
	void saveOperationLog(SysOperationLog sysOperation);
	
	
	/**
	 * 获取系统操作日志
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	List<SysOperationLog> querySysOprationLog(RequestQueryCondition queryEntity);
	
    /**
     * 获取操作日志
     * @return
     */
    List<SysOperationLog> queryAllSysOprationLog();
    
    void deleteOprationLog(List<Integer> sysLogIds);
	
	/**
	 * 服务状态
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	List<ServerStatisticinfo> serverStatusStatstic();
	
	
	/**
	 * 服务类型统计
	 * @param startTime 
	 * @param endTime
	 * @return
	 */
	List<ServerStatisticinfo> serverTypeStatstic(@Param("startTime") Date startTime,
		       									 @Param("endTime") Date endTime);
	
	
	/**
	 * 获取监控访问量
	 * @param paramMap 参数map
	 * @return
	 */
	List<ServerStatisticinfo> queryServerVisitNum(Map<String, Object> paramMap);
	
	
	/**
	 * 检索服务性能
	 * @param paramMap 请求参数
	 * @return
	 */
	List<ServerStatisticinfo> queryServerPerformance(Map<String, Object> paramMap);
	
	
	/**
	 * 获取服务信息，用于计算并发量
	 * @param paramMap 请求参数
	 * @return
	 */
	List<ServerRequestInfo> queryServerReqInfo(Map<String, Object> paramMap);
	
	/**
	 * 保存系统资源信息
	 * @param sysResource
	 */
	void saveSysResourceInfo(SysResourceRecord sysResource);
	
	/**
	 * 获取系统资源
	 * @return
	 */
	List<SysResourceRecord> querySysResource(@Param("startTime")Date startTime,
											 @Param("endTime")Date endTime);
	
	/**
	 * 查询服务异常信息
	 * @param paramMap
	 * @return
	 */
	List<AbnormalServerInfo> queryAbnormalServerInfo(Map<String, Object> paramMap);
	
	/**
	 * 根据产品名称和服务名称删除统计信息
	 * @param serverName
	 */
	void deleteServerReqInfoByServerName(Map<String, Object> paramMap);
	
	
	/**
	 * 根据服务id删除服务统计信息
	 * @param ids 服务id
	 */
	void deleteServerReqInfoByServerIds(List<Integer> ids);
	
	
	/**
	 * 根据服务名称删除统计信息
	 * @param serverNames 服务名称
	 */
	int deleteServerReqInfoByName(List<String> serverNames);
	
	
	/**
	 * 删除服务异常记录
	 * @param ids  服务id集合
	 */
	void deleteAbnormalInfo(List<Integer> ids);
	

	/**
	 * 获取当前统计表中的所有图层
	 * @return
	 */
	List<String> queryAllLayers();
	
	/**
	 * 是否支持此格式
	 * @param layerName
	 * @param serviceType
	 * @param format
	 * @return
	 */
	boolean isSupportTheFormat(@Param("layer") String layerName,
							   @Param("type") String serviceType,
							   @Param("format") String format);
	
    /**
     * 保存系统日志信息
     * @return
     */
    void saveSystemLog(SystemLogEntity sysLog);
    /**
     * 查询符合条件的系统日志个数
     * @return
     */
    int querySystemLogCount(RequestQueryCondition queryEntity);
    int queryOprationLogCount(RequestQueryCondition queryEntity);
    /**
     * 查询符合条件的系统日志
     * @return
     */
    List<SystemLogEntity> querySystemLogs(RequestQueryCondition queryEntity);
    /**
     * 删除系统日志信息
     * @return
     */
    void deleteSystemLog(List<Integer> sysLogIds);
	
	/**
	 * 清除统计信息
	 * @param layers
	 */
	void deleteServerStatstic(List<String> layers);

	/**
	 * 修改请求表
	 *
	 * @param oldServerName
	 * @param newServerName
	 */
	void updateRequestServerName(@Param("serverId") int serverId,
								 @Param("newServerName") String newServerName);

	void updateVisitServerName(@Param("serverId") int serverId,
							   @Param("newServerName") String newServerName);


	List<ServerRequestInfo> queryServerRequestInfo(@Param("saveLastEndTime") Date saveLastEndTime,
												   @Param("saveEndTime") Date saveEndTime);
}
