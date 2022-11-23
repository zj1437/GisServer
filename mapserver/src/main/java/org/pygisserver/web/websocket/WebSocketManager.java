package org.pygisserver.web.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Resource;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.StatusException;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.web.dao.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@ServerEndpoint("/mapserver/websocket/{userId}")
@Component
@Slf4j
public class WebSocketManager {
	
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;
	
	@Resource
	private UserMapper userMapper;

	private static int onLineCount = 0;
	
	/**
	 * 一个用户对应多个session
	 */
	private static CopyOnWriteArraySet<Session> sessionCacheMap = new CopyOnWriteArraySet<Session>();
	
	
	/**
	 * @param username
	 * @param session
	 * @throws IOException
	 */
	@OnOpen
	public void onOpen(@PathParam("userId") String keyId, Session session) {
		sessionCacheMap.add(session);
		addOnlineCount();
		log.info("当前连接人数为：" + onLineCount);
	}

	/**
	 * 关闭钩子
	 * @throws IOException
	 */
	@OnClose
	public void onClose(Session session) {
		Map<String,String> map = session.getPathParameters();
		sessionCacheMap.remove(session);
		log.info("客户端" + map.get("userId") + "关闭连接");
		subLineCount();
	}

	/**
	 * 接受消息
	 * @param message
	 * @throws IOException
	 */
	@OnMessage
	public void onMessage(String message,Session session) {
		if (message.equals("check-heart")) {
//			log.info("来自客户端" + session.getId() + "发来消息:" + message);
			//心跳检测返回信息不能为空，否则会抛出参数不合法异常
			sendMessage(session,"check-heart");
		} else {
			log.info("来自客户端" + session.getId() + "发来消息:" + message);
		}
	}

	/**
	 * 错误钩子
	 * 
	 * @param session
	 * @param error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		log.error(error.getStackTrace().toString() + error.getLocalizedMessage());
	}

	/**
	 * 发送消息给所有连接客户（实现群发）  
	 * @param message
	 * @param To
	 * @throws IOException
	 */
	public void sendMessage(String message) {
		try {
			if (sessionCacheMap.isEmpty()) {
				log.warn("当前没有可用客户端");
			}
			for (Session session : sessionCacheMap) {
				if (session != null && session.isOpen()) {
					session.getBasicRemote().sendText(message);
				}
			}
		} catch (IOException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.WEB_SOCKET_ERROR, 
					"System","WebSocket服务", ExceptionsUtil.getServerIp(null)));
			log.error("WebSocketManager sendMessage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_SEND_MESSAGE, "WebSocket"));
		}
	}

	public void sendMessage(Session session,String message) {
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, ExceptionsUtil.WEB_SOCKET_ERROR, 
					"System","WebSocket服务", ExceptionsUtil.getServerIp(null)));
			log.error("WebSocketManager sendMessage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new StatusException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.STATUS_SEND_MESSAGE, "WebSocket"));
		}
	}
	
	public static synchronized int getOnLineCount() {
		return onLineCount;
	}

	public static synchronized void addOnlineCount() {
		WebSocketManager.onLineCount++;
	}
	
	public static synchronized void subLineCount() {
		WebSocketManager.onLineCount--;
	}
}
