package org.pygisserver.render.service;

import org.pygisserver.common.entity.PyRenderRequestParam;

/**
 * @Description: PYGISServer矢量瓦片数据处理
 * @author: 
 * @date: 2021-03-22
 */
public interface PyRenderVectoreTile extends PyRender {

	public byte[] vectorTiles(PyRenderRequestParam requestParam); 
}
