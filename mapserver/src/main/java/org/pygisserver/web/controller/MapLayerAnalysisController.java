package org.pygisserver.web.controller;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.AnalysisResult;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.web.aop.MapLayerAnalysisAdvices;
import org.pygisserver.web.entity.AnalysisResultObject;
import org.pygisserver.web.entity.AnalysisTaskParam;
import org.pygisserver.web.service.MapLayerService;
import org.pygisserver.web.utils.JTSUtil;
import org.pygisserver.render.format.TiffResponseFormat;
import org.pygisserver.terrainanalysis.service.MapLayerAnalysis;
import org.pygisserver.web.factory.PySpiServiceFactory;
import org.pygisserver.web.utils.MapServerUtils;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 对已经发布的数据进行分析计算
 * @author:  
 * @date: 2020-08-18
 */
@Api(tags = "地形分析接口")
@RestController
@RequestMapping("/analysis")
@Slf4j
public class MapLayerAnalysisController {

	@Resource
	private PySpiServiceFactory spserviceFactory;
	
	@Autowired
	private MapLayerAnalysisAdvices mapLayerAnalysisAdvices;
	
	@Autowired
	private MapLayerService mapLayerService;
	
	@Autowired
	private TiffResponseFormat tiffReponseFormat;
	
	/**
	 * 获取单个点的高程值
	 * @param layer 图层名称
	 * @param lon   经度
	 * @param lat   纬度
	 * @return
	 */
	
