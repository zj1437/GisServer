package org.pygisserver.render.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.entity.TDBDataConfigOld;
import org.pygisserver.common.enumm.ErrorCode;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
//import org.pygisserver.common.tdb.util.TdbScheduledTask;
//import org.pygisserver.common.tdb.util.TdbUtils;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.render.cache.BundlxCache;
import org.pygisserver.render.service.CustomDataLoad;
import org.pygisserver.render.util.TdbScheduledTask;
import org.pygisserver.render.util.TdbUtils;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CustomDataLoadImpl implements CustomDataLoad {
	private TdbScheduledTask task = new TdbScheduledTask();
	private static final Lock lock = new ReentrantLock();
	public static final ConcurrentHashMap<String,Integer> tdbFileStoreMaxCol = new ConcurrentHashMap<String, Integer>();
	
	public static BundlxCache indexCache = new BundlxCache(10000);
	
	@Override
	public void insertTileImage(TileResourceInfo tileResourceInfo, TDBDataConfig config,Map<String, String> tdbParams, String rowNum) {
		if (tileResourceInfo.getByteResource().length == 0) {
			return;
		}
		try {
			lock.lock();
			task.writeData(tileResourceInfo, config, tdbParams, rowNum);
		} catch (Exception e) {
			log.error("CustomDataLoadImpl insertTileImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, tdbParams.get("serverName") + "数据"));
		} finally {
			lock.unlock();
		}
	}

	public static void clearTDBCacheInfo(String keyWord,String serverName) {
		//tdb单个文件最大最小缓存信息与服务名称相关
		Set<String> tdbKeys = tdbFileStoreMaxCol.keySet();
		List<String> keys = new ArrayList<String>();
		if(tdbKeys.size() > 0) {
			Iterator<String> it = tdbKeys.iterator();
			while(it.hasNext()) {
				String key = it.next();
				if(key.contains(serverName)) {
					keys.add(key);
				}
			}
		}
		if(keys.size() > 0) {
			keys.forEach((key) -> {
				tdbFileStoreMaxCol.remove(key);
			});
		}
		TdbUtils.deleteTdbCache(keyWord,serverName);
		TdbScheduledTask.clearPosInfoMap(serverName);
	}
	
	@Override
	public TDBDataConfigOld analysisOldTdbConfig(String filepath, String serverName) {
		//如果文件不存在，返回null
		File file = new File(filepath);
		if (!file.exists()) {
			return null;
		}
		if (file.isDirectory()) {
        	file = new File(filepath,"config.tdb");
		}
		
		TDBDataConfigOld tdbDataConfig = null;
		String oldTdbMapKey = file.getParentFile().getName();
		if(serverName != null && TdbUtils.oldTdbMap.containsKey(oldTdbMapKey)) {
			tdbDataConfig = TdbUtils.oldTdbMap.get(oldTdbMapKey);
			if(tdbDataConfig != null) return tdbDataConfig;
		}
		if(tdbDataConfig == null) {
			tdbDataConfig = new TDBDataConfigOld();
		}
		FileChannel fileChannel = null;
		RandomAccessFile fWriter = null;
        
        byte[] bytes1 = new byte[1];
		byte[] bytes8 = new byte[8];
		byte[] bytes4 = new byte[4];
		ByteBuffer bytePos = null;
		try {
			fWriter = new RandomAccessFile(file.toString(),"rw");
			fileChannel = fWriter.getChannel();	
			long filesize = fileChannel.size();
			//切图任务开始第一个切图时，会存在配置文件获取不到问题，此时可能会进入旧版本配置文件解析流程，需要通过该配置屏蔽可能出现的解析问题
			if(filesize > 80) return null;
			
			fileChannel.position(4);
			bytePos = ByteBuffer.allocate(1);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes1);
			int isMercator = bytes1[0];
			boolean result = isMercator == 1 ? true : false;
			String srs = result ? "EPSG:3857" : "EPSG:4326";
			tdbDataConfig.setBIsMercator(result);
			
			fileChannel.position(5);
			bytePos = ByteBuffer.allocate(1);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes1);
			int isTex = bytes1[0];
			boolean re = isTex == 1 ? true : false;
			tdbDataConfig.setBIsTex(re);
			
			fileChannel.position(8);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer width = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer tileWidth = width.getInt();
			
			fileChannel.position(12);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer height = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer tileHeight = height.getInt();	
			
			fileChannel.position(24);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double minLa = bufferPos.getDouble();
			
			fileChannel.position(32);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos1 = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double minLo = bufferPos1.getDouble();
			
			fileChannel.position(40);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos2 = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double maxLa = bufferPos2.getDouble();
			
			fileChannel.position(48);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos3 = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double maxLo = bufferPos3.getDouble();
			
			int format = 0;
			int row = 0;
			int col = 0;
			if (filesize == 56 || filesize == 60) {
				format =  TdbUtils.readTdbCompressType(filepath,serverName,srs);
			} else {
				fileChannel.position(56);
				bytePos = ByteBuffer.allocate(4);
				fileChannel.read(bytePos);
				bytePos.flip();
				bytePos.get(bytes4);
				ByteBuffer extension = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
				format = extension.getInt();
				
				fileChannel.position(60);
				bytePos = ByteBuffer.allocate(4);
				fileChannel.read(bytePos);
				bytePos.flip();
				bytePos.get(bytes4);
				ByteBuffer maxRow = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
				row = maxRow.getInt();
				
				fileChannel.position(64);
				bytePos = ByteBuffer.allocate(4);
				fileChannel.read(bytePos);
				bytePos.flip();
				bytePos.get(bytes4);
				ByteBuffer maxCol = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
				col = maxCol.getInt();
			}
			tdbDataConfig.setDMinLat(minLa);
			tdbDataConfig.setDMinLon(minLo);
			tdbDataConfig.setDMaxLat(maxLa);
			tdbDataConfig.setDMaxLon(maxLo);
			tdbDataConfig.setNTleSizeX(tileWidth);
			tdbDataConfig.setNTleSizeY(tileHeight);
			tdbDataConfig.setExtension(format);
			tdbDataConfig.setDMaxRowNum(row);
			tdbDataConfig.setDMaxColNum(col);
		} catch (IOException e) {
			log.error("CustomDataLoadImpl analysisOldTdbConfig has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, serverName + "配置文件"));
		} finally {
			FileUtil.closeStream(fileChannel,fWriter);
		}
		if(serverName != null &&!TdbUtils.oldTdbMap.containsKey(oldTdbMapKey)) {
			TdbUtils.oldTdbMap.put(oldTdbMapKey, tdbDataConfig);
		}
		return tdbDataConfig;
	}
	
	@Override
	public TDBDataConfig analysisNewTdbConfig(String filepath,String serverName) {
		//如果文件不存在，返回null
		File file = new File(filepath);
		if (!file.exists()) {
			return null;
		}
		if (file.isDirectory()) {
		   	file = new File(filepath,"config.tdb");
		}
		
		TDBDataConfig tdbDataConfig = null;
		String tdbMapKey = file.getParentFile().getName();
		if(serverName != null && TdbUtils.tdbMap.containsKey(tdbMapKey)) {
			tdbDataConfig = TdbUtils.tdbMap.get(tdbMapKey);
			if(tdbDataConfig != null) return tdbDataConfig;
		}
		if(tdbDataConfig == null) {
			tdbDataConfig = new TDBDataConfig();
		}
		
        byte[] bytes1 = new byte[1];
        byte[] bytes4 = new byte[4];
		byte[] bytes8 = new byte[8];
		byte[] bytes20 = new byte[20];
		byte[] bytes100 = new byte[100];
		FileChannel fileChannel = null;
		RandomAccessFile fWriter = null;
		ByteBuffer bytePos = null;
		try {
			fWriter = new RandomAccessFile(file.toString(),"rw");
			fileChannel = fWriter.getChannel();
			if (fileChannel.size() < 80) return null;
			
			fileChannel.position(0);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			String versionChar = new String(bytes4);
			tdbDataConfig.setDwVersionNo(versionChar);
			
			fileChannel.position(4);
			bytePos = ByteBuffer.allocate(20);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes20);
			String srsChar = new String(bytes20);
			if(srsChar.contains(":4326")) {
				srsChar = "EPSG:4326";
			} else if (srsChar.contains(":3857")) {
				srsChar = "EPSG:3857";
			}
			tdbDataConfig.setSrsType(srsChar);
			
			fileChannel.position(24);
			bytePos = ByteBuffer.allocate(1);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes1);
			char tileTypeChar = (char)bytes1[0];
			tdbDataConfig.setCBlkType(tileTypeChar);
			
			fileChannel.position(25);
			bytePos = ByteBuffer.allocate(1);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes1);
			char compTypeChar = (char)bytes1[0];
			tdbDataConfig.setCCompressType(compTypeChar);
			
			fileChannel.position(26);
			bytePos = ByteBuffer.allocate(1);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes1);
			char typeChar = (char)bytes1[0];
			tdbDataConfig.setCSourceDataType(typeChar);
			
			fileChannel.position(28);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer width = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer tileWidth = width.getInt();	
			tdbDataConfig.setNTleSizeX(tileWidth);
			
			fileChannel.position(32);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer height = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer tileHeight = height.getInt();
			tdbDataConfig.setNTleSizeY(tileHeight);
			
			fileChannel.position(36);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer minL = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer minLod = minL.getInt();
			tdbDataConfig.setNMinLod(minLod);
			
			fileChannel.position(40);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer maxL = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer maxLod = maxL.getInt();
			tdbDataConfig.setNMaxLod(maxLod);
			
			fileChannel.position(44);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double minLa = bufferPos.getDouble();
			tdbDataConfig.setDMinLat(minLa);
			
			fileChannel.position(52);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos1 = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double minLo = bufferPos1.getDouble();
			tdbDataConfig.setDMinLon(minLo);
			
			fileChannel.position(60);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos2 = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double maxLa = bufferPos2.getDouble();
			tdbDataConfig.setDMaxLat(maxLa);
			
			fileChannel.position(68);
			bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes8);
			ByteBuffer bufferPos3 = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			double maxLo = bufferPos3.getDouble();
			tdbDataConfig.setDMaxLon(maxLo);
			
			fileChannel.position(76);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer maxX = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer maxCol = maxX.getInt();
			tdbDataConfig.setNPerFileBlkNumX(maxCol);
			
			fileChannel.position(80);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes4);
			ByteBuffer maxY = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			Integer maxRow = maxY.getInt();
			tdbDataConfig.setNPerFileBlkNumY(maxRow);
			
			fileChannel.position(84);
			bytePos = ByteBuffer.allocate(100);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bytes100);
			String desChar = new String(bytes100);
			tdbDataConfig.setStrDes(desChar);
		} catch (IOException e) {
			log.error("CustomDataLoadImpl analysisNewTdbConfig has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, serverName + "配置文件"));
		} finally {
			FileUtil.closeStream(fileChannel,fWriter);
		}
		if(serverName != null && !TdbUtils.tdbMap.containsKey(tdbMapKey)) {
			TdbUtils.tdbMap.put(tdbMapKey, tdbDataConfig);
		}
		return tdbDataConfig;
	}

	
	private byte[] readResourceByCacheEntry(BundlxCache.CacheEntry entry) {
		RandomAccessFile fReader = null;
		FileChannel fileChannel = null;
		try {
			//缓存文件不存在，则直接返回，并且清除这个缓存
			File file = new File(entry.pathToBundleFile);
			if (!file.exists()) {
				return null;
			}
			fReader = new RandomAccessFile(entry.pathToBundleFile, "rw");
			fileChannel = fReader.getChannel();
			fileChannel.position(entry.offset);
			ByteBuffer imgBuf = ByteBuffer.allocate(entry.size);
			fileChannel.read(imgBuf);
			byte[] imgByte = new byte[entry.size];
			imgBuf.flip();
			imgBuf.clear();
			imgBuf.get(imgByte);
			return imgByte;
		} catch (IOException e) {
			log.error("CustomDataLoadImpl readResourceByCacheEntry has error {}",ExceptionsUtil.stackTraceToStr(e));
			throw new BaseException(ErrorCode.RUNTIME_EXCEPTION.getErrorCode(), "Tdb瓦片读取失败");
		} finally {
			FileUtil.closeStream(fileChannel,fReader);
		}
		
	}
	
	
	/**
	 * 读取tdb影像数据
	 */
	@Override
	public byte[] getImageInfo(int tileRow, int tileCol, int zoomLevel, String dataPath, String serverName) {
//		tileRow = (int) (Math.pow(2, zoomLevel)) - tileRow - 1;
		if(tileRow < 0 || tileCol < 0) {
			return null;
		}
		BundlxCache.CacheKey key = new BundlxCache.CacheKey(zoomLevel, tileRow, tileCol,serverName);
        BundlxCache.CacheEntry entry = null;
        FileOutputStream fos = null;
		if ((entry = indexCache.get(key)) != null) {
			if (entry.size > 0 && entry.pathToBundleFile.startsWith(dataPath)) {
				return readResourceByCacheEntry(entry);
			}
		}
		File temFile = new File(dataPath);
		TDBDataConfig configN = TdbUtils.tdbMap.containsKey(temFile.getName()) ? TdbUtils.tdbMap.get(temFile.getName()) : analysisNewTdbConfig(dataPath,serverName);
		TDBDataConfigOld configO = null;
		String srs = null;
		boolean isOld = false;
		if(configN != null) {
			srs = configN.getSrsType().toString().trim();
			isOld = false;
		} else {
			isOld = true;
			configO = analysisOldTdbConfig(dataPath,serverName);
			if(configO != null) {
				srs = configO.isBIsMercator() == true ? "EPSG:3857" : "EPSG:4326";
			}
		}
		File tempFile = new File(dataPath);
		String cacheKey = tempFile.getName();
		int[] tdbCoorInfo = TdbUtils.getTdbCoordinate(zoomLevel, tileRow, tileCol, srs, cacheKey, "");
		//根据瓦片坐标确定切图的tdb文件名称，如果该文件不存在则返回空
		String fileName = TdbUtils.getTdbFilePathAndName(zoomLevel, dataPath, tdbCoorInfo[2], tdbCoorInfo[4],true,cacheKey);
		File file = new File(fileName);
		if(!file.exists()) {
			return null;
		}
		int maxCol = 0;
		//获取tdb文件最大存储的列数，该字段会被缓存以提高效率
		String tdbName = file.getAbsolutePath() + "_" + serverName;
		if(tdbFileStoreMaxCol.containsKey(tdbName)) {
			maxCol = tdbFileStoreMaxCol.get(tdbName);
		}
		RandomAccessFile fReader = null;
		FileChannel fileChannel = null;
		byte[] bytes8 = new byte[8];
		byte[] bytes4 = new byte[4];
		try {
			fReader = new RandomAccessFile(fileName,"rw");
			fileChannel = fReader.getChannel();
			fileChannel.position(0);
			if(maxCol == 0 && configN!= null) {
				byte[] bytes = new byte[4];
				fileChannel.position(32);
				ByteBuffer bytePos = ByteBuffer.allocate(4);
				fileChannel.read(bytePos);
				bytePos.flip();
				bytePos.clear();
				bytePos.get(bytes);
				ByteBuffer bufferPos = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN);	
				maxCol = bufferPos.getInt();
//						maxCol = TdbUtils.readNewTdbFileMaxCol(fileName)[0];
			}
			if(maxCol == 0 &&  configO!= null) {
				maxCol = TdbUtils.readOldTdbFileMaxCol(fileName)[0];
			}
			if(maxCol != 0 && !tdbFileStoreMaxCol.containsKey(tdbName)) tdbFileStoreMaxCol.put(tdbName, maxCol);
			
			//off动态 link信息的起始位置
			int linkStart = TdbUtils.caculatePos(tileRow,tileCol,tdbCoorInfo[3], tdbCoorInfo[5], maxCol,isOld);
			fileChannel.position(linkStart);
			ByteBuffer bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.clear();
			bytePos.get(bytes8);
			ByteBuffer bufferPos = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			//pos加1用于去掉压缩格式选项
			long pos = bufferPos.getLong();
			if(pos == 0) {
				return null;
			} else if(configN != null) {
				pos += 2;
			} else if(configO != null) {
				pos += 1;
			}
			
			int len = 0;
			if(configN != null) {
				fileChannel.position(linkStart + 8);
				ByteBuffer byteLen = ByteBuffer.allocate(8);
				fileChannel.read(byteLen);
				byteLen.flip();
				byteLen.clear();
				byteLen.get(bytes8);	
				ByteBuffer buffer = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
				len = buffer.getInt() - 2;
			} else {
				fileChannel.position(linkStart + 8);
				ByteBuffer byteLen = ByteBuffer.allocate(4);
				fileChannel.read(byteLen);
				byteLen.flip();
				byteLen.clear();
				byteLen.get(bytes4);	
				ByteBuffer buffer = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
				len = buffer.getInt() - 1;
			}
			if(len == 0) {
				return null;
			}
			fileChannel.position(pos);
			ByteBuffer imgBuf = ByteBuffer.allocate(len);
			fileChannel.read(imgBuf);
			byte[] imgByte = new byte[len];
			imgBuf.flip();
			imgBuf.clear();
			imgBuf.get(imgByte);
			//记录最终的缓存文件
			entry = new BundlxCache.CacheEntry(fileName, pos, len);
			indexCache.put(key, entry);
			return imgByte;
		} catch (IOException e) {
			log.error("CustomDataLoadImpl getImageInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, serverName + "数据"));
		} finally {
			FileUtil.closeStream(fos,fileChannel,fReader);
		}
	}

	/**
	 * 读取tdb高程数据
	 */
	@Override
	public byte[] getElvationInfo(int tileRow, int tileCol, int zoomLevel, String dataPath,String serverName) {
		tileRow = (int)Math.pow(2, zoomLevel) - tileRow -1;
		if(tileRow < 0 || tileCol < 0) {
			return null;
		}
		String tdbMapKey = (new File(dataPath)).getName();
		TDBDataConfig configN = analysisNewTdbConfig(dataPath,tdbMapKey);
		TDBDataConfigOld configO = null;
		String srs = null;
		boolean isOld = false;
		if(configN != null) {
			srs = configN.getSrsType().toString();
			isOld = false;
		} else {
			isOld = true;
			configO = analysisOldTdbConfig(dataPath,tdbMapKey);
			if(configO != null) {
				srs = configO.isBIsMercator() == true ? "EPSG:3857" : "EPSG:4326";
			}
		}
		int[] tdbCoorInfo = TdbUtils.getTdbCoordinate(zoomLevel, tileRow, tileCol, srs, tdbMapKey, "");
		
		//根据瓦片坐标确定切图的tdb文件名称，如果该文件不存在则返回空
		String fileName = TdbUtils.getTdbFilePathAndName(zoomLevel, dataPath, tdbCoorInfo[2], tdbCoorInfo[4],true,tdbMapKey);
		File file = new File(fileName);
		if(!file.exists()) {
			return null;
		}
		
		int maxCol = 0;
		String tdbName = file.getName() + "_" + serverName;
		if(tdbFileStoreMaxCol.containsKey(tdbName)) {
			maxCol = tdbFileStoreMaxCol.get(tdbName);
		}
		if(maxCol == 0 && configN!= null) {
			maxCol = TdbUtils.readNewTdbFileMaxCol(fileName)[0];
		}
		if(maxCol == 0 &&  configO!= null) {
			maxCol = TdbUtils.readOldTdbFileMaxCol(fileName)[0];
		}
		if(maxCol != 0 && !tdbFileStoreMaxCol.containsKey(tdbName)) tdbFileStoreMaxCol.put(tdbName, maxCol);
		
		RandomAccessFile fReader = null;
		FileChannel fileChannel = null;
		byte[] bytes4 = new byte[4];
		byte[] bytes8 = new byte[8];
		try {
			fReader = new RandomAccessFile(fileName,"rw");
			fileChannel = fReader.getChannel();
			//off动态 link信息的起始位置
			int linkStart = TdbUtils.caculatePos(tileRow,tileCol,tdbCoorInfo[3], tdbCoorInfo[5], maxCol,isOld);	
			
			fileChannel.position(linkStart);
			ByteBuffer bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.clear();
			bytePos.get(bytes8);	
			ByteBuffer bufferPos = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			//高程旧版本的数据块前没有没有压缩格式，新版本中添加了两个byte，因此在新版本时需要给pos加2
			long pos = bufferPos.getLong();
			if(pos == 0) {
				return null;
			} else if(configN != null) {
				pos += 2;
			}
			
			int len = 0;
			if(configN != null) {
				fileChannel.position(linkStart + 8);
				ByteBuffer byteLen = ByteBuffer.allocate(8);
				fileChannel.read(byteLen);
				byteLen.flip();
				byteLen.clear();
				byteLen.get(bytes8);	
				ByteBuffer buffer = ByteBuffer.wrap(bytes8, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
				len = buffer.getInt() -2;
			} else {
				fileChannel.position(linkStart + 8);
				ByteBuffer byteLen = ByteBuffer.allocate(4);
				fileChannel.read(byteLen);
				byteLen.flip();
				byteLen.clear();
				byteLen.get(bytes4);	
				ByteBuffer buffer = ByteBuffer.wrap(bytes4, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
				len = buffer.getInt();
			}
			if(len == 0) {
				return null;
			}
			
			fileChannel.position(pos);
			ByteBuffer imgBuf = ByteBuffer.allocate(len);
			fileChannel.read(imgBuf);
			byte[] imgByte = new byte[len];
			imgBuf.flip();
			imgBuf.clear();
			imgBuf.get(imgByte);	
			return imgByte;
		} catch (IOException e) {
			log.error("CustomDataLoadImpl getElvationInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, serverName + "高程数据"));
		} finally {
			FileUtil.closeStream(fileChannel,fReader);
		}
	}



}
