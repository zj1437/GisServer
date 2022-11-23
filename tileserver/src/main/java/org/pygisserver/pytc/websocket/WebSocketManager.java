//package org.pygisserver.pytc.websocket;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//
//import javax.websocket.OnClose;
//import javax.websocket.OnError;
//import javax.websocket.OnMessage;
//import javax.websocket.OnOpen;
//import javax.websocket.Session;
//import javax.websocket.server.PathParam;
//import javax.websocket.server.ServerEndpoint;
//
//import org.pygisserver.common.exception.ExceptionsUtil;
//import org.pygisserver.common.exception.StatusException;
//import org.pygisserver.common.service.SystemLogService;
//import org.pygisserver.pytc.util.SystemLogUtil;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import lombok.extern.slf4j.Slf4j;
//
//@ServerEndpoint("/tiled/websocket/{userId}")
//@Component
//@Slf4j
//public class WebSocketManager {
//
//	@Autowired
//	private ExceptionsUtil exceptionsUtil;
//
//	@Autowired
//	private SystemLogUtil systemLogUtil;
//
//	@Autowired
//	private SystemLogService systemLogService;
//
//	private static int onLineCount = 0;
//
//	/**
//	 * 服务端与单一客户端的通信
//	 */
//	private static ConcurrentMap<String, WebSocketManager> webSocketMap = new ConcurrentHashMap<String, WebSocketManager>();
//
//	public static ConcurrentMap<String, WebSocketManager> getWebSocketMap(){
//		return webSocketMap;
//	}
//
//	private Session session;
//
//
//	/**
//	 * @param username
//	 * @param session
//	 * @throws IOException
//	 */
//	@OnOpen
//	public void onOpen(@PathParam("userId") String keyId, Session session) {
//		this.session = session;
//		webSocketMap.put(keyId, this);
//		addOnlineCount();
//	}
//
//	/**
//	 * 关闭钩子
//	 *
//	 * @throws IOException
//	 */
//	@OnClose
//	public void onClose() {
//		Map<String,String> map = session.getPathParameters();
//		webSocketMap.remove(map.get("userId"));
//		log.info("客户端" + map.get("userId") + "关闭连接");
//		subLineCount();
//	}
//
//	/**
//	 * 接受消息
//	 * @param message
//	 * @throws IOException
//	 */
//	@OnMessage
//	public void onMessage(String message,@PathParam("userId") String keyId) {
//		log.info("来自客户端" + session.getId() + "发来消息:" + message);
//		if (message.equals("check-heart")) {
//			//心跳检测返回信息不能为空，否则会抛出参数不合法异常
//			sendMessage(keyId,"check-heart");
//		}
//	}
//
//	/**
//	 * 发送给指定用户
//	 * @param userId 用户id
//	 * @param msg  消息
//	 */
//	public void sendMessage(String userId,String msg) {
//		if (webSocketMap.containsKey(userId)) {
//			WebSocketManager socketManager = webSocketMap.get(userId);
//			sendMessage(socketManager,msg);
//		}
//	}
//
//	public void sendMessage(WebSocketManager socketManager,String message) {
//		try {
//			socketManager.session.getBasicRemote().sendText(message);
//		} catch (IOException e) {
//			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.WEB_SOCKET_ERROR,
//					"System","WebSocket服务", ExceptionsUtil.getServerIp(null)));
//			log.error("WebSocketManager sendMessage has error:" + ExceptionsUtil.stackTraceToStr(e));
//			throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_SEND_MESSAGE, "WebSocket"));
//		}
//	}
//
//	/**
//	 * 错误钩子
//	 *
//	 * @param session
//	 * @param error
//	 */
//	@OnError
//	public void onError(Session session, Throwable error) {
//		log.error(error.getStackTrace().toString() + error.getLocalizedMessage());
//	}
//
//	/**
//	 * 发送消息给所有连接客户（实现群发）
//	 * @param message
//	 * @param To
//	 * @throws IOException
//	 */
//	public void sendMessage(String message) {
//		if (webSocketMap.isEmpty()) {
//			log.warn("当前没有可用客户端");
//		}
//		for (WebSocketManager socketManager : webSocketMap.values()) {
//			try {
//				socketManager.session.getBasicRemote().sendText(message);
//			} catch (IOException e) {
//				systemLogUtil.synchSysLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.WEB_SOCKET_ERROR,
//						"System","WebSocket服务", ExceptionsUtil.getServerIp(null)));
//				log.error("WebSocketManager sendMessage has error:" + ExceptionsUtil.stackTraceToStr(e));
//				throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_SEND_MESSAGE, "WebSocket"));
//			}
//		}
//	}
//
//	public static synchronized int getOnLineCount() {
//		return onLineCount;
//	}
//
//	public static synchronized void addOnlineCount() {
//		WebSocketManager.onLineCount++;
//	}
//
//	public static synchronized void subLineCount() {
//		WebSocketManager.onLineCount--;
//	}
//}
//
