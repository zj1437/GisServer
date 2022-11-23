package org.pygisserver.render.service;

import org.pygisserver.common.entity.PyRenderRequestParam;

/**
 * @Description: 渲染提供的接口工程 
 * @author: 刘洋 
 * @date: 2021-04-09
 */
public interface RenderService {

	/**
	 * 通过渲染请求渲染图片
	 * @param requestParam  渲染请求
	 * @return
	 */
	public byte[] renderImageByRequestParam(PyRenderRequestParam requestParam);
}
