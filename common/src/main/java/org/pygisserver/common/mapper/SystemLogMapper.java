package org.pygisserver.common.mapper;

import org.pygisserver.common.entity.SystemLogEntity;
import org.springframework.stereotype.Repository;

/**
 * 地图服务Dao
 * 
 * @author 
 *
 */
@Repository
public interface SystemLogMapper {
	
	/**
     * 保存系统日志信息
     * @return
     */
    void saveSystemLog(SystemLogEntity sysLog);
}
