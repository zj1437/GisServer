package org.pygisserver.render.controller;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.render.service.PyRenderImage;
import org.pygisserver.render.service.PyRenderVectoreTile;
import org.pygisserver.render.vector.GeoJsonBuilderFactory;
import org.pygisserver.render.vector.MapBoxTileBuilderFactory;
import org.pygisserver.render.vector.TopoJSONBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Api(tags = "服务渲染接口")
@RestController
@Slf4j
public class RenderController {

	@Autowired
	private PyRenderImage renderImage;
	
	
	@Resource(name="mapbox")
	private PyRenderVectoreTile mapBoxRenderVectoreTile;
	
	
	@Resource(name="geojson")
	private PyRenderVectoreTile geojsonRenderVectoreTile;
	
	
	@Resource(name="topjson")
	private PyRenderVectoreTile topjsonRenderVectoreTile;
	
	/**
	 * 直接渲染图片，适用于，数据没有发布，不是内部服务，直接渲染使用
	 * @param renderParam
	 * @param rep
	 */
	@ApiOperation(value = "服务直接渲染接口",notes = "服务直接渲染")
	@ApiImplicitParam(value = "渲染请求参数",dataType = "renderParam",name = "renderParam",required = true)
	@PostMapping("/render")
	public ReponseEntity<String> rendering(@RequestBody(required=true) PyRenderRequestParam renderParam,
						  				   HttpServletResponse rep){
		byte[] buff = null;
		if (MapBoxTileBuilderFactory.MIME_TYPE.equalsIgnoreCase(renderParam.getFormat())){
			buff = mapBoxRenderVectoreTile.vectorTiles(renderParam);
		}else if(TopoJSONBuilderFactory.MIME_TYPE.equalsIgnoreCase(renderParam.getFormat())){
			buff = topjsonRenderVectoreTile.vectorTiles(renderParam);
		}else if (GeoJsonBuilderFactory.MIME_TYPE.equalsIgnoreCase(renderParam.getFormat())) {
			buff = geojsonRenderVectoreTile.vectorTiles(renderParam);
		}else {
			buff = renderImage.renderImageByRequestParam(renderParam);
		}
		dealMapReponse(renderParam, rep, buff);
		return null;
	}

	public void dealMapReponse(PyRenderRequestParam renderParam, HttpServletResponse rep, byte[] buff) {
		rep.setContentType(renderParam.getFormat());
		OutputStream outPutStream = null;
		try {
			outPutStream = rep.getOutputStream();
			outPutStream.write(buff);
			outPutStream.flush();
		} catch (IOException e) {
			log.error("RenderController rendering has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ExceptionsUtil.ERROR_500,ExceptionsUtil.BASE_ERROR);
		}finally {
			FileUtil.closeStream(outPutStream);
		}
	}
	
	/**
	 * 图层渲染，适用于不是内部服务，直接渲染使用
	 * @param fileUrl 文件url
	 * @param sldurl  sldurl
	 * @param bbox bbox
	 * @param srs 投影坐标
	 * @param format 渲染格式（image/png）
	 * @param mapWidth 渲染宽度
	 * @param mapHeight 渲染高度
	 * @param transparent 是否透明
	 * @param bgcolor 背景色
	 * @param antialias 抗锯齿
	 * @param interpolation 插值算法
	 * @param angle 旋转角度
	 */
	@ApiOperation(value = "图层渲染接口",notes = "图层渲染")
	@ApiImplicitParams({
			@ApiImplicitParam(value = "图层文件路径",dataType = "String",required = true,name = "fileUrl",paramType = "query"),
			@ApiImplicitParam(value = "样式文件路径",dataType = "String",required = true,name = "sldurl",paramType = "query"),
			@ApiImplicitParam(value = "bbox",dataType = "String",required = true,name = "bbox",paramType = "query"),
			@ApiImplicitParam(value = "投影坐标系",dataType = "String",required = true,name = "srs",paramType = "query"),
			@ApiImplicitParam(value = "渲染格式",dataType = "String",required = true,name = "format",paramType = "query"),
			@ApiImplicitParam(value = "渲染高度",dataType = "integer",required = true,name = "mapWidth",paramType = "query"),
			@ApiImplicitParam(value = "渲染宽度",dataType = "integer",required = true,name = "mapHeight",paramType = "query"),
			@ApiImplicitParam(value = "透明度",dataType = "Boolean",required = true,name = "transparent",paramType = "query"),
			@ApiImplicitParam(value = "渲染背景颜色",dataType = "String",required = true,name = "bgcolor",paramType = "query"),
			@ApiImplicitParam(value = "抗锯齿设置",dataType = "String",required = true,name = "antialias",paramType = "query"),
			@ApiImplicitParam(value = "插值算法",dataType = "String",required = true,name = "interpolation",paramType = "query"),
			@ApiImplicitParam(value = "旋转角度",dataType = "Double",required = true,name = "angle",paramType = "query")
	})
	@GetMapping("/render")
	public void rendering(@RequestParam(value="fileurl",required=true) String fileUrl,
						  @RequestParam(value="sldurl",required=true) String sldurl,
						  @RequestParam(value="bbox",required=true) String bbox,
						  @RequestParam(value="srs",required=true) String srs,
						  @RequestParam(value="format",required=true) String format,
						  @RequestParam(value="mapWidth",required=true) int mapWidth,
						  @RequestParam(value="mapHeight",required=true) int mapHeight,
						  @RequestParam(value="transparent",required=false,defaultValue="true") boolean transparent,
						  @RequestParam(value="bgcolor",required=false) Color bgcolor,
						  @RequestParam(value="antialias",required=false) String antialias,
						  @RequestParam(value="interpolation",required=false) String interpolation,
						  @RequestParam(value="angle",required=false,defaultValue="0.0") double angle,
						  HttpServletResponse rep) {
		
		PyRenderRequestParam pyRenderRequestParam = new PyRenderRequestParam();
		pyRenderRequestParam.setFileUrl(fileUrl);
		pyRenderRequestParam.setSldUrl(sldurl);
		pyRenderRequestParam.setRequestBbox(bbox);
		pyRenderRequestParam.setRequestSrs(srs);
		pyRenderRequestParam.setFormat(format);
		pyRenderRequestParam.setMapWidth(mapWidth);
		pyRenderRequestParam.setMapHeight(mapHeight);
		pyRenderRequestParam.setTransparent(transparent);
		if (bgcolor != null) {
			pyRenderRequestParam.setBgcolor(bgcolor);
		}
		pyRenderRequestParam.setAntialias(antialias);
		pyRenderRequestParam.setInterpolation(interpolation);
		pyRenderRequestParam.setAngle(angle);
		byte[] buff = null;
		if (MapBoxTileBuilderFactory.MIME_TYPE.equalsIgnoreCase(pyRenderRequestParam.getFormat())){
			buff = mapBoxRenderVectoreTile.vectorTiles(pyRenderRequestParam);
		}else if(TopoJSONBuilderFactory.MIME_TYPE.equalsIgnoreCase(pyRenderRequestParam.getFormat())){
			buff = topjsonRenderVectoreTile.vectorTiles(pyRenderRequestParam);
		}else if (GeoJsonBuilderFactory.MIME_TYPE.equalsIgnoreCase(pyRenderRequestParam.getFormat())) {
			buff = geojsonRenderVectoreTile.vectorTiles(pyRenderRequestParam);
		}else {
			buff = renderImage.renderImageByRequestParam(pyRenderRequestParam);
		}
		dealMapReponse(pyRenderRequestParam, rep, buff);
	}
	
}
