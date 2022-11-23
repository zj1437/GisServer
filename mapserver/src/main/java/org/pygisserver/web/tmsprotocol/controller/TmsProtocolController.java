package org.pygisserver.web.tmsprotocol.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.enumm.ResponseFormat;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.entity.ServerRequestInfo;
import org.pygisserver.web.wmtsprotocol.interceptor.WmtsServiceInterceptor;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.tmsprotocol.service.TmsProtocolService;
import org.pygisserver.web.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Date;

/**
 * @Description: tms协议Controller
 * @author: 张亨特
 * @date: 2020/08/02
 */
@Api(value = "切图服务接口", tags = "切图服务接口")
@RestController
@RequestMapping("/pytc/service")
@Slf4j
public class TmsProtocolController {
    @Autowired
    private MapServerService mapServerService;

    @Autowired
    private WmtsServiceInterceptor serviceInterceptor;

    @Autowired
    private TmsProtocolService tmsProtocolService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 获取tms的能力值
     *
     * @param req 请求参数
     * @param rep 响应参数
     */
    @ApiOperation(value = "获取tms能力值", notes = "获取tms能力值")
    @ApiImplicitParam(name = "layerInfo", value = "层级信息", required = true, paramType = "path")
    @GetMapping(value = { "/tms/{version}/{layerInfo}" })
    public void tmsCapabilities(@PathVariable(value = "layerInfo", required = true) String layerInfo,
                                HttpServletRequest req, HttpServletResponse rep) {
        String url = req.getRequestURI();
        int indexOf = url.lastIndexOf("/") + 1;
        String string = url.substring(indexOf);
        String baseUrl = queryServerUrl(req);
        int lastIndexOf = layerInfo.lastIndexOf("@") + 1;
        String substring = layerInfo.substring(lastIndexOf);
        if("terrain".equals(substring) && "layer.json".equals(string)) {
            try {
                rep.sendRedirect("/pygisserver/config/layer.json");
            } catch (IOException e) {
                log.error("重定向跳转失败：" + ExceptionsUtil.stackTraceToStr(e));
            }
        }else {
            byte[] resourceInfo = tmsProtocolService.getTmsCapabilities(layerInfo, baseUrl);
            if (resourceInfo == null) {
                return;
            }
            FileUtil.writeResponse(rep, req, resourceInfo, null, "text/xml", HttpServletResponse.SC_OK);
        }


    }

    private String queryServerUrl(HttpServletRequest req) {
        StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://").append(req.getServerName()).append(":").append(req.getServerPort())
                .append(req.getContextPath()).append("/");
        return sb.toString();
    }


    /**
     * tms协议图像获取
     * @param req 请求参数
     * @param rep 响应参数
     */
    @ApiOperation(value = "tms协议图像获取", notes = "tms协议图像获取")
    @ApiImplicitParams({ @ApiImplicitParam(name = "layerInfo", value = "层级信息", required = true, paramType = "query"),
            @ApiImplicitParam(name = "zoomLevel", value = "层级", required = false, paramType = "query"),
            @ApiImplicitParam(name = "col", value = "列数", required = false, paramType = "query"),
            @ApiImplicitParam(name = "row", value = "行数", required = false, paramType = "query") })
    @GetMapping(value = { "/tms/{version}/{layerInfo}/{zoom}/{col}/{row:.+}" })
    public ReponseEntity<String> tmsTile(@PathVariable(value = "layerInfo", required = true) String layerInfo,
                                         @PathVariable(value = "zoom", required = false) int zoomLevel,
                                         @PathVariable(value = "col", required = false) int col,
                                         @PathVariable(value = "row", required = false) String row, HttpServletRequest req,
                                         HttpServletResponse rep) {
        //记录开始时间
        long startTime = System.currentTimeMillis();
        TileRequestInfo tileRequestInfo = new TileRequestInfo();
        String reqIP = UserUtil.getRequestIp(req);
        LayerBaseInfo baseInfo = null;
        //构造参数
        try {
            String[] layerInfoArray = layerInfo.split("@");
            String format = ResponseFormat.getFormatByPrefix(layerInfoArray[2]);
            baseInfo = mapServerService.getLayerBaseInfo(layerInfoArray[0], reqIP);
            tileRequestInfo.setServerId(baseInfo.getServerId());
            tileRequestInfo.setLayerName(layerInfoArray[0]);
            int y = Integer.parseInt(row.split("\\.")[0]);
            y = (int) (Math.pow(2, zoomLevel) - 1 - y);
            tileRequestInfo.setTileRow(y);
            String gridSet = URLDecoder.decode(layerInfoArray[1], "UTF-8");
            tileRequestInfo.setGridSet(gridSet);
            tileRequestInfo.setTileCol(col);
            tileRequestInfo.setZoomLevel(zoomLevel);
            tileRequestInfo.setMineType(format);
        } catch (Exception e) {
            log.error("TileServiceController tmsTile has error:" + ExceptionsUtil.stackTraceToStr(e));
            throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "TMS协议请求参数"));
        }
        //检验参数有效性
        tileRequestInfo.setStyle("");
        serviceInterceptor.checkUnValidReq(baseInfo,tileRequestInfo);
        byte[] bufferResource = tmsProtocolService.getTmsTile(baseInfo,tileRequestInfo,reqIP);
        recordReqData(reqIP, startTime,tileRequestInfo);
        if (bufferResource == null) {
            throw new BaseException(ErrorCode.NOT_FOUND.getErrorCode(), "请求的资源不存在");
        }
        FileUtil.writeResponse(rep, req, bufferResource, null, tileRequestInfo.getMineType(), HttpServletResponse.SC_OK);
        return null;
    }

    public void recordReqData(String reqIP, long startTime,TileRequestInfo tileRequestInfo) {
        // 记录当前的访问请求
        ServerRequestInfo serverRequest = new ServerRequestInfo();
        serverRequest.setServerName(tileRequestInfo.getLayerName());

        // 0 是地图服务 1是模型服务
        serverRequest.setServerType(0);
        serverRequest.setProtocolName("TMS");
        serverRequest.setReqIP(reqIP);
        serverRequest.setRequetStartTime(new Date(startTime));
        long endTime = System.currentTimeMillis();
        serverRequest.setRequetEndTime(new Date());
        serverRequest.setReponseTime(endTime - startTime);
        serverRequest.setServerId(tileRequestInfo.getServerId());

        //缓存5分钟，保证数据可以被完全记录到数据库,且如果定时器坏了，无法插入到数据库，保证数据
        redisUtils.storeList("request-static", serverRequest, 1*60);
    }

    /**
     * 获取tms的terrain能力值
     *
     * @param layerInfo layerInfo
     */
    @ApiOperation(value = "获取tms的terrain能力值", notes = "获取tms的terrain能力值")
    @GetMapping(value = { "/tms/{version}/{layerInfo}/{layerJson}"})
    public void tmsTerrainCapabilities(@PathVariable(value = "layerInfo", required = true) String layerInfo,
                                       @PathVariable(value = "layerJson", required = true) String layerJson,
                                       HttpServletRequest request, HttpServletResponse response) {
        int lastIndexOf = layerInfo.lastIndexOf("@") + 1;
        String substring = layerInfo.substring(lastIndexOf);
        if("terrain".equals(substring) && "layer.json".equals(layerJson)) {
            try {
                response.sendRedirect("/pygisserver/config/layer.json");
            } catch (IOException e) {
                log.error("重定向跳转失败：" + ExceptionsUtil.stackTraceToStr(e));
            }
        } else {
            throw new ArgumentException(ExceptionsUtil.ERROR_400, "接口参数异常");
        }
    }
}
