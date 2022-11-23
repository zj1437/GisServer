package org.pygisserver.terrainanalysis.service;

import org.pygisserver.common.entity.AnalysisResult;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.spi.service.PySpiService;
import org.gdal.gdal.Dataset;

public interface MapLayerAnalysis extends PySpiService {
	
	/**
	 * 计算某一个点的高程值
	 * @param fileUrl   文件路径
	 * @param nativeBbox  当前数据的bbox，中间以分号隔扣
	 * @param longitude  点经度
	 * @param latitude   点纬度
	 * @return
	 */
	double getDemByPosition(String fileUrl,String nativeBbox,double longitude,double latitude);

	/**
	 * 从瓦片中获取某一个像素点的高程值
	 * @param layerBaseInfo   图层信息携带实体
	 * @param nativeBbox  当前数据的bbox，中间以分号隔扣
	 * @param longitude  点经度
	 * @param latitude   点纬度
	 * @return
	 */
	double getDemByPixel(LayerBaseInfo layerBaseInfo, String nativeBbox,double longitude,double latitude);

	/**
	 * 根据起点和终点以及采样间隔点数获取线段上的高程值（必须返回起点和终点的高程）（采样点分析）
	 * @param fileUrl 图层名称
	 * @param startPos 起始点
	 * @param startPos 终点
	 * @param sampingNum  采样点数
	 * @return
	 */
	void getDemByLinePoint(String fileUrl,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult);

	/**
	 * 根据起点和终点以及采样间隔来获取线段上的高程值（必须返回起点和终点的高程）（采样间隔分析）
	 * @param fileUrl	文件路径
	 * @param nativeBbox 数据的bbox
	 * @param startPos 起始点
	 * @param endPos   终点
	 * @param stride   采样间距
	 * @return
	 */
	void getDemByLinePoint(String fileUrl,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult);

