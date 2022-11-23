package org.pygisserver.web.thread;

import java.util.Map;

import org.pygisserver.common.entity.AnalysisResult;
import org.pygisserver.web.entity.AnalysisTaskParam;
import org.pygisserver.terrainanalysis.service.MapLayerAnalysis;

public class AnalysisTask  {

	private MapLayerAnalysis mapLayerAnalysis;
	
	private AnalysisTaskParam analysisParam;
	
	private AnalysisResult analysisResult ;
	
	public AnalysisTask(MapLayerAnalysis mapLayerAnalysis,AnalysisTaskParam analysisParam,AnalysisResult analysisResult) {
		this.analysisParam = analysisParam;
		this.mapLayerAnalysis = mapLayerAnalysis;
		this.analysisResult = analysisResult;
	}

	public AnalysisResult call(AnalysisTaskParam analysisParam) throws Exception {
		//根据分析类型获取对应参数
		analysisResult.setAnalysisStatus(1);  //1 分析进行中
		int analysisType = analysisParam.getTaskType();
		Map<String,Object> paramMap = analysisParam.getMapParam();
		switch (analysisType) {
		
		case 1:
			mapLayerAnalysis.getDemByLinePoint(analysisParam.getFileUrl(), 
											   analysisParam.getNativeBbox(),
											   paramMap.get("startPos").toString(), 
											   paramMap.get("endPos").toString(), 
											   (int)paramMap.get("sampleNum"),
											   analysisResult);
			break;
		case 2:
			mapLayerAnalysis.getDemByLinePoint(analysisParam.getFileUrl(), 
											   analysisParam.getNativeBbox(),
											   paramMap.get("startPos").toString(), 
											   paramMap.get("endPos").toString(), 
											   (double)paramMap.get("stride"),
											   analysisResult);
			break;
		case 3:
			mapLayerAnalysis.getRegionDemBySampingNum(analysisParam.getFileUrl(),
													  analysisParam.getNativeBbox(),
													  paramMap.get("leftUpperPos").toString(), 
													  paramMap.get("rightLowerPos").toString(),
													  (int)paramMap.get("sampleNumX"),
													  (int)paramMap.get("sampleNumY"),
													  analysisResult);
			break;
		case 4:
			mapLayerAnalysis.getRegionDemBySampingRatio(analysisParam.getFileUrl(),
													     analysisParam.getNativeBbox(),
													     paramMap.get("leftUpperPos").toString(), 
													     paramMap.get("rightLowerPos").toString(),
													     (double)paramMap.get("strideX"),
													     (double)paramMap.get("strideY"),
													     analysisResult);
			
			break;
		case 5:
			mapLayerAnalysis.getSlopeByLinePoint(analysisParam.getFileUrl(), 
											   analysisParam.getNativeBbox(),
											   paramMap.get("startPos").toString(), 
											   paramMap.get("endPos").toString(), 
											   (int)paramMap.get("sampleNum"),
											   analysisResult);
			break;
		case 6:
			mapLayerAnalysis.getSlopeByLinePoint(analysisParam.getFileUrl(), 
											   analysisParam.getNativeBbox(),
											   paramMap.get("startPos").toString(), 
											   paramMap.get("endPos").toString(), 
											   (double)paramMap.get("stride"),
											   analysisResult);
			break;
			
		case 7:
			mapLayerAnalysis.getSlopeBySampingNum(analysisParam.getFileUrl(),
													  analysisParam.getNativeBbox(),
													  paramMap.get("leftUpperPos").toString(), 
													  paramMap.get("rightLowerPos").toString(),
													  (int)paramMap.get("sampleNumX"),
													  (int)paramMap.get("sampleNumY"),
													  analysisResult);
			break;
		case 8:
			mapLayerAnalysis.getSlopeBySampingRatio(analysisParam.getFileUrl(),
													     analysisParam.getNativeBbox(),
													     paramMap.get("leftUpperPos").toString(), 
													     paramMap.get("rightLowerPos").toString(),
													     (double)paramMap.get("strideX"),
													     (double)paramMap.get("strideY"),
													     analysisResult);
			break;
		case 9:
			mapLayerAnalysis.getAspectByLinePoint(analysisParam.getFileUrl(), 
											   analysisParam.getNativeBbox(),
											   paramMap.get("startPos").toString(), 
											   paramMap.get("endPos").toString(), 
											   (int)paramMap.get("sampleNum"),
											   analysisResult);
			break;
		case 10:
			mapLayerAnalysis.getAspectByLinePoint(analysisParam.getFileUrl(), 
											   analysisParam.getNativeBbox(),
											   paramMap.get("startPos").toString(), 
											   paramMap.get("endPos").toString(), 
											   (double)paramMap.get("stride"),
											   analysisResult);
			break;
			
		case 11:
			mapLayerAnalysis.getAspectBySampingNum(analysisParam.getFileUrl(),
													  analysisParam.getNativeBbox(),
													  paramMap.get("leftUpperPos").toString(), 
													  paramMap.get("rightLowerPos").toString(),
													  (int)paramMap.get("sampleNumX"),
													  (int)paramMap.get("sampleNumY"),
													  analysisResult);
			break;
		case 12:
			mapLayerAnalysis.getAspectBySampingRatio(analysisParam.getFileUrl(),
													     analysisParam.getNativeBbox(),
													     paramMap.get("leftUpperPos").toString(),
													     paramMap.get("rightLowerPos").toString(),
													     (double)paramMap.get("strideX"),
													     (double)paramMap.get("strideY"),
													     analysisResult);
			break;


			case 13:
				mapLayerAnalysis.getTileDemByLinePoint( analysisParam.getLayerBaseInfo(),
														analysisParam.getNativeBbox(),
														paramMap.get("startPos").toString(),
														paramMap.get("endPos").toString(),
														(int)paramMap.get("sampleNum"),
														analysisResult);
				break;


			case 14:
				mapLayerAnalysis.getTileDemByLinePoint( analysisParam.getLayerBaseInfo(),
														analysisParam.getNativeBbox(),
														paramMap.get("startPos").toString(),
														paramMap.get("endPos").toString(),
														(double)paramMap.get("stride"),
														analysisResult);
				break;
			case 15:
				mapLayerAnalysis.getTileDemBySampingNum(
														analysisParam.getLayerBaseInfo(),
														analysisParam.getNativeBbox(),
														paramMap.get("leftUpperPos").toString(),
														paramMap.get("rightLowerPos").toString(),
														(int)paramMap.get("sampleNumX"),
														(int)paramMap.get("sampleNumY"),
														analysisResult);
				break;
			case 16:
				mapLayerAnalysis.getTileDemBySampingRatio(
														analysisParam.getLayerBaseInfo(),
														analysisParam.getNativeBbox(),
														paramMap.get("leftUpperPos").toString(),
														paramMap.get("rightLowerPos").toString(),
														(double)paramMap.get("strideX"),
														(double)paramMap.get("strideY"),
														analysisResult);
				break;
			case 17:
				mapLayerAnalysis.getTileSlopeByLinePoint(
													analysisParam.getLayerBaseInfo(),
													analysisParam.getNativeBbox(),
													paramMap.get("startPos").toString(),
													paramMap.get("endPos").toString(),
													(int)paramMap.get("sampleNum"),
													analysisResult);
				break;
			case 18:
				mapLayerAnalysis.getTileSlopeByLinePoint(
													analysisParam.getLayerBaseInfo(),
													analysisParam.getNativeBbox(),
													paramMap.get("startPos").toString(),
													paramMap.get("endPos").toString(),
													(double)paramMap.get("stride"),
													analysisResult);
				break;
			case 19:
				mapLayerAnalysis.getTileSlopeBySampingNum(
														analysisParam.getLayerBaseInfo(),
														analysisParam.getNativeBbox(),
														paramMap.get("leftUpperPos").toString(),
														paramMap.get("rightLowerPos").toString(),
														(int)paramMap.get("sampleNumX"),
														(int)paramMap.get("sampleNumY"),
														analysisResult);
				break;
			case 20:
				mapLayerAnalysis.getTileSlopeBySampingRatio(
															analysisParam.getLayerBaseInfo(),
															analysisParam.getNativeBbox(),
															paramMap.get("leftUpperPos").toString(),
															paramMap.get("rightLowerPos").toString(),
															(double)paramMap.get("strideX"),
															(double)paramMap.get("strideY"),
															analysisResult);
				break;
			case 21:
				mapLayerAnalysis.getTileAspectByLinePoint(
														analysisParam.getLayerBaseInfo(),
														analysisParam.getNativeBbox(),
														paramMap.get("startPos").toString(),
														paramMap.get("endPos").toString(),
														(int)paramMap.get("sampleNum"),
														analysisResult);
				break;
			case 22:
				mapLayerAnalysis.getTileAspectByLinePoint(
														analysisParam.getLayerBaseInfo(),
														analysisParam.getNativeBbox(),
														paramMap.get("startPos").toString(),
														paramMap.get("endPos").toString(),
														(double)paramMap.get("stride"),
														analysisResult);
				break;
			case 23:
				mapLayerAnalysis.getTileAspectBySampingNum(
															analysisParam.getLayerBaseInfo(),
															analysisParam.getNativeBbox(),
															paramMap.get("leftUpperPos").toString(),
															paramMap.get("rightLowerPos").toString(),
															(int)paramMap.get("sampleNumX"),
															(int)paramMap.get("sampleNumY"),
															analysisResult);
				break;
			case 24:
				mapLayerAnalysis.getTileAspectBySampingRatio(
															analysisParam.getLayerBaseInfo(),
															analysisParam.getNativeBbox(),
															paramMap.get("leftUpperPos").toString(),
															paramMap.get("rightLowerPos").toString(),
															(double)paramMap.get("strideX"),
															(double)paramMap.get("strideY"),
															analysisResult);
				break;
			default:
			break;
		}
		analysisResult.setAnalysisStatus(2);   //分析完成
		return analysisResult;
	}
	
}
