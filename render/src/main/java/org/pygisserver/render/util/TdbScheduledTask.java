package org.pygisserver.render.util;

import java.io.File;
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

import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.entity.TdbHeadEntity;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileResourceInfo;
import org.pygisserver.common.util.FileUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TdbScheduledTask {
	//用于存放切图保存起始信息，每次开始切图任务时，会检查并清理该Map
	public static final ConcurrentHashMap<String, Long> POS_INFO_MAP = new ConcurrentHashMap<>();
	
	public static void clearPosInfoMap(String serverName) {
		Set<String> tdbKeys = POS_INFO_MAP.keySet();
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
				POS_INFO_MAP.remove(key);
			});
		}
	}
	
	public void writeData(TileResourceInfo tileResourceInfo, TDBDataConfig tdbConfig, Map<String, String> tdbParams, String rowNum) {
		//获取瓦片详细信息，如果行或列小于0则直接退出
		if(tileResourceInfo.getY() < 0 || tileResourceInfo.getX() < 0) return;
		String serverName = tdbParams.get("serverName");
		//根据行、列、级别计算瓦片存放的相关信息
		File tempFile = new File(tdbParams.get("destPath"));
		String cacheKey = tempFile.getName();
		//config配置信息
		TDBDataConfig config = tdbConfig == null ? TdbUtils.tdbMap.get(cacheKey) : tdbConfig;
		//tdb文件的head信息
		TdbHeadEntity tdbHeadEntity = TdbUtils.getTdbHeadEntity(config);
		byte[] buffer = tileResourceInfo.getByteResource();
		//瓦片数据块前增加压缩格式和数据格式，因此其长度应该为buffersize + 2
		int tileBlkSize = 2 + buffer.length;

		int[] tdbFileInfo = TdbUtils.getTdbCoordinate((int)tileResourceInfo.getZ(),(int)tileResourceInfo.getY(),(int)tileResourceInfo.getX(), tdbHeadEntity.getSrsType().toString(), cacheKey, rowNum);
		//当前级别最大行列号对单个tdb文件最大存储瓦片行列号取余获取当前tdb文件的实际存储行列数--待大数据检验
		int tNBlkNumX = tdbFileInfo[0] < config.getNPerFileBlkNumX() ? tdbFileInfo[0] : config.getNPerFileBlkNumX();
		int tNBlkNumY = tdbFileInfo[1] < config.getNPerFileBlkNumY() ? tdbFileInfo[1] : config.getNPerFileBlkNumY();
		tdbHeadEntity.setNBlkNumX(tNBlkNumX);
		tdbHeadEntity.setNBlkNumY(tNBlkNumY);
		RandomAccessFile fWriter = null;
		FileChannel fileChannel = null;
		//瓦片数据存储的文件完整路径
		String filePath = TdbUtils.getTdbFilePathAndName((int)tileResourceInfo.getZ(), tdbParams.get("destPath"), tdbFileInfo[2], tdbFileInfo[4],false,cacheKey);
		if(filePath == null) {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_EXIST, tdbParams.get("destPath")));
		} 
		String mapKey = cacheKey + "_" + filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf(".")) + "_" + serverName;
		//beforeInfo是每个tdb文件的文件头和索引link的总长度，文件头长度固定，索引长度随着文件实际存储的瓦片个数变化
		int beforeInfo = TdbUtils.TDB_DATA_FILE_HEADER_LENGTH + (tdbHeadEntity.getNBlkNumX() * tdbHeadEntity.getNBlkNumY() * 16);
		//字节流用于缓存head和link信息，以防止下次更新覆盖之前的记录
		ByteBuffer headAndLinkBuffer = ByteBuffer.allocate(beforeInfo);
		try {
            fWriter = new RandomAccessFile(filePath,"rw");
		    fileChannel = fWriter.getChannel();
			//position是info信息的起始位置
			long position = 0;
			if (checkKey(mapKey)) {
				position = POS_INFO_MAP.get(mapKey);
				fileChannel.read(headAndLinkBuffer);
			} else {
				position = beforeInfo;
				TdbUtils.writeTdbHead(tdbHeadEntity, headAndLinkBuffer);
				TdbUtils.writeTdbLink(tNBlkNumX, tNBlkNumY, headAndLinkBuffer);	
			}
			//影像数据块之前要分别存储一个byte的压缩格式和数据格式，因此在每次修改position时需要将其长度增加2byte,高程和法线则不需要
			POS_INFO_MAP.put(mapKey, position + tileBlkSize);
			
			//动态读取link信息的起始位置，如果将要写数据的位置数不为0，意味着已经写了数据，就不再写数据
			int linkStart1 = TdbUtils.caculatePos(0,0,tdbFileInfo[3], tdbFileInfo[5], tdbHeadEntity.getNBlkNumX(),false);	
			fileChannel.position(linkStart1);
			ByteBuffer bytePos = ByteBuffer.allocate(8);
			fileChannel.read(bytePos);
			byte[] bytes1 = new byte[8];
			bytePos.flip();
			bytePos.clear();
			bytePos.get(bytes1);	
			ByteBuffer bufferPos = ByteBuffer.wrap(bytes1, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			long tilePos = bufferPos.getLong();
			if(tilePos != 0) {
				return;
			}
			
			//动态写link信息的position和length信息，此时用真实数据填充补位的默认值
			int linkStart = TdbUtils.caculatePos(0,0,tdbFileInfo[3], tdbFileInfo[5], tdbHeadEntity.getNBlkNumX(),false);
			byte[]  pos = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(position).array();
			byte[]  len = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(tileBlkSize).array();
			headAndLinkBuffer.position(linkStart);
			headAndLinkBuffer.put(pos).put(len);
			headAndLinkBuffer.position(headAndLinkBuffer.capacity());
			headAndLinkBuffer.flip();
			fileChannel.write(headAndLinkBuffer, 0);
			
			//info的前两个byte分别记录瓦片格式和压缩格式
			byte[]  tileType = TdbUtils.boolConvertToBinaryString(config.getCCompressType());
			byte[]  comprType = TdbUtils.boolConvertToBinaryString(config.getCSourceDataType());
			fileChannel.position(position);
			//影像数据块之前要分别存储一个byte的压缩格式和数据格式，因此在每次修改position时需要将其长度增加2byte
			ByteBuffer info = ByteBuffer.allocate(tileBlkSize);
			info.put(tileType).put(comprType).put(buffer);
			info.flip();
			fileChannel.write(info);
		}  catch (IOException e) {
			log.error("TdbScheduledTask writeData has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, serverName + "数据"));
		} finally {
			FileUtil.closeStream(fileChannel,fWriter);
		}
	}
	
	private boolean checkKey(String key) {
		Set<String> keys = POS_INFO_MAP.keySet();
		return keys.contains(key);
	}

}
