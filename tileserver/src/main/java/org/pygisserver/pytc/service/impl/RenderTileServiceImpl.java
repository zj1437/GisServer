package org.pygisserver.pytc.service.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.pygisserver.common.entity.*;
import org.pygisserver.common.enumm.ServerStatusEnum;
import org.pygisserver.common.enumm.StoreType;
import org.pygisserver.common.enumm.TileTaskStatus;
import org.pygisserver.common.pytc.entity.LayerStoreTypeInfo;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.logback.LogBackXmlConfigHelper;
import org.pygisserver.common.util.DateUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.pytc.common.TileTaskManager;
import org.pygisserver.pytc.configuration.DataSourceContextHolder;
import org.pygisserver.pytc.configuration.DataSourceUtil;
import org.pygisserver.pytc.dao.CommonMapper;
import org.pygisserver.pytc.dao.TileCacheMapper;
import org.pygisserver.pytc.entity.TileTask;
import org.pygisserver.pytc.entity.TileTask.TILESTATUS;
import org.pygisserver.pytc.thread.TileTaskConsumerThread;
import org.pygisserver.pytc.thread.TmsExportThread;
import org.pygisserver.pytc.util.ExportTaskUtils;
import org.pygisserver.pytc.util.SeedTaskUtil;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.tileserver.service.RenderTileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description:  dubbo注解的service
 * @author:  
 * @date: 2021-04-10
 */
@DubboService(version="3.0.0")
public class RenderTileServiceImpl implements RenderTileService {
	
	@Autowired
	private TileCacheMapper tileCacheMapper;
	
	@Autowired
	private SeedTaskUtil seedTaskUtil;
	
	@Autowired
	private CommonMapper commonMapper;
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private LogBackXmlConfigHelper logBackXmlConfigHelper;
	
	@Autowired
	private TileCacheServiceImpl tileCacheService;
	
	private volatile static AtomicBoolean flag = new AtomicBoolean(true);
	
	public static boolean zosIsFlag = true;

	@Override
	public void seedTaskByParam(SeedTaskInstance tileTaskInstance, int openOverView) {
		//如果是重新切图，则需要清除掉已经切好的瓦片
		if ("reseed".equalsIgnoreCase(tileTaskInstance.getSeedType()) && (tileTaskInstance.getStoreType() != StoreType.TMS.getTypeId())) {
			clearSameSysWidthAndFormatData(tileTaskInstance);
		}
		//构造整个切图任务信息
		String zipName = ExportTaskUtils.getExportDataName(tileTaskInstance);
		if (!ExportTaskUtils.exportTaskMap.containsKey(zipName)) {
			constructTileTaskInstance(tileTaskInstance);
		}
		
		//切图任务信息添加到等待队列
		if (openOverView == 0) {
			TileTaskManager.addWaitTileTask(tileTaskInstance);
		} else if(openOverView == 1) {
			TileTaskManager.addPreWaitTileTask(tileTaskInstance);
		}

		startTileThread();
	}

	private void startTileThread() {
		//开启切图任务管理线程
		if (!TileTaskConsumerThread.isStart()) {
			flag.getAndSet(false);
			openInspectThread();
		}
	}

