package org.pygisserver.web.wmsprotocol.entity;

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
public class WmsRequestParam {

    private String service;

    private String version;

    private String request;

    private String format;

    private boolean transparent;

    private String layerName;

    private String bbox;

    private String style;

    private String srs;

    private String styles;

    private int width;

    private int height;

    private String time;

    private List<String> queryLayers;

    private int featureCount;

    private int xPixel;

    private int yPixel;

    private String exceptions;//"application/vnd.ogc.se_xml"

    private List<List<String>> propertyName;

    private String filter;

    private String cqlFilter;

    private int buffer;

    protected HttpServletRequest httpRequest;

    protected HttpServletResponse httpResponse;

    /**
     * 输入透明色
     */
    private String inputTransparentColor;

    /**
     * 输出透明色
     */
    private String outputTransparentColor;
}