	/**
	 * 根据起点和终点以及采样间隔点数获取线段上瓦片数据的像素点的高程值（必须返回起点和终点的高程）（采样点数分析）
	 * @param layerBaseInfo   图层信息携带实体
	 * @param startPos 起始点
	 * @param startPos 终点
	 * @param sampingNum  采样点数
	 * @return
	 */
	void getTileDemByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult);

	/**
	 * 根据起点和终点以及采样间隔来获取线段上瓦片数据的像素点的高程值（必须返回起点和终点的高程）（采样间隔分析）
	 * @param layerBaseInfo   图层信息携带实体
	 * @param nativeBbox 数据的bbox
	 * @param startPos 起始点
	 * @param endPos   终点
	 * @param stride   采样间距
	 * @return
	 */
	void getTileDemByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的高程数据（采样点数分析）
	 * @param fileUrl 文件url
	 * @param nativeBbox 数据bbox
	 * @param leftUpperPos  左上角
	 * @param rightLowerPos 右下角
	 * @param sampleXCount  水平采样点数
	 * @param sampleYCount  垂直采样点数
	 * @return
	 */
	void getRegionDemBySampingNum(String fileUrl,String nativeBbox,String leftUpperPos,String rightLowerPos,int sampleXCount,int sampleYCount,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的高程数据（采样间隔分析）
	 * @param fileUrl 文件url
	 * @param nativeBbox 数据bbox
	 * @param leftUpperPos  左上角
	 * @param rightLowerPos 右下角
	 * @param strideX  水平采样间距
	 * @param strideY  垂直采样间距
	 * @return
	 */
	void getRegionDemBySampingRatio(String fileUrl,String nativeBbox,String leftUpperPos,String rightLowerPos,double strideX,double strideY,AnalysisResult analysisResult);

	/**
	 *
	 * 根据采样点数获取矩形上的瓦片数据的像素点的高程值（采样点数分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param leftUpperPos  左上角
	 * 	 @param rightLowerPos 右下角
	 * 	 @param sampleXCount  水平采样点数
	 * 	 @param sampleYCount  垂直采样点数
	 * 	 @return
	 */
	void getTileDemBySampingNum(LayerBaseInfo layerBaseInfo, String nativeBbox, String leftUpperPos, String rightLowerPos, int sampleXCount, int sampleYCount, AnalysisResult analysis);

	/**
	 *
	 * 根据采样点间距获取矩形上的瓦片数据的像素点的高程值（采样间隔分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param leftUpperPos  左上角
	 * 	 @param rightLowerPos 右下角
	 * 	 @param strideX  水平采样间距
	 * 	 @param strideY  垂直采样间距
	 * 	 @return
	 */
	void getTileDemBySampingRatio(LayerBaseInfo layerBaseInfo, String nativeBbox, String leftUpperPos, String rightLowerPos, double strideX, double strideY, AnalysisResult analysis);

	/**
	 * 计算某一个点的坡度值
	 * @param fileUrl 文件路径
	 * @param nativeBbox  数据bbox
	 * @param longitude   点经度
	 * @param latitude   点纬度
	 */
	double getPointSlope(String fileUrl,String nativeBbox,double longitude,double latitude);

	/**
	 * 从瓦片中获取某一个像素点的坡度值
	 * @param layerBaseInfo   图层信息携带实体
	 * @param nativeBbox  当前数据的bbox，中间以分号隔扣
	 * @param longitude  点经度
	 * @param latitude   点纬度
	 * @return
	 */
	double getSlopeByPixel(LayerBaseInfo layerBaseInfo, String nativeBbox, double longitude, double latitude);

	/**
	 * 根据起点和终点以及采样间隔点数获取线段上的坡度值（必须返回起点和终点的高程）（采样点数分析）
	 * @param fileUrl 文件路径
	 * @param startPos 起始点
	 * @param startPos 终点
	 * @param sampingNum  采样点数
	 * @return
	 */
	void getSlopeByLinePoint(String fileUrl,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult);
	
	/**
	 * 根据起点和终点以及采样间隔来获取线段上的坡度值（必须返回起点和终点的高程）（采样间隔分析）
	 * @param fileUrl  文件路径
	 * @param nativeBbox 数据的bbox
	 * @param startPos 起始点
	 * @param endPos   终点
	 * @param stride   采样间距
	 * @return
	 */
	void getSlopeByLinePoint(String fileUrl,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取线段上的瓦片数据的像素点的坡度值（采样点数分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param startPos  起始点
	 * 	 @param endPos   终点
	 * 	 @param sampingNum	采样点数
	 * 	 @return
	 */
	void getTileSlopeByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取线段上的瓦片数据的像素点的坡度值（采样间隔分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param startPos  起始点
	 * 	 @param endPos   终点
	 * 	 @param stride	采样间距
	 * 	 @return
	 */
	void getTileSlopeByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的坡度值（采样点数分析）
	 * @param fileUrl 文件url
	 * @param nativeBbox 数据bbox
	 * @param leftUpperPos  左上角
	 * @param rightLowerPos 右下角
	 * @param sampleXCount  水平采样点数
	 * @param sampleYCount  垂直采样点数
	 * @return
	 */
	void getSlopeBySampingNum(String fileUrl,String nativeBbox,String leftUpperPos,String rightLowerPos,int sampleXCount,int sampleYCount,AnalysisResult analysisResult);
	
	/**
	 * 根据采样间距获取矩形上的坡度值（采样间隔分析）
	 * @param nativeBbox
	 * @param leftUpperPos
	 * @param rightLowerPos
	 * @param strideX
	 * @param strideY
	 * @return
	 */
	void getSlopeBySampingRatio(String fileUrl,String nativeBbox,String leftUpperPos,String rightLowerPos,double strideX,double strideY,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的瓦片数据的像素点的坡度值（采样点数分析）
	 * @param layerBaseInfo 图层信息携带实体
	 * @param nativeBbox 数据bbox
	 * @param leftUpperPos  左上角
	 * @param rightLowerPos 右下角
	 * @param sampleXCount  水平采样点数
	 * @param sampleYCount  垂直采样点数
	 * @return
	 */
	void getTileSlopeBySampingNum(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,int sampleXCount,int sampleYCount,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的瓦片数据的像素点的坡度值（采样间隔分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param leftUpperPos  左上角
	 * 	 @param rightLowerPos 右下角
	 * 	 @param strideX  水平采样间距
	 * 	 @param strideY  垂直采样间距
	 * 	 @return
	 */
	void getTileSlopeBySampingRatio(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,double strideX,double strideY,AnalysisResult analysisResult);

	/**
	 * 计算某一个点的坡向值
	 * @param fileUrl 文件url
	 * @param nativeBbox 数据源bbox
	 * @param longitude  经度
	 * @param latitude   纬度
	 */
	double getPointAspect(String fileUrl,String nativeBbox,double longitude,double latitude);

	/**
	 * 从瓦片中获取单个像素点的坡向值
	 * @param layerBaseInfo   图层信息携带实体
	 * @param nativeBbox  当前数据的bbox，中间以分号隔扣
	 * @param longitude  点经度
	 * @param latitude   点纬度
	 * @return
	 */
	double getAspectByPixel(LayerBaseInfo layerBaseInfo, String nativeBbox, double longitude, double latitude);

	/**
	 * 根据起点和终点以及采样间隔点数获取线段上的坡向值（必须返回起点和终点的高程）（采样点数分析）
	 * @param fileUrl 文件路径
	 * @param startPos 起始点
	 * @param startPos 终点
	 * @param sampingNum  采样点数
	 * @return
	 */
	void getAspectByLinePoint(String fileUrl,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult);
	
	/**
	 * 根据起点和终点以及采样间隔来获取线段上的坡向值（必须返回起点和终点的高程）（采样间隔分析）
	 * @param fileUrl  文件路径
	 * @param nativeBbox 数据的bbox
	 * @param startPos 起始点
	 * @param endPos   终点
	 * @param stride   采样间距
	 * @return
	 */
	void getAspectByLinePoint(String fileUrl,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取线段上的瓦片数据的像素点的坡向值（采样点数分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param startPos  起始点
	 * 	 @param endPos   终点
	 * 	 @param sampingNum	采样点数
	 * 	 @return
	 */
	void getTileAspectByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox, String startPos,String endPos,int sampingNum,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取线段上的瓦片数据的像素点的坡向值（采样间隔分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param startPos  起始点
	 * 	 @param endPos   终点
	 * 	 @param stride	采样间距
	 * 	 @return
	 */
	void getTileAspectByLinePoint(LayerBaseInfo layerBaseInfo,String nativeBbox,String startPos,String endPos,double stride,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的坡向值（采样点数分析）
	 * @param fileUrl 文件url
	 * @param nativeBbox 数据bbox
	 * @param leftUpperPos  左上角
	 * @param rightLowerPos 右下角
	 * @param sampleXCount  水平采样点数
	 * @param sampleYCount  垂直采样点数
	 * @return
	 */
	void getAspectBySampingNum(String fileUrl,String nativeBbox,String leftUpperPos,String rightLowerPos,int sampleXCount,int sampleYCount,AnalysisResult analysisResult);
	
	/**
	 * 根据采样间距获取矩形上的坡向值（采样间隔分析）
	 * @param nativeBbox
	 * @param leftUpperPos
	 * @param rightLowerPos
	 * @param strideX
	 * @param strideY
	 * @return
	 */
	void getAspectBySampingRatio(String fileUrl,String nativeBbox,String leftUpperPos,String rightLowerPos,double strideX,double strideY,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的瓦片数据的像素点的坡向值（采样点数分析）
	 * @param layerBaseInfo 图层信息携带实体
	 * @param nativeBbox 数据bbox
	 * @param leftUpperPos  左上角
	 * @param rightLowerPos 右下角
	 * @param sampleXCount  水平采样点数
	 * @param sampleYCount  垂直采样点数
	 * @return
	 */
	void getTileAspectBySampingNum(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,int sampleXCount,int sampleYCount,AnalysisResult analysisResult);

	/**
	 * 根据采样点数获取矩形上的瓦片数据的像素点的坡向值（采样间隔分析）
	 * 	 @param layerBaseInfo   图层信息携带实体
	 * 	 @param nativeBbox 数据bbox
	 * 	 @param leftUpperPos  左上角
	 * 	 @param rightLowerPos 右下角
	 * 	 @param strideX  水平采样间距
	 * 	 @param strideY  垂直采样间距
	 * 	 @return
	 */
	void getTileAspectBySampingRatio(LayerBaseInfo layerBaseInfo,String nativeBbox,String leftUpperPos,String rightLowerPos,double strideX,double strideY,AnalysisResult analysisResult);

	Dataset getDataSetByFilePath(String fileUrl);
	
}
