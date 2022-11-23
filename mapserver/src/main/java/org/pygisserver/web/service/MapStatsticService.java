package org.pygisserver.web.service;

import org.pygisserver.common.entity.SystemLogEntity;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.*;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.concurrent.Future;

/**
 * 地图服务接口，主要用于发布地图数据的存储、地图数据请求的存储
 * 
 * @author 
 * @since 2018-08-17
 */
public interface MapStatsticService {
    /**
     * 保存服务请求信息
     * 
     * @param reqEntity 请求实体
     */
    public void saveServerRequest(List<ServerRequestInfo> reqEntity);
    
    /**
     * 保存异常服务信息
     * @param abnormalServerInfo
     */
    void saveAbnormalServerInfo(AbnormalServerInfo abnormalServerInfo);
    
    
    /**
     * 保存统计信息
     * @param list
     */
    void saveStatisticServerRequest(List<ServerStatisticinfo> list);
    
    
    /**
     * 保存操作日志
     * @param abnormalServerInfo
     */
    void saveOperationLog(SysOperationLog sysOperation);
    
    /**
     * 根据图层名称获取服务信息
     * @param layerName 图层名称
     * @return 返回服务信息
     */
    public DataServerEntity queryServerInfo(String layerName);
    
    
    boolean isSupportTheFormat(String layer,String serviceType,String format);
    
    /**
     * 判断是否发布了对应的协议和版本号
     * @param layer
     * @param serviceName
     * @param version
     * @return
     */
    public boolean isPublishedServer(String layer,String serviceName,String version);
    
    /**
     * 获取服务的访问数量
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return
     */
    List<ServerStatisticinfo> queryServerVisitNum(String startTime,String endTime);
    
    
    /**
     * 获取服务性能指标
     * @param startTime
     * @param endTime
     * @return
     */
    List<ServerStatisticinfo> queryServerPerformance(String filed, String startTime, String endTime, String sort);
    
    /**
     * 保存系统资源
     * @param sysResource
     */
    void saveSysResource(SysResourceRecord sysResource);
    
    /**
     * 查询系统记录资源 
     * @return
     */
    List<SysResourceRecord> querySysResourceRecords();
    
    /**
     * 
     * 获取异常服务数据信息
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return
     */
    List<AbnormalServerInfo> queryAbnormalServerInfo(String startTime,String endTime);
    
    /**
     * 获取服务状态统计信息，默认是当月数据
     * @return
     */
    List<ServerStatisticinfo> serverTypeStatstic();
    
    /**
     * 服务状态统计
     * @return
     */
    List<ServerStatisticinfo> serverStatusStatstic();
    
    /**
     * 获取操作日志
     * @return
     */
    List<SysOperationLog> querySysOprationLog(RequestQueryCondition queryEntity);
    /**
     * 查询符合条件的系统日志个数
     * @return
     */
    int queryOperationLogCount(RequestQueryCondition queryEntity);
    
    /**
     * 获取操作日志
     * @return
     */
    List<SysOperationLog> queryAllSysOprationLog();
    
    void deleteOprationLog(List<Integer> sysLogIds);
    
    /**
     * 根据服务名称和产品名称删除请求信息
     * @param serverName 服务名称
     * @param productName 产品名称
     */
    void deleteServerReqInfo(String serverName,String productName);
    
    /**
     * 根据服务名称删除统计服务统计信息
     * @param serverNames 服务名称
     */
    void deleteServerReqInfoByName(List<String> serverNames);
    
    /**
     * 根据服务id删除服务统计信息
     * @param ids 服务id
     */
    void deleteServerReqInfoById(List<Integer> ids);
    
    /**
     * 删除异常服务信息
     * @param ids
     */
    void deleteAbnormalInfo(List<Integer> ids);
    
    /**
     * 查询当前统计列表中的所图层服务
     * @return
     */
    List<String> queryAllLayers();
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

    void updateRequestServerName(int serverId, String newServerName);

    void updateVisitServerName(int serverId, String newServerName);

    /**
     * 生成金字塔缓存数据
     *
     * @param dataServerEntity
     */
    @Async
    Future buildOverviews(DataServerEntity dataServerEntity);

    /**
     * 异步处理ImageMosaic数据发布
     *
     * @param dataServer
     * @param reqIP
     */
    @Async
    Future publishImageMosaic(DataServerEntity dataServer,String reqIP);

    /**
     * 异步处理请求统计信息
     *
     * @param requestInfo
     */
    @Async
    void saveReqEntity(ServerRequestInfo requestInfo);
}
