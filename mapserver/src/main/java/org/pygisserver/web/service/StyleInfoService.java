package org.pygisserver.web.service;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.RasterColorMap;
import org.pygisserver.web.entity.StyleInfo;
import org.geotools.styling.Style;
import org.springframework.web.multipart.MultipartFile;

public interface StyleInfoService {

	String validateSldIllegel(HttpServletRequest req, MultipartFile files, String targetPath);

	String importCustomSld(HttpServletRequest req, File file, String destPath,String fileName);

	void queryMaxStyleIdOfTypeId();
	
	List<RasterColorMap> queryColorMap();
	
	/**
	 * 按照查询条件获取对应的样式信息
	 * @param queryCondition
	 * @return
	 */
	List<StyleInfo> queryStyleInfoByCondition(RequestQueryCondition queryCondition);
	
	/**
	 * 
	 * @param fileType
	 * @param fileUrl
	 * @return
	 */
	List<StyleInfo> queryStyleInfoList(String fileType,String fileUrl,String reqIP);
	
	/**
	 * 
	 * @param queryCondition 查询条件
	 * @return
	 */
	int queryStyleCount(RequestQueryCondition queryCondition);
	
	/**
	 * 删除服务前警告
	 * @param req
	 * @param ids
	 */
	String warnBeforedelete(HttpServletRequest req,List<Integer> ids);
	
	/**
	 * 删除样式文件信息
	 */
	void deleteStyleInfo(HttpServletRequest req,List<Integer> ids);
	
	/**
	 * 根据styleiD 获取sld name
	 * @param ids
	 * @return
	 */
	List<String> querySldName(List<Integer> ids);
	
	/**
	 * 根据styleId查询样式信息
	 * @param styleId
	 * @return
	 */
	StyleInfo queryStyleInfoById(String styleId);
	
	/**
	 * 根据样式名称解析样式信息
	 * @param styleName
	 * @return
	 */
	Style parserStyleByStyleName(String styleName);
	
	/**
	 * 根据sld文件名称解析样式信息
	 * @param sldFileName
	 * @return
	 */
	Style parserStyleBySldFileName(String sldFileName);
	
	/**
	 * 根据sld文件路径解析样式信息
	 * @param sldFilePath
	 * @return
	 */
	Style parserStyleBySldFilePath(String sldFilePath);
	
	/**
	 * 根据样式名称获取样式预览图
	 * @param styleName
	 * @return
	 */
	byte[] getStyleImageByStyleName(String styleName);
	
	/**
	 * 根据样式文件名称获取样式预览图
	 * @param sldFileName
	 * @return
	 */
	byte[] getStyleImageBySldFileName(String sldFileName);
	
	/**
	 * 根据样式文件路径获取样式预览图
	 * @param sldFilePath
	 * @return
	 */
	byte[] getStyleImageBySldFilePath(String sldFilePath);

	/**
	 * 根据样式查询可适用的所有服务名称
	 *
	 * @param typeId 样式类型id
	 * @param reqIP 请求ip
	 * @return List
	 */
	List<DataServerEntity> queryServerByStyle(int typeId, String reqIP);
}
