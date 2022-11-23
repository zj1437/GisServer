package org.pygisserver.web.service.impl;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.SeedTaskParam;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.dao.TileCacheMapper;
import org.pygisserver.web.entity.GuassCoordinates;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.service.MapServerService;
import org.pygisserver.web.service.TileCacheService;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TileCacheServiceImpl implements TileCacheService {

	@Resource
	private MapServerMapper mapServerMapper;
	
	@Autowired
	private MapServerService mapServerService;
	
	@Autowired
	private TileCacheMapper tileCacheMapper;
	
	@Autowired
	private GuassCoordinates guassCoordinates;
	
	@Override
	public String transformCordinateBbox(String layerName, int epsgCode) {
		String nativeBBox = mapServerMapper.queryTileServerBbox(layerName);
		String[] nativeBboxArray = nativeBBox.split(";");
		ReferencedEnvelope envelope = null;
		try {
			envelope = new ReferencedEnvelope(Double.parseDouble(nativeBboxArray[0]), 
														Double.parseDouble(nativeBboxArray[2]), 
														Double.parseDouble(nativeBboxArray[1]), 
														Double.parseDouble(nativeBboxArray[3]), 
														CRS.decode("EPSG:4326", true));
			envelope = GeometryUtil.transformCoordinateSystem(envelope, "EPSG:"+epsgCode);
		} catch (FactoryException e) {
			log.error("TileCacheServiceImpl transformCordinateBbox has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_TRANSFOR, "坐标系Bbox"));
		}
		if (envelope != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(envelope.getMinimum(0)).append(";").append(envelope.getMinimum(1)).append(";")
			.append(envelope.getMaximum(0)).append(";").append(envelope.getMaximum(1));
			return sb.toString();
		}
		return null;
	}

	@Override
	public List<DataServerEntity> querySeedTaskInfo(RequestQueryCondition queryCondition) {
		try {
			return mapServerMapper.queryTileServerData(queryCondition);
		} catch (Exception e) {
			log.error("TileCacheServiceImpl querySeedTaskInfo has error: {}",ExceptionsUtil.stackTraceToStr(e));
		}
		return null;
	}
	
	@Override
	public int querySeedTaskNum(RequestQueryCondition queryCondition) {
		try {
			return mapServerMapper.querySeedTaskNum(queryCondition);
		} catch (Exception e) {
			log.error("TileCacheServiceImpl querySeedTaskNum has error: {}",ExceptionsUtil.stackTraceToStr(e));
		}
		return 0;
	}

	@Override
	public void saveSeedParam(SeedTaskParam seedParam) {
		try {
			tileCacheMapper.saveSeedTileConfig(seedParam);
		} catch (Exception e) {
			log.error("TileCacheServiceImpl saveSeedParam has error: {}",ExceptionsUtil.stackTraceToStr(e));
		}
	}

	@Override
	public SeedTaskParam queryGlobalSeedParam() {
		try {
			return tileCacheMapper.queryGlobalSeedParam();
		} catch (Exception e) {
			log.error("TileCacheServiceImpl queryGlobalSeedParam has error: {}",ExceptionsUtil.stackTraceToStr(e));
		}
		return null;
	}

//	@Override
//	public double calcuteTileSize(String layer, String gridSetId,String format,int startZoom, int endZoom, int tileWidth,String reqIP) {
//		LayerBaseInfo layerBaseInfo = mapServerService.getLayerBaseInfo(layer,reqIP);
//		String nativeBBox = layerBaseInfo.getNativaBbox();
//		try {
//			String[] nativeBboxArray = nativeBBox.split(";");
//			ReferencedEnvelope envelope = new ReferencedEnvelope(Double.parseDouble(nativeBboxArray[0]), Double.parseDouble(nativeBboxArray[2]), 
//																 Double.parseDouble(nativeBboxArray[1]), Double.parseDouble(nativeBboxArray[3]), 
//																 CRS.decode("EPSG:4326", true));
//			if (!gridSetId.equals("EPSG:4326")) {
//				envelope = (ReferencedEnvelope) GeometryUtil.transformCoordinateSystem(envelope, gridSetId);
//			}
//			//计算当前的瓦片数量
//			int totalSize = 0;
//			for (int i = startZoom; i <= endZoom; i++) {
//				int[] tileRange = null;
//				if (OgcEpsg.getOgcEpsgCodeType(gridSetId) == OgcEpsg.MERCATOR_CODE) {
//					tileRange = GeometryUtil.convert3857ToTiledRange(i, envelope);
//				}else {
//					tileRange = GeometryUtil.convert4326ToTiledRange(i, envelope);
//				}
//				totalSize += (tileRange[2] - tileRange[0] + 1) * (tileRange[3] - tileRange[1] + 1);
//			}
//			double singleFileSize = 0.0;
//			if (format.contains("png")) {
//				singleFileSize = tileWidth*tileWidth*4 / 1024 * 0.5;
//			} else if (format.contains("jpeg")) {
//				singleFileSize = tileWidth*tileWidth*3 / 1024 * 0.5;
//			} else if (format.contains("tiff")) {
//				singleFileSize = tileWidth*tileWidth*2 / 1024 * 0.5;
//			} else {
//				singleFileSize = 20;
//			}
//			return CommonUtil.format2Decimal(totalSize * singleFileSize / 1024, 2);
//		} catch (MismatchedDimensionException | NumberFormatException | FactoryException e) {
//			log.error("TileCacheServiceImpl calcuteTileSize has error:" + ExceptionsUtil.stackTraceToStr(e));
//		}
//		return 0.0;
//	}
	
	
	@Override
	public double calcuteTileSize(String layer, String gridSetId,String format,int startZoom, int endZoom, String reqIP) {
		LayerBaseInfo layerBaseInfo = mapServerService.getLayerBaseInfo(layer,reqIP);
		String nativeBBox = layerBaseInfo.getNativaBbox();
		try {
			String[] nativeBboxArray = nativeBBox.split(";");
			ReferencedEnvelope envelope = new ReferencedEnvelope(Double.parseDouble(nativeBboxArray[0]), Double.parseDouble(nativeBboxArray[2]), 
																 Double.parseDouble(nativeBboxArray[1]), Double.parseDouble(nativeBboxArray[3]), 
																 CRS.decode("EPSG:4326", true));
			if (!gridSetId.equals("EPSG:4326")) {
				envelope = (ReferencedEnvelope) GeometryUtil.transformCoordinateSystem(envelope, gridSetId);
			}
			//计算当前的瓦片数量
			BigDecimal totalSize = BigDecimal.ZERO;
			for (int i = startZoom; i <= endZoom; i++) {
				int[] tileRange = null;
				if (OgcEpsg.getOgcEpsgCodeType(gridSetId) == OgcEpsg.MERCATOR_CODE) {
					tileRange = GeometryUtil.convert3857ToTiledRange(i, envelope);
				}else if (guassCoordinates.getGuassCoordinates().contains(gridSetId)) {
					List<String> tilecount = guassCoordinates.getTileParam().get(gridSetId.replace(":", "")).get("tilecount");
					List<String> guassCoordinateBbox = guassCoordinates.getTileParam().get(gridSetId.replace(":", "")).get("bbox");
					tileRange = GeometryUtil.convertGuassToTiledRange(tilecount.get(i), guassCoordinateBbox, envelope, i);
				}else{
					tileRange = GeometryUtil.convert4326ToTiledRange(i, envelope);
				}
				totalSize = totalSize.add(BigDecimal.valueOf(tileRange[2] - tileRange[0] + 1).multiply(BigDecimal.valueOf(tileRange[3] - tileRange[1] + 1)));
			}
			String fileType = layerBaseInfo.getFileType();
			double singleFileSize = 0.0;
			if("Shape".equals(fileType)) {
				if (format.contains("png")) {
					singleFileSize = 2.18; // 单位是kb
				} else if (format.contains("jpeg")) {
					singleFileSize = 3.38;
				} else if (format.contains("tiff")) {
					singleFileSize = 100;
				} else {
					singleFileSize = 100;
				}
			}else {
				if (format.contains("png")) {
					singleFileSize = 26.55; // 单位是kb
				} else if (format.contains("jpeg")) {
					singleFileSize = 4.25;
				} else if (format.contains("tiff")) {
					singleFileSize = 100;
				} else {
					singleFileSize = 100;
				}
			}
			double result = totalSize.multiply(BigDecimal.valueOf(singleFileSize)).divide(BigDecimal.valueOf(1024), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
			if(result == 0.0 && totalSize != BigDecimal.ZERO) {
				result = 0.01;
			}
			return result;
		} catch (MismatchedDimensionException | NumberFormatException | FactoryException e) {
			log.error("TileCacheServiceImpl calcuteTileSize has error:" + ExceptionsUtil.stackTraceToStr(e));
		}
		return 0.0;
	}

	@Override
	public SeedTaskInstance queryTaskInstance(String taskName) {
		try {
			return tileCacheMapper.queryTaskInstance(taskName);
		} catch (Exception e) {
			log.error("TileCacheServiceImpl queryTaskInstance has error:" + ExceptionsUtil.stackTraceToStr(e));
		}
		return null;
	}

	@Override
	public void updateExportPath(String exportPath) {
		try {
			tileCacheMapper.updateExportPath(exportPath);
		} catch (Exception e) {
			log.error("TileCacheServiceImpl updateExportPath has error:" + ExceptionsUtil.stackTraceToStr(e));
		}
	}
}
