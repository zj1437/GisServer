package org.pygisserver.common.service;

import org.pygisserver.common.entity.SystemLogEntity;

/**
 * @Description: 地图服务中间模块数据处理 
 * @author:  
 * @date: 2018-12-26
 */

public interface SystemLogService 
{
    /**
     * 保存系统日志信息
     * @return
     */
    void saveSystemLog(SystemLogEntity sysLog);
}
