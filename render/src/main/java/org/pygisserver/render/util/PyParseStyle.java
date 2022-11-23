package org.pygisserver.render.util;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.redis.util.RedisUtils;
import org.pygisserver.render.dao.RenderMapper;
import org.pygisserver.render.style.StyleInfoParseAndGenerate;
import org.geotools.styling.Style;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: 样式文件解析（TODO 需要精简样式加载流程） 
 * @author:  
 * @date: 2021-03-18
 */
@Component
public class PyParseStyle {
	
	@Autowired
	private RedisUtils redisUtil;
	
	@Autowired
	private StyleInfoParseAndGenerate styleHelper;
	
	@Autowired
	private RenderMapper renderMapper;
	
	private static final Map<String,Style> STYLE_CACHE = new ConcurrentHashMap<String, Style>();

	public Style parserStyleByStyleName(String styleName) {
		return parseStyleInfo(styleName, false);
	}

	public Style parserStyleBySldFileName(String sldFileName) {
		return parseStyleInfo(sldFileName, false);
	}

	public Style parserStyleBySldFilePath(String sldFilePath) {
		return parseStyleInfo(sldFilePath, true);
	}

	private Style parseStyleInfo(String info, boolean isSldPath) {
		String[] styleParamter = getStyleInfos(info, isSldPath);
		if (STYLE_CACHE.containsKey(styleParamter[0])) {
			return STYLE_CACHE.get(styleParamter[0]);
			
		}
		Style sldStyle = styleHelper.getStyle(styleParamter[1]);
		//只缓存系统内部的样式，对于用户输入的，不缓存
		if (!isSldPath){
			STYLE_CACHE.put(styleParamter[0], sldStyle);
		}
		return sldStyle;
	}
	private String[] getStyleInfos(String info, boolean isSldPath) {
		String sldFilePath = null;
		String styleName = null;
		String sldFileName = null;
		if (isSldPath) {
			styleName = info.substring(info.lastIndexOf(File.separator)+1, info.lastIndexOf("."));
			sldFilePath = info;
		} else {
			styleName = info.contains(".sld") ? info.substring(0, info.indexOf(".")) : info;
			sldFileName = info.contains(".sld") ? info : info + ".sld";
			String sldDir = (String)redisUtil.getValue("SYSTEM_STYLE_PATH");
			if (StringUtils.isBlank(sldDir)) {
				sldDir = renderMapper.querySysStylePath();
				redisUtil.storeValue(ExceptionsUtil.getServerIp("localhost"), "SYSTEM_STYLE_PATH", sldDir);
			}
			sldFilePath = sldDir + File.separator + sldFileName;
		}
		String[] strs = new String[] { styleName, sldFilePath };
		return strs;
	}
}
