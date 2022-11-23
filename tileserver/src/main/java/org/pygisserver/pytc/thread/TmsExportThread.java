package org.pygisserver.pytc.thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.TileTaskEntity;
import org.pygisserver.common.enumm.StoreType;
import org.pygisserver.common.enumm.TileTaskStatus;
import org.pygisserver.common.pytc.entity.LayerStoreTypeInfo;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.pytc.configuration.DataSourceContextHolder;
import org.pygisserver.pytc.dao.CommonMapper;
import org.pygisserver.pytc.dao.TileCacheMapper;
import org.pygisserver.pytc.service.impl.RenderTileServiceImpl;
import org.pygisserver.pytc.service.impl.TileCacheServiceImpl;
import org.pygisserver.pytc.util.ExportTaskUtils;
import org.pygisserver.pytc.util.SeedTaskUtil;

/**
 * 导出TMS数据线程
 * @author admin
 *
 */
public class TmsExportThread implements Runnable {
	//已有导出文件路径，该参数只有存在已导出数据情况下使用
	private String srcPath;
	//是否存在已经切好的瓦片数据，当从导出数据或数据库获取时，都可能会用到
	private boolean existTileData;
	//当前存在数据的最大级别，从导出数据或数据库获取的数据不够时，需要将该参数进行传递
	private int existedMaxLevel;
	//查询已有切图数据的接口
	private TileCacheServiceImpl tileCacheService;
	//切图任务信息
	private SeedTaskInstance tileTaskInstance;
	private CommonMapper commonMapper;
	private TileCacheMapper tileCacheMapper;
	//
	private RenderTileServiceImpl renderTileServiceImpl;
	
	public TmsExportThread(String srcPath,SeedTaskInstance tileTaskInstance,TileCacheServiceImpl tileCacheService, boolean existTileData, 
			CommonMapper commonMapper,int existedMaxLevel,RenderTileServiceImpl renderTileServiceImpl,TileCacheMapper tileCacheMapper) {
		this.srcPath = srcPath;
		this.tileTaskInstance = tileTaskInstance;
		this.tileCacheService = tileCacheService;
		this.existTileData = existTileData;
		this.commonMapper = commonMapper;
		this.existedMaxLevel = existedMaxLevel;
		this.renderTileServiceImpl = renderTileServiceImpl;
		this.tileCacheMapper = tileCacheMapper;
	}

	@Override
	public void run() {
		//srcPath不为空说明导出任务是基于已有的导出任务，如果existedMaxLevel大于或等于导出级别，则无需保留zip流，否则需要保留zip流
		if (StringUtils.isNoneBlank(srcPath)) {
			if (tileTaskInstance.getZoomEnd() <= existedMaxLevel) {
				readOldZipFile2New(srcPath, tileTaskInstance.getStorePath(), tileTaskInstance.getZoomEnd(),true);
			} else {
				readOldZipFile2New(srcPath, tileTaskInstance.getStorePath(), tileTaskInstance.getZoomEnd(),false);
				if (existTileData) {
					writeTiledData2TmsFile(existedMaxLevel + 1,-1,true);
				} else {
					seedTileData2TmsFile();
				}
			}
		} else {
			if (tileTaskInstance.getZoomEnd() <= existedMaxLevel) {
				writeTiledData2TmsFile(0,-1,true);
			} else {
				int maxLevel = tileTaskInstance.getZoomEnd();
				writeTiledData2TmsFile(0,existedMaxLevel,false);
				tileTaskInstance.setZoomEnd(maxLevel);
				seedTileData2TmsFile();
			}
		}
	}
	
	private void updateTmsExportProgress(SeedTaskInstance tileTaskInstance,int progress,long totalTiles) {
		DataSourceContextHolder.setDataSource("default");
		List<String> taskNames = new ArrayList<String>();
		taskNames.add(tileTaskInstance.getTaskName());
		List<TileTaskEntity> tasks = tileCacheMapper.queryTileTaskEntity(taskNames);
		if (tasks.size() == 0 || tasks == null) return;
		long endTime = System.currentTimeMillis();
		long spentTime = (endTime - tileTaskInstance.getStartTime().getTime()) / 1000;
		TileTaskEntity tileTaskEntity = new TileTaskEntity(tileTaskInstance.getTaskName(),(progress == 0 ? 1 : progress) + "%","--","--");
		tileTaskEntity.setSpentTime(spentTime);
		tileTaskEntity.setTaskEndTime(new Date().getTime());
		if (progress == 100) {
			tileTaskEntity.setTaskStatus(TileTaskStatus.DONE.getStatus());
		} else {
			tileTaskEntity.setTaskStatus(TileTaskStatus.RUNNING.getStatus());
		}
		tileTaskEntity.setTotalTiles(totalTiles);
		tileTaskEntity.setSuspendSpentTime(0);
		List<TileTaskEntity> list = new ArrayList<TileTaskEntity>();
		list.add(tileTaskEntity);
		tileCacheMapper.updateTileTaskEntity(list);
	}
	
