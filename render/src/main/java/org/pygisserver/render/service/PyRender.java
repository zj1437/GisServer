package org.pygisserver.render.service;

import java.io.IOException;

import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.render.entity.PyMapContent;
import org.pygisserver.render.entity.PyRenderedParam;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public interface PyRender {
	
	/**
	 * 构造请求参数
	 * @param requestParam
	 * @return
	 */
	public void constructRenderRequestParam(PyRenderRequestParam requestParam);
	
	/**
	 * 构造渲染参数
	 * @param requestParam
	 * @return
	 */
	public PyRenderedParam constructRenderParam(PyRenderRequestParam requestParam) ;
	
	/**
	 * 构造MapContent
	 * @param requestParam  请求参数
	 * @param envelope
	 * @return
	 */
	public PyMapContent constructMapContent(PyRenderRequestParam requestParam,ReferencedEnvelope envelope) throws NoSuchAuthorityCodeException, IOException, FactoryException ;
	
}