	public void clearSameSysWidthAndFormatData(SeedTaskInstance tileTaskInstance) {
		List<LayerStoreTypeInfo> layerStoreInfo = seedTaskUtil.getLayerStoreInfo(tileTaskInstance.getLayerName());
		List<SeedTaskInstance> tasks = tileCacheMapper.queryTaskInstancesByLayerName(tileTaskInstance.getLayerName());
		if (layerStoreInfo != null && !layerStoreInfo.isEmpty()) {
			for (LayerStoreTypeInfo layerStoreTypeInfo : layerStoreInfo) {
				String dbName = layerStoreTypeInfo.getDataSource();
				if (!dbName.endsWith(buildLayerStoreInfo(tileTaskInstance))) continue;
				if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.SQLITE)) {
					String storePath = new StringBuilder(layerStoreTypeInfo.getStorePath()).append(File.separator).append(layerStoreTypeInfo.getLayerName()).toString();
					flushAndDeleteSqlite(dbName,storePath);
//				}else if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.POSTGRRSQL)) {
//					flushAndDeletePsql(dbName,true);
				}else if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.TDB)) {
					flushAndDeleteTdb(dbName,layerStoreTypeInfo.getStorePath(),false);
				}else if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.MONGODB)) {
					flushAndDeleteMongodb(dbName);
				}
			}
		}
		DataSourceContextHolder.setDataSource("default");
		if (tasks != null && tasks.size() != 0) {
			tasks.forEach(task -> {
				if (buildLayerStoreInfo(tileTaskInstance).equalsIgnoreCase(buildLayerStoreInfo(task)) && (task.getStoreType() != StoreType.TMS.getTypeId())) {
					String taskName = task.getTaskName();
					List<String> dbNames = new ArrayList<String>();
					layerStoreInfo.forEach(store -> {
						if (store.getDataSource().startsWith(taskName)) {
							dbNames.add(store.getDataSource());
						}
					});
					if (dbNames.size() > 0) {
						dbNames.forEach(name -> {
							commonMapper.deleteDynamicTableInfo(null,name);
							commonMapper.deleteLayerStoreByDatasource(name);
						});
					}
					commonMapper.deleteTileTaskInfo(-1,taskName);
				}
			});
		}
	}
	private String buildLayerStoreInfo(SeedTaskInstance tileTaskInstance) {
		String format = SeedTaskUtil.simpleMineType(tileTaskInstance.getTileFormat());
		String reault = tileTaskInstance.getSrs() + "_" + format + "_" + tileTaskInstance.getTileWidth();
		return reault.split(":")[1];
	}

	public void openInspectThread() {
		TileTaskConsumerThread.setStart();
		TileTaskConsumerThread tileTaskConsumer = new TileTaskConsumerThread(seedTaskUtil);
		new Thread(tileTaskConsumer).start();
	}
	
	/**
	 * 当新提交的切图任务与原有的切图任务名称、投影格式、瓦片格式、瓦片大小相同时，就不再切图
	 */
	@Override
	public boolean submitTaskIsSeeding(SeedTaskInstance tileTaskInstance,boolean isExportTask) {
		DataSourceContextHolder.setDataSource("default");
		//如果是重新切图，不用校验直接返回false
		if (tileTaskInstance.getSeedType().equals("reseed")) return false;
		//1.将已经暂停的、准备切图的、正在切图的任务都筛选出来
		List<TileTaskEntity> taskEntities = new ArrayList<>();
		RequestQueryCondition condition = new RequestQueryCondition();
		condition.setServerName(tileTaskInstance.getLayerName());
		condition.setPageInfo(null);
		condition.setServerStatus(TileTaskStatus.DEAD.getStatus());
		List<TileTaskEntity> deadEntities = tileCacheMapper.queryTileTaskEntityByParam(condition);
		taskEntities.addAll(deadEntities);
		condition.setServerStatus(TileTaskStatus.READY.getStatus());
		List<TileTaskEntity> readyEntities = tileCacheMapper.queryTileTaskEntityByParam(condition);
		taskEntities.addAll(readyEntities);
		condition.setServerStatus(TileTaskStatus.RUNNING.getStatus());
		List<TileTaskEntity> runEntities = tileCacheMapper.queryTileTaskEntityByParam(condition);
		taskEntities.addAll(runEntities);
		//2.如果数组为空，则可以继续切图
		if (taskEntities.size() == 0) return false;
		//3.通过taskName查询每个切图任务并做校验
		for (TileTaskEntity taskEntity : taskEntities) {
			SeedTaskInstance instance = tileCacheMapper.queryTaskInstance(taskEntity.getTaskName());
			if(instance.getSrs().equals(tileTaskInstance.getSrs())
					&& instance.getTileFormat().equals(tileTaskInstance.getTileFormat())
					&& (instance.getTileHeight() == tileTaskInstance.getTileHeight())
					&& (instance.getTileWidth() == tileTaskInstance.getTileWidth())) {
				if (isExportTask) {
					if ((instance.getStoreType() == tileTaskInstance.getStoreType())) {
						return true;
					}
				} else {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 导出只针对同样是导出且已完成的导出任务
	 */
	public List<SeedTaskInstance> getRepeatExport(SeedTaskInstance tileTaskInstance,boolean isFoundSameExport) {
		//1.将已经暂停的、准备切图的、正在切图的任务都筛选出来
		List<TileTaskEntity> taskEntities = new ArrayList<>();
		RequestQueryCondition condition = new RequestQueryCondition();
		condition.setServerName(tileTaskInstance.getLayerName());
		condition.setPageInfo(null);
		condition.setServerStatus(TileTaskStatus.DONE.getStatus());
		DataSourceContextHolder.setDataSource("default");
		List<TileTaskEntity> doneEntities = tileCacheMapper.queryTileTaskEntityByParam(condition);
		taskEntities.addAll(doneEntities);
		//2.如果数组为空，则可以继续切图
		if (taskEntities.size() == 0) return null;
		//3.通过taskName查询每个切图任务并做校验
		List<SeedTaskInstance> list = new ArrayList<SeedTaskInstance>();
		for (TileTaskEntity taskEntity : taskEntities) {
			DataSourceContextHolder.setDataSource("default");
			SeedTaskInstance instance = tileCacheMapper.queryTaskInstance(taskEntity.getTaskName());
			//投影坐标系、瓦片格式、瓦片宽高、切图始终级别、切图存储类型相同时不再重复切图，而是直接导出文件
			if (instance == null) continue;
			if(instance.getSrs().equals(tileTaskInstance.getSrs()) 
					&& (instance.getStoreType() == tileTaskInstance.getStoreType())
					&& (instance.getZoomStart() == tileTaskInstance.getZoomStart())
					&& (instance.getTileHeight() == tileTaskInstance.getTileHeight())
					&& (instance.getTileFormat().equals(tileTaskInstance.getTileFormat()))) {
				String zipName = ExportTaskUtils.getExportDataName(instance);
				File file = new File(instance.getStorePath(),zipName + ".zip");
				if (!file.exists()) continue;
				if (isFoundSameExport) {
					if ((instance.getTileHeight() == tileTaskInstance.getTileHeight())
							&& (instance.getTileWidth() == tileTaskInstance.getTileWidth())
							&& (instance.getZoomEnd() == tileTaskInstance.getZoomEnd())) {
						list.add(instance);
					}
				} else {
					list.add(instance);
				}
			}
		}
		return list;
	}
	
	/**
	 * 导出能导出的数据，这里指已经导出过或切过图的数据
	 * 1.对于已经导出过的数据，如果各项参数均符合则直接返回，如果请求级别小于已有数据则直接写zip，
	 *              否则将已有数据写入zip后再执行切图并追加瓦片数据。其他情况均重新导出。
	 * 2.对于已经切图的数据，如果各项参数均符合则读取切图写入zip，如果请求级别小于已有数据读取切图写入zip，
	 *              否则已有数据写入zip后再执行切图并追加瓦片数据。如果格式、大小不同时则执行重新导出。
	 */
	@Override
	public boolean exportServerData(SeedTaskInstance tileTaskInstance) {
		//1.先查找是否有相同的已经导出过的包--这个已经导出的文件必须存在
		List<SeedTaskInstance> olds = getRepeatExport(tileTaskInstance,true);
		String zipName;
		if(olds != null && olds.size() != 0) {
			constructTileTaskInstance(tileTaskInstance);
			zipName = ExportTaskUtils.getExportDataName(tileTaskInstance);
			ExportTaskUtils.exportTaskMap.put(zipName, tileTaskInstance);
			if (!exportZipData(tileTaskInstance,olds)) {
				updateTmsExportProgress(tileTaskInstance,100);
				ExportTaskUtils.cleanTmsZipCache(zipName);
				return false;
			}
		}
		//2.查看是否存在已经导出过zip包，但是级别不同的任务
		olds = getRepeatExport(tileTaskInstance,false);
		if(olds != null && olds.size() != 0) {
			constructTileTaskInstance(tileTaskInstance);
			zipName = ExportTaskUtils.getExportDataName(tileTaskInstance);
			ExportTaskUtils.exportTaskMap.put(zipName, tileTaskInstance);
			if (!readZipedDataAndExportTmsData(tileTaskInstance,olds)) {
				updateTmsExportProgress(tileTaskInstance,100);
				return false;
			}
		}

		//3.如果之前没有zip导出、则通过查询切图数据导出
		if (isExistTiledData(tileTaskInstance)) {
			List<SeedTaskInstance> existed = getRepeatExport(tileTaskInstance,true);
			if (existed.size() == 0) {
				constructTileTaskInstance(tileTaskInstance);
				zipName = ExportTaskUtils.getExportDataName(tileTaskInstance);
				ExportTaskUtils.exportTaskMap.put(zipName, tileTaskInstance);
				if (!readTiledDataAndExportTmsData(tileTaskInstance)) {
					updateTmsExportProgress(tileTaskInstance,100);
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean exportZipData(SeedTaskInstance tileTaskInstance,List<SeedTaskInstance> tasks) {
		String fileName = ExportTaskUtils.getExportDataName(tileTaskInstance);
		String zipFileName = fileName + ".zip";
		for (SeedTaskInstance seedTaskInstance : tasks) {
			String path = seedTaskInstance.getStorePath();
			File[] zipFiles = ExportTaskUtils.getSameExportFile(path,zipFileName);
			if (zipFiles.length != 0) {
				if (path.equals(tileTaskInstance.getStorePath())) {
					return false;
				}
				if (!path.equals(tileTaskInstance.getStorePath())) {
					File src = new File(path,zipFileName);
					File dest = new File(tileTaskInstance.getStorePath(),zipFileName);
					Thread t = new Thread(new ZipCopyRunnable(src.getAbsolutePath(), dest.getAbsolutePath(),tileTaskInstance));
					t.start();
					try {
						FileUtils.copyFile(src, dest);
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}
			}
		}
		return true;
	}
	
	class ZipCopyRunnable implements Runnable {
		private String src;
		private String dest;
		private boolean continueNow = true;
		private SeedTaskInstance tileTaskInstance;
		ZipCopyRunnable(String src,String dest,SeedTaskInstance tileTaskInstance) {
			this.src = src;
			this.dest = dest;
			this.tileTaskInstance = tileTaskInstance;
		}
		@Override
		public void run() {
			while (continueNow) {
				File srcFile = new File(src);
				File destFile = new File(dest);
				if (srcFile.length() == destFile.length()) {
					continueNow = false;
					updateTmsExportProgress(tileTaskInstance,100);
				} else {
					updateTmsExportProgress(tileTaskInstance,(int)(destFile.length()/srcFile.length()));
				}
			}
		}
		
	}
	
	private void updateTmsExportProgress(SeedTaskInstance tileTaskInstance,int progress) {
		long endTime = System.currentTimeMillis();
		long spentTime = (endTime - tileTaskInstance.getStartTime().getTime()) / 1000;
		TileTaskEntity tileTaskEntity = new TileTaskEntity(tileTaskInstance.getTaskName(), progress + "%","--","--");
		tileTaskEntity.setSpentTime(spentTime);
		tileTaskEntity.setTaskEndTime(new Date().getTime());
		if (progress == 100) {
			tileTaskEntity.setTaskStatus(TileTaskStatus.DONE.getStatus());
		} else {
			tileTaskEntity.setTaskStatus(TileTaskStatus.RUNNING.getStatus());
		}
		List<TileTaskEntity> list = new ArrayList<TileTaskEntity>();
		list.add(tileTaskEntity);
		DataSourceContextHolder.setDataSource("default");
		tileCacheMapper.updateTileTaskEntity(list);
	}
	
	private boolean readZipedDataAndExportTmsData(SeedTaskInstance tileTaskInstance,List<SeedTaskInstance> tasks) {
		List<SeedTaskInstance> bigLevels = new ArrayList<SeedTaskInstance>();
		List<Integer> lowLevels = new ArrayList<Integer>();
		Map<Integer,SeedTaskInstance> map = new HashMap<Integer,SeedTaskInstance>();
		//1.将获取到的导出任务分别按大于目标级别和小于目标级别进行分类
		for (SeedTaskInstance seedTaskInstance : tasks) {
			if (tileTaskInstance.getZoomEnd() <= seedTaskInstance.getZoomEnd()) {
				bigLevels.add(seedTaskInstance);
			} else {
				map.put(seedTaskInstance.getZoomEnd(), seedTaskInstance);
				lowLevels.add(seedTaskInstance.getZoomEnd());
			}
		}
		String fileName = null;
		String zipFileName = null;
		//2.如果存在大于目标级别的已经导出的任务，则直接从其中读取数据并写入zip文件
		if (bigLevels.size() != 0) {
			for (SeedTaskInstance seedTaskInstance : bigLevels) {
				fileName = ExportTaskUtils.getExportDataName(seedTaskInstance);
				zipFileName = fileName + ".zip";
				File file = new File(seedTaskInstance.getStorePath(),zipFileName);
				if (file.exists()) {
					Thread t = new Thread(new TmsExportThread(file.getAbsolutePath(),tileTaskInstance,null,false,null,seedTaskInstance.getZoomEnd(),null,tileCacheMapper));
					t.start();
					return false;
				}
			}
		}
		//3.对小于目标级别的导出任务进行排序，从最接近目标级别的导出任务进行操作或现切数据
		if (lowLevels.size() != 0) {
			Collections.sort(lowLevels);
			for (int i = 1;i <= lowLevels.size();i++) {
				SeedTaskInstance task = map.get(lowLevels.get(lowLevels.size() - i));
				fileName = ExportTaskUtils.getExportDataName(task);
				zipFileName = fileName + ".zip";
				File file = new File(task.getStorePath(),zipFileName);
				if (file.exists()) {
					int existedMaxLevel = task.getZoomEnd();
					boolean result = isExistTiledData(tileTaskInstance);
					Thread t = new Thread(new TmsExportThread(file.getAbsolutePath(),tileTaskInstance,tileCacheService,result,commonMapper,existedMaxLevel,this,tileCacheMapper));
					t.start();
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean readTiledDataAndExportTmsData(SeedTaskInstance tileTaskInstance) {
		TileRequestInfo tileRequestInfo = new TileRequestInfo();
		tileRequestInfo.setLayerName(tileTaskInstance.getLayerName());
		tileRequestInfo.setMineType(SeedTaskUtil.simpleMineType(tileTaskInstance.getTileFormat()));
		tileRequestInfo.setGridSet(tileTaskInstance.getSrs());
		tileRequestInfo.setTileWidth(tileTaskInstance.getTileWidth());
		DataSourceContextHolder.setDataSource("default");
		List<LayerStoreTypeInfo> storeTypes = commonMapper.queryLayerStoreByReq(tileRequestInfo);
		if (storeTypes == null || storeTypes.size() == 0) return true;
		List<LayerStoreTypeInfo> bigLevels = new ArrayList<LayerStoreTypeInfo>();
		List<Integer> lowLevels = new ArrayList<Integer>();
		Map<Integer,LayerStoreTypeInfo> map = new HashMap<Integer,LayerStoreTypeInfo>();
		//1.将获取到的导出任务分别按大于目标级别和小于目标级别进行分类
		for (LayerStoreTypeInfo storeInfo : storeTypes) {
			if (storeInfo.getStoreTypeId() ==  StoreType.TMS.getTypeId()) continue;
			int maxLevel = getMaxLevel(storeInfo);
			if (maxLevel == -1) continue;
			if (maxLevel >= tileTaskInstance.getZoomEnd()) {
				bigLevels.add(storeInfo);
			} else {
				map.put(maxLevel, storeInfo);
				lowLevels.add(maxLevel);
			}
		}
		//2.如果存在大于目标级别的切图任务，则直接从其中读取数据并写入zip文件
		if (bigLevels.size() != 0) {
			Thread t = new Thread(new TmsExportThread(null,tileTaskInstance,tileCacheService,true,null,tileTaskInstance.getZoomEnd(),null,tileCacheMapper));
			t.start();
			return false;
		}
		//3.对小于目标级别的导出任务进行排序，从最接近目标级别的导出任务进行操作
		if (lowLevels.size() != 0) {
			Collections.sort(lowLevels);
			for (int i = 1;i <= lowLevels.size();i++) {
				LayerStoreTypeInfo storeInfo = map.get(lowLevels.get(lowLevels.size() - i));
				int maxLevel =  getMaxLevel(storeInfo);
				Thread t = new Thread(new TmsExportThread(null,tileTaskInstance,tileCacheService,true,commonMapper,maxLevel,this,tileCacheMapper));
				t.start();
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 检查是否存在切图，因为可能存在清除缓存的情况，需要检查是否真的存在瓦片数据。目前只是通过0级的两块数据判断数据是否存在切图
	 * @param tileTaskInstance
	 * @return
	 */
	private boolean isExistTiledData(SeedTaskInstance tileTaskInstance) {
		int targetLevel = tileTaskInstance.getZoomEnd();
		TileRequestInfo tileRequestInfo = new TileRequestInfo();
		tileRequestInfo.setLayerName(tileTaskInstance.getLayerName());
		tileRequestInfo.setMineType(SeedTaskUtil.simpleMineType(tileTaskInstance.getTileFormat()));
		tileRequestInfo.setGridSet(tileTaskInstance.getSrs());
		tileRequestInfo.setTileWidth(tileTaskInstance.getTileWidth());
		DataSourceContextHolder.setDataSource("default");
		List<LayerStoreTypeInfo> storeTypes = commonMapper.queryLayerStoreByReq(tileRequestInfo);
		if (storeTypes.size() == 0) {
			return false;
		} else {
			for (LayerStoreTypeInfo layerStoreTypeInfo : storeTypes) {
				if(layerStoreTypeInfo.getStoreTypeId() == StoreType.TMS.getTypeId()) {
					int maxLevel = getMaxLevel(layerStoreTypeInfo);
					tileRequestInfo.setTileCol(0);
					tileRequestInfo.setZoomLevel(0);
					tileRequestInfo.setTileRow(0);
					//已有切图历史时，需要校验是否存在瓦片。这里为了防止清除切图缓存导致的TMS导出失败情况出现
					try {
//						if (tileTaskInstance.getStoreType() != StoreType.TMS.getTypeId()) {
							byte[] image0 = tileCacheService.inspectLocalStorInfo(tileRequestInfo);
							tileRequestInfo.setTileCol(1);
							byte[] image1 = tileCacheService.inspectLocalStorInfo(tileRequestInfo);
							if (image0 == null && image1 == null) return false;
//						} else {
//							byte[] image0 = tileCacheService.getOldLocalStorInfo(tileRequestInfo,tileTaskInstance);
//							tileRequestInfo.setTileCol(1);
//							byte[] image1 = tileCacheService.getOldLocalStorInfo(tileRequestInfo,tileTaskInstance);
//							if (image0 == null && image1 == null) return false;
//						}
					} catch (Exception e) {
						return false;
					}
					if (targetLevel <= maxLevel) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private int getMaxLevel(LayerStoreTypeInfo storeType) {
		if (storeType.getStoreTypeId() == 4) {
			File file = new File(storeType.getStorePath(),storeType.getDataSource());
			int maxLevel = 0;
			File[] files = file.listFiles();
			if (files == null || files.length == 0) return -1;
			for (File file2 : files) {
				String name = file2.getName();
				if (name.endsWith(".xml") || name.endsWith(".tdb")) continue;
				int level = Integer.valueOf(name.substring(0, name.indexOf("_")));
				if (maxLevel < level) maxLevel = level;
			}
			return maxLevel;
		} else {
			String sourceName = storeType.getDataSource();
			return Integer.valueOf(sourceName.split("_")[4]);
		}
	}
	
	@Override
	public boolean isCanDownloadTmsFile(SeedTaskInstance taskInstance) {
		//如果任务为空或存储路径为空或存储类型不等于4则返回false
		if (taskInstance == null || StringUtils.isBlank(taskInstance.getStorePath()) || taskInstance.getStoreType() != StoreType.TMS.getTypeId()) return false;
		String fileName = ExportTaskUtils.getExportDataName(taskInstance);
		String zipFileName = fileName + ".zip";
		String path = taskInstance.getStorePath();
		File file = new File(path,zipFileName);
		if (file.exists()) {
			return true;
		}
		return false;
	}

	private void constructTileTaskInstance(SeedTaskInstance tileTaskInstance) {
		//查询配置基础信息
		DataSourceContextHolder.setDataSource("default");
		SeedTaskParam seedTaskParam = tileCacheMapper.queryTileCacheConfig();
		tileTaskInstance.setMaxTaskNum(seedTaskParam.getMaxTaskNum());
		tileTaskInstance.setThreadNum(seedTaskParam.getThreadCount());
		if (!(tileTaskInstance.getStoreType() == StoreType.TMS.getTypeId())) {
			tileTaskInstance.setStorePath(seedTaskParam.getStorePath());
			tileTaskInstance.setStoreType(seedTaskParam.getStoreType());
		}
		tileTaskInstance.setTaskName(createTileTaskName());
		tileTaskInstance.setStartTime(new Date());
		DataSourceContextHolder.setDataSource("default");
		tileCacheMapper.saveTileTaskInstance(tileTaskInstance);
		//组装tileTaskEntity,将结果存储到当前的系统中
		TileTaskEntity task = new TileTaskEntity(tileTaskInstance.getTaskName(), "--", "--", "--");
		task.setTaskId(tileTaskInstance.getTaskId());
		task.setTaskStatus(TileTaskStatus.READY.getStatus());
		task.setLayerName(tileTaskInstance.getLayerName());
		task.setTaskStartTime(DateUtil.covertAccuteTimeTostr(tileTaskInstance.getStartTime()));
		//任务只分为导出和切图，导出任务的类型值为5，其他均为-1
		task.setTaskType(tileTaskInstance.getStoreType() == StoreType.TMS.getTypeId() ?  StoreType.TMS.getTypeId() : -1);
		String levelRange = tileTaskInstance.getZoomStart() + "-" + tileTaskInstance.getZoomEnd();
		task.setLevelRange(levelRange);
		DataSourceContextHolder.setDataSource("default");
		tileCacheMapper.saveTileTaskEntity(task);
	}

	/**
	 * taskName 为当前时间戳的后四位
	 * @param tileTaskInstance
	 * @return
	 */
	private String createTileTaskName() {
		Random random = new Random();
		long t1 = 0x7FFFFFFF & System.currentTimeMillis();
		long uuid = t1 << 32 | Math.abs(random.nextInt());
		return Long.toString(uuid);
	}
	
	/**
	 * 激活任务
	 */
	@Override
	public void activeSeedTask(String taskName) {
		SeedTaskInstance taskInstance = seedTaskUtil.getTaskInstance(taskName);
		TileTaskManager.addWaitTileTask(taskInstance);
		TileTaskManager.removeSuspendTileTask(taskInstance);
		//更新任务状态信息
		tileCacheMapper.updateTileTaskEntityStatus(taskName, TileTaskStatus.READY.getStatus());
		if(taskInstance.getStoreType() == StoreType.TMS.getTypeId()) {
			zosIsFlag = true;
		}
	}

	/**
	 * 暂停任务
	 */
	@Override
	public void stopSeedTask(String taskName) {
		SeedTaskInstance taskInstance = seedTaskUtil.getTaskInstance(taskName);
		taskInstance.setSeedType("seed");
		TileTaskManager.addSuspendTileTask(taskInstance);
		TileTaskManager.removeDoingTileTask(taskInstance);
		//记录最后状态，通过查询线程池，获取当前的记录数据，插入到数据库
		List<TileTaskEntity> taskEntity = statisticSeedTaskInfo(taskName);
		if (taskEntity == null || taskEntity.isEmpty()) {
			return;
		}
		taskEntity.get(0).setTaskStatus(TileTaskStatus.DEAD.getStatus());
		taskEntity.get(0).setSuspendTileSize(taskEntity.get(0).getTiledTiles());
		taskEntity.get(0).setSuspendSpentTime(taskEntity.get(0).getSpentTime());
		DataSourceContextHolder.setDataSource("default");
		tileCacheMapper.updateTileTaskEntity(taskEntity);
		//线程挂起
		stopThread(taskName);
		if(taskInstance.getStoreType() == StoreType.TMS.getTypeId()) {
			String tmsStorePath = taskInstance.getStorePath();
			String tmsDirName = ExportTaskUtils.getExportDataName(taskInstance);
			StringBuffer oldPath = new StringBuffer(tmsStorePath);
			oldPath.append(File.separator).append(tmsDirName).append(".zip");
			StringBuffer sb = new StringBuffer(tmsStorePath);
			sb.append(File.separator).append("temp").append(File.separator)
			.append(tmsDirName).append(".zip");
			zosIsFlag = false;
			ExportTaskUtils.closeZos(tmsDirName);
			//文件
			File file = new File(oldPath.toString());
			if (file.exists()) {
				FileUtil.transferResToDest(oldPath.toString(), sb.toString());
			}
		}
	}

	/**
	 * 暂停当前的线程
	 * @param taskName
	 */
	private void stopThread(String taskName) {
		seedTaskUtil.stoptTileTask(taskName);
	}
	
	@Override
	public int queryTileTaskNum(RequestQueryCondition queryCondition) {
		DataSourceContextHolder.setDataSource("default");
		return tileCacheMapper.queryTileTaskNum(queryCondition);
	}
	

	@Override
	public List<TileTaskEntity> queryTileTaskInfo(RequestQueryCondition queryCondition) {
		//获取进行中的任务状态
		List<SeedTaskInstance> doingList = TileTaskManager.getDoingTaskBlockQueue();
		if (doingList != null && !doingList.isEmpty()) {
			//获取进行中任务，并更新状态
			String[] taskNames = new String[doingList.size()];
			int i = 0;
			for (SeedTaskInstance seedTaskInstance : doingList) {
				if (StringUtils.isBlank(seedTaskInstance.getTaskName())) continue;
				taskNames[i] = seedTaskInstance.getTaskName();
				i++;
			}
			int index = 0;
			for (String taskName : taskNames) {
				if (StringUtils.isBlank(taskName)) {
					continue;
				}
				taskNames[index] = taskName;
				index++;
			}
			if (taskNames.length > 0) {
				List<TileTaskEntity> list = statisticSeedTaskInfo(taskNames);
				DataSourceContextHolder.setDataSource("default");
				tileCacheMapper.updateTileTaskEntity(list);
			}
		}
		DataSourceContextHolder.setDataSource("default");
		List<TileTaskEntity> tileTaskEntitys = tileCacheMapper.queryTileTaskEntityByParam(queryCondition);
		//遍历将暂停前的已用时间加上
		for (TileTaskEntity tileTaskEntity : tileTaskEntitys) {
			tileTaskEntity.setSpentTimeStr(formatSecondsToHourMinutes(tileTaskEntity.getSpentTime()));
		}
		//第一次进来需要处理查看
		if (tileTaskEntitys != null && !tileTaskEntitys.isEmpty() && flag.get()) {
			boolean isOpenThread = false;
			for (TileTaskEntity tileTaskEntity : tileTaskEntitys) {
				//获取到对应的任务列表，则可以
				SeedTaskInstance seedTaskInstance = seedTaskUtil.getTaskInstance(tileTaskEntity.getTaskName());
				if (tileTaskEntity.getTaskStatus() == TileTaskStatus.RUNNING.getStatus()) {
					if (doingList == null || doingList.isEmpty() || !doingList.contains(seedTaskInstance)) {
						TileTaskManager.addWaitTileTask(seedTaskInstance);
						isOpenThread = true;
					}
				}else if (tileTaskEntity.getTaskStatus() == TileTaskStatus.DEAD.getStatus()) {
					LinkedList<SeedTaskInstance> suspendList = TileTaskManager.getSuspendTileTaskBlockQueue();
					if (suspendList == null || suspendList.isEmpty() || !suspendList.contains(seedTaskInstance)) {
						TileTaskManager.addSuspendTileTask(seedTaskInstance);
						isOpenThread = true;
					}
				}else if (tileTaskEntity.getTaskStatus() == TileTaskStatus.READY.getStatus()) {
					BlockingQueue<SeedTaskInstance> waitTaskList = TileTaskManager.getWaitTileTaskBlockQueue();
					if (waitTaskList == null || waitTaskList.isEmpty() || !waitTaskList.contains(seedTaskInstance)) {
						TileTaskManager.addWaitTileTask(seedTaskInstance);
						isOpenThread = true;
					}
				}
			}
			//开启线程自己搞
			if (isOpenThread) {
				openInspectThread();
			}
		}
		flag.getAndSet(false);
		return tileTaskEntitys;
	}
	
	public List<TileTaskEntity> statisticSeedTaskInfo(String...taskNames) {
		List<TileTaskEntity> tileTaskList = new ArrayList<TileTaskEntity>(taskNames.length);
		for (String layer : taskNames) {
			List<long[]> list = new ArrayList<long[]>();
			List<TileTask> List = seedTaskUtil.filterTileTask(TILESTATUS.RUNNING,TILESTATUS.DONE);
			for (TileTask tileTask : List) {
				if (layer.equals(tileTask.getTaskName())) {
					long[] ret = new long[2];
	                ret[0] = tileTask.getTilesDone();
	                ret[1] = tileTask.getSpentTime();
	                list.add(ret);
				}
			}
			TileTaskEntity tileTask = analysisTileDatas(list,layer);
			if (tileTask != null) {
				tileTaskList.add(tileTask);
			}
		}
		return tileTaskList;
	}

	private TileTaskEntity analysisTileDatas(List<long[]> list,String layerName) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		List<String> asList = Arrays.asList(layerName);
		List<TileTaskEntity> queryTileTaskEntity = tileCacheMapper.queryTileTaskEntity(asList);
	    TileTaskEntity entity = new TileTaskEntity();
		entity.setTaskName(layerName);
		long tiledTiles = 0l;
		long spentTime = 0l;
		long remainTime = 0l;
		String oldRemainTime = "";
		for (long[] thread : list) {
			tiledTiles += thread[0];
			//已经用时，使用当前线程中的最大值
			if (thread[1] > spentTime) {
				spentTime = thread[1];
			}
		}
		entity.setSpentTime(spentTime);
		if(tiledTiles != 0) {
			for (TileTaskEntity tileTaskEntity : queryTileTaskEntity) {
				remainTime = (tileTaskEntity.getTotalTiles() - tiledTiles) * spentTime / tiledTiles;
				oldRemainTime = tileTaskEntity.getTimeRemainingStr();
			}
		}
		if((!StringUtils.isBlank(oldRemainTime) && !"0秒".equals(oldRemainTime) && !"--".equals(oldRemainTime) )&& StringTimeToSecond(oldRemainTime) < remainTime) {
			entity.setTimeRemainingStr(oldRemainTime);
		}else {
			entity.setTimeRemainingStr(formatSecondsToHourMinutes(remainTime));
		}
		
		if (tiledTiles < 0) {
			tiledTiles = 0;
		}
		entity.setTiledTiles(tiledTiles);
		entity.setTaskStatus(TileTaskStatus.RUNNING.getStatus());
		return entity;
	}

	/**
	 * 将秒数转成时分秒
	 * @param seconds
	 * @return
	 */
	private String formatSecondsToHourMinutes(long seconds) {
		long day = 0;
		if (seconds >= 60*60*24) {
			day = (seconds / (60*60*24));
		}
		//计算小时
		int hour = 0;
		seconds = (seconds - (60*60*24) * day);
		if (seconds >= 60*60) {
			hour = (int) (seconds / (60*60));
		}
		//计算分钟
		int minutes = 0;
		seconds = (seconds - (60*60) * hour);
		if (seconds >= 60) {
			minutes = (int) (seconds / 60);
		}
		seconds = (seconds - 60 * minutes);
		if (day > 0) {
			return day + "天" + hour + "小时" + minutes + "分钟" + seconds + "秒";
		}else if (hour > 0) {
			return hour + "小时" + minutes + "分钟" + seconds + "秒";
		} else if (minutes > 0) {
			return  minutes + "分钟" + seconds + "秒";
		} else {
			return seconds + "秒";
		}
	}
	//时间转为秒
	private long StringTimeToSecond(String time) {
		long seconds = 0;
		if(time.contains("天")) {
			String[] d = time.split("天");
			seconds = seconds + Integer.parseInt(d[0]) * 60*60*24;
			String[] h = d[1].split("小时");
			seconds = seconds + Integer.parseInt(h[0]) * 60*60;
			String[] m = h[1].split("分钟");
			seconds = seconds + Integer.parseInt(m[0]) * 60;
			String[] s = m[1].split("秒");
			seconds = seconds + Integer.parseInt(s[0]);
		}else if(time.contains("小时")){
			String[] h = time.split("小时");
			seconds = seconds + Integer.parseInt(h[0]) * 60*60;
			String[] m = h[1].split("分钟");
			seconds = seconds + Integer.parseInt(m[0]) * 60;
			String[] s = m[1].split("秒");
			seconds = seconds + Integer.parseInt(s[0]);
		}else if(time.contains("分钟")){
			String[] m = time.split("分钟");
			seconds = seconds + Integer.parseInt(m[0]) * 60;
			String[] s = m[1].split("秒");
			seconds = seconds + Integer.parseInt(s[0]);
		}else if(time.contains("秒")){
			String[] s = time.split("秒");
			seconds = seconds + Integer.parseInt(s[0]);
		}
		return seconds;
		
	}
	
	/**
	 * 删除切图任务
	 */
	@Override
	public void deleteTileTask(List<String> ids,boolean isDelExportData) {
		DataSourceContextHolder.setDataSource("default");
		List<TileTaskEntity> list = tileCacheMapper.queryTileTaskEntity(ids);
		for (TileTaskEntity tileTaskEntity : list) {
			SeedTaskInstance seedTask = seedTaskUtil.getTaskInstance(tileTaskEntity.getTaskName());
			//清理对应的缓存
			if (seedTask != null) {
				seedTaskUtil.clearFinshedTiles(seedTask.getTaskName());
				if (isDelExportData && seedTask.getStoreType() == StoreType.TMS.getTypeId()) delExportData(seedTask);
				if (seedTask.getStoreType() == StoreType.TMS.getTypeId()) {
					String tmsPath = seedTask.getStorePath();
					StringBuffer stringBuffer = new StringBuffer(tmsPath);
					stringBuffer.append(File.separator).append("temp");
					FileUtil.deleteFile(stringBuffer.toString());
				} 
			}
			if (tileTaskEntity.getTaskStatus() == TileTaskStatus.DEAD.getStatus()) {
				TileTaskManager.removeSuspendTileTask(seedTask);
			} else if (tileTaskEntity.getTaskStatus() == TileTaskStatus.DONE.getStatus()) {
				
			} else if (tileTaskEntity.getTaskStatus() == TileTaskStatus.RUNNING.getStatus()) {
				TileTaskManager.removeDoingTileTask(seedTask);
				//停止线程
				stopThread(seedTask.getTaskName());
			} else if (tileTaskEntity.getTaskStatus() == TileTaskStatus.READY.getStatus()) {
				//从等待队列中删除
				TileTaskManager.removeWaitTileTask(seedTask);
			}
			if (seedTask != null){
				seedTaskUtil.updateServerStatus(seedTask.getLayerName(),ServerStatusEnum.PUBLISHED.getCode());
			}
		}
		DataSourceContextHolder.setDataSource("default");
		tileCacheMapper.deleteTileTaskEntityById(ids);
		//暂时不清除切图实例任务，清除缓存的时候，则清除对应的实例任务
//		tileCacheMapper.deleteTileTaskInstance(ids);
	}
	
	public void delExportData(SeedTaskInstance seedTask) {
		String zipName = ExportTaskUtils.getExportDataName(seedTask);
		File file = new File(seedTask.getStorePath(),zipName + ".zip");
		if (file.exists()) file.delete();
		ExportTaskUtils.cleanTmsZipCache(zipName);
	}

	@Override
	public void flushTileTask(String layerName,boolean flag,boolean isClearExportFile) {
		List<LayerStoreTypeInfo> layerStoreInfo = seedTaskUtil.getLayerStoreInfo(layerName);
		//1.如果任务存在，通过导出任务存储路径删除导出数据
		DataSourceContextHolder.setDataSource("default");
		List<SeedTaskInstance> tasks = tileCacheMapper.queryTaskInstancesByLayerName(layerName);
		if (tasks != null && tasks.size() != 0) {
			tasks.forEach((task) -> {
				seedTaskUtil.clearFinshedTiles(task.getTaskName());
				if (task.getStoreType() == StoreType.TMS.getTypeId() && isClearExportFile) {
					String zipName = ExportTaskUtils.getExportDataName(task);
					File file = new File(task.getStorePath(),zipName + ".zip");
					if (file.exists()) file.delete();
					ExportTaskUtils.cleanTmsZipCache(zipName);
				}
				//清除TDB数据读取相关缓存
				CustomDataLoadImpl.clearTDBCacheInfo(SeedTaskUtil.createTdbName(task.getTileFormat(), task.getTaskName(), task.getSrs(), task.getTileWidth()),task.getLayerName());
			});
		}
		//2.如果用户在删除导出任务时没有删除文件，在清除缓存时就需要在缓存路径下通过名称获取服务相关导出文件并删除
		DataSourceContextHolder.setDataSource("default");
		String exportPath =  tileCacheMapper.queryTileCacheConfig().getExportPath();
		if (StringUtils.isNotBlank(exportPath)) {
			File export = new File(exportPath);
			File[] files = export.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if (name.contains("_4326_") && name.endsWith(".zip")) {
						String nm = name.substring(0, name.indexOf("_4326"));
						if (nm.equals(layerName)) {
							return true;
						}
					}
					return false;
				}
			});
			if (files.length > 0) (Arrays.asList(files)).forEach(file -> file.delete());
		}
		//清除服务缓存时，同时将没有完成的任务同步删除暂停、运行以及等待中的任务
		RequestQueryCondition condition = new RequestQueryCondition();
		condition.setServerName(layerName);
		List<TileTaskEntity> entities = queryTileTaskInfo(condition);
		List<String> taskNames = new ArrayList();
		entities.forEach(entity -> {
			if (entity.getTaskStatus() != 4) {
				taskNames.add(entity.getTaskName());
				deleteTileTask(taskNames,true);
				taskNames.clear();
			}
				
		});
		
		if (layerStoreInfo != null && !layerStoreInfo.isEmpty()) {
			for (LayerStoreTypeInfo layerStoreTypeInfo : layerStoreInfo) {
				String dbName = layerStoreTypeInfo.getDataSource();
				if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.SQLITE)) {
					String storePath = new StringBuilder(layerStoreTypeInfo.getStorePath()).append(File.separator).append(layerStoreTypeInfo.getLayerName()).toString();
					flushAndDeleteSqlite(dbName,storePath);
//				}else if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.POSTGRRSQL)) {
//					flushAndDeletePsql(dbName,flag);
				}else if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.TDB)) {
					flushAndDeleteTdb(dbName,layerStoreTypeInfo.getStorePath(),false);
				}else if (StoreType.getStoreType(layerStoreTypeInfo.getStoreTypeId()).equals(StoreType.MONGODB)) {
					flushAndDeleteMongodb(dbName);
				}
			}
		}
		if (tasks != null && tasks.size() != 0) {
			SeedTaskInstance task = tasks.get(0);
			clearTileTaskTables(task.getLayerName(),task.getTaskId());
		}
	}
	
	private void clearTileTaskTables(String layerName,Integer serverId) {
		DataSourceContextHolder.setDataSource("default");
		commonMapper.deleteDynamicTableInfo(layerName,null);
		commonMapper.deleteTaskEntityInfo(serverId);
		commonMapper.deleteTileTaskInfo(serverId,null);
		Set<String> layers = new HashSet<String>();
		layers.add(layerName);
		commonMapper.deleteLayerStore(layers);
	}
	private void flushAndDeletePsql(String dbSource,boolean isDel) {
		if (!isDel) {
			//清空该图层对应数据库下的所有表
			List<String> tableNames = seedTaskUtil.getTblNames(dbSource);
			DataSourceContextHolder.setDataSource(dbSource);
			commonMapper.flushTable(tableNames);
		}else {
			DataSourceContextHolder.setDataSource("default");
			commonMapper.terminateBackend(dbSource);
			commonMapper.dropDataBase(Arrays.asList(dbSource));
			DataSourceUtil.removeDataSource(dbSource);
			seedTaskUtil.removeDbAndTables(dbSource);
		}
	}
	
	private void flushAndDeleteMongodb(String collectionName) {
		mongoTemplate.dropCollection(collectionName);
	}
	
	/**
	 * 清空或删除tdb缓存
	 * 清空的话，删除文件夹下的文件
	 * 删除的话，整个文件夹全部删除
	 */
	private void flushAndDeleteTdb(String dbSource,String storePath,boolean isDel) {
		File file = new File(storePath,dbSource);
		FileUtil.deleteFile(file.getAbsolutePath());
		//删除文件，如果只是清空，则下次重新切的时候，不会生成
		file.delete();
	}
	
	/**
	 * 删除当前图层下的数据信息
	 * 清空数据源下的动态数据库
	 * 删除数据源
	 */
	private void flushAndDeleteSqlite(String dbSource,String storePath) {
		DataSourceContextHolder.setDataSource("default");
		commonMapper.deleteSourceTable(dbSource);
		//删除数据源
		DataSourceUtil.removeDataSource(dbSource);
		File file = new File(storePath,dbSource + ".db");
		if (file.exists() && file.length() > 0) {
			file.delete();
			file.getParentFile().delete();
		}
	}
	
	@Override
	public void resetLogbackInfo(UserLogbackInfo logInfo) {
		logBackXmlConfigHelper.resetLogbackInfo(logInfo, LogBackXmlConfigHelper.tileserverLogBack);
	}

	@Override
	public void stopSeedTaskByIds(List<Integer> ids) {
		DataSourceContextHolder.setDataSource("default");
		//获取当前图层所有正在进行中的任务
		List<TileTaskEntity> taskEntitys = tileCacheMapper.queryTileTaskEntityByServerId(ids);
		Set<String> layerNames = null;
		if (taskEntitys != null && !taskEntitys.isEmpty()) {
			layerNames = clearTileTaskEntity(taskEntitys);
		}else {
			List<String> list = tileCacheMapper.queryTileLayerName(ids);
			layerNames = new HashSet<>(list);
		}
		if (layerNames != null && !layerNames.isEmpty()) {
			for (String layerName : layerNames) {
				List<SeedTaskInstance> instances = tileCacheMapper.queryTaskInstancesByLayerName(layerName);
				if (instances.size() > 0) {
					instances.forEach(task -> {
						String keyWord = SeedTaskUtil.createTdbName(task.getTileFormat(), task.getTaskName(), task.getSrs(), task.getTileWidth());
						CustomDataLoadImpl.clearTDBCacheInfo(keyWord,layerName);
					});
				}
				updateCacheInfo(layerName);
				flushTileTask(layerName,true,true);
			}
			commonMapper.deleteLayerStore(layerNames);
			tileCacheMapper.deleteSourceTable(layerNames);
		}
	}

	public Set<String> clearTileTaskEntity(List<TileTaskEntity> taskEntitys) {
		Set<String> layerNames = new HashSet<String>();
		for (TileTaskEntity tileTaskEntity : taskEntitys) {
			String taskName = tileTaskEntity.getTaskName();
			String layerName = tileTaskEntity.getLayerName();
			SeedTaskInstance taskInstance = seedTaskUtil.getTaskInstance(taskName);
			if (tileTaskEntity.getTaskStatus() == TileTaskStatus.RUNNING.getStatus()) {
				TileTaskManager.removeDoingTileTask(taskInstance);
				stopThread(taskName);
			} else if (tileTaskEntity.getTaskStatus() == TileTaskStatus.READY.getStatus()) {
				TileTaskManager.removeWaitTileTask(taskInstance);
			} else if (tileTaskEntity.getTaskStatus() == TileTaskStatus.DEAD.getStatus()) {
				TileTaskManager.removeSuspendTileTask(taskInstance);
			}
			layerNames.add(layerName);
		}
		return layerNames;
	}

	@Override
	public void seedTaskByLayerName(String layerName, int serverId, String reqIP, int openOverView) {
		//构造切图任务信息
		SeedTaskInstance seedTaskInstance = new SeedTaskInstance();
		LayerBaseInfo layerBaseInfo = seedTaskUtil.queryServerBaseInfo(layerName);
		SeedTaskParam seedTaskParam = tileCacheMapper.queryGlobalSeedParam();
		seedTaskInstance.setLayerName(layerName);
		seedTaskInstance.setTileWidth(seedTaskParam.getTileWidth());
		seedTaskInstance.setTileHeight(seedTaskParam.getTileHeight());
		seedTaskInstance.setTaskId(serverId);
		if("同源数据".equals(seedTaskParam.getGridSetName())) {
			seedTaskInstance.setSrs(layerBaseInfo.getResourceSrs());
		} else {
			seedTaskInstance.setSrs(seedTaskParam.getGridSetName());
		}
		String bboxStr = layerBaseInfo.getNativaBbox();
		String[] bboxArray = bboxStr.split(";");
		seedTaskInstance.setMinX(Double.parseDouble(bboxArray[0]));
		seedTaskInstance.setMaxX(Double.parseDouble(bboxArray[2]));
		seedTaskInstance.setMinY(Double.parseDouble(bboxArray[1]));
		seedTaskInstance.setMaxY(Double.parseDouble(bboxArray[3]));
		seedTaskInstance.setZoomStart(0);
		seedTaskInstance.setSeedType(seedTaskParam.getType());
		seedTaskInstance.setZoomEnd(Math.min(tileCacheMapper.queryGlobalSeedParam().getDefaultMaxZoomLevel(), layerBaseInfo.getSupportMaxLevel()));
		int dataformat = layerBaseInfo.getIsLayerGroup() == 1 ? -1 : tileCacheMapper.queryDataformatByServerid(layerBaseInfo.getServerId());
		if (dataformat == 1) {
			seedTaskInstance.setTileFormat(seedTaskParam.getRasterFormat());
		} else if (dataformat == 2) {
			seedTaskInstance.setTileFormat(seedTaskParam.getDemFormat());
			if (seedTaskParam.getDemFormat().endsWith("terrain")) {
				seedTaskInstance.setTileWidth(65);
				seedTaskInstance.setTileHeight(65);
			}
		}else if (dataformat == -1){
			//图层组自动切图时，没有dataFormat
			seedTaskInstance.setTileFormat("image/jpeg");
		} else {
			seedTaskInstance.setTileFormat(seedTaskParam.getVectorFormat());
		}
		seedTaskByParam(seedTaskInstance, openOverView);
	}

	@Override
	public void updateCacheInfo(String layerName) {
		//清除layerbaseInfo信息
		seedTaskUtil.removeLayerStoreType(layerName);
	}

	@Override
	public void deleteTdbCacheInfo(String taskName,String serverName) {
		DataSourceContextHolder.setDataSource("default");
		//清除TDB数据读取相关缓存
		SeedTaskInstance instance = tileCacheMapper.queryTaskInstance(taskName);
		if (instance == null) return;
		String keyWord = SeedTaskUtil.createTdbName(instance.getTileFormat(), instance.getTaskName(), instance.getSrs(), instance.getTileWidth());
		CustomDataLoadImpl.clearTDBCacheInfo(keyWord,serverName);
	}

	@Override
	public void addWaitTileTask(String serverName) {
		TileTaskManager.addWaitTileTask(serverName);
		startTileThread();
	}
}
