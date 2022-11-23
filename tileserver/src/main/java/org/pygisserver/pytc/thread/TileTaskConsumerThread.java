package org.pygisserver.pytc.thread;

import java.io.File;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.SeedTaskParam;
import org.pygisserver.common.entity.TileTaskEntity;
import org.pygisserver.common.enumm.ServerStatusEnum;
import org.pygisserver.common.enumm.StoreType;
import org.pygisserver.common.enumm.TileTaskStatus;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.pytc.common.TileTaskManager;
import org.pygisserver.pytc.entity.TileTask;
import org.pygisserver.pytc.entity.TileTask.TILESTATUS;
import org.pygisserver.pytc.util.ExportTaskUtils;
import org.pygisserver.pytc.util.SeedTaskUtil;

/**
 * @Description: 执行切图任务任务的消费线程
 * 主要用来实现执行当前切图任务，
 * @author: 
 * @date: 2021-03-26
 */
public class TileTaskConsumerThread implements Runnable {

	private SeedTaskUtil seedTaskUtil;
	
	public TileTaskConsumerThread(SeedTaskUtil seedTaskUtil) {
		this.seedTaskUtil = seedTaskUtil;
	}
	
	/**
	 * 开启标识
	 */
	private volatile static boolean startFlag = false;
	
	@Override
	public void run() {
		while(startFlag) {
			//检查进行中的任务是否完成
			checkSubmitTaskIsComplete();
			//检查是否需要启动等待中的任务
			checkAwaitTaskIsLaunch();
			//是否需要跳出循环
			checkThreadIsStop();
		}
	}

	/**
	 * 检查是否需要启动等待中的任务
	 */
	private void checkAwaitTaskIsLaunch() {
		int num = TileTaskManager.getDoingTaskBlockQueue().size();
		//如果进行中的任务数量小于目前设置的最大进行任务数量，则从等待队列中获取，并启动
		SeedTaskParam seedParam = seedTaskUtil.getGlobalSeedParam();
		if (num < seedParam.getMaxTaskNum()) {
			SeedTaskInstance taskInstance = TileTaskManager.nextWaitTileTaskBlockQueue();
			if (taskInstance != null) {
				TileTaskManager.addDoingTileTask(taskInstance);

				// 将切图任务开始的时间作为切图工作的开始执行时间
				taskInstance.setStartTime(new Date());
				seedTaskUtil.dispatchTask(taskInstance);
				seedTaskUtil.updateServerStatus(taskInstance.getLayerName(),ServerStatusEnum.TILEING.getCode());
			}
		}
	}
	
	/**
	 * 检查当前任务是否已经完成
	 */
	private void checkSubmitTaskIsComplete() {
		List<SeedTaskInstance> seedTaskInstances = TileTaskManager.getDoingTaskBlockQueue();
		if (seedTaskInstances != null && !seedTaskInstances.isEmpty()) {
			Iterator<SeedTaskInstance> it = seedTaskInstances.iterator();
			boolean isdel = false;
			while (it.hasNext() && !isdel){
				SeedTaskInstance seedTaskInstance = it.next();
				boolean flag = checkedTileLayer(seedTaskInstance.getTaskName());
				if (flag) {
					if(seedTaskInstance.getStoreType() == StoreType.TMS.getTypeId()) {
						String tmsPath = seedTaskInstance.getStorePath();
						StringBuffer stringBuffer = new StringBuffer(tmsPath);
						stringBuffer.append(File.separator).append("temp");
						FileUtil.deleteFile(stringBuffer.toString());
					}
					//更新任务状态
					long endTime = System.currentTimeMillis();
					long spentTime = (endTime - seedTaskInstance.getStartTime().getTime()) / 1000;
					TileTaskEntity tileTaskEntity = new TileTaskEntity(seedTaskInstance.getTaskName(),"100%","--","--");
					tileTaskEntity.setSpentTime(spentTime);
					tileTaskEntity.setTaskEndTime(new Date().getTime());
					tileTaskEntity.setTaskStatus(TileTaskStatus.DONE.getStatus());
					TileTaskManager.removeDoingTileTask(seedTaskInstance);
					isdel = true;
					seedTaskUtil.updateTileEntity(tileTaskEntity);
					seedTaskUtil.drain();
					//清除TMS导出相关缓存信息
					if (seedTaskInstance.getStoreType() == StoreType.TMS.getTypeId()) {
						ExportTaskUtils.cleanTmsZipCache(ExportTaskUtils.getExportDataName(seedTaskInstance));
					}
					//推送消息
					seedTaskUtil.updateServerStatus(seedTaskInstance.getLayerName(),ServerStatusEnum.PUBLISHED.getCode());
					seedTaskUtil.noticeWebSocketTileFinshed(MessageFormat.format("切图任务{0}已经完成", seedTaskInstance.getLayerName()));
				}
			}
		}
	}
	
	private boolean checkedTileLayer(String taskName) {
		List<TileTask> list = seedTaskUtil.filterTileTask(TILESTATUS.RUNNING,TILESTATUS.DONE);
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (TileTask tileTask : list) {
			if (tileTask.getTaskName().equals(taskName)) {
				if (tileTask.getThreadState() != TILESTATUS.DONE) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	
	public static boolean isStart() {
		return startFlag;
	}
	
	public static void setStart() {
		startFlag = true;
	}
	
	private void stop() {
		startFlag = false;
	}
	
	/**
	 * 检查是否需要跳出线程的循环执行
	 */
	private void checkThreadIsStop() {
		if (TileTaskManager.isEmpty()) {
			stop();
		}
	}
}
