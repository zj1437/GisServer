package org.pygisserver.render.factory;

import org.pygisserver.render.service.PyRenderImage;
import org.pygisserver.render.service.PyRenderVectoreTile;

public interface RenderImageBuild {

	/**
	 * 获取图片渲染类
	 * @return
	 */
	PyRenderImage createRenderImage();
	
	/**
	 * 根据返回类型获取瓦片渲染类
	 * @param mineType
	 * @return
	 */
	PyRenderVectoreTile createVectorTile(String mineType);
	
}
