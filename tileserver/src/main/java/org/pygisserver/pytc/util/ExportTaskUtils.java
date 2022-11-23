package org.pygisserver.pytc.util;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.entity.Constant;
import org.pygisserver.common.entity.SeedTaskInstance;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @Description: gdal的工具类
 * @author:  
 * @date: 2020-09-17
 */
@Slf4j
public class ExportTaskUtils {
	//导出TMS协议格式的瓦片数据
	public volatile static Map<String,ZipOutputStream> zosStreamMap = new HashMap<String,ZipOutputStream>();
	public volatile static Map<String,File> tmsConfigFileMap = new ConcurrentHashMap<String,File>();
	public volatile static Map<String,SeedTaskInstance> exportTaskMap = new ConcurrentHashMap<String,SeedTaskInstance>();
	
	public static void cleanTmsZipCache(String zipName) {
		if (tmsConfigFileMap.containsKey(zipName)) {
			File file = tmsConfigFileMap.get(zipName);
			File tempFile = new File(Constant.TILE_CACHE_TMP);
			if (file.exists() && file.getName().equals(tempFile.getName())) file.delete();
			tmsConfigFileMap.remove(zipName);
		}
		if (zosStreamMap.containsKey(zipName)) {
			ZipOutputStream zos = zosStreamMap.get(zipName);
			if (zos != null) FileUtil.closeStream(zos);
			zosStreamMap.remove(zipName);
		}
		if (exportTaskMap.containsKey(zipName)) {
			exportTaskMap.remove(zipName);
		}
	}
	
	public static void closeZos(String zipName) {
		if (zosStreamMap.containsKey(zipName)) {
			ZipOutputStream zos = zosStreamMap.get(zipName);
			if (zos != null) FileUtil.closeStream(zos);
			zosStreamMap.remove(zipName);
		}
	}
	public static boolean containsTmsConfigFile(String zipName) {
		if (tmsConfigFileMap.containsKey(zipName)) {
			return true;
		}
		return false;
	}
	
	public static void addTmsConfigFile2Map(String zipName,File config) {
		if (!tmsConfigFileMap.containsKey(zipName)) {
			tmsConfigFileMap.put(zipName, config);
		}
	}
	
	public static boolean containsZos(String zipName) {
		if (zosStreamMap.containsKey(zipName)) {
			return true;
		}
		return false;
	}
	
	public static void addZos2Map(String zipName,ZipOutputStream zos) {
		if (!zosStreamMap.containsKey(zipName)) {
			zosStreamMap.put(zipName, zos);
		}
	}
	
	public static void removeZosFromMap(String zipName) {
		if (zosStreamMap.containsKey(zipName)) {
			ZipOutputStream zos = zosStreamMap.get(zipName);
			if (zos != null) FileUtil.closeStream(zos);
			zosStreamMap.remove(zipName);
		}
	}
	
