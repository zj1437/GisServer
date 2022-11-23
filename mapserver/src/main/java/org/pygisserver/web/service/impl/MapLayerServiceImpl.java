package org.pygisserver.web.service.impl;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.AnalysisResult;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.web.aop.MapLayerAnalysisAdvices;
import org.pygisserver.terrainanalysis.service.MapLayerAnalysis;
import org.pygisserver.web.entity.AnalysisTaskParam;
import org.pygisserver.web.factory.PySpiServiceFactory;
import org.pygisserver.web.service.MapLayerService;
import org.pygisserver.web.thread.AnalysisTask;
import org.pygisserver.web.websocket.WebSocketManager;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MapLayerServiceImpl implements MapLayerService {

	@Autowired
	private MapLayerAnalysisAdvices mapLayerAnalysisAdvices;

	@Resource
	private PySpiServiceFactory spserviceFactory;

	@Autowired
	private WebSocketManager webSocketManager;

	private static ExecutorService excutorService = Executors.newFixedThreadPool(60);

	private Map<Integer, AnalysisResult> currentPool = new TreeMap<Integer, AnalysisResult>();

	private Map<Integer, CompletableFuture> futurePool = new TreeMap<Integer, CompletableFuture>();

	private AtomicInteger currentId = new AtomicInteger();

	private static GeometryFactory geomtryFactory = JTSFactoryFinder.getGeometryFactory();
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Override
	public AnalysisTaskParam getRectangleFromSampleNum(String layerName, String leftUpperPos, String rightLowerPos,
			int sampleNumX, int sampleNumY, String format,int analysisType) {
		validRegionParam(layerName, leftUpperPos, rightLowerPos);
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		// 提交任务
		AnalysisTaskParam analysisTask = new AnalysisTaskParam();
		analysisTask.setTaskType(analysisType); // 矩形分析 根据采样点数
		analysisTask.setFileUrl(layerBaseInfo.getFilePath());
		analysisTask.setNativeBbox(layerBaseInfo.getNativaBbox());
		int taskId = currentId.incrementAndGet();
		analysisTask.setTaskId(taskId);
		analysisTask.setTaskName(createTileTaskName());
		Map<String, Object> analysisMap = new HashMap<String, Object>();
		analysisMap.put("leftUpperPos", leftUpperPos);
		analysisMap.put("rightLowerPos", rightLowerPos);
		analysisMap.put("sampleNumX", sampleNumX);
		analysisMap.put("sampleNumY", sampleNumY);
		analysisTask.setMapParam(analysisMap);
		analysisTask.setFormat(format);
		AnalysisResult analysisResult = new AnalysisResult();
		analysisResult.setTaskId(taskId);
		analysisTask.setLayerBaseInfo(layerBaseInfo);
		MapLayerAnalysis mapLayerAnalysis = (MapLayerAnalysis) spserviceFactory.findClazzInstance("MapLayerAnalysis");
		//监听结果分析结果是否完成
		CompletableFuture.supplyAsync(() -> {
			AnalysisTask task = new AnalysisTask(mapLayerAnalysis, analysisTask, analysisResult);
			AnalysisResult asyncResult = null;
			try {
				asyncResult = task.call(analysisTask);
			} catch (Exception e) {
				log.error("MapLayerServiceImpl getRectangleFromSampleNum has error {}",e.getLocalizedMessage());
			}
			return asyncResult;
		}, excutorService).whenComplete((result, r) -> {
			JSONObject jsonObject = new JSONObject();
			if (result.getAnalysisStatus() == 2) {
				jsonObject.put("msg", "分析已完成;" + result.getTaskId());
				jsonObject.put("msgKey", "mapServer");
				currentPool.put(taskId,result);
				webSocketManager.sendMessage(jsonObject.toJSONString());
			}
		});

		return analysisTask;
	}

	/**
	 * taskName 为当前时间戳的后四位
	 * 
	 * @param tileTaskInstance
	 * @return
	 */
	private String createTileTaskName() {
		Random random = new Random();
		long t1 = 0x7FFFFFFF & System.currentTimeMillis();
		long uuid = t1 << 32 | Math.abs(random.nextInt());
		String uuidStr = Long.toString(uuid);
		return uuidStr.substring(uuidStr.length() - 5, uuidStr.length());
	}

	@Override
	public AnalysisTaskParam getRectangleFromStride(String layerName, String leftUpperPos, String rightLowerPos,
			double strideX, double strideY, String format,int analysisType) {

		validRegionParam(layerName, leftUpperPos, rightLowerPos);
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		// 提交任务
		AnalysisTaskParam analysisTask = new AnalysisTaskParam();
		analysisTask.setTaskType(analysisType); // 矩形分析 根据采样点数
		analysisTask.setFileUrl(layerBaseInfo.getFilePath());
		analysisTask.setNativeBbox(layerBaseInfo.getNativaBbox());
		int taskId = currentId.incrementAndGet();
		analysisTask.setTaskId(taskId);
		analysisTask.setTaskName(createTileTaskName());
		Map<String, Object> analysisMap = new HashMap<String, Object>();
		analysisMap.put("leftUpperPos", leftUpperPos);
		analysisMap.put("rightLowerPos", rightLowerPos);
		analysisMap.put("strideX", strideX);
		analysisMap.put("strideY", strideY);
		analysisTask.setMapParam(analysisMap);
		analysisTask.setFormat(format);
		AnalysisResult analysisResult = new AnalysisResult();
		analysisResult.setTaskId(taskId);
		MapLayerAnalysis mapLayerAnalysis = (MapLayerAnalysis) spserviceFactory.findClazzInstance("MapLayerAnalysis");
		//监听结果分析结果是否完成
		CompletableFuture.supplyAsync(() -> {
			AnalysisTask task = new AnalysisTask(mapLayerAnalysis, analysisTask, analysisResult);
			AnalysisResult asyncResult = null;
			try {
				asyncResult = task.call(analysisTask);
			} catch (Exception e) {
				log.error("MapLayerServiceImpl getRectangleFromStride has error {}",e.getLocalizedMessage());
			}
			return asyncResult;
		}, excutorService).whenComplete((result, r) -> {
			JSONObject jsonObject = new JSONObject();
			if (result.getAnalysisStatus() == 2) {
				jsonObject.put("msg", "分析已完成;" + result.getTaskId());
				jsonObject.put("msgKey", "mapServer");
				currentPool.put(taskId, result);
				webSocketManager.sendMessage(jsonObject.toJSONString());
			}
		});
		// 启动线程监测当前任务是否已经完成
		// if (!inspectFlag) {
		// new Thread(new AnalysisTaskInspect(webSocketManager)).start();
		// }
		return analysisTask;
	}

	public void validRegionParam(String layerName, String leftUpperPos, String rightLowerPos) {
		ReferencedEnvelope envelope = mapLayerAnalysisAdvices.getLayerEnvelope(layerName);
		if (leftUpperPos.indexOf(",") < 0 || rightLowerPos.indexOf(",") < 0) {
			throw new BaseException(ExceptionsUtil.ERROR_400,
					ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "起点或者终点坐标"));
		}
		String[] startPointArray = leftUpperPos.split(",");
		String[] endPointArray = rightLowerPos.split(",");
		Coordinate startCoor = new Coordinate(Double.parseDouble(startPointArray[0]),
				Double.parseDouble(startPointArray[1]));
		Coordinate endCoor = new Coordinate(Double.parseDouble(endPointArray[0]), Double.parseDouble(endPointArray[1]));
		if (!envelope.intersects(startCoor) && !envelope.intersects(endCoor)) {
			throw new BaseException(ExceptionsUtil.ERROR_500, MessageFormat.format("当前请求的数据范围[{0},{1},{2},{3}]与原数据无交集",
					startCoor.getX(), startCoor.getY(), endCoor.getX(), endCoor.getY()));
		}
		if (startCoor.equals2D(endCoor)) {
			throw new BaseException(ExceptionsUtil.ERROR_500,
					"起点和终点坐标相同,请输入有效的矩形范围");
		}
	}

	// class AnalysisTaskInspect implements Runnable{
	// private boolean startInsepct = true;
	//
	// private WebSocketManager webSocketManager;
	//
	// public AnalysisTaskInspect(WebSocketManager webSocketManager){
	// this.webSocketManager = webSocketManager;
	// }
	// @Override
	// public void run() {
	// while(startInsepct) {
	// inspectFlag = true;
	// Iterator<Entry<Integer, SubmittedAnalysisTask>> it =
	// currentPool.entrySet().iterator();
	// while(it.hasNext()) {
	// Entry<Integer, SubmittedAnalysisTask> entry = it.next();
	// SubmittedAnalysisTask submittedTask = entry.getValue();
	// try {
	// if (submittedTask.getResult() != null) {
	// AnalysisResult result = submittedTask.getResult().get();
	// int taskStatus = result.getAnalysisStatus();
	// if (taskStatus == 2) {
	// //推送消息，使用
	//// sendMessage(result,submittedTask.getAnalysisTaks().getFormat());
	// completedTasks.add(submittedTask.getAnalysisTaks().getTaskId());
	// }
	// }
	// } catch (InterruptedException | ExecutionException e) {
	// e.printStackTrace();
	// }
	// }
	// if (currentPool.entrySet().size() == 0 || completedTasks.size() ==
	// currentPool.entrySet().size()) {
	// startInsepct = false;
	// inspectFlag = false;
	// }
	// }
	// }

	/**
	 * 推送消息结果
	 * 
	 * @param result
	 * @param format
	 */
	private void sendMessage(AnalysisResult result, String format) {
		if (StringUtils.isNotBlank(format) && "application/json".equals(format)) {
			String message = convertGeojsonFromGeometry(result.getDemArray(), 1, result.getCoordinates());
			webSocketManager.sendMessage(message);
		} else if (StringUtils.isNotBlank(format) && "image/tif".equals(format)) {
			double[] demArray = result.getDemArray();
			webSocketManager.sendMessage(convertBase64Image(demArray, result.getImageWidth(), result.getImageHeight()));
		} else {
			double[] demArray = result.getDemArray();
			webSocketManager.sendMessage(Arrays.toString(demArray));
		}
	}

	private String convertBase64Image(double[] demArray, int width, int height) {
		DataBufferDouble dataBuffer = new DataBufferDouble(demArray, width, height);
		ColorSpace space = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		ComponentSampleModel sampleModle = new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, width, height, 1, width,
				new int[] { 0 });
		int[] bits = { 32 };
		ComponentColorModel color1 = new ComponentColorModel(space, bits, false, false, Transparency.TRANSLUCENT,
				DataBuffer.TYPE_DOUBLE);
		WritableRaster raster = Raster.createWritableRaster(sampleModle, dataBuffer, new Point(0, 0));
		BufferedImage image = new BufferedImage(color1, raster, false, null);
		// 些图片到输出流
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "tiff", bos);
		} catch (IOException e) {
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(),
					"MapLayerServiceImpl convertBase64Image has error:" + e.getMessage(), e.getCause());
		}
		byte[] bufferResource = bos.toByteArray();
		if (bufferResource != null) {
			return Base64.getEncoder().encodeToString(bufferResource);
		}
		return null;
	}

	/**
	 * 将geometry数据转化为geojson对象
	 * 
	 * @param demArray
	 *            高程数组
	 * @param dataType
	 *            数据类型
	 * @param coordinates
	 *            坐标
	 * @return
	 */
	private String convertGeojsonFromGeometry(double[] demArray, int dataType, Coordinate... coordinates) {
		Geometry geometry = null;
		if (dataType == 0) {
			geometry = geomtryFactory.createPoint(coordinates[0]);
		} else if (dataType == 1) {
			geometry = geomtryFactory.createMultiPointFromCoords(coordinates);
		}
		GeometryJSON json = new GeometryJSON(6);
		StringWriter writer = new StringWriter();
		try {
			json.write(geometry, writer);
		} catch (IOException e) {
			log.error("MapLayerAnalysisController convertGeojsonFromGeometry has error:" + e.getLocalizedMessage());
		}
		String geometryStr = writer.toString();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Geometry", JSONObject.parse(geometryStr));
		jsonObject.put("DemArray", demArray);
		return jsonObject.toString();
	}

	@Override
	public void stopAnalysisTask(int taskId) {
		Iterator<Entry<Integer, AnalysisResult>> it = currentPool.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, AnalysisResult> entry = it.next();
			AnalysisResult analysisTask = entry.getValue();
			if (taskId == analysisTask.getTaskId()) {
//				submittedTask.getResult().cancel(true);
				it.remove();
			}
		}
	}

	@Override
	public AnalysisResult getAnalysisResult(int taskId) {
		Iterator<Entry<Integer, AnalysisResult>> it = currentPool.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, AnalysisResult> entry = it.next();
			AnalysisResult result = entry.getValue();
			if (taskId == result.getTaskId() && result.getAnalysisStatus() == 2) {
				AnalysisResult resultClone = result.clone();
				it.remove();  //清理缓存 否则内存会涨
				return resultClone;
			}
		}
		return null;
	}

	@Override
	public AnalysisTaskParam getLineFromSampleNum(String layerName, String startPos, String endPos, int sampleNum,
			String format,int analysisType) {
		validLineParam(layerName, startPos, endPos);
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		// 提交任务
		AnalysisTaskParam analysisTask = new AnalysisTaskParam();
		analysisTask.setTaskType(analysisType); // 线采样 根据采样点数
		analysisTask.setFileUrl(layerBaseInfo.getFilePath());
		analysisTask.setNativeBbox(layerBaseInfo.getNativaBbox());
		int taskId = currentId.incrementAndGet();
		analysisTask.setTaskId(taskId);
		analysisTask.setTaskName(createTileTaskName());
		Map<String, Object> analysisMap = new HashMap<String, Object>();
		analysisMap.put("startPos", startPos);
		analysisMap.put("endPos", endPos);
		analysisMap.put("sampleNum", sampleNum);
		analysisTask.setMapParam(analysisMap);
		analysisTask.setFormat(format);
		analysisTask.setLayerBaseInfo(layerBaseInfo);
		AnalysisResult analysisResult = new AnalysisResult();
		analysisResult.setTaskId(taskId);
		MapLayerAnalysis mapLayerAnalysis = (MapLayerAnalysis) spserviceFactory.findClazzInstance("MapLayerAnalysis");
		//监听结果分析结果是否完成
		CompletableFuture.supplyAsync(() -> {
			AnalysisTask task = new AnalysisTask(mapLayerAnalysis, analysisTask, analysisResult);
			AnalysisResult asyncResult = null;
			try {
				asyncResult = task.call(analysisTask);
			} catch (Exception e) {
				log.error("MapLayerServiceImpl getLineFromSampleNum has error {}",e.getLocalizedMessage());
			}
			return asyncResult;
		}, excutorService).whenComplete((result, r) -> {
			JSONObject jsonObject = new JSONObject();
			if (analysisResult.getAnalysisStatus() == 2) {
				jsonObject.put("msg", "分析已完成;" + analysisResult.getTaskId());
				jsonObject.put("msgKey", "mapServer");
				currentPool.put(taskId, result);
				webSocketManager.sendMessage(jsonObject.toJSONString());
			}
		});
		return analysisTask;
	}

	public void validLineParam(String layerName, String startPos, String endPos) {
		ReferencedEnvelope envelope = mapLayerAnalysisAdvices.getLayerEnvelope(layerName);
		if (startPos.indexOf(",") < 0 || endPos.indexOf(",") < 0) {
			throw new BaseException(ExceptionsUtil.ERROR_400,
					ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, "起点或者终点坐标"));
		}
		String[] startPointArray = startPos.split(",");
		String[] endPointArray = endPos.split(",");
		Coordinate startCoor = new Coordinate(Double.parseDouble(startPointArray[0]),
				Double.parseDouble(startPointArray[1]));
		Coordinate endCoor = new Coordinate(Double.parseDouble(endPointArray[0]), Double.parseDouble(endPointArray[1]));
		if (!envelope.intersects(startCoor)) {
			throw new BaseException(ExceptionsUtil.ERROR_500,
					MessageFormat.format("起点坐标{0},{1}不在当前数据范围内", startCoor.getX(), startCoor.getY()));
		}
		if (!envelope.intersects(endCoor)) {
			throw new BaseException(ExceptionsUtil.ERROR_500,
					MessageFormat.format("终点坐标{0},{1}不在当前数据范围内", endCoor.getX(), endCoor.getY()));
		}
		if (startCoor.equals2D(endCoor)) {
			throw new BaseException(ExceptionsUtil.ERROR_500,
					"起点和终点坐标相同,请输入有效的线段范围");
		}
	}

	@Override
	public AnalysisTaskParam getLineFromStride(String layerName, String startPos, String endPos, double stride,
			String format,int analysisType) {
		// 提交任务
		validLineParam(layerName, startPos, endPos);
		LayerBaseInfo layerBaseInfo = mapLayerAnalysisAdvices.getLayerBaseInfo(layerName);
		AnalysisTaskParam analysisTask = new AnalysisTaskParam();
		analysisTask.setTaskType(analysisType); // 线采样 根据采样间距
		analysisTask.setFileUrl(layerBaseInfo.getFilePath());
		analysisTask.setNativeBbox(layerBaseInfo.getNativaBbox());
		int taskId = currentId.incrementAndGet();
		analysisTask.setTaskId(taskId);
		analysisTask.setTaskName(createTileTaskName());
		Map<String, Object> analysisMap = new HashMap<String, Object>();
		analysisMap.put("startPos", startPos);
		analysisMap.put("endPos", endPos);
		analysisMap.put("stride", stride);
		analysisTask.setMapParam(analysisMap);
		analysisTask.setFormat(format);
		AnalysisResult analysisResult = new AnalysisResult();
		analysisResult.setTaskId(taskId);
		MapLayerAnalysis mapLayerAnalysis = (MapLayerAnalysis) spserviceFactory.findClazzInstance("MapLayerAnalysis");
		//监听结果分析结果是否完成
		CompletableFuture.supplyAsync(() -> {
			AnalysisTask task = new AnalysisTask(mapLayerAnalysis, analysisTask, analysisResult);
			AnalysisResult asyncResult = null;
			try {
				asyncResult = task.call(analysisTask);
			} catch (Exception e) {
				log.error("MapLayerServiceImpl getLineFromStride has error {}",e.getLocalizedMessage());
			}
			return asyncResult;
		}, excutorService).whenComplete((result, r) -> {
			JSONObject jsonObject = new JSONObject();
			if (analysisResult.getAnalysisStatus() == 2) {
				jsonObject.put("msg", "分析已完成;" + analysisResult.getTaskId());
				jsonObject.put("msgKey", "mapServer");
				currentPool.put(taskId, result);
				webSocketManager.sendMessage(jsonObject.toJSONString());
			}
		});
		return analysisTask;
	}
}
