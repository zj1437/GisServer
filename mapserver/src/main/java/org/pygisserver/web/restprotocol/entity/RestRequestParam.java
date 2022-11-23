package org.pygisserver.web.restprotocol.entity;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Description wms请求参数实体类
 * @author: 张亨特
 * @date: 2022/07/20
 */
@Data
public class RestRequestParam {

    /**
     * 服务名（表名）
     */
    private String restName;

    /**
     * 数据库连接名
     */
    private String sourceName;

    /**
     * 服务名(别名)
     */
    private String restServerName;

    private Integer pageNum=0;

    private Integer pageSize=10;

}
