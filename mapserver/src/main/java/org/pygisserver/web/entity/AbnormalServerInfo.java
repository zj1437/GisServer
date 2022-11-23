package org.pygisserver.web.entity;

import java.util.Date;

import lombok.Data;
/**
 * @Description: 异常服务信息 
 * @author:  
 * @date: 2018-11-22
 */
@Data
public class AbnormalServerInfo {

	
	/**
	 * 主键id
	 */
    private int id;
    
    /**
     * 服务名称
     */
    private String serverName;
    
    /**
     * 服务类型  0是地图服务，  1是模型服务
     */
    private int serverType;
	
    /**
     * 异常原因
     */
    private String abnormalReason;
    
    /**
     * 异常时间
     */
    private Date abnormalTime;
	
}
