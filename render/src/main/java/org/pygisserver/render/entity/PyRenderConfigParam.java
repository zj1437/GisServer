package org.pygisserver.render.entity;

import lombok.Data;

/**
 * @Description: pygisserver渲染配置参数 
 * @author: 
 * @date: 2021-03-19
 */
@Data
public class PyRenderConfigParam {

	
	/**
	 * 插值算法(即可以在配置中配置，也可以用户在请求的时候手动设置)
	 */
	private String interpolation;
	
	/**
	 * 单次最大渲染时间
	 */
	private int maxRenderTime;
	
	/**
	 * 瓦片X方向尺寸
	 */
	private int tileSizeX;
	
	/**
	 * 瓦片Y方向尺寸
	 */
	private int tileSizeY;
}
