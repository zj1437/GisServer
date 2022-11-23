package org.pygisserver.web.wmsprotocol.entity;

import lombok.Data;
import org.pygisserver.render.entity.PyRenderedParam;

import java.util.List;
import java.util.Map;

@Data
public class PyGetFeatureInfoRequest {

    private String baseUrl;

    //所有请求参数,可以不拼接
    private Map<String, String> rawKvp;

    protected String request;

    protected String version;

    private String requestCharset;

    private static final String DEFAULT_EXCEPTION_FORMAT = "application/vnd.ogc.se_xml";

    private static final int DEFAULT_MAX_FEATURES = 1;

    private PyRenderedParam renderedParam;

    private List<String> queryLayers;

    private String infoFormat;

    private int featureCount = DEFAULT_MAX_FEATURES;

    private int XPixel;// WMS 1.3.0 i

    private int YPixel;// WMS 1.3.0 j

    private List<List<String>> propertyNames;

    private String exceptionFormat = DEFAULT_EXCEPTION_FORMAT;

    private boolean excludeNodataResults = false;

    private String styles;

    private int buffer;//geoserver extension

    private String cqlFilter;//geoserver extension

    private String filter;//geoserver extension

}
