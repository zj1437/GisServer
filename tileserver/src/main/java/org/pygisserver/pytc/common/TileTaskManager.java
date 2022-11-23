package org.pygisserver.pytc.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.pygisserver.common.entity.SeedTaskInstance;

/**
 * @Description: 切图任务管理
 * 主要管理进行中切图任务、等待中的切图任务、暂停中的切图任务 
 * @author:  
 * @date: 2021-03-26
 */
public class TileTaskManager {
	
	private static final int MAX_TILE_TASK = 100; 
	
	/**
	 * 正在进行中的任务队列
	 */
	protected static List<SeedTaskInstance> tileTaskDoingQueue = new ArrayList<SeedTaskInstance>();

	/**
	 * 等待中的任务队列
	 */
	protected static BlockingQueue<SeedTaskInstance> tileTaskWaitQueue = new ArrayBlockingQueue<SeedTaskInstance>(MAX_TILE_TASK);

	/**
	 * 等待中的任务队列
	 */
	protected static BlockingQueue<SeedTaskInstance> tileTaskPreWaitQueue = new ArrayBlockingQueue<SeedTaskInstance>(MAX_TILE_TASK);
	
	/**
	 * 暂停中的任务队列
	 */
	protected static LinkedList<SeedTaskInstance> tileTaskSuspendQueue = new LinkedList<SeedTaskInstance>();

	/**
	 * 获取等待队列中下一个要处理任务
	 * @return
	 */
	public synchronized static SeedTaskInstance nextWaitTileTaskBlockQueue() {
		return tileTaskWaitQueue.poll();
	}
	
	/**
	 * 获取等待队列中的任务
	 * @return
	 */
	public synchronized static BlockingQueue<SeedTaskInstance> getWaitTileTaskBlockQueue() {
		return tileTaskWaitQueue;
	}
	
	
	/**
	 * 获取暂停中的任务
	 * @return
	 */
	public synchronized static LinkedList<SeedTaskInstance> getSuspendTileTaskBlockQueue() {
		return tileTaskSuspendQueue;
	}
	
	/**
	 * 获取正在进行的切图任务队列
	 * @return
	 */
	public synchronized static List<SeedTaskInstance> getDoingTaskBlockQueue() {
		return tileTaskDoingQueue;
	}
	
	/**
	 * 等待队列中插入元素
	 * @param taskInstance
	 */
	public synchronized static void addWaitTileTask(SeedTaskInstance taskInstance) {
		try {
			tileTaskWaitQueue.put(taskInstance);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 等待队列中插入元素
	 * @param taskInstance
	 */
	public synchronized static void addPreWaitTileTask(SeedTaskInstance taskInstance) {
		try {
			tileTaskPreWaitQueue.put(taskInstance);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 等待队列中插入元素
	 * @param serverName
	 */
	public synchronized static void addWaitTileTask(String serverName) {
		SeedTaskInstance poll = tileTaskPreWaitQueue.poll();
		while(!poll.getLayerName().equals(serverName)) {
			tileTaskPreWaitQueue.add(poll);
			poll = tileTaskPreWaitQueue.poll();
		}
		try {
			tileTaskWaitQueue.put(poll);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 进行中的队列中加入元素
	 * @param taskInstance
	 */
	public synchronized static void addDoingTileTask(SeedTaskInstance taskInstance) {
		tileTaskDoingQueue.add(taskInstance);
	}
	
	/**
	 * 暂停队列中插入元素
	 * @param taskInstance
	 */
	public synchronized static void addSuspendTileTask(SeedTaskInstance taskInstance) {
		tileTaskSuspendQueue.add(taskInstance);
	}
	
	/**
	 * 暂停队列中移除指定元素
	 * @param taskInstance
	 */
	public synchronized static void removeSuspendTileTask(SeedTaskInstance taskInstance) {
		Iterator<SeedTaskInstance> it= tileTaskSuspendQueue.iterator();
		while (it.hasNext()) {
			SeedTaskInstance instance = it.next();
			if (instance.getTaskName().equals(taskInstance.getTaskName())) {
				it.remove();
			}
		}
	}
	
	/**
	 * 进行中任务移除
	 * @param taskInstance
	 */
	public synchronized static void removeDoingTileTask(SeedTaskInstance taskInstance) {
		Iterator<SeedTaskInstance> it = tileTaskDoingQueue.iterator();
		while (it.hasNext()) {
			SeedTaskInstance instance = it.next();
			if (instance.getTaskName().equals(taskInstance.getTaskName())) {
				it.remove();
			}
		}
	}
	
	/**
	 * 等待队列中任务移除
	 * @param taskInstance
	 */
	public synchronized static void removeWaitTileTask(SeedTaskInstance taskInstance) {
		Iterator<SeedTaskInstance> it= tileTaskWaitQueue.iterator();
		while (it.hasNext()) {
			SeedTaskInstance instance = it.next();
			if (instance.getTaskName().equals(taskInstance.getTaskName())) {
				it.remove();
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static boolean isEmpty() {
		return tileTaskWaitQueue.isEmpty() && tileTaskDoingQueue.isEmpty() && tileTaskSuspendQueue.isEmpty();
	}
}
