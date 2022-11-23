package org.pygisserver.render.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.entity.TDBDataConfigOld;
import org.pygisserver.common.entity.TdbHeadEntity;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ArgumentException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.util.FileUtil;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TdbUtils {	
	
	//影像数据保存tdb时的文件后缀
	public static final String TDB_FILE_TEX_SUFFIX = ".itdb";
	//高程数据保存tdb时的文件后缀
	public static final String TDB_FILE_DEM_SUFFIX = ".etdb";
	//法线数据保存tdb时的文件后缀
	public static final String TDB_FILE_NORMAL_SUFFIX = ".ntdb";
	
	//数据保存为jpeg格式
	public static final int TDB_IMAGECODETYPE_JPG = 4;
	//数据保存为png格式
	public static final int TDB_IMAGECODETYPE_PNG = 5;
	//最新TDB文件头长度
	public static final int TDB_DATA_FILE_HEADER_LENGTH = 48;
	//老TDB文件头长度
	public static final int TDB_DATA_FILE_HEADER_LENGTH_OLD = 32;
	
	//单个tdb文件最大保存列数
	public static final int TDB_MAX_BLK_NUM_X = 400;
	//单个tdb文件最大保存行数
	public static final int TDB_MAX_BLK_NUM_Y = 400;
	
	public static final String TDB_SRS_4326 = "EPSG:4326";
	//tdb服务版本信息
	public static final String TDB_STORE_VERSION_INFO = "4.0.0.0";
	//TDB高程数据字段
	public static final String TDB_DEM_TYPE = "heightarray";
	
	public static final String USER_VISIT_CACHE_DIR = "USER_VISIT_CACHE_DIR";
	
	//新规则下的配置文件信息
	public static final ConcurrentHashMap<String,TDBDataConfig> tdbMap = new ConcurrentHashMap<String, TDBDataConfig>();
	//旧规则下的配置文件信息
	public static final ConcurrentHashMap<String,TDBDataConfigOld> oldTdbMap = new ConcurrentHashMap<String, TDBDataConfigOld>();
	
	public static final String REDIS_CONFIG_PATH = "REDIS_CONFIG_PATH";
	
	public static final String REDIS_START_BAT_PATH = "REDIS_START_BAT_PATH";
	
	/**
	 * 根据请求信息构造Redis缓存的key
	 * Redis缓存的key包含的信息：服务名称、投影坐标系、X、Y、Z、请求类型
	 * @param value
	 * @return
	 */
	public static String getRedisCacheKey(TileRequestInfo requestInfo) {
		//key的Y字段使用服务转换后的row
		int row = (int) (Math.pow(2, requestInfo.getZoomLevel())) - requestInfo.getTileRow() - 1;
		StringBuilder sb = new StringBuilder();
		sb.append(requestInfo.getLayerName()).append("_")
		.append(requestInfo.getGridSet()).append("_")
		.append(requestInfo.getTileCol()).append("_")
		.append(row).append("_")
		.append(requestInfo.getZoomLevel()).append("_")
		.append(requestInfo.getMineType());
		return sb.toString();
	}
	
	public static void deleteTdbCache(String keyWord,String serverName) {
		if(tdbMap.containsKey(keyWord)) {
			tdbMap.remove(keyWord);
		}
		if(oldTdbMap.containsKey(keyWord)) {
			oldTdbMap.remove(keyWord);
		}
		Set<String> tdbKeys = tdbMap.keySet();
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
				tdbMap.remove(key);
			});
		}
		keys.clear();
		Set<String> tdbKeys1 = oldTdbMap.keySet();
		if(tdbKeys1.size() > 0) {
			Iterator<String> it = tdbKeys1.iterator();
			while(it.hasNext()) {
				String keys1 = it.next();
				if(keys1.contains(serverName)) {
					keys.add(keys1);
				}
			}
		}
		if(keys.size() > 0) {
			keys.forEach((key) -> {
				oldTdbMap.remove(key);
			});
		}
		keys.clear();
	}
	
	/**
	 * 用户访问缓存的目录名称为 自定义路径/服务名称_参考坐标系_瓦片大小_返回类型
	 * @param serverName
	 * @param crs
	 * @param format
	 * @return
	 */
	public static String getVistitCacheDirName(String serverName,String crs,String format,int width) {
		String parentPath = System.getProperty(USER_VISIT_CACHE_DIR);
		crs = crs.replace(":", "_");
		StringBuilder sb = new StringBuilder();
		sb.append(parentPath).append(File.separator)
		.append(serverName).append("_")
		.append(crs).append("_")
		.append(width).append("_")
		.append(format).append(File.separator);
		return sb.toString();
	}
	
	public static int[] readOldTdbFileMaxCol(String filePath) {
		int[] maxColAndRow = new int[2];
		FileChannel fileChannel = null;
		RandomAccessFile fReader = null;
		byte[] bb = new byte[4];
		ByteBuffer bytePos = null;
		try {
			fReader = new RandomAccessFile(filePath,"r");
            fileChannel = fReader.getChannel();			
			
			fileChannel.position(4);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bb);
			ByteBuffer bufferX = ByteBuffer.wrap(bb, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			maxColAndRow[0] = bufferX.getInt();
			
			fileChannel.position(8);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bb);
			ByteBuffer bufferY = ByteBuffer.wrap(bb, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			maxColAndRow[1] = bufferY.getInt();
		} catch (IOException e) {
			log.error("TdbUtils readOldTdbFileMaxCol has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "TDB文件头信息"));
		} finally {
			FileUtil.closeStream(fileChannel,fReader);
		}
		return maxColAndRow;
	}
	
	public static int[] readNewTdbFileMaxCol(String filePath) {
		int[] maxColAndRow = new int[2];
		byte[] bytes = new byte[4];
		RandomAccessFile fReader = null;
		FileChannel fileChannel = null;
		try {
			fReader = new RandomAccessFile(filePath,"rw");
			fileChannel = fReader.getChannel();
			
			fileChannel.position(32);
			ByteBuffer bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.clear();
			bytePos.get(bytes);
			ByteBuffer bufferPos = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN);	
			maxColAndRow[0] = bufferPos.getInt();
			
			fileChannel.position(36);
			ByteBuffer bytePos1 = ByteBuffer.allocate(4);
			fileChannel.read(bytePos1);
			bytePos1.flip();
			bytePos1.clear();
			bytePos1.get(bytes);
			ByteBuffer bufferPos1 = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN);	
			maxColAndRow[1] = bufferPos1.getInt();
		} catch (IOException e) {
			log.error("TdbUtils readNewTdbFileMaxCol has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "TDB文件头信息"));
		} finally {
			FileUtil.closeStream(fileChannel,fReader);
		}
		return maxColAndRow;
	}
	
	/**
	 * 根据切片类型和瓦片格式动态确定每个tdb文件可以存储的最大瓦片个数
	 * @param rowSize 切片时单个瓦片的高
	 * @param colSize 切片时单个瓦片的宽
	 * @param tileType  切片类型，是高程还是影像或是法线
	 * @param extension  瓦片格式，png或jpeg或其他
	 * @return
	 */
	public static Map<String,Integer> getMaxRowAndColNum(int rowSize,int colSize,int tileType,int extension) {
		Map<String,Integer> result = new HashMap<String, Integer>();
		int row = 0;
		int col = 0;
		//影像是2
		if(tileType == 2) {
			//影像的存储格式为png
			if(extension == 2 || extension == 3) {
				//瓦片尺寸为512时，单个tdb文件可以存储64*64个瓦片
				if(rowSize == 512) {
					row = 64;
					col = 64;
				} else if (rowSize == 256) {
					row = 128;
					col = 128;
				} else if (rowSize == 128) {
					row = 256;
					col = 256;
				} else if (rowSize == 64 || rowSize == 16) {
					row = 512;
					col = 512;
				}
			//影像的存储格式为jpeg
			} else if (extension == 1) {
				//瓦片尺寸为512时，单个tdb文件可以存储128*128个瓦片
				if(rowSize == 512) {
					row = 128;
					col = 128;
				} else if (rowSize == 256) {
					row = 256;
					col = 256;
				} else if (rowSize == 128) {
					row = 512;
					col = 512;
				} else if (rowSize == 64 || rowSize == 16) {
					row = 1024;
					col = 1024;
				}
			}else if(extension == 0) {
				row = 256;
				col = 256;
			} else if (extension == 4) {
                row = 64;
                col = 64;
            } else if (extension == 5) {
                row = 64;
                col = 64;
            }  else if (extension == 6) {
				row = 64;
				col = 64;
			}
		//高程是1
		} else if (tileType == 1) {
			//瓦片尺寸为16时，单个tdb文件可以存储1024*1024个瓦片
			if(rowSize <= 16) {
				row = 1024;
				col = 1024;
			} else if (rowSize == 32) {
				row = 512;
				col = 512;
			} else if (rowSize == 64) {
				row = 256;
				col = 256;
			} else if (rowSize == 128) {
                row = 128;
                col = 128;
            } else if (rowSize == 256) {
				row = 64;
				col = 64;
			}  else if (rowSize == 512) {
				row = 32;
				col = 32;
			}
		//法线是2，暂时未做处理
		} else if (tileType == 3) {
			
		}
		result.put("row", row);
		result.put("col", col);
		return result;
	}
	
	public static TDBDataConfig getTDBDataConfig(TileRequestInfo tileInfo,SeedTaskInstance tileTask) {
		TDBDataConfig tdbDataConfig = new TDBDataConfig();
		tdbDataConfig.setDwVersionNo(TDB_STORE_VERSION_INFO);
		tdbDataConfig.setSrsType(tileInfo.getGridSet());
		//数据是高程还是影像，暂时支持影像
		tdbDataConfig.setCBlkType(getCBlkTypeId(getTileDataType(tileInfo)));
		//数据是否是Zip压缩，当前为未压缩
	    tdbDataConfig.setCCompressType(getTDBCompressType("unzip"));
		//瓦片格式，png或其他
	    tdbDataConfig.setCSourceDataType(getTDBSourceDataExtension(getSimpleFormat(tileInfo.getMineType()),tileInfo.getStyle()));
	    tdbDataConfig.setNTleSizeX(tileTask.getTileWidth());
		tdbDataConfig.setNTleSizeY(tileTask.getTileHeight());
		tdbDataConfig.setNMinLod(tileTask.getZoomStart());
		tdbDataConfig.setNMaxLod(tileTask.getZoomEnd());
		tdbDataConfig.setDMinLat(tileTask.getMinY());
		tdbDataConfig.setDMinLon(tileTask.getMinX());
		tdbDataConfig.setDMaxLat(tileTask.getMaxY());
		tdbDataConfig.setDMaxLon(tileTask.getMaxX());
		//获取单个TDB文件可以存储的最大瓦片个数
		Map<String,Integer> map = getMaxRowAndColNum(tileTask.getTileHeight(), tileTask.getTileWidth(),tdbDataConfig.getCBlkType(), tdbDataConfig.getCSourceDataType());
		tdbDataConfig.setNPerFileBlkNumY(map.get("row"));
		tdbDataConfig.setNPerFileBlkNumX(map.get("col"));
		String desc = null;
		tdbDataConfig.setStrDes(desc);
		return tdbDataConfig;
	}
	
	private static String getTileDataType(TileRequestInfo tileInfo) {
		if((tileInfo.getMineType().contains("tiff") || tileInfo.getMineType().contains("tif")) &&  tileInfo.getImageWidth() < 65 
				&& ("高程".equals(tileInfo.getStyle()) || "等高线".equals(tileInfo.getStyle()))) {
			return "dem";
		}
		return "tex";
	}
	
	public static TdbHeadEntity getTdbHeadEntity(TDBDataConfig config) {
		TdbHeadEntity entity = new TdbHeadEntity();
		entity.setDwVersionNo(config.getDwVersionNo());
		entity.setSrsType(config.getSrsType());
		entity.setNTileSizeX(config.getNTleSizeX());
		entity.setNTileSizeY(config.getNTleSizeY());
		entity.setNLodMin(config.getNMinLod());
		entity.setNLodMax(config.getNMaxLod());
		return entity;
	}
	
	private static char getTDBCompressType(String format) {
		switch(format) {
		case "unzip":
			return 1;
		case "mixed":
			return 2;
		case "zlib":
			return 3;
		default:
			return 0;
		}
	}
	
	public static String getTDBCompressTypeById(int id) {
		switch(id) {
		case 0:
			return "unkonw";
		case 1:
			return "unzip";
		case 2:
			return "mixed";
		case 3:
			return "zlib";
		default:
			return "unkonw";
		}
	}
	
	/**
	 * 将瓦片格式转换成对应的int
	 * @param format
	 * @return
	 */
	private static char getTDBSourceDataExtension(String format,String styleName) {
		switch(format) {
		case "unknow":
			return 0;
		case "jpeg":
			return 1;
		case "png":
			return 2;
		case "tiff":
			if(("高程".equals(styleName) || "等高线".equals(styleName))) {
				return 16;
			}
			return 3;
		case "mixed":
			return 15;
		case "heightfield":
			return 16;
        case "x-protobuf;type=mapbox-vector":
            return 4;
        case "json;type=geojson":
            return 5;
		case "terrain":
			return 6;
		default:
			return 0;
		}
	}
	
	/**
	 * 将瓦片的int值转换为对应的字符串
	 * @param format
	 * @return
	 */
	public static String getTDBSourceDataIndex(int format) {
		switch(format) {
		case 0:
			return "unknow";
		case 1:
			return "jpeg";
		case 2:
			return "png";
		case 3:
			return "tiff";
        case 4:
            return "protobuf";
        case 5:
            return "geojson";
		case 6:
			return "terrain";
		case 15:
			return "mixed";
		case 16:
			return "heightfield";
		default:
			return "unknow";
		}
	}
	
	private static String getSimpleFormat(String mimeType) {
		if (StringUtils.isNotBlank(mimeType) && mimeType.indexOf("/") > -1) {
			mimeType = mimeType.split("/")[1];
		}
		return mimeType;
	}
	


	public static int readHeadInfo(String filePath,String serverName) {
		if(serverName != null) {
			if(tdbMap.containsKey(serverName)) {
				return tdbMap.get(serverName).getNPerFileBlkNumX();
			}
			if(oldTdbMap.containsKey(serverName)) {
				return oldTdbMap.get(serverName).getDMaxColNum();
			}
		}
		RandomAccessFile fReader = null;
		FileChannel fileChannel = null;
		try {
			fReader = new RandomAccessFile(filePath,"rw");
			fileChannel = fReader.getChannel();
			
			byte[] bytes = new byte[4];
			fileChannel.position(4);
			ByteBuffer bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.clear();
			bytePos.get(bytes);
			ByteBuffer bufferPos = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			return bufferPos.getInt();
		} catch (IOException e) {
			log.error("TdbUtils readHeadInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "TDB文件头信息"));
		} finally {
			FileUtil.closeStream(fileChannel,fReader);
		}
	}
	
	public static void writeTdbHead(TdbHeadEntity tdbHeadEntity, ByteBuffer headAndLinkBuffer) throws IOException {
		int int_len = 4;
		
		String version = tdbHeadEntity.getDwVersionNo().replace(".", "");
		byte[] versionBytes = version.getBytes();
		
		byte[] srsBytes = tdbHeadEntity.getSrsType().trim().getBytes();
		int srsLen = 20 - srsBytes.length;
		byte[] comp = null;
		if (srsLen == 11) {
			comp = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array();
		} else {
			comp = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array();
		}
		
		byte[] x = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbHeadEntity.getNTileSizeX()).array();

		byte[] y = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbHeadEntity.getNTileSizeY()).array();

		byte[] maxX = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbHeadEntity.getNBlkNumX()).array();

		byte[] maxY = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbHeadEntity.getNBlkNumY()).array();
		byte[] minlev = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbHeadEntity.getNLodMin()).array();
		byte[] maxlev = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbHeadEntity.getNLodMax()).array();

		headAndLinkBuffer.put(versionBytes).put(srsBytes).put(comp).put(x).put(y).put(maxX).put(maxY).put(minlev).put(maxlev);
	}
	
	/**
	 * 写tdb文件的link块
	 * @param nBlkNumX 
	 * @param nBlkNumY
	 * @param dataOutputStream
	 */
	public static void writeTdbLink(int nBlkNumX, int nBlkNumY, ByteBuffer headAndLinkBuffer) throws IOException {
		int totalLink = nBlkNumX * nBlkNumY;
		int long_len = 8;
		//link的position和length字段，分别为8个byte，初始化时将其置为0
		byte[] bytesPos = ByteBuffer.allocate(long_len).order(ByteOrder.LITTLE_ENDIAN).putLong(0).array();
		byte[] bytesLen = ByteBuffer.allocate(long_len).order(ByteOrder.LITTLE_ENDIAN).putLong(0).array();
		for (int i = 0; i < totalLink; i++) {
			headAndLinkBuffer.put(bytesPos).put(bytesLen);
		}
	}
	
	/**
	  *  获取TDB数据文件保存的图片压缩类型(png or jpeg)，需要注意的是高程数据没有该字段
	 * @param nBlkNumX 
	 * @param nBlkNumY
	 * @param dataOutputStream
	 */
	public static int readTdbCompressType(String dataPath,String serverName,String srs) {
		int format = -1;
		File file = new File(dataPath);
		
		String cacheKey = serverName;
		if(cacheKey != null) {
			File tf = new File(dataPath);
			String name = tf.getName();
			if(dataPath.contains(serverName) && name.contains("EPSG")) cacheKey = name;
		}
		
		int[] tdbCoorInfo = getTdbCoordinate(0, 0, 0, srs, cacheKey, "");
		String temPath = file.isDirectory() ? file.getPath() : file.getParent();
		String	tempFile = temPath + File.separator + "0_0_0.itdb";
		File tempfile = new File(tempFile);
		if(!tempfile.exists()) {
			String demFile = temPath + File.separator + "0_0_0.etdb";
			File demF = new File(demFile);
			if(demF.exists()) {
				return 0;
			}
		}
		format = readTDBCompresFormat(tempFile,tdbCoorInfo,serverName);
		if(format == -1) {
			tdbCoorInfo = getTdbCoordinate(0, 0, 1,  srs, cacheKey, "");
			format = readTDBCompresFormat(tempFile,tdbCoorInfo,serverName);
		}
		return format;
	}
	
	private static int readTDBCompresFormat(String tempFile,int[] tdbCoorInfo,String serverName) {
		RandomAccessFile tempfWriter = null;
		FileChannel tempFileChannel = null;
		try {
			byte[] bytes1 = new byte[1];
			tempfWriter = new RandomAccessFile(tempFile,"rw");
			tempFileChannel = tempfWriter.getChannel();
			
			int maxCol = TdbUtils.readHeadInfo(tempFile,serverName);
			int linkStart = TdbUtils.caculatePos(0,0,tdbCoorInfo[3], tdbCoorInfo[5], maxCol,true);	
			
			tempFileChannel.position(linkStart);
			ByteBuffer bytePo = ByteBuffer.allocate(8);
			tempFileChannel.read(bytePo);
			byte[] bytes = new byte[8];
			bytePo.flip();
			bytePo.clear();
			bytePo.get(bytes);
			ByteBuffer bufferPo = ByteBuffer.wrap(bytes, 0, 8).order(ByteOrder.LITTLE_ENDIAN);
			long pos = bufferPo.getLong();
			
			tempFileChannel.position(pos);
			ByteBuffer byteP = ByteBuffer.allocate(1);
			tempFileChannel.read(byteP);
			byteP.flip();
			byteP.get(bytes1);
			return bytes1[0];
		} catch(IOException e) {
			log.error("TdbUtils readTDBCompresFormat has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, serverName + "数据"));
		} finally {
			FileUtil.closeStream(tempFileChannel,tempfWriter);
		}
	}
	
	/**
	  * 获取TDB文件名称以及对应文件名称的绝对路径
	 * @param zoomLevel
	 * @param destPath
	 * @param coorX
	 * @param coorY
	 * @param isRead
	 * @return
	 */
	public static String getTdbFilePathAndName(int zoomLevel, String destPath, int coorX, int coorY, boolean isRead,String severName) {
		String fileType = getTdbFileType(destPath,severName);
		if(fileType.equals("unknow") || StringUtils.isBlank(fileType)) {
			return null;
		}
		//根据瓦片坐标确定切图的tdb文件名称
		StringBuilder sBuilder = new StringBuilder();
		if (destPath.endsWith("\\") || destPath.endsWith("\\\\")) {
			sBuilder.append(destPath);
		} else {
			sBuilder.append(destPath).append(File.separator);
		}
		sBuilder.append(zoomLevel).append("_").append(coorX).append("_").append(coorY).append(fileType);
		//如果是读数据，就不需要创建文件，写文件是false，读文件是true
		if (!isRead) {
			File file = new File(sBuilder.toString());
			if (!file.exists()) {
				try {
					File file2 = new File(file.getParent());
					if (!file2.exists()) {
						file2.mkdirs();
					}
					file.createNewFile();
				} catch (IOException e) {
					log.error("TdbUtils getTdbFilePathAndName has error:" + ExceptionsUtil.stackTraceToStr(e));
					throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "TDB数据存储目录"));
				}
			}
		}
		return sBuilder.toString();
	}
	
	private static String getCBlkType(int id) {
		switch(id) {
		case 0:
			return "unknow";
		case 1:
			return TDB_FILE_DEM_SUFFIX;  //高程
		case 2:
			return TDB_FILE_TEX_SUFFIX;   //影像
		case 3:
			return TDB_FILE_NORMAL_SUFFIX;  //法线
		case 4:
			return "";  //预留
		default:
			return "unknow";
		}
	}
	
	private static char getCBlkTypeId(String format) {
		switch(format) {
		case "unknow":
			return 0;
		case "dem":
			return 1;
		case "tex":
			return 2;
		case "normal":
			return 3;
		case "color":
			return 4;
		default:
			return 2;
		}
	}
	
	/**
	 * 根据文件名称后缀确定TDB文件存储的是影像数据还是高程数据
	 * @param destPath
	 * @return
	 */
	public static String getTdbFileType(String destPath,String serverName) {
		String fileType = null;
		// 此处在缓存中获取存储类型主要用于新规则下的TDB切图以及访问
		File file = new File(destPath,"config.tdb");
		String mapKey = null;
		if (file.exists()) mapKey = file.getParentFile().getName();
		if(serverName != null && tdbMap.containsKey(mapKey)) {
			TDBDataConfig config = tdbMap.get(mapKey);
			if(config != null) {
				int extension = config.getCBlkType();
				fileType = getCBlkType(extension);
			}
		}
		if(fileType == null) {
			File itdb = new File(destPath,"0_0_0.itdb");
			if(itdb.exists() && itdb.isFile()) {
				fileType = TDB_FILE_TEX_SUFFIX;
			}
		}
		if(fileType == null) {
			File etdb = new File(destPath,"0_0_0.etdb");
			if(etdb.exists() && etdb.isFile()) {
				fileType = TDB_FILE_DEM_SUFFIX;
			}
		}
		if(fileType == null) {
			File etdb = new File(destPath,"0_0_0.ntdb");
			if(etdb.exists() && etdb.isFile()) {
				fileType = TDB_FILE_NORMAL_SUFFIX;
			}
		}
		if(fileType == null) {
			File tdbFile = new File(destPath);
			String fileName = tdbFile.getName();
			if(fileName.endsWith("png") || fileName.endsWith("jpeg") || fileName.endsWith("jpg")) {
				fileType = TDB_FILE_TEX_SUFFIX;
			} else if(fileName.endsWith("tif") || fileName.endsWith("tiff") || fileName.endsWith("heightfield")) {
				fileType = TDB_FILE_DEM_SUFFIX;
			}
		}
		return fileType;
	}
	
	/**
	  *  将TDB高程数据转换为BufferedImage并返回
	 * @param imageBytes
	 * @return
	 */
	public static BufferedImage getTDBDemImage(byte[] imageBytes) {
		DataBufferByte dataBuffer = new DataBufferByte(imageBytes, 16 * 16);
        ColorSpace space = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ComponentSampleModel sampleModle = new ComponentSampleModel(DataBuffer.TYPE_BYTE, 16, 16, 1, 16, new int[] {0,0,0});
        int[] bits = {8,8,8};
        ComponentColorModel color1 = new ComponentColorModel(space, bits, false, false, Transparency.TRANSLUCENT,DataBuffer.TYPE_BYTE);
        WritableRaster raster = Raster.createWritableRaster(sampleModle,dataBuffer,new Point(0,0));
        BufferedImage image = new BufferedImage(color1,raster,false,null);
		return image;
	}
	
	/**
	 * 由TDB压缩文件类型获取对应的数字表示
	 * @param format
	 * @return
	 */
	public static int getSavePicType(String format) {
		//当前tdb支持jpeg和png两种图片压缩方式，默认使用png
		int picType = 0;
		if (format.equalsIgnoreCase("jpeg")) {
			picType = TDB_IMAGECODETYPE_JPG;
		} else  {
			picType = TDB_IMAGECODETYPE_PNG;
		}
		return picType;
	}
	
	/**
	 * 根据切图级别计算tdb文件名称信息
	 * @param zoomLevel
	 * @param isMercator
	 * @return
	 */
	public static int[] getTdbCoordinate(int zoomLevel, int row, int col, String sysType,String cacheKey, String rowNum) {
		int[] tdbInfo = new int[6];
		int numX = 0;
		int numY = 0;
		//根据srs计算各个级别对应的最大行列号
		if(OgcEpsg.getOgcEpsgCodeType(sysType) == OgcEpsg.MERCATOR_CODE) {
			numX = (int)Math.pow(2, zoomLevel);
			numY = (int)Math.pow(2, zoomLevel);
		} else if (StringUtils.isNotBlank(rowNum)) {
			String[] split = rowNum.split("\\*");
			numX = Integer.parseInt(split[0]);
			numY = Integer.parseInt(split[1]);
		} else {
			numX = (int)Math.pow(2, zoomLevel + 1);
			numY = (int)Math.pow(2, zoomLevel);
		}
		
		int[] rowAndCol = getMaxRowNum(cacheKey);
		if (row <= numY && col <= numX) {
			int X = col / rowAndCol[1];    //除法结果用于生成tdb文件名称level_col_row.itdb
			int pX = col % rowAndCol[1];
			int Y = row / rowAndCol[0];    //除法结果用于生成tdb文件名称
			int pY = row % rowAndCol[0];
			tdbInfo[0] = numX;             //当前级别下的最大列号
			tdbInfo[1] = numY;             //当前级别下的最大行号
			tdbInfo[2] = X;
			tdbInfo[3] = pX;               //当前级别下某一列的数据存储位置，具体存放位置为level_col_row.itdb的pX列
			tdbInfo[4] = Y;
			tdbInfo[5] = pY;               //当前级别下某一行的数据存储位置，具体存放位置为level_col_row.itdb的pY列
		} else {
			throw new ArgumentException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.ARGGUMENT_ILLEGAL, cacheKey + "请求行列号"));
		}
		return tdbInfo;
	}
	
	/**
	 * 返回单个文件存储的最大瓦片数量
	 * @param serverName
	 * @return
	 */
	public static int[] getMaxRowNum(String serverName) {
		int[] maxRowAndColNum = new int[] {TDB_MAX_BLK_NUM_Y,TDB_MAX_BLK_NUM_X};
		if(serverName == null) {
			return maxRowAndColNum;
		}
		//new cache即新规则下的TDB存储
		if(tdbMap.containsKey(serverName)) {
			TDBDataConfig newTdb = tdbMap.get(serverName);
			if(newTdb != null && newTdb.getNPerFileBlkNumY() != 0) {
				maxRowAndColNum[0] = newTdb.getNPerFileBlkNumY();
			}
			if(newTdb != null && newTdb.getNPerFileBlkNumX() != 0) {
				maxRowAndColNum[1] = newTdb.getNPerFileBlkNumX();
			}
		} else if(oldTdbMap.containsKey(serverName)) {
			TDBDataConfigOld oldTdb = oldTdbMap.get(serverName);
			if(oldTdb != null && oldTdb.getDMaxRowNum() != 0) {
				maxRowAndColNum[0] = oldTdb.getDMaxRowNum();
			}
			if(oldTdb != null && oldTdb.getDMaxColNum() != 0) {
				maxRowAndColNum[1] = oldTdb.getDMaxColNum();
			}
		}
		return maxRowAndColNum;
	}
	
	public static int getMaxTDBLevel(String path) {
		String filePath = path.endsWith(".tdb") ? path : path + File.separator + "config.tdb";
		FileChannel fileChannel = null;
		RandomAccessFile fReader = null;
		int maxLod = 0;
		byte[] bb = new byte[4];
		ByteBuffer bytePos = null;
		try {
			fReader = new RandomAccessFile(filePath,"rw");
            fileChannel = fReader.getChannel();			
			
			fileChannel.position(20);
			bytePos = ByteBuffer.allocate(4);
			fileChannel.read(bytePos);
			bytePos.flip();
			bytePos.get(bb);
			ByteBuffer bufferPos = ByteBuffer.wrap(bb, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
			maxLod = bufferPos.getInt();
		} catch (IOException e) {
			log.error("TdbUtils getMaxTDBLevel has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, "TDB最大级别"));
		} finally {
			FileUtil.closeStream(fileChannel,fReader);
		}
		return maxLod;
	}
	
	public static int caculatePos(int tileRow, int tileCol,int pX, int pY, int nBlkNumX,boolean isOld) {
		//计算某个(tileCol,tileRow)坐标的link值，其中TDB_DATA_FILE_HEADER_LENGTH是head的长度
		//由于索引是由position和link组成，且各自长度是8byte，因此在具体计算时需要乘以16byte
		int postion =  0;
		if(isOld) {
			if (tileRow <= pY && tileCol <= pX) {
				postion = TDB_DATA_FILE_HEADER_LENGTH_OLD + (nBlkNumX * tileRow + tileCol) * 16;
			} else {
				postion = TDB_DATA_FILE_HEADER_LENGTH_OLD + (nBlkNumX * pY + pX) * 16;
			}
		} else {
			postion = TDB_DATA_FILE_HEADER_LENGTH + (nBlkNumX * pY + pX) * 16;
		}
		return postion;
	}
	
	public static int caculateDemPos(int tileRow, int tileCol,int pX, int pY, int nBlkNumX,boolean isOld) {
		//计算某个(tileCol,tileRow)坐标的link值，其中TDB_DATA_FILE_HEADER_LENGTH是head的长度
		//由于索引是由position和link组成，且各自长度是8byte，因此在具体计算时需要乘以16byte
		int postion =  0;
		if(isOld) {
			postion = TDB_DATA_FILE_HEADER_LENGTH_OLD + (nBlkNumX * pY + pX) * 16;
		} else {
			postion = TDB_DATA_FILE_HEADER_LENGTH + (nBlkNumX * pY + pX) * 16;
		}
		return postion;
	}
	
	public static byte[] boolConvertToBinaryString(int intNun) {
		byte[] bytes = new byte[1];
		bytes[0] = (byte)(intNun & 0xFF);
		return bytes;
	}
}
