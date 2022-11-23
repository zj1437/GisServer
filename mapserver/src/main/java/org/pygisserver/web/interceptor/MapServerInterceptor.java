package org.pygisserver.web.interceptor;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.web.entity.ServerRequestInfo;
import org.pygisserver.web.service.MapStatsticService;
import org.pygisserver.web.service.impl.MapServerServiceImpl;
import org.pygisserver.web.application.SpringUtil;
import org.pygisserver.web.entity.AbnormalServerInfo;
import org.pygisserver.web.entity.SysOperationLog;
import org.pygisserver.web.utils.UserUtil;
import org.pygisserver.web.websocket.WebSocketManager;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.Dispatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.MessageFormat;
import java.util.*;

/**
 * @Description: 地图服务请求拦截器
 * @author: 
 * @date: 2018-11-22
 */
//@Component
public class MapServerInterceptor implements HandlerInterceptor {

	private MapStatsticService mapStatsticService = SpringUtil.getBean("mapStatsticService");
	
	private WebSocketManager webSocket = SpringUtil.getBean("webSocketManager");
	
	private Catalog catalog = SpringUtil.getBean("rawCatalog");
	
	private RedisUtils redisUtils = SpringUtil.getBean("redisUtils");
	
	/**
	 * 已经记录过错误信息的图层
	 */
	private static Map<String, String> mapLayers = new HashMap<String, String>();

	private MapServerServiceImpl mapServerService = SpringUtil.getBean("mapServerServiceImpl");
	
	private static Map<String,LayerBaseInfo> layerBaseInfo = new HashMap<String,LayerBaseInfo>();
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
				
