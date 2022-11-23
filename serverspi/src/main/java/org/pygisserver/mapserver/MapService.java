package org.pygisserver.mapserver;

/**
 * @Description: 渲染提供的接口工程 
 * @author: 刘洋 
 * @date: 2021-04-09
 */
public interface MapService {

	/**
	 * 通过渲染请求渲染图片
	 * @param msg  渲染请求
	 * @return
	 */
	public void noticeWebSocketTileFinshed(String msg);
}
