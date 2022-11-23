package org.pygisserver.web.wmsprotocol.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.wmsprotocol.entity.WmsRequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * @Description: wms的拦截请求
 * @author: 张亨特
 * @date: 2022/07/20
 */
@Slf4j
@Component
public class WmsServiceInterceptor implements HandlerInterceptor {

    @Autowired
    private MapServerService mapServerService;

    public static final ThreadLocal<WmsRequestParam> TILEREQUEST = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        WmsRequestParam requestInfo = getRequestParam(request);
        TILEREQUEST.set(requestInfo);
        if (StringUtils.equalsAnyIgnoreCase(requestInfo.getRequest(), "getcapabilities", "GetLegendGraphic")) {
            return true;
        }
        if(StringUtils.isNotBlank(requestInfo.getLayerName())){
            if (requestInfo.getLayerName().contains(",")) {
                return true;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        TILEREQUEST.remove();
    }

    /**
     * 处理wms的请求参数
     *
     * @param req
     * @return
     */
    private WmsRequestParam getRequestParam(HttpServletRequest req) {
        WmsRequestParam tileInfo = new WmsRequestParam();
        for (Enumeration<String> paraNames = req.getParameterNames(); paraNames.hasMoreElements();) {
            String paramName = paraNames.nextElement();
            if ("service".equalsIgnoreCase(paramName)) {
                tileInfo.setService(req.getParameter(paramName));
            }
            if ("version".equalsIgnoreCase(paramName)) {
                tileInfo.setVersion(req.getParameter(paramName));
            }
            if ("request".equalsIgnoreCase(paramName)) {
                tileInfo.setRequest(req.getParameter(paramName));
            }
            if ("format".equalsIgnoreCase(paramName)) {
                tileInfo.setFormat(req.getParameter(paramName));
            }
            if ("transparent".equalsIgnoreCase(paramName)) {
                tileInfo.setTransparent(Boolean.parseBoolean(req.getParameter(paramName)));
            }
            if ("layers".equalsIgnoreCase(paramName)) {
                tileInfo.setLayerName(req.getParameter(paramName));
            }
            if ("bbox".equalsIgnoreCase(paramName)) {
                tileInfo.setBbox(req.getParameter(paramName));
            }
            if ("style".equalsIgnoreCase(paramName)) {
                tileInfo.setStyle(req.getParameter(paramName));
            }
            if (StringUtils.equalsAnyIgnoreCase(paramName,"SRS","CRS")) {
                tileInfo.setSrs(req.getParameter(paramName));
            }
            if ("styles".equalsIgnoreCase(paramName)) {
                tileInfo.setStyles(req.getParameter(paramName));
            }
            if ("width".equalsIgnoreCase(paramName)) {
                tileInfo.setWidth(Integer.parseInt(req.getParameter(paramName)));
            }
            if ("height".equalsIgnoreCase(paramName)) {
                tileInfo.setHeight(Integer.parseInt(req.getParameter(paramName)));
            }
            if ("t".equalsIgnoreCase(paramName)) {
                tileInfo.setTime(req.getParameter(paramName));
            }
            if ("query_Layers".equalsIgnoreCase(paramName)) {
                String[] layers = !StringUtils.isEmpty(req.getParameter(paramName))
                        && req.getParameter(paramName).split(",").length >= 1
                        ? req.getParameter(paramName).split(",")
                        : new String[]{tileInfo.getLayerName()};
                tileInfo.setQueryLayers(Arrays.asList(layers));
            }
            if ("feature_count".equalsIgnoreCase(paramName)) {
                tileInfo.setFeatureCount(Integer.parseInt(req.getParameter(paramName)));
            }
            if ("x".equalsIgnoreCase(paramName) || (tileInfo.getVersion() != null && tileInfo.getVersion().equals("1.3.0") && "j".equalsIgnoreCase(paramName))) {
                tileInfo.setXPixel(Integer.parseInt(req.getParameter(paramName)));
            }
            if ("y".equalsIgnoreCase(paramName)|| (tileInfo.getVersion() != null && tileInfo.getVersion().equals("1.3.0") && "y".equalsIgnoreCase(paramName))) {
                tileInfo.setYPixel(Integer.parseInt(req.getParameter(paramName)));
            }
            if ("exceptions".equalsIgnoreCase(paramName)) {
                tileInfo.setExceptions(req.getParameter(paramName));
            }
            //未处理propertyName
//            if ("propertyName".equalsIgnoreCase(paramName)) {
//                tileInfo.setPropertyName(req.getParameter(paramName));
//            }
            if ("filter".equalsIgnoreCase(paramName)) {
                tileInfo.setFilter(req.getParameter(paramName));
            }
            if ("cql_filter".equalsIgnoreCase(paramName)) {
                tileInfo.setCqlFilter(req.getParameter(paramName));
            }
            if ("buffer".equalsIgnoreCase(paramName)) {
                tileInfo.setBuffer(Integer.parseInt(req.getParameter(paramName)));
            }
        }
        return tileInfo;
    }
}
