package org.pygisserver.render.service;

import java.awt.image.RenderedImage;

import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.render.entity.PyRenderedParam;
import org.pygisserver.render.entity.MapBaseDataInfo;

/**
 * @Description: PYGISServer图像绘制，对外提供所有可能用到的接口
 * @author:  
 * @date: 2021-03-19
 */
public interface PyRenderImage extends PyRender {

	/**
	 * 通过渲染请求渲染图片
	 * @param requestParam  渲染请求
	 * @return
	 */
	public byte[] renderImageByRequestParam(PyRenderRequestParam requestParam);

	/**
	 * 渲染处理高程数据
	 * @param requestParam
	 * @return
	 */
	byte[] renderedDem(PyRenderRequestParam requestParam);
	
	/**
	 * 根据数据信息渲染图片
	 * @param requestParam
	 * @param mapBaseDataInfo
	 * @return
	 */
	public RenderedImage renderImageByRequestParam(PyRenderedParam requestParam,MapBaseDataInfo mapBaseDataInfo);
	
}
