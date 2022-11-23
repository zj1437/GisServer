package org.pygisserver.web.wmtsprotocol.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.wmtsprotocol.interceptor.WmtsServiceInterceptor;
import org.pygisserver.web.wmtsprotocol.service.WmtsProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: wmts协议Controller
 * @author: 张亨特
 * @date: 2020/08/02
 */

@Api(value = "切图服务接口", tags = "切图服务接口")
@RestController
@RequestMapping("/pytc/service")
@Slf4j
public class WmtsProtocolController {

    @Autowired
    private WmtsProtocolService wmtsProtocolService;

    @ApiOperation(value = "查询WMTS切片信息", notes = "查询WMTS切片信息")
    @GetMapping(value = "/wmts")
    public ReponseEntity<String> getTileInfo(HttpServletRequest req, HttpServletResponse rep) {
        TileRequestInfo requestInfo = WmtsServiceInterceptor.TILEREQUEST.get();
        if (requestInfo == null) {
            return new ReponseEntity<>(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),
                    ErrorCode.RUNTIME_EXCEPTION.getErrorMsg(), "请求参数不能为空");
        }
        byte[] resourceInfo = null;
        // 先从Redis中查询缓存数据，有则直接返回，否则获取瓦片数据后再缓存在Redis中
        String operationName = requestInfo.getRequest();
        int httpCode = HttpServletResponse.SC_OK;
        String mineType;
        if ("GetTile".equalsIgnoreCase(operationName)) {
            LayerBaseInfo baseInfo = WmtsServiceInterceptor.LAYERBASE.get();
            String reqIP = UserUtil.getRequestIp(req);
            resourceInfo = wmtsProtocolService.getWmtsTile(baseInfo,requestInfo,reqIP);
            mineType = requestInfo.getMineType();
        } else {
            String baseUrl = queryServerUrl(req);
            resourceInfo = wmtsProtocolService.getWmtsCapabilities(baseUrl);
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
