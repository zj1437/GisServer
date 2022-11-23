package org.pygisserver.web.wmsprotocol.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.wmsprotocol.interceptor.WmsServiceInterceptor;
import org.pygisserver.web.wmsprotocol.service.WmsProtocolService;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.wmsprotocol.entity.WmsRequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description: wms协议Controller
 * @author: 张亨特
 * @date: 2020/07/19
 */
@Api(tags="wms协议接口")
@RestController
@Slf4j
public class WmsProtocolController {

    @Autowired
    private WmsProtocolService wmsProtocolService;

    @ApiOperation(value = "查询WMS信息", notes = "查询WMS信息")
    @GetMapping(value = "/wms")
    public ReponseEntity<String> getTileInfo(HttpServletRequest req, HttpServletResponse rep) {
        WmsRequestParam requestInfo = WmsServiceInterceptor.TILEREQUEST.get();
        if (requestInfo == null) {
            return new ReponseEntity<>(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),
                    ErrorCode.RUNTIME_EXCEPTION.getErrorMsg(), "请求参数不能为空");
        }
        byte[] resourceInfo = null;
        String operationName = requestInfo.getRequest();
        int httpCode = HttpServletResponse.SC_OK;
        String mineType;
        String reqIP = UserUtil.getRequestIp(req);
        if ("GetLegendGraphic".equalsIgnoreCase(operationName)) {
            resourceInfo = wmsProtocolService.getWmsLegendGraphic(requestInfo, reqIP);
            mineType = requestInfo.getFormat();
        } else if ("GetMap".equalsIgnoreCase(operationName)) {
            mineType = requestInfo.getFormat();
            resourceInfo = wmsProtocolService.getWmsImageByte(requestInfo, reqIP);

        } else if ("GetFeatureInfo".equalsIgnoreCase(operationName)){
            requestInfo.setHttpRequest(req);
            requestInfo.setHttpResponse(rep);
            wmsProtocolService.getWmsFeatureInfo(requestInfo);
            mineType = requestInfo.getFormat();
            return null;
        } else {
            String baseUrl = queryServerUrl(req);
            String version = requestInfo.getVersion();
            if(StringUtils.isBlank(version)) {
            	version = "1.3.0";
            }
            resourceInfo = wmsProtocolService.getWmsCapabilities(baseUrl, version);
            mineType = "text/xml";
        }
        FileUtil.writeResponse(rep, req, resourceInfo, null, mineType, httpCode);
        return null;
    }

    private String queryServerUrl(HttpServletRequest req) {
        StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://").append(req.getServerName()).append(":").append(req.getServerPort())
                .append(req.getContextPath()).append("/");
        return sb.toString();
    }
}
