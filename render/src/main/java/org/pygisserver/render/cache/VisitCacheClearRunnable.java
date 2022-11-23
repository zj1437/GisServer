package org.pygisserver.render.cache;

import org.pygisserver.common.util.FileUtil;
import org.pygisserver.render.tiled.TiledInfoReadAbstract;

import java.io.File;

public class VisitCacheClearRunnable implements Runnable {
	//需要清除的目录
	private String filePath;
	//需要清除的目录
	private String newPath;
	//是否将整个目录删除
	private boolean isClearDir;
	//最大缓存数，为0时表明不需要业务处理
	private long maxCacheSize;
	
	//是否将整个目录删除
	private boolean isMoveCacheFile;
	
	public VisitCacheClearRunnable(String filePath, String newPath, boolean isClearDir, long maxCacheSize, boolean isMoveCacheFile) {
		this.filePath = filePath;
		this.newPath = newPath;
		this.isClearDir = isClearDir;
		this.maxCacheSize = maxCacheSize;
		this.isMoveCacheFile = isMoveCacheFile;
	}

	@Override
	public void run() {
		String chachePath = null;
		File file = new File(filePath);
		if(!file.getName().equals("PYGISServerCache")) {
			file = new File(filePath,"PYGISServerCache");
			chachePath = file.getAbsolutePath();
			if(!file.exists()) return;
		}
		File[] files = file.listFiles();
		if(files.length == 0 || files == null) return;
		//1.如果是切换缓存目录，则将该目录删除
		if(isClearDir) {
			if (isMoveCacheFile) {
				String path = null;
				File file1 = new File(newPath);
				if(!file1.getName().equals("PYGISServerCache")) {
					path = file1.getAbsolutePath();
				} else {
					path = file1.getParentFile().getAbsolutePath();
				}
				FileUtil.moveFile(path, chachePath);
			} else {
				FileUtil.deleteFile(file.getAbsolutePath());
			}
		} else {
			//2.缓存瓦片时如果磁盘空间补足，需要根据使用频率进行筛选和删除
			if(maxCacheSize == 0) {
				clearVisitCache(file);
			} else {
				//3.设置最大缓存配置参数时，如果当前存储的文件数大于最大设置数，需要根据使用频率删除文件直到在规定的设置值之内
				while (maxCacheSize < FileUtil.getFileLength(filePath)) {
					clearVisitCache(file);
				}
			}
			//4.更新当前缓存目录下的磁盘使用情况。这里需要注意的是，用户切换目录后，新目录可能为空或者只有少部分数据，此时可以忽略为0
			TiledInfoReadAbstract.curDirSize = FileUtil.getFileLength(filePath);
		}
	}
	
	private void clearVisitCache(File file) {
		File clrFile = null;
		File[] files = file.listFiles();
		for (File f : files) {
            if(clrFile == null) {
                clrFile = f;
                continue;
            } else {
                if(clrFile.lastModified() > f.lastModified()){
                    clrFile = f;
                }
            }
        }
		//清除该目录
		if(clrFile.exists()) {
			FileUtil.deleteFile(clrFile.getAbsolutePath());
		}
	}

}