	public static void writeTiled2ZipFile(byte[] image,String entryName,ZipOutputStream zos) throws IOException {
		if (image == null) return;
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(image));
		zos.putNextEntry(new ZipEntry(entryName));
		int c;
		while ((c = dis.read()) != -1) {
			zos.write(c);
		}
		dis.close();
		zos.flush();
	}
	
	public static void writeZipData(ZipOutputStream zos,String name,ZipFile zfile,ZipEntry entry) throws IOException  {
		DataInputStream dis = new DataInputStream(zfile.getInputStream(entry));
		zos.putNextEntry(new ZipEntry(name));
		int c;
		while ((c = dis.read()) != -1) {
			zos.write(c);
		}
		dis.close();
		zos.flush();
	}
	
	/**
	 * 压缩包中写瓦片数据(tms.xml和瓦片数据)
	 * @param image
	 * @param entryName
	 * @param zos
	 * @throws IOException
	 */
	public static void writeTmsZipFile(File configPath,byte[] image,String entryName,ZipOutputStream zos) throws IOException {
		DataInputStream dis = null;
		if (configPath != null) {
			dis = new DataInputStream(new FileInputStream(configPath));
		} else {
			dis = new DataInputStream(new ByteArrayInputStream(image));
		}
		zos.putNextEntry(new ZipEntry(entryName));
		int c;
		while ((c = dis.read()) != -1) {
			zos.write(c);
		}
		dis.close();
		zos.flush();
	}
	
	/**
	 * 创建导出任务压缩包的名称,格式为：layername_srs_format_width_maxlevel
	 * @param tileInfo 请求信息
	 * @return
	 */
	public static String getExportDataName(SeedTaskInstance seedTaskInstance) {
		String grid = seedTaskInstance.getSrs().contains(":") ? seedTaskInstance.getSrs().split(":")[1] : seedTaskInstance.getSrs();
		String mimeType = SeedTaskUtil.simpleMineType(seedTaskInstance.getTileFormat());
		String dbName = new StringBuilder(seedTaskInstance.getLayerName()).append("_").append(grid)
				.append("_").append(mimeType).append("_").append(seedTaskInstance.getTileWidth()).append("_").append(seedTaskInstance.getZoomEnd()).toString();
		return dbName;
	}
	
	public static String getUTF8FilePath(String path) {
		try {
			return URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("MapServerUtils getUTF8FilePath has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_DECODE, path));
		}
	}
	
	/**
	 * 计算当前请求的瓦片总数
	 * 
	 * @param seedTaskInstance
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws MismatchedDimensionException
	 */
	public static List<long[]> calcuteTiles(SeedTaskInstance seedTaskInstance)
			throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException {
		String srs = seedTaskInstance.getSrs();
		ReferencedEnvelope envelope = new ReferencedEnvelope(seedTaskInstance.getMinX(), seedTaskInstance.getMaxX(),
				seedTaskInstance.getMinY(), seedTaskInstance.getMaxY(), CRS.decode(OgcEpsg.getOgcEpsgCodeType(srs) == OgcEpsg.MERCATOR_CODE ? "EPSG:3857" : srs, true));
		int startZoom = seedTaskInstance.getZoomStart();
		int endZoom = seedTaskInstance.getZoomEnd();
		List<long[]> tiles = new ArrayList<long[]>();
		for (int i = startZoom; i <= endZoom; i++) {
			int[] tilesRange = null;
			if (OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
				tilesRange = GeometryUtil.convert3857ToTiledRange(i, envelope);
			} else if (OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(srs)) {
				tilesRange = GeometryUtil.convert4326ToTiledRange(i, envelope);
			}
			/**
			 * 按照每次取16张瓦片为基础，行取4，列取4
			 */
			for (int m = tilesRange[1]; m <= tilesRange[3]; m++) {
				for (int n = tilesRange[0]; n <= tilesRange[2]; n++) {
					//如果是补全切片，则需要查看当前切片是否已经且过，如果切过，则不加入
					//{ n, m, i }  => { x, y, z }
					long[] tile = { n, m, i };
					tiles.add(tile);
				}
			}
		}
		return tiles;
	}
	
	public static File[] getSameExportFile(String path,String fileName) {
		File file = new File(path);
		File[] files = file.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				if (name.equals(fileName)) {
					return true;
				}
				return false;
			}
		});
		return files;
	}
	
	/**
	 * 生成TMS配置文件并写入临时目录下
	 * @param taskInstance
	 * @param tmpPath
	 * @return
	 */
	public static File buildTmsConfigFile(SeedTaskInstance taskInstance,String tmpPath) {
		try {
			File file = new File(tmpPath);
			if (!file.exists()) file.mkdirs();
			String tmsPath = tmpPath + File.separator + "tms.xml";
			
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = fac.newDocumentBuilder();
			Document doc = db.newDocument();
			doc.setXmlStandalone(true);
			Element root = doc.createElement("tilemap");
			root.setAttribute("tilemapservice", "");
			root.setAttribute("version", "1.0");
			
			Element title = doc.createElement("title");
			title.setTextContent("earth-image");
			
			Element abstr = doc.createElement("abstract");
			abstr.setTextContent("");
			
			Element srs = doc.createElement("srs");
			srs.setTextContent(taskInstance.getSrs());
			
			Element vsrs = doc.createElement("vsrs");
			vsrs.setTextContent("");
			
			// <boundingbox maxx="180.0000000" maxy="90.0000000" minx="-180.0000000" miny="-90.0000000" />
			Element boundingbox = doc.createElement("boundingbox");
			//<origin x="-180.0000000" y="-90.0000000" />
			Element origin = doc.createElement("origin");
			
			Element tileformat = doc.createElement("tileformat");
			String extension;
			if ("application/json;type=geojson".equals(taskInstance.getTileFormat())) {
				extension = "geojson";
			} else if ("application/x-protobuf;type=mapbox-vector".equals(taskInstance.getTileFormat())) {
				extension = "protobuf";
			} else {
				extension = taskInstance.getTileFormat().split("/")[1];
			}
			tileformat.setAttribute("extension", extension);
			tileformat.setAttribute("height", taskInstance.getTileHeight() + "");
			tileformat.setAttribute("mime-type", taskInstance.getTileFormat());
			tileformat.setAttribute("width", taskInstance.getTileWidth() + "");

			Element tilesets = doc.createElement("tilesets");
			tilesets.setAttribute("profile", "global-geodetic");
			double radian = 0;
			double zeroUnitsPerPixel = 0;
			if (OgcEpsg.getOgcEpsgCodeType(taskInstance.getSrs()) == OgcEpsg.MERCATOR_CODE) {
				radian = GeometryUtil.MERCATOR_UNITS * 2;
				zeroUnitsPerPixel = CommonUtil.format2Decimal(radian,taskInstance.getTileWidth(), 7);
				boundingbox.setAttribute("maxx", taskInstance.getMaxX() + "");
				boundingbox.setAttribute("maxy", taskInstance.getMaxY() + "");
				boundingbox.setAttribute("minx", taskInstance.getMinX() + "");
				boundingbox.setAttribute("miny", taskInstance.getMinY() + "");
				origin.setAttribute("x", "-20037508.34");
				origin.setAttribute("y", "-20037508.34");
			} else {
				radian = 180;
				zeroUnitsPerPixel = CommonUtil.format2Decimal(radian,taskInstance.getTileWidth(), 7);
				boundingbox.setAttribute("maxx", taskInstance.getMaxX() + "");
				boundingbox.setAttribute("maxy", taskInstance.getMaxY() + "");
				boundingbox.setAttribute("minx", taskInstance.getMinX() + "");
				boundingbox.setAttribute("miny", taskInstance.getMinY() + "");
				origin.setAttribute("x", "-180.00");
				origin.setAttribute("y", "-90.00");
			}
			int zoom = taskInstance.getZoomEnd();
			for (int i = 0; i <= zoom; i++) {
				Element tileset = doc.createElement("tileset");
				tileset.setAttribute("href", "");
				tileset.setAttribute("order", i + "");
				tileset.setAttribute("units-per-pixel", CommonUtil.format2Decimal(zeroUnitsPerPixel,Math.pow(2, i), 7) + "");
				tilesets.appendChild(tileset);
			}
			
			root.appendChild(title);
			root.appendChild(abstr);
			root.appendChild(srs);
			root.appendChild(vsrs);
			root.appendChild(boundingbox);
			root.appendChild(origin);
			root.appendChild(tileformat);
			root.appendChild(tilesets);
			
			doc.appendChild(root);
			
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(new DOMSource(doc), new StreamResult(new File(getUTF8FilePath(tmsPath))));
			return new File(tmsPath);
		} catch (ParserConfigurationException | TransformerException e) {
			log.error("SeedTaskUtil buildTmsConfigFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, "tms临时配置文件"));
		}
		
	}
}
