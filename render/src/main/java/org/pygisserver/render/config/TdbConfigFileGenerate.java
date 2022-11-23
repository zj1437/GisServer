package org.pygisserver.render.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.render.util.TdbUtils;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

//import org.pygisserver.common.tdb.util.TdbUtils;

@Slf4j
public class TdbConfigFileGenerate {
	private static final String FILE_NAME = "config.tdb";
	
	TdbConfigFileGenerate(){}	
	//写config.tdb配置文件
	public static void writeTDBConfigFile(String filePath, TDBDataConfig tdbConfig) {
		if (filePath == null || tdbConfig == null) {
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "filePath或tdbConfig"));
		}
		
		String path = null;
		if (filePath.endsWith("\\") || filePath.endsWith("\\\\")) {
			path = filePath + FILE_NAME;
		} else {
			path = filePath + File.separator + FILE_NAME;
		}
		
		File file = new File(path);
		try {
			if (file.exists()) {
				file.delete();
				file.createNewFile();
			} else {
				file.createNewFile();
			}
			FileChannel fileChannel = null;
			FileOutputStream fWriter = new FileOutputStream(file.toString());
			fileChannel = fWriter.getChannel();
			ByteBuffer byteBuffer = ByteBuffer.allocate(184);
			convertToBinary(tdbConfig, byteBuffer);
			byteBuffer.flip();
			fileChannel.write(byteBuffer);
			fileChannel.close();
			fWriter.flush();
			fWriter.close();
		} catch (IOException e) {
			log.error("TdbConfigFileGenerate writeTDBConfigFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, "TDB配置文件"));
		}
	}
	
	//将参数转换为byte[]传递给本地接口
	private static void convertToBinary(TDBDataConfig tdbDataConfig, ByteBuffer byteBuffer) {
		int int_len = 4;
		int dou_len = 8;
		byte[] BYTES1  = TdbUtils.boolConvertToBinaryString(0);
		
		Charset cs = Charset.forName("UTF-8");
		String version = tdbDataConfig.getDwVersionNo().replace(".", "");
		byte[] versionBytes = version.getBytes();
		
		byte[] srsBytes = tdbDataConfig.getSrsType().getBytes();
		int srsLen = 20 - srsBytes.length;
		
		byte[] comp = null;
		if (srsLen == 11) {
			comp = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array();
		} else {
			comp = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array();
		}
		
		CharBuffer extension = CharBuffer.allocate(1);
		extension.put(tdbDataConfig.getCBlkType());
		extension.flip();
		byte[] blkBytes = cs.encode(extension).array();
		
		CharBuffer compress = CharBuffer.allocate(1);
		compress.put(tdbDataConfig.getCCompressType());
		compress.flip();
		byte[] compressBytes = cs.encode(compress).array();
		
		CharBuffer type = CharBuffer.allocate(1);
		type.put(tdbDataConfig.getCSourceDataType());
		type.flip();
		byte[] typeBytes = cs.encode(type).array();
		
		byte[] nTleSizeX = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbDataConfig.getNTleSizeX()).array();
		
		byte[] nTleSizeY = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbDataConfig.getNTleSizeY()).array();

		byte[] nMinLod = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbDataConfig.getNMinLod()).array();

		byte[] nMaxLod = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbDataConfig.getNMaxLod()).array();
		
		byte[] dMinLat = ByteBuffer.allocate(dou_len).order(ByteOrder.LITTLE_ENDIAN).putDouble(tdbDataConfig.getDMinLat()).array();
		
		byte[] dMinLon = ByteBuffer.allocate(dou_len).order(ByteOrder.LITTLE_ENDIAN).putDouble(tdbDataConfig.getDMinLon()).array();
		
		byte[] dMaxLat = ByteBuffer.allocate(dou_len).order(ByteOrder.LITTLE_ENDIAN).putDouble(tdbDataConfig.getDMaxLat()).array();
		
		byte[] dMaxLon = ByteBuffer.allocate(dou_len).order(ByteOrder.LITTLE_ENDIAN).putDouble(tdbDataConfig.getDMaxLon()).array();
		
		byte[] row = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbDataConfig.getNPerFileBlkNumY()).array();
		
		byte[] col = ByteBuffer.allocate(int_len).order(ByteOrder.LITTLE_ENDIAN).putInt(tdbDataConfig.getNPerFileBlkNumX()).array();
		
		byte[] descBytes = null;
		if(StringUtils.isNoneBlank(tdbDataConfig.getStrDes())) {
			descBytes = tdbDataConfig.getStrDes().getBytes();
		} else {
			descBytes = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array();
		}
		
		byteBuffer.put(versionBytes).put(srsBytes).put(comp).put(blkBytes).put(compressBytes).put(typeBytes).put(BYTES1).put(nTleSizeX)
		    .put(nTleSizeY).put(nMinLod).put(nMaxLod).put(dMinLat).put(dMinLon).put(dMaxLat).put(dMaxLon).put(row).put(col).put(descBytes);
	}
	
	
	public static void writeReadmeFile(String filePath,TDBDataConfig config) {
		try {
			if (filePath == null || config == null) {
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, "filePath或tdbConfig"));
			}
			String readmePath = filePath + File.separator + "readme.xml";
			
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = fac.newDocumentBuilder();
			Document doc = db.newDocument();
			doc.setXmlStandalone(true);
			Element root = doc.createElement("tilemap");
			String version = config.getDwVersionNo();
			root.setAttribute("version", version);
			
			Element type = doc.createElement("type");
			String dataType = null;
			if(config.getCBlkType() == 2) {
				dataType = "影像切片";
			} else if (config.getCBlkType() == 1){
				dataType = "高程切片";
			} else if (config.getCBlkType() == 3) {
				dataType = "法线切片";
			}
			type.setTextContent(dataType);
			
			Element srs = doc.createElement("srs");
			srs.setTextContent(config.getSrsType());
			
			Element ext = doc.createElement("extension");
			ext.setTextContent(TdbUtils.getTDBSourceDataIndex(config.getCSourceDataType()));
			
			Element compType = doc.createElement("compresstype");
			compType.setTextContent(TdbUtils.getTDBCompressTypeById(config.getCCompressType()));
			
			Element tileLevel = doc.createElement("tilelevel");
			tileLevel.setAttribute("maxlevel", config.getNMaxLod() + "");
			tileLevel.setAttribute("minlevel", config.getNMinLod() + "");
			
			Element boundingbox = doc.createElement("boundingbox");
			boundingbox.setAttribute("maxlat", config.getDMaxLat() + "");
			boundingbox.setAttribute("maxlon", config.getDMaxLon() + "");
			boundingbox.setAttribute("minlat", config.getDMinLat() + "");
			boundingbox.setAttribute("minlon", config.getDMinLon() + "");
			
			Element tileformat = doc.createElement("tileformat");
			tileformat.setAttribute("height", config.getNTleSizeY() + "");
			tileformat.setAttribute("width", config.getNTleSizeX() + "");
			
			Element fileformat = doc.createElement("fileformat");
			fileformat.setAttribute("rowsum", config.getNPerFileBlkNumY() + "");
			fileformat.setAttribute("columnsum", config.getNPerFileBlkNumX() + "");
			
			Element desc = doc.createElement("des");
			desc.setTextContent("");
			
			root.appendChild(type);
			root.appendChild(srs);
			root.appendChild(ext);
			root.appendChild(compType);
			root.appendChild(tileLevel);
			root.appendChild(boundingbox);
			root.appendChild(tileformat);
			root.appendChild(fileformat);
			root.appendChild(desc);
			
			doc.appendChild(root);
			
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(doc), new StreamResult(readmePath));
		} catch (ParserConfigurationException | TransformerException e) {
			log.error("TdbConfigFileGenerate writeReadmeFile has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SAVE, "readme文件"));
		}
	}
}
