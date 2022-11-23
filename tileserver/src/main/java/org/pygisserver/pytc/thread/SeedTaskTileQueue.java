package org.pygisserver.pytc.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author ly
 *
 */
@Data
@Slf4j
public class SeedTaskTileQueue {

	private String taskName;
	
	private List<LinkedBlockingQueue<int[]>> tileTaskQueues;
	
//	private LinkedBlockingQueue<int[]> tileTaskQueue;
	
	private int threadCount;
	
	private AtomicInteger num = new AtomicInteger(0);
	
	public SeedTaskTileQueue(String taskName,int queueInitSize,int threadCount) {
		this.taskName = taskName;
		this.threadCount = threadCount;
		tileTaskQueues = new ArrayList<LinkedBlockingQueue<int[]>>(threadCount);
		for (int i = 0; i < threadCount; i++) {
			LinkedBlockingQueue<int[]> tileTaskQueue = new LinkedBlockingQueue<int[]>(queueInitSize);
			tileTaskQueues.add(tileTaskQueue);
		}
//		tileTaskQueue = new LinkedBlockingQueue<int[]>(queueInitSize);
	}
	
	public synchronized void addTileTask(int[] tile,boolean isRaster,boolean isStart) {
		try {
			int index = -1;
			if (isRaster && !isStart) {
				index = num.get() % (threadCount - 1);
			} else if (isRaster && isStart) {
				index = threadCount - 1;
			} else {
				index = num.get() % threadCount;
			}
			this.tileTaskQueues.get(index).put(tile);
//			this.tileTaskQueue.put(tile);
			num.incrementAndGet();
		} catch (InterruptedException e) {
			log.error("SeedTaskTileQueue addTileTask has error {}",e.getLocalizedMessage());
		}
	}
	
	/**
	 * 如果5s未获取到数据，则直接返回空
	 * 前端业务直接报告完成即可
	 * @param index
	 * @return
	 */
	public int[] getTileTask(int index) {
		try {
			return this.tileTaskQueues.get(index).poll(60, TimeUnit.SECONDS);
//			return this.tileTaskQueue.poll(8, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("SeedTaskTileQueue getTileTask has error {}",e.getLocalizedMessage());
			return null;
		}
	}
	
	
	public boolean isEmpty() {
		for (LinkedBlockingQueue<int[]> linkedBlockingQueue : tileTaskQueues) {
			if (!linkedBlockingQueue.isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
}