	public void readOldZipFile2New(String src,String dest,int maxLevel,boolean isCloseZipStream) {
		String zipFileName = (new File(src)).getName().replace(".zip", "");
		String zipName = ExportTaskUtils.getExportDataName(tileTaskInstance);
		ZipOutputStream zos = null;
		ZipInputStream zip = null;
		ZipFile zfile = null;
		ZipEntry entry = null;
		long entrySize = 0;
		int i = 1;
		try {
			List<long[]> totalTiles = ExportTaskUtils.calcuteTiles(tileTaskInstance);
			zfile = new ZipFile(new File(src));
			zos = new ZipOutputStream(new FileOutputStream(new File(dest,zipName + ".zip")));
			zip = new ZipInputStream(new FileInputStream(new File(src)));
			entrySize = totalTiles.size() - zfile.size() > 0 ? totalTiles.size() : zfile.size();
			while ((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();
				name = name.replace(zipFileName, zipName);
				if (name.endsWith("tms.xml")) {
					ExportTaskUtils.writeZipData(zos,name,zfile,entry);
				} else {
					int v = name.indexOf("\\");
					int level = Integer.valueOf(name.substring(v + 1, v + 2));
					if (level <= maxLevel) {
						if (!entry.isDirectory()) {
							ExportTaskUtils.writeZipData(zos,name,zfile,entry);
							if (i % 100 == 0) {
								updateTmsExportProgress(tileTaskInstance,(int)(i/entrySize),totalTiles.size());
							}
							i++;
						}
					}
				}
			}
			if (isCloseZipStream) {
				updateTmsExportProgress(tileTaskInstance,100,totalTiles.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (isCloseZipStream) {
				FileUtil.closeStream(zfile,zip,zos);
			} else {
				FileUtil.closeStream(zfile,zip);
				if (!ExportTaskUtils.containsZos(zipName)) {
					ExportTaskUtils.addZos2Map(zipName, zos);
					//这里只是起到了占位作用
					ExportTaskUtils.addTmsConfigFile2Map(zipName, new File(Constant.TILE_CACHE_TMP));
					ExportTaskUtils.exportTaskMap.put(zipName, tileTaskInstance);
				}
			}
		}
	}
	
	/**
	 * 执行切图操作，并将切图瓦片写入zip文件
	 */
	private void seedTileData2TmsFile() {
		int zoomStart = existedMaxLevel + 1;
		tileTaskInstance.setZoomStart(zoomStart);
		renderTileServiceImpl.seedTaskByParam(tileTaskInstance, 0);
	}
	
	/**
	 * 从切图缓存中读取瓦片数据并写入压缩文件
	 * @param isCloseZipStream 写完zip文件后是否关闭流
	 */
	private void writeTiledData2TmsFile(int zoomStart,int zoomEnd,boolean isCloseZipStream) {
		ZipOutputStream zos = null;
		String zipName = null;
		try {
			//构造requestInfo请求
			TileRequestInfo tileRequestInfo = new TileRequestInfo();
			tileRequestInfo.setLayerName(tileTaskInstance.getLayerName());
			tileRequestInfo.setMineType(SeedTaskUtil.simpleMineType(tileTaskInstance.getTileFormat()));
			tileRequestInfo.setGridSet(tileTaskInstance.getSrs());
			tileRequestInfo.setTileWidth(tileTaskInstance.getTileWidth());
			//构造导出文件名，zos缓存也是以其为键保存的
			zipName = ExportTaskUtils.getExportDataName(tileTaskInstance);
			//根据实际级别获取瓦片个数
			tileTaskInstance.setZoomStart(zoomStart);
			if (zoomEnd != -1) tileTaskInstance.setZoomEnd(zoomEnd);
			List<long[]> totalTiles = ExportTaskUtils.calcuteTiles(tileTaskInstance);
			//获取瓦片存储信息，如果为空则返回
			int totalSize = 0;
			int i = 1;
			if (StringUtils.isBlank(srcPath)) {
				totalSize = totalTiles.size();
			} else {
				tileTaskInstance.setZoomStart(0);
//				tileTaskInstance.setZoomEnd(zoomStart);
				totalSize = ExportTaskUtils.calcuteTiles(tileTaskInstance).size();
				i = totalSize - totalTiles.size();
			}
			if (StringUtils.isBlank(srcPath)) {
				zos = new ZipOutputStream(new FileOutputStream(new File(tileTaskInstance.getStorePath(),zipName + ".zip")));
				if (!ExportTaskUtils.containsTmsConfigFile(zipName)) {
					String tmpPath = Constant.TILE_CACHE_TMP + "exportDir" + File.separator + System.currentTimeMillis();
					File config = ExportTaskUtils.buildTmsConfigFile(tileTaskInstance,tmpPath);
					if (!isCloseZipStream) {
						ExportTaskUtils.addTmsConfigFile2Map(zipName, config);
						ExportTaskUtils.exportTaskMap.put(zipName, tileTaskInstance);
					}
					ExportTaskUtils.writeTmsZipFile(config,null,zipName + File.separator + "tms.xml",zos);
				}
				for (long[] ls : totalTiles) {
					tileRequestInfo.setTileCol((int)ls[0]);
					tileRequestInfo.setZoomLevel((int)ls[2]);
					tileRequestInfo.setTileRow((int)ls[1]);
					writeTileData2Zip(tileRequestInfo,zos,zipName);
					if (i % 100 == 0) {
						updateTmsExportProgress(tileTaskInstance,(int)(i/totalSize),totalSize);
					}
					i++;
				}
				if (!isCloseZipStream) {
					ExportTaskUtils.addZos2Map(zipName, zos);
				}
			} else {
				zos = ExportTaskUtils.zosStreamMap.get(zipName);
				for (long[] ls : totalTiles) {
					tileRequestInfo.setTileCol((int)ls[0]);
					tileRequestInfo.setZoomLevel((int)ls[2]);
					tileRequestInfo.setTileRow((int)ls[1]);
					writeTileData2Zip(tileRequestInfo,zos,zipName);
					if (i % 500 == 0) {
						updateTmsExportProgress(tileTaskInstance,(int)(i/totalSize),totalSize);
					}
					i++;
				}
			}
			if (isCloseZipStream) {
				updateTmsExportProgress(tileTaskInstance,100,totalTiles.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (isCloseZipStream) {
				FileUtil.closeStream(zos);
				if (ExportTaskUtils.zosStreamMap.containsKey(zipName)) ExportTaskUtils.zosStreamMap.remove(zipName);
			}
		}
	}
	
	private void writeTileData2Zip(TileRequestInfo tileRequestInfo,ZipOutputStream zos,String zipName) throws IOException {
		byte[] image = tileCacheService.inspectLocalStorInfo(tileRequestInfo);
		int tileRow = (int) (Math.pow(2, tileRequestInfo.getZoomLevel()) - tileRequestInfo.getTileRow() - 1);
		String entryName = new StringBuilder().append(zipName).append(File.separator).
				append(tileRequestInfo.getZoomLevel()).append(File.separator)
				.append(tileRequestInfo.getTileCol()).append(File.separator)
				.append(tileRow).append(".").append(SeedTaskUtil.simpleMineType(tileRequestInfo.getMineType())).toString();
		ExportTaskUtils.writeTiled2ZipFile(image, entryName, zos);
	}
	
	/**
	 * 查询已经存在缓存的数据
	 * @param tileTaskInstance 切图任务信息
	 * @param isExcludeTms 是否将TMS导出记录从返回结果排除
	 * @return
	 */
	private LayerStoreTypeInfo getExistTiledData(SeedTaskInstance tileTaskInstance,boolean isExcludeTms) {
		int targetLevel = tileTaskInstance.getZoomEnd();
		TileRequestInfo tileRequestInfo = new TileRequestInfo();
		tileRequestInfo.setLayerName(tileTaskInstance.getLayerName());
		tileRequestInfo.setMineType(SeedTaskUtil.simpleMineType(tileTaskInstance.getTileFormat()));
		tileRequestInfo.setGridSet(tileTaskInstance.getSrs());
		tileRequestInfo.setImageWidth(tileTaskInstance.getTileWidth());
		List<LayerStoreTypeInfo> storeTypes = commonMapper.queryLayerStoreByReq(tileRequestInfo);
		for (LayerStoreTypeInfo layerStoreTypeInfo : storeTypes) {
			//isExcludeTms为true时，只返回存储类型为非TMS的记录
			if (isExcludeTms && layerStoreTypeInfo.getStoreTypeId() == StoreType.TMS.getTypeId()) continue;
			String sourceName = layerStoreTypeInfo.getDataSource();
			int maxLevel = Integer.valueOf(sourceName.split("_")[2]);
			if (targetLevel < maxLevel) {
				return layerStoreTypeInfo;
			}
		}
		return null;
	}
}
