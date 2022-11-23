package org.pygisserver.web.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.dubbo.config.annotation.DubboService;
import org.pygisserver.mapserver.MapService;
import org.pygisserver.web.websocket.WebSocketManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSONObject;

@DubboService(version="3.0.0")
public class MapServiceImpl implements MapService {
	@Autowired
	private WebSocketManager webSocketManager;

	@Override
	public void noticeWebSocketTileFinshed(String msg) {
		Map<String,Object> msgMap = new HashMap<String,Object>();
		msgMap.put("msg", msg);
		msgMap.put("msgKey", "mapServer");
		JSONObject jsonObject = new JSONObject(msgMap);
		webSocketManager.sendMessage(jsonObject.toJSONString());
	}

}
