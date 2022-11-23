package org.pygisserver.web.interceptor;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.enumm.ServerStatusEnum;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.web.entity.ServerRequestInfo;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

/**
 * @Description: 预览接口相关切片类
 * @author: 张亨特
 * @date: 2020/08/04
 */
@Component
@Slf4j
public class PreViewMapServerInterceptor implements HandlerInterceptor {
    @Autowired
    private MapServerService mapServerService;

    @Autowired
    private RedisUtils redisUtils;

//    private WebSocketManager webSocket = SpringUtil.getBean("webSocketManager");

    @Autowired
    private WebSocketManager webSocket;

    public static final ThreadLocal<ServerRequestInfo> SERVERREQUESTINFO = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse response, Object handler)
            throws Exception {
        String servletPath = req.getServletPath();
        String layerName = "";
        String service = "";
        String request = "";
        LayerBaseInfo baseInfo;
        ServerRequestInfo serverRequest = new ServerRequestInfo();
        if (StringUtils.equalsIgnoreCase(servletPath, "/pytc/service/wmts")) {
            for (Enumeration<String> paraNames = req.getParameterNames(); paraNames.hasMoreElements();) {
                String paramName = paraNames.nextElement();
                if ("layer".equalsIgnoreCase(paramName)) {
                    layerName = req.getParameter(paramName);
                    break;
                }
            }
            for (Enumeration<String> paraNames = req.getParameterNames(); paraNames.hasMoreElements();) {
                String paramName = paraNames.nextElement();
                if ("request".equalsIgnoreCase(paramName)) {
                    request = req.getParameter(paramName);
                    break;
                }
            }
            service = "WMTS";

        } else if (StringUtils.equalsIgnoreCase(servletPath, "/wms")) {
            for (Enumeration<String> paraNames = req.getParameterNames(); paraNames.hasMoreElements();) {
                String paramName = paraNames.nextElement();
                if ("layers".equalsIgnoreCase(paramName)) {
                    layerName = req.getParameter(paramName);
                    break;
                }
            }
            for (Enumeration<String> paraNames = req.getParameterNames(); paraNames.hasMoreElements();) {
                String paramName = paraNames.nextElement();
                if ("request".equalsIgnoreCase(paramName)) {
                    request = req.getParameter(paramName);
                    break;
                }
            }
            service = "WMS";
        }

        // getcapabilities接口和GetLegendGraphic接口
        if (StringUtils.equalsAnyIgnoreCase(request, "getcapabilities", "GetLegendGraphic")) {
            return true;
        }
        baseInfo = getLayerBaseInfo(req, layerName, service, serverRequest);
        if (baseInfo == null) {
            return true;
        }
        if (!StringUtils.containsIgnoreCase(baseInfo.getSupportProtocal(), service.toLowerCase(Locale.ENGLISH))){
            String msg = "该服务不支持该协议，请联系管理员";
            log.error(msg);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", 500);
            jsonObject.put("msg", "该服务不支持该协议，请联系管理员");
            webSocket.sendMessage(jsonObject.toJSONString());
            throw new BaseException(ErrorCode.NOT_FOUND.getErrorCode(), msg);
        }
        if (ServerStatusEnum.PUBLISHED.getCode() != baseInfo.getStatus()) {
            String msg = "PreViewMapServerAdvices has error:当前服务非运行中服务，请联系管理员处理";
            log.warn(msg);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", 500);
            jsonObject.put("msg", "当前服务非运行中服务，请联系管理员处理");
            webSocket.sendMessage(jsonObject.toJSONString());
            throw new BaseException(ErrorCode.NOT_FOUND.getErrorCode(), msg);
        }
        return true;
    }

    private LayerBaseInfo getLayerBaseInfo(HttpServletRequest req, String layerName, String service, ServerRequestInfo serverRequest) {
        LayerBaseInfo baseInfo = mapServerService.getLayerBaseInfo(layerName, UserUtil.getRequestIp(req));

        // WMS协议编组预览时服务并未发布，也不计入统计数据
        if (baseInfo == null && "WMS".equalsIgnoreCase(service)) {
            return null;
        }
        serverRequest.setServerId(baseInfo.getServerId());
        serverRequest.setServerName(layerName);
        serverRequest.setServerType(0);// 0 是地图服务 1是模型服务
        serverRequest.setProtocolName(service);
        serverRequest.setRequetStartTime(new Date());
        serverRequest.setReqIP(UserUtil.getRequestIp(req));
        SERVERREQUESTINFO.set(serverRequest);
        return baseInfo;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        ServerRequestInfo requestInfo = SERVERREQUESTINFO.get();
        if (requestInfo != null) {
            long startTime = requestInfo.getRequetStartTime().getTime();
            long endTime = System.currentTimeMillis();
            requestInfo.setReponseTime(endTime - startTime);
            requestInfo.setRequetEndTime(new Date());
            // 缓存1分钟，保证数据可以被完全记录到数据库,且如果定时器坏了，无法插入到数据库，保证数据
            redisUtils.storeList("request-static", requestInfo, 1 * 60);
        }
        SERVERREQUESTINFO.remove();
    }
}