	@GetMapping("/dem/position")
	@ApiOperation(value="点类型高程分析",notes="点类型高程分析")
	@ApiImplicitParams({
			@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
			@ApiImplicitParam(name="lon",value="经度",required=true,paramType="quety"),
			@ApiImplicitParam(name="lat",value="纬度",required=true,paramType="quety"),
			@ApiImplicitParam(name="format",value="返回类型",required=false,paramType="quety")
			})
	public ReponseEntity<AnalysisResultObject> getDemByPostion(@RequestParam(name="layer",required=true) String layerName,
															   @RequestParam(name="lon",required=true) double lon,
															   @RequestParam(name="lat",required=true) double lat,
															   @RequestParam(name="format",required=false) String format,
															   HttpServletRequest request){
		MapLayerAnalysis mapLayerAnalysis = (MapLayerAnalysis) spserviceFactory.findClazzInstance("MapLayerAnalysis");
		ReferencedEnvelope envelope = mapLayerAnalysisAdvices.getLayerEnvelope(layerName);
		Coordinate coordinate = new Coordinate(lon, lat);
		if (!envelope.intersects(coordinate)) {
			log.error("MapLayerAnalysisController getDemByPostion has error {0}",String.format("计算点坐标[%s,%s]不在当前数据范围内", lon,lat));
			throw new BaseException(ExceptionsUtil.ERROR_500, MessageFormat.format("计算点坐标[{0},{1}]不在当前数据范围内", lon,lat));
		}
		Double dem = 0.0;
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")) {
			dem = mapLayerAnalysis.getDemByPixel(layerBaseInfo, layerBaseInfo.getNativaBbox(), lon, lat);
		}else {
			dem = mapLayerAnalysis.getDemByPosition(layerBaseInfo.getFilePath(), layerBaseInfo.getNativaBbox(), lon, lat);
		}
		AnalysisResultObject result = new AnalysisResultObject();
		if (StringUtils.isNotBlank(format) && "application/json".equals(format)) {
			Object geometryStr = MapServerUtils.getgeometryObject(0,coordinate);
			result.setGeometry(geometryStr);
		}
		result.setDemArray(new double[] {dem});
		return new ReponseEntity<AnalysisResultObject>(ReponseStatus.SUCCESS.getCode(), 
													    ReponseStatus.SUCCESS.getResult(),
													    result);
	}
	
	/**
	 * 根据起点，终点以及要求的步幅来计算高程值
	 * @param layer  图层名称
	 * @param startPos 起点
	 * @param endPos  终点
	 * @param stride  间隔(单位m)
	 * @param format  返回类型（application/array,application/json）
	 * @return
	 */
	@GetMapping("/dem/line")
	@ApiOperation(value="线类型高程分析",notes="线类型高程分析")
	@ApiImplicitParams({
			@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
			@ApiImplicitParam(name="startPos",value="起点坐标",required=true,paramType="quety"),
			@ApiImplicitParam(name="endPos",value="终点坐标",required=true,paramType="quety"),
			@ApiImplicitParam(name="sampleType",value="采样类型（0）",required=true,paramType="quety"),
			@ApiImplicitParam(name="stride",value="间隔(单位m)",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNum",value="采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="format",value="返回类型（application/array,application/json）",required=false,paramType="quety")
			})
	public ReponseEntity<AnalysisTaskParam> getDemByLinePoint(@RequestParam(name="layer",required=true) String layerName,
															  @RequestParam(name="startpos",required=true) String startPos,
															  @RequestParam(name="endpos",required=true) String endPos,
															  @RequestParam(name="sampleType",required=true) int sampleType,
															  @RequestParam(name="stride",required=false,defaultValue="0.0") double stride,
															  @RequestParam(name="samplenum",required=false,defaultValue="0") int sampleNum,
															  @RequestParam(name="format",required=false) String format,
															  HttpServletRequest request){
		final AnalysisTaskParam taskParam;
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		if (sampleType == 0) {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getLineFromSampleNum(layerName, startPos, endPos, sampleNum, format,13);
			} else {
				taskParam = mapLayerService.getLineFromSampleNum(layerName, startPos, endPos, sampleNum, format,1);
			}
		}else {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getLineFromStride(layerName, startPos, endPos, sampleNum, format,14);
			} else {
				taskParam = mapLayerService.getLineFromStride(layerName, startPos, endPos, stride, format, 2);
			}
		}
		String baseUrl = queryServerUrl(request);
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("analysis/result/").append(taskParam.getTaskId());
		taskParam.setResultUrl(sb.toString());
		return new ReponseEntity<AnalysisTaskParam>(ReponseStatus.SUCCESS.getCode(), 
												    ReponseStatus.SUCCESS.getResult(),
												    taskParam);
	}
	
	
	/**
	 * 计算矩形区域的高程值
	 * @param layerName 图层信息
	 * @param leftUpperPos 左上方点
	 * @param rightLowerPos 右下方点
	 * @param sampleType  采样类型
	 * @param strideX     X方向步幅
	 * @param strideY     Y方向步幅
	 * @param sampleNumX  X方向采样点数
	 * @param sampleNumY  Y方向采样点数
	 * @param format      返回类型（application/array,application/json，image/tiff）
	 * @param rep
	 * @return
	 */
	@GetMapping("/dem/region")
	@ApiOperation(value="矩形类型高程分析",notes="矩形类型高程分析")
	@ApiImplicitParams({
			@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
			@ApiImplicitParam(name="leftUpperPos",value="左上方点",required=true,paramType="quety"),
			@ApiImplicitParam(name="rightLowerPos",value="右上方点",required=true,paramType="quety"),
			@ApiImplicitParam(name="sampleType",value="采样类型（0）",required=true,paramType="quety"),
			@ApiImplicitParam(name="strideX",value="X方向步幅",required=false,paramType="quety"),
			@ApiImplicitParam(name="strideY",value="Y方向步幅",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNumX",value="X方向采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNumY",value="Y方向采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="format",value="返回类型（application/array,application/json，image/tiff）",required=false,paramType="quety")
			})
	public ReponseEntity<AnalysisTaskParam> getDemByRegion(@RequestParam(name="layer",required=true) String layerName,
														   @RequestParam(name="leftUpperPos",required=true) String leftUpperPos,
														   @RequestParam(name="rightLowerPos",required=true) String rightLowerPos,
														   @RequestParam(name="sampleType",required=true) int sampleType,
														   @RequestParam(name="strideX",required=false,defaultValue="0.0") double strideX,
														   @RequestParam(name="strideY",required=false,defaultValue="0.0") double strideY,
														   @RequestParam(name="sampleNumX",required=false,defaultValue="0") int sampleNumX,
														   @RequestParam(name="sampleNumY",required=false,defaultValue="0") int sampleNumY,
														   @RequestParam(name="format",required=false) String format,
														   HttpServletRequest request){
		
		final AnalysisTaskParam taskParam;
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		if (sampleType == 0) {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getRectangleFromSampleNum(layerName,leftUpperPos,rightLowerPos,sampleNumX,sampleNumY,format,15);
			}else {
				taskParam = mapLayerService.getRectangleFromSampleNum(layerName, leftUpperPos, rightLowerPos, sampleNumX, sampleNumY, format, 3);
			}
		}else {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getRectangleFromStride(layerName,leftUpperPos,rightLowerPos,sampleNumX,sampleNumY,format,16);
			}else {
				taskParam = mapLayerService.getRectangleFromStride(layerName, leftUpperPos, rightLowerPos, strideX, strideY, format, 4);
			}
		}
		String baseUrl = queryServerUrl(request);
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("analysis/result/").append(taskParam.getTaskId());
		taskParam.setResultUrl(sb.toString());
		return new ReponseEntity<AnalysisTaskParam>(ReponseStatus.SUCCESS.getCode(), 
												    ReponseStatus.SUCCESS.getResult(),
												    taskParam);
		
	}
	
	/**
	 * 获取分析结果，前端应该是一个轮训的过程
	 * @param taskId
	 * @return
	 */
	@ApiOperation(value="获取高程分析结果",notes="获取高程分析结果")
	@ApiImplicitParams({
		@ApiImplicitParam(name="taskId",value="任务id",required=true,paramType="path"),
		@ApiImplicitParam(name="format",value="类型",required=true,paramType="quety")
		})
	@GetMapping("/result/{taskId}")
	public ReponseEntity<?> getAnalysisResult(@PathVariable("taskId") int taskId,
															    @RequestParam(name="format",required=false) String format,
															    HttpServletRequest request,
															    HttpServletResponse response){
		AnalysisResult result = mapLayerService.getAnalysisResult(taskId);
		if (result == null) {
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(),
				    						 ReponseStatus.SUCCESS.getResult(), String.format("分析进行中",taskId));
		}
		AnalysisResultObject resultObject = new AnalysisResultObject();
			
		if (StringUtils.isNotBlank(format) && "application/json".equals(format)) {
			Object geometryStr = MapServerUtils.getgeometryObject(1,result.getCoordinates());
			resultObject.setGeometry(geometryStr);
			resultObject.setSampleX(result.getImageWidth());
			resultObject.setSampleY(result.getImageHeight());
		}
			
		if (StringUtils.isNotBlank(format) && format.equals("image/tif")) {
			double[] demArray = result.getDemArray();
			float[] floatArray = new float[demArray.length];
			for (int i = 0; i < demArray.length; i++) {
				floatArray[i] = (float) demArray[i];
			}
			int width = result.getImageWidth();
			int height = result.getImageHeight();
			DataBufferFloat dataBuffer = new DataBufferFloat(floatArray, width*height);
			ColorSpace space = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	    	ComponentSampleModel sampleModle = new ComponentSampleModel(DataBuffer.TYPE_FLOAT, width ,height, 1, width, new int[] {0});
	    	int[] bits = {32};
	    	ComponentColorModel color1 = new ComponentColorModel(space, bits, false, false, Transparency.TRANSLUCENT,DataBuffer.TYPE_FLOAT);
	    	WritableRaster raster = Raster.createWritableRaster(sampleModle,dataBuffer,new Point(0,0));
	    	BufferedImage image = new BufferedImage(color1,raster,false,null);
	    	//些图片到输出流
			try {
				response.setContentType("image/tiff");
				response.setStatus(HttpServletResponse.SC_OK);
				OutputStream os = response.getOutputStream();
				//获取bbox
				Envelope envelope = JTSUtil.getInnerEnvelope(0, result.getCoordinates());
				Rectangle rectangle = new Rectangle(0, 0, width, height);
				GridEnvelope2D gridEnvelope = new GridEnvelope2D(rectangle);
				AffineTransform2D worldToScree = (AffineTransform2D) RendererUtilities.worldToScreenTransform(envelope, rectangle,CRS.decode("EPSG:4326",true));
				GridGeometry2D gridGeometry2D = new GridGeometry2D(gridEnvelope, worldToScree.inverse(), CRS.decode("EPSG:4326",true));
				tiffReponseFormat.writeImage2OutPutformat(image, gridGeometry2D, os);
				return null;
			} catch (IOException | IllegalArgumentException | FactoryException | TransformException e) {
				throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(), "MapLayerAnalysisController getAnalysisResult has error:" + e.getMessage(), e.getCause());
			}
		}
		resultObject.setDemArray(result.getDemArray());
		return new ReponseEntity<AnalysisResultObject>(ReponseStatus.SUCCESS.getCode(),
													   ReponseStatus.SUCCESS.getResult(),
													    resultObject);
	}
	
	@ApiOperation(value="停止分析",notes="停止分析")
	@ApiImplicitParam(name="taskId",value="任务id",required=true,paramType="path")
	@GetMapping("/stop/{taskId}")
	public ReponseEntity<String> stopAnalysisTask(@PathVariable("taskId") int taskId){
		try {
			mapLayerService.stopAnalysisTask(taskId);
			return new ReponseEntity<String>(ReponseStatus.SUCCESS.getCode(), 
											 ReponseStatus.SUCCESS.getResult(),
											 "停止成功！");
		} catch (Exception e) {
			throw new BaseException(ExceptionsUtil.ERROR_500, "MapLayerAnalysisController stopAnalysisTask has error:" + e.getMessage());
		}
	}
	
	
	private String queryServerUrl(HttpServletRequest req) {
		StringBuffer sb = new StringBuffer(req.getScheme());
        sb.append("://").append(req.getServerName()).append(":").append(req.getServerPort())
        .append(req.getContextPath()).append("/");
        return sb.toString();
	}
	
	
	/**
	 * 获取单个点的坡度值
	 * @param layer 图层名称
	 * @param lon   经度
	 * @param lat   纬度
	 * @return
	 */
	@GetMapping("/slope/position")
	@ApiOperation(value = "点类型坡度分析" ,notes = "点类型坡度分析")
	@ApiImplicitParams({
		@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
		@ApiImplicitParam(name="lon",value="经度",required=true,paramType="quety"),
		@ApiImplicitParam(name="lat",value="纬度",required=true,paramType="quety"),
		@ApiImplicitParam(name="format",value="返回类型",required=false,paramType="quety")
		})
	public ReponseEntity<AnalysisResultObject> getSlopeByPostion(@RequestParam(name="layer",required=true) String layerName,
																 @RequestParam(name="lon",required=true) double lon,
																 @RequestParam(name="lat",required=true) double lat,
																 @RequestParam(name="format",required=false) String format,
																 HttpServletRequest request){
		
		ReferencedEnvelope envelope = mapLayerAnalysisAdvices.getLayerEnvelope(layerName);
		Coordinate coordinate = new Coordinate(lon, lat);
		if (!envelope.intersects(coordinate)) {
			log.error("MapLayerAnalysisController getDemByPostion has error {0}",String.format("计算点坐标[%s,%s]不在当前数据范围内", lon,lat));
			throw new BaseException(ExceptionsUtil.ERROR_500, MessageFormat.format("计算点坐标[{0},{1}]不在当前数据范围内", lon,lat));
		}
		MapLayerAnalysis mapLayerAnalysis = (MapLayerAnalysis) spserviceFactory.findClazzInstance("MapLayerAnalysis");
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
        double dem = 0.0;
        if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")) {
            dem = mapLayerAnalysis.getSlopeByPixel(layerBaseInfo, layerBaseInfo.getNativaBbox(), lon, lat);
        }else {
            dem = mapLayerAnalysis.getPointSlope(layerBaseInfo.getFilePath(), layerBaseInfo.getNativaBbox(), lon, lat);
        }
		AnalysisResultObject result = new AnalysisResultObject();
		if (StringUtils.isNotBlank(format) && "application/json".equals(format)) {
			Object geometryStr = MapServerUtils.getgeometryObject(0,coordinate);
			result.setGeometry(geometryStr);
		}
		result.setDemArray(new double[] {dem});
		return new ReponseEntity<AnalysisResultObject>(ReponseStatus.SUCCESS.getCode(), 
													    ReponseStatus.SUCCESS.getResult(),
													    result);
	}
	
	
	/**
	 * 根据起点，终点以及采样信息计算坡度值
	 * @param layer  图层名称
	 * @param startPos 起点
	 * @param endPos  终点
	 * @param stride  间隔(单位m)
	 * @param format  返回类型（application/array,application/json）
	 * @return
	 */
	@GetMapping("/slope/line")
	@ApiOperation(value="线类型坡度分析",notes="线类型坡度分析")
	@ApiImplicitParams({
			@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
			@ApiImplicitParam(name="startPos",value="起点坐标",required=true,paramType="quety"),
			@ApiImplicitParam(name="endPos",value="终点坐标",required=true,paramType="quety"),
			@ApiImplicitParam(name="sampleType",value="采样类型（0）",required=true,paramType="quety"),
			@ApiImplicitParam(name="stride",value="间隔(单位m)",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNum",value="采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="format",value="返回类型（application/array,application/json）",required=false,paramType="quety")
			})
	public ReponseEntity<AnalysisTaskParam> getSlopeByLinePoint(@RequestParam(name="layer",required=true) String layerName,
												   				@RequestParam(name="startpos",required=true) String startPos,
												   				@RequestParam(name="endpos",required=true) String endPos,
												   				@RequestParam(name="sampleType",required=true) int sampleType,
												   				@RequestParam(name="stride",required=false,defaultValue="0.0") double stride,
												   				@RequestParam(name="samplenum",required=false,defaultValue="0") int sampleNum,
												   				@RequestParam(name="format",required=false) String format,
												   				HttpServletRequest request){
		final AnalysisTaskParam taskParam;
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		if (sampleType == 0) {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getLineFromSampleNum(layerName,startPos,endPos,sampleNum,format,17);
			}else {
				taskParam = mapLayerService.getLineFromSampleNum(layerName, startPos, endPos, sampleNum, format, 5);
			}
		}else {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getLineFromStride(layerName, startPos, endPos, stride, format, 18);
			}else {
				taskParam = mapLayerService.getLineFromStride(layerName, startPos, endPos, stride, format, 6);
			}
		}
		String baseUrl = queryServerUrl(request);
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("analysis/result/").append(taskParam.getTaskId());
		taskParam.setResultUrl(sb.toString());
		return new ReponseEntity<AnalysisTaskParam>(ReponseStatus.SUCCESS.getCode(), 
												    ReponseStatus.SUCCESS.getResult(),
												    taskParam);
	}
	
	
	
	/**
	 * 计算矩形区域的坡度值
	 * @param layerName 图层信息
	 * @param leftUpperPos 左上方点
	 * @param rightLowerPos 右下方点
	 * @param sampleType  采样类型
	 * @param strideX     X方向步幅
	 * @param strideY     Y方向步幅
	 * @param sampleNumX  X方向采样点数
	 * @param sampleNumY  Y方向采样点数
	 * @param format      格式
	 * @param rep
	 * @return
	 */
	@GetMapping("/slope/region")
	@ApiOperation(value="矩形类型坡度分析",notes="矩形类型坡度分析")
	@ApiImplicitParams({
			@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
			@ApiImplicitParam(name="leftUpperPos",value="左上方点",required=true,paramType="quety"),
			@ApiImplicitParam(name="rightLowerPos",value="右上方点",required=true,paramType="quety"),
			@ApiImplicitParam(name="sampleType",value="采样类型（0）",required=true,paramType="quety"),
			@ApiImplicitParam(name="strideX",value="X方向步幅",required=false,paramType="quety"),
			@ApiImplicitParam(name="strideY",value="Y方向步幅",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNumX",value="X方向采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNumY",value="Y方向采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="format",value="返回类型（application/array,application/json，image/tiff）",required=false,paramType="quety")
			})
	public ReponseEntity<AnalysisTaskParam> getSlopeByRegion(@RequestParam(name="layer",required=true) String layerName,
														   @RequestParam(name="leftUpperPos",required=true) String leftUpperPos,
														   @RequestParam(name="rightLowerPos",required=true) String rightLowerPos,
														   @RequestParam(name="sampleType",required=true) int sampleType,
														   @RequestParam(name="strideX",required=false,defaultValue="0.0") double strideX,
														   @RequestParam(name="strideY",required=false,defaultValue="0.0") double strideY,
														   @RequestParam(name="sampleNumX",required=false,defaultValue="0") int sampleNumX,
														   @RequestParam(name="sampleNumY",required=false,defaultValue="0") int sampleNumY,
														   @RequestParam(name="format",required=false) String format,
														   HttpServletRequest request){
		
		final AnalysisTaskParam taskParam;
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		if (sampleType == 0) {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getRectangleFromSampleNum(layerName, leftUpperPos, rightLowerPos, sampleNumX, sampleNumY, format, 19);
			}else {
				taskParam = mapLayerService.getRectangleFromSampleNum(layerName, leftUpperPos, rightLowerPos, sampleNumX, sampleNumY, format, 7);
			}
		}else {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getRectangleFromStride(layerName, leftUpperPos, rightLowerPos, sampleNumX, sampleNumY, format, 20);
			}else {
				taskParam = mapLayerService.getRectangleFromStride(layerName, leftUpperPos, rightLowerPos, strideX, strideY, format, 8);
			}
		}
		String baseUrl = queryServerUrl(request);
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("analysis/result/").append(taskParam.getTaskId());
		taskParam.setResultUrl(sb.toString());
		return new ReponseEntity<AnalysisTaskParam>(ReponseStatus.SUCCESS.getCode(), 
												    ReponseStatus.SUCCESS.getResult(),
												    taskParam);
	}
	
	
	/**
	 * 获取单个点的坡向值
	 * @param layer 图层名称
	 * @param lon   经度
	 * @param lat   纬度
	 * @return
	 */
	@GetMapping("/aspect/position")
	@ApiOperation(value = "点类型坡向分析" ,notes = "点类型坡向分析")
	@ApiImplicitParams({
		@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
		@ApiImplicitParam(name="lon",value="经度",required=true,paramType="quety"),
		@ApiImplicitParam(name="lat",value="纬度",required=true,paramType="quety"),
		@ApiImplicitParam(name="format",value="返回类型",required=false,paramType="quety")
		})
	public ReponseEntity<AnalysisResultObject> getAspectByPostion(@RequestParam(name="layer",required=true) String layerName,
																 @RequestParam(name="lon",required=true) double lon,
																 @RequestParam(name="lat",required=true) double lat,
																 @RequestParam(name="format",required=false) String format,
																 HttpServletRequest request){
		
		ReferencedEnvelope envelope = mapLayerAnalysisAdvices.getLayerEnvelope(layerName);
		Coordinate coordinate = new Coordinate(lon, lat);
		if (!envelope.intersects(coordinate)) {
			log.error("MapLayerAnalysisController getAspectByPostion has error {0}",String.format("计算点坐标[%s,%s]不在当前数据范围内", lon,lat));
			throw new BaseException(ExceptionsUtil.ERROR_500, MessageFormat.format("计算点坐标[{0},{1}]不在当前数据范围内", lon,lat));
		}
		MapLayerAnalysis mapLayerAnalysis = (MapLayerAnalysis) spserviceFactory.findClazzInstance("MapLayerAnalysis");
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		double dem = 0.0;
        if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")) {
            dem = mapLayerAnalysis.getAspectByPixel(layerBaseInfo, layerBaseInfo.getNativaBbox(), lon, lat);
        }else {
            dem = mapLayerAnalysis.getPointAspect(layerBaseInfo.getFilePath(), layerBaseInfo.getNativaBbox(), lon, lat);
        }
		AnalysisResultObject result = new AnalysisResultObject();
		if (StringUtils.isNotBlank(format) && "application/json".equals(format)) {
			Object geometryStr = MapServerUtils.getgeometryObject(0,coordinate);
			result.setGeometry(geometryStr);
		}
		result.setDemArray(new double[] {dem});
		return new ReponseEntity<AnalysisResultObject>(ReponseStatus.SUCCESS.getCode(), 
													    ReponseStatus.SUCCESS.getResult(),
													    result);
	}
	
	
	/**
	 * 根据起点，终点以及采样信息计算坡向值
	 * @param layer  图层名称
	 * @param startPos 起点
	 * @param endPos  终点
	 * @param stride  间隔(单位m)
	 * @param format  返回类型（application/array,application/json）
	 * @return
	 */
	@GetMapping("/aspect/line")
	@ApiOperation(value="线类型坡向分析",notes="线类型坡向分析")
	@ApiImplicitParams({
			@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
			@ApiImplicitParam(name="startPos",value="起点坐标",required=true,paramType="quety"),
			@ApiImplicitParam(name="endPos",value="终点坐标",required=true,paramType="quety"),
			@ApiImplicitParam(name="sampleType",value="采样类型（0）",required=true,paramType="quety"),
			@ApiImplicitParam(name="stride",value="间隔(单位m)",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNum",value="采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="format",value="返回类型（application/array,application/json）",required=false,paramType="quety")
			})
	public ReponseEntity<AnalysisTaskParam> getAspectByLinePoint(@RequestParam(name="layer",required=true) String layerName,
												   				@RequestParam(name="startpos",required=true) String startPos,
												   				@RequestParam(name="endpos",required=true) String endPos,
												   				@RequestParam(name="sampleType",required=true) int sampleType,
												   				@RequestParam(name="stride",required=false,defaultValue="0.0") double stride,
												   				@RequestParam(name="samplenum",required=false,defaultValue="0") int sampleNum,
												   				@RequestParam(name="format",required=false) String format,
												   				HttpServletRequest request){
		final AnalysisTaskParam taskParam;
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		if (sampleType == 0) {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getLineFromSampleNum(layerName, startPos, endPos, sampleNum, format, 21);
			}else {
				taskParam = mapLayerService.getLineFromSampleNum(layerName, startPos, endPos, sampleNum, format, 9);
			}
		}else {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getLineFromStride(layerName, startPos, endPos, stride, format, 22);
			}else {
				taskParam = mapLayerService.getLineFromStride(layerName, startPos, endPos, stride, format, 10);
			}
		}
		String baseUrl = queryServerUrl(request);
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("analysis/result/").append(taskParam.getTaskId());
		taskParam.setResultUrl(sb.toString());
		return new ReponseEntity<AnalysisTaskParam>(ReponseStatus.SUCCESS.getCode(), 
												    ReponseStatus.SUCCESS.getResult(),
												    taskParam);
	}
	
	
	
	/**
	 * 计算矩形区域的坡向值
	 * @param layerName 图层信息
	 * @param leftUpperPos 左上方点
	 * @param rightLowerPos 右下方点
	 * @param sampleType  采样类型
	 * @param strideX     X方向步幅
	 * @param strideY     Y方向步幅
	 * @param sampleNumX  X方向采样点数
	 * @param sampleNumY  Y方向采样点数
	 * @param format      格式
	 * @param rep
	 * @return
	 */
	@GetMapping("/aspect/region")
	@ApiOperation(value="矩形类型坡向分析",notes="矩形类型坡向分析")
	@ApiImplicitParams({
			@ApiImplicitParam(name="layerName",value="图层名称",required=true,paramType="quety"),
			@ApiImplicitParam(name="leftUpperPos",value="左上方点",required=true,paramType="quety"),
			@ApiImplicitParam(name="rightLowerPos",value="右上方点",required=true,paramType="quety"),
			@ApiImplicitParam(name="sampleType",value="采样类型（0）",required=true,paramType="quety"),
			@ApiImplicitParam(name="strideX",value="X方向步幅",required=false,paramType="quety"),
			@ApiImplicitParam(name="strideY",value="Y方向步幅",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNumX",value="X方向采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="sampleNumY",value="Y方向采样点数",required=false,paramType="quety"),
			@ApiImplicitParam(name="format",value="返回类型（application/array,application/json，image/tiff）",required=false,paramType="quety")
			})
	public ReponseEntity<AnalysisTaskParam> getAspectByRegion(@RequestParam(name="layer",required=true) String layerName,
														   @RequestParam(name="leftUpperPos",required=true) String leftUpperPos,
														   @RequestParam(name="rightLowerPos",required=true) String rightLowerPos,
														   @RequestParam(name="sampleType",required=true) int sampleType,
														   @RequestParam(name="strideX",required=false,defaultValue="0.0") double strideX,
														   @RequestParam(name="strideY",required=false,defaultValue="0.0") double strideY,
														   @RequestParam(name="sampleNumX",required=false,defaultValue="0") int sampleNumX,
														   @RequestParam(name="sampleNumY",required=false,defaultValue="0") int sampleNumY,
														   @RequestParam(name="format",required=false) String format,
														   HttpServletRequest request){
		
		final AnalysisTaskParam taskParam;
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		if (sampleType == 0) {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getRectangleFromSampleNum(layerName, leftUpperPos, rightLowerPos, sampleNumX, sampleNumY, format, 23);
			}else {
				taskParam = mapLayerService.getRectangleFromSampleNum(layerName, leftUpperPos, rightLowerPos, sampleNumX, sampleNumY, format, 11);
			}
		}else {
			if (layerBaseInfo.getFileType().equals("TMS")||layerBaseInfo.getFileType().equals("TDB")){
				taskParam = mapLayerService.getRectangleFromStride(layerName, leftUpperPos, rightLowerPos, strideX, strideY, format, 24);
			}else {
				taskParam = mapLayerService.getRectangleFromStride(layerName, leftUpperPos, rightLowerPos, strideX, strideY, format, 12);
			}
		}
		String baseUrl = queryServerUrl(request);
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("analysis/result/").append(taskParam.getTaskId());
		taskParam.setResultUrl(sb.toString());
		return new ReponseEntity<AnalysisTaskParam>(ReponseStatus.SUCCESS.getCode(), 
												    ReponseStatus.SUCCESS.getResult(),
												    taskParam);
	}
}