		/**
		 * 解决服务重启后wms样式，替换为默认样式处理
		 */
//		String styleName = request.getParameter("style");
//		if(StringUtils.isBlank(styleName) || "null".equalsIgnoreCase(styleName.toString())) {
//			styleName = request.getParameter("STYLES");
//		}
//		if(!StringUtils.isBlank(styleName)) {
//			catalog.getStyleByName(styleName);
//		}
//		if(StringUtils.isBlank(styleName)) {
//			String layerName = request.getParameter("LAYERS");
//			if(layerName == null) {
//				layerName = request.getParameter("layer");
//			}
//			if (StringUtils.isNotBlank(layerName)) {
//				RequestQueryCondition condition = new RequestQueryCondition();
//				condition.setServerName(layerName);
//				List<DataServerEntity> entities = mapServerService.queryServerDataList(condition);
//				if(entities.size() > 0) {
//					for(DataServerEntity entity: entities) {
//						if(entity.getServerName().equalsIgnoreCase(layerName) && entity.getStyleInfo() !=null) {
//							catalog.getStyleByName(entity.getStyleInfo().getSldName());
//						}
//					}
//				}
//			}
//		}
		return IsServerAccess(request, response);
	}
	

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		Dispatcher dispatcher = (Dispatcher) handler;
		// 如果errorMsg不为空，则证明当前有服务异常，则将异常信息记录即可
		Map<String, String> msgError = dispatcher.layerErrorMsg;
		if (!msgError.isEmpty()) {
			// 如果该图层没有记录异常则将该异常记录到异常信息表中
			Iterator<Map.Entry<String, String>> entries = msgError.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<String, String> entry = entries.next();
				String layer = entry.getKey();
				String msg = entry.getValue();
				//放置大量同一请求，因为同一异常，对外显示出大量重复异常
				if (mapLayers.containsKey(layer) && mapLayers.containsValue(msg)) {
					
					return;
				}
				//存储缓存
				mapLayers.put(layer, msg);
				//保存当前异常信息
				AbnormalServerInfo abnormalInfo = new AbnormalServerInfo();
				abnormalInfo.setServerName(layer);
				//地图服务
				abnormalInfo.setServerType(0);
				abnormalInfo.setAbnormalReason(msg);
				abnormalInfo.setAbnormalTime(new Date());
				mapStatsticService.saveAbnormalServerInfo(abnormalInfo);
				SysOperationLog operationLog = new SysOperationLog();
				operationLog.setOperatorIP(UserUtil.getRequestIp(request));
				operationLog.setOperateTime(new Date());
				operationLog.setOperator(UserUtil.getUserName());
				operationLog.setContent(MessageFormat.format("地图服务{0}对应的数据路径已经变化，当前服务可能不能正常使用",layer));
				mapStatsticService.saveOperationLog(operationLog);
			}
			// 处理完毕，清空Map
			msgError.clear();
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		long startTime = (long) request.getAttribute("startTime");
		ServerRequestInfo requestInfo = (ServerRequestInfo) request.getAttribute("reqEntity");
		if (requestInfo != null) {
			long endTime = System.currentTimeMillis();
			requestInfo.setReponseTime(endTime - startTime);
			requestInfo.setRequetEndTime(new Date());

//			mapStatsticService.saveReqEntity(requestInfo);
			//缓存1分钟，保证数据可以被完全记录到数据库
			redisUtils.storeList("request-static", requestInfo, 1*60);
		}
	}

	private boolean IsServerAccess(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		long startTime = System.currentTimeMillis();
		httpRequest.setAttribute("startTime", startTime);
		String request = "";
		String version = "";
		for (Enumeration<String> e = httpRequest.getParameterNames(); e.hasMoreElements();) {
			String paramName = e.nextElement();
			if ("request".equalsIgnoreCase(paramName)) {
				request = httpRequest.getParameter(paramName);
				if (StringUtils.isNotBlank(version)) {
					break;
				}
			}
		}
		// 检查如果为GetCapabilities则不拦截
		if ("GetCapabilities".equalsIgnoreCase(request) || "GetLegendGraphic".equalsIgnoreCase(request)) {
			return true;
		}
		String serviceType = "";
		String layer = "";
		String contextPath = httpRequest.getRequestURI();
		for (Enumeration<String> e = httpRequest.getParameterNames(); e.hasMoreElements();) {
			String paramName = e.nextElement();
			
			if (contextPath.contains("wcs") && version.equals("2.0.1")) {
				if ("CoverageId".equalsIgnoreCase(paramName)) {
					layer = httpRequest.getParameter(paramName);
					serviceType = "wcs";
					break;
				}
			}
			
			if (contextPath.contains("wcs") && version.equals("1.0.0")) {
				if ("Coverage".equalsIgnoreCase(paramName)) {
					layer = httpRequest.getParameter(paramName);
					serviceType = "wcs";
					break;
				}
			}
			
			if (contextPath.contains("wcs") && (version.equals("1.1.0") || version.equals("1.1.1"))) {
				if ("IDENTIFIER".equalsIgnoreCase(paramName)) {
					layer = httpRequest.getParameter(paramName);
					serviceType = "wcs";
					break;
				}
			}
			if (contextPath.contains("wfs")) {
				if ("typeName".equalsIgnoreCase(paramName)) {
					layer = httpRequest.getParameter(paramName);
					serviceType = "wfs";
					break;
				}
			}
		}
		
		// 如果包含pygis,
		if (layer.indexOf("pygis:") > -1) {
			layer = layer.replace("pygis:", "");
		}
		final LayerBaseInfo layerInfo;
//		if (layerBaseInfo.containsKey(layer)) {
//			layerInfo = layerBaseInfo.get(layer);
//		}else {
			layerInfo = mapServerService.getLayerBaseInfo(layer,UserUtil.getRequestIp(httpRequest));
			if (layerInfo == null) {
				// 考虑图层组未发布预览
				return true;
			}
//			if (layerInfo != null) {
//				layerBaseInfo.put(layer, layerInfo);
//			}else{
//				//考虑图层组未发布预览
//				return true;
//			}
//		}
		/* 不对协议做处理，因为前段如果能够选择到协议，说明肯定已经发布
		String proctolStr = layerInfo.getSupportProtocal();
		if (StringUtils.isBlank(proctolStr) || !proctolStr.contains(serviceType)) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("msg", String.format("对不起当前服务不支持%s协议", serviceType));
			webSocket.sendMessage(jsonObject.toJSONString());
			return false;
		}*/
		// 判断当前服务是否开启
		if (layerInfo != null) {
			int status = layerInfo.getStatus();
			if (status == 0) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("msg", "对不起该服务已经暂停");
				jsonObject.put("msgKey", "mapServer");
				webSocket.sendMessage(jsonObject.toJSONString());
				return false;
			}
			
		}
		ServerRequestInfo requestInfo = new ServerRequestInfo();
		requestInfo.setServerName(layer);
		requestInfo.setServerId(layerInfo.getServerId());
		requestInfo.setServerType(0);// 0 是地图服务
		requestInfo.setProtocolName(serviceType);
		requestInfo.setRequetStartTime(new Date());
		requestInfo.setReqIP(UserUtil.getRequestIp(httpRequest));
		httpRequest.setAttribute("reqEntity", requestInfo);
		return true;
	}
	
}
