package org.pygisserver.pytc.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.pytc.entity.TileTask.TILESTATUS;
import org.pygisserver.pytc.util.SeedTaskUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SeedTask {

	private TileTask tileTask;
	
	private SeedTaskUtil seedTaskUtil;

	private GuassCoordinates guassCoordinates;
	
	/**
	 * 记录已处理的瓦片数
	 */
	private AtomicLong atomicLong;
	
	/**
	 * 记录完成瓦片数
	 */
	private AtomicLong atomicLongNum;
	
	private AtomicBoolean atomicStart;
	
	public SeedTask(TileTask tileTask, 
					SeedTaskUtil seedTaskUtil, 
					GuassCoordinates guassCoordinates) {
		this.tileTask = tileTask;
		this.seedTaskUtil = seedTaskUtil;
		this.guassCoordinates = guassCoordinates;
		this.atomicLong = new AtomicLong(0);
		this.atomicLongNum = new AtomicLong(0);
		this.atomicStart = new AtomicBoolean(true);
	}
	
	public SeedTask(TileTask tileTask, SeedTaskUtil seedTaskUtil) {
		this.tileTask = tileTask;
		this.seedTaskUtil = seedTaskUtil;
	}
	
	
	public String begin(int index) {
		//当前已经完成的小于总数即可不停的去处理
		synchronized (this) {
			if (tileTask.getStartTime() == 0l) {
				tileTask.setStartTime(System.currentTimeMillis());
			}
		}
		tileTask.setThreadState(TILESTATUS.RUNNING);
		while(!tileTask.isTerminate() && (atomicLong.get() < tileTask.getTotalSize() || !tileTask.getSeedTaskTileQueue().isEmpty())) {
			int[] tileIndex = tileTask.getSeedTaskTileQueue().getTileTask(index);
			if (tileIndex == null) {
				return "切图执行完毕";
			}
			//单个数组完成的数量
			int totalSize = (tileIndex[2] - tileIndex[0] + 1) * (tileIndex[3] - tileIndex[1] + 1);
			//判断第一个瓦片如果存在，则跳过
			int[] firstTile = new int[] {tileIndex[0],tileIndex[1],tileIndex[4]};
			
			if (seedTaskUtil.isExists(firstTile, tileTask.getTaskName())) {
				//已完成的数量
//				updateSeedProgress(totalSize);
				atomicLong.getAndAdd(totalSize);
//				tileTask.getTilesDone().set(tileTask.getTilesDone().addAndGet(totalSize));
				continue;
			}
			try {
				if (tileTask.getMediaLevel() != -1 && index == tileTask.getThreadCount() - 1) {
					SeedTaskInstance taskInstance = seedTaskUtil.getTaskInstance(tileTask.getTaskName());
					for (int i = tileIndex[1]; i <= tileIndex[3]; i++) {
						for (int j = tileIndex[0]; j <= tileIndex[2]; j++) {
							int[] singleTileIndex = {j,i,tileIndex[4]};
							TileResourceInfo tileResourceInfo = seedTaskUtil.composeTile(singleTileIndex,taskInstance);
							seedTaskUtil.saveTileResource(tileResourceInfo,taskInstance,taskInstance.getStyleName());
						}
					}
				}else {
					PyRenderRequestParam renderRequestParam = constructRenderRequestParam(tileIndex);
					//开始请求渲染
					seedTaskUtil.startSeedTileTask(renderRequestParam,tileIndex,tileTask.getTaskName());
				}
				//中间数据完成后，开启拼接
				if (tileTask.getMediaLevel() != -1 && atomicStart.get()) {
					//开始一定是从中间计算，只要计数器到了这个值，就可以开启
					if (atomicLong.get() >= tileTask.getMediaLvelTileCount()) {
						synchronized (this) {
							if (atomicStart.get()) {
								atomicStart.set(false);
								startCombineTileThread(this.tileTask.getThreadCount() - 1);
							}
						}
					}
				}
			} catch (Exception e) {
				//记录某个瓦片渲染失败，是否需要重新再次启动渲染
				log.error("图层{},渲染瓦片{}失败", tileTask.getLayerName(),Arrays.toString(tileIndex));
//				updateSeedProgress(totalSize);
				atomicLong.getAndAdd(totalSize);
				continue;
			}
			atomicLong.getAndAdd(totalSize);
			updateSeedProgress(totalSize);
		}
		tileTask.setThreadState(TILESTATUS.DONE);
		return "切图执行完毕";
	}

	private void startCombineTileThread(int index) {
//		//两个线程用来处理拼接请求
//		for (int i = 0; i < 2; i++) {
			new Thread(()->{
				//保证所有的数据全部写入
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				this.begin(index);
			}).start();
//		}
	}
	
	
	/**
	 * 更新服务切图进度
	 * @param totalSize
	 */
	private void updateSeedProgress(int totalSize) {
		atomicLongNum.getAndAdd(totalSize);
		tileTask.setTilesDone(atomicLongNum.get() > tileTask.getTotalSize() ? tileTask.getTotalSize() : atomicLongNum.get());
		long currentTime = System.currentTimeMillis();
		long spentTime = currentTime - tileTask.getStartTime();
		tileTask.setSpentTime((int) spentTime / 1000); 
	}
	
	private PyRenderRequestParam constructRenderRequestParam(int[] tile) {
		SeedTaskInstance taskInstance = seedTaskUtil.getTaskInstance(tileTask.getTaskName());
		String srs = taskInstance.getSrs();
		ReferencedEnvelope envelope = null;
		if (OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
			//最小x,最小y
			envelope = GeometryUtil.calcute3857bbox((int)tile[4], (int)tile[1], (int)tile[0], "EPSG:3857");
			ReferencedEnvelope envelopeRight = GeometryUtil.calcute3857bbox((int)tile[4], (int)tile[3], (int)tile[2], "EPSG:3857");
			envelope.expandToInclude(envelopeRight);
		} else if (guassCoordinates != null && guassCoordinates.getGuassCoordinates().contains(srs)) {
			Map<String, List<String>> guassCoordinatePara = guassCoordinates.getTileParam().get(srs.replace(":", ""));
			envelope = GeometryUtil.calcuteGuassBbox((int)tile[4], (int)tile[1], (int)tile[0], srs, guassCoordinatePara);
			ReferencedEnvelope envelopeRight = GeometryUtil.calcuteGuassBbox((int)tile[4], (int)tile[3], (int)tile[2], srs, guassCoordinatePara);
			envelope.expandToInclude(envelopeRight);
		} else if (OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
			envelope = GeometryUtil.calcute4326bbox((int)tile[4], (int)tile[1], (int)tile[0]);
			ReferencedEnvelope envelopeRight = GeometryUtil.calcute4326bbox((int)tile[4], (int)tile[3], (int)tile[2]);
			envelope.expandToInclude(envelopeRight);
		}
		int metaTileWidth = (int) (tile[2] - tile[0] + 1);
		int metaTileHeight = (int) (tile[3] - tile[1] + 1);
		//获取服务图层基础信息
		LayerBaseInfo layerBaseInfo = seedTaskUtil.queryServerBaseInfo(tileTask.getLayerName());
		PyRenderRequestParam renderParam = new PyRenderRequestParam();
		renderParam.setFileUrl(layerBaseInfo.getFilePath());  //TODO: 如果是远程文件路经，如何处理
		renderParam.setStyleName(layerBaseInfo.getStyleName());  //直接使用样式名称，保证在redis中已经处理
		StringBuilder sb = new StringBuilder().append(envelope.getMinimum(0)).append(",").append(envelope.getMaximum(0)).
				append(",").append(envelope.getMinimum(1)).append(",").append(envelope.getMaximum(1));
		renderParam.setRequestBbox(sb.toString());
		renderParam.setFormat(taskInstance.getTileFormat());
		renderParam.setLayerGroup(layerBaseInfo.getIsLayerGroup()==1); //1是图层组
		renderParam.setLayerName(tileTask.getLayerName());
		renderParam.setMapHeight(taskInstance.getTileHeight()*metaTileHeight);
		renderParam.setMapWidth(taskInstance.getTileWidth()*metaTileWidth);
		renderParam.setTransparent(true);
		renderParam.setDataFormat(layerBaseInfo.getFileType());
		renderParam.setDataType(layerBaseInfo.getDataType());
		renderParam.setRequestSrs(taskInstance.getSrs());
		renderParam.setResourcesSrs(layerBaseInfo.getResourceSrs());
		if ("PostGIS".equalsIgnoreCase(layerBaseInfo.getFileType())) {
			PostGisConnectEntity postGisConnectEntity = seedTaskUtil.queryPostGisInfos(layerBaseInfo.getFilePath().substring(0, layerBaseInfo.getFilePath().indexOf(";")));
			renderParam.setPostgisConnection(postGisConnectEntity);
		}
		return renderParam;
	}
}
