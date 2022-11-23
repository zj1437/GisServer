package org.pygisserver.web.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.service.SystemLogService;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.web.entity.ModelEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ModelFileUtil {
	@Autowired
	private ExceptionsUtil exceptionsUtil;
	
	@Autowired
	private SystemLogService systemLogService;

	private static final String X_LON = "0.0000000000";

	private static final String Y_lat = "0.0000000000";

	private static final String Z_HEI = "0.0000000000";

	private static final String X = "1.0000000000";

	private static final String Y = "1.0000000000";

	private static final String Z = "1.0000000000";
	
	/**
	 * 根据服务名称和文件的url生成资源文件
	 * @param fileUrl
	 * @param serverName
	 */
	public void createSourceDocument(ModelEntity entity, boolean isEdit, String reqIP) {
		if (isEdit) {
			deleteFileByPublishor(entity);
		}
		//产看当前文件夹下是否有metadata.xml文件，如果有直接获取该文件中的position值
//		Map<String, Double> positionMap = analysisXmlFile(fileUrl);
		//如果未获取到当前的位置点，则直接给取值为0.0,0.0,0.0
		double lon = entity.getLon();
		double lat = entity.getLat();
		double hei = entity.getHei();
//		if (positionMap != null && !positionMap.isEmpty()) {
//			lon = positionMap.get("lon");
//			lat = positionMap.get("lat");
//			hei = positionMap.get("hei");
//		}
		try {
			createScpFile(entity,lon,lat,hei);
			createPytFile(entity,lon,lat,hei);
			createFlpFile(entity,lon,lat,hei);
		} catch (ResourceException e) {
			systemLogService.saveSystemLog(exceptionsUtil.buildSysLogEntity(ExceptionsUtil.SYS_LOG_LEVEL_ERROR, e.getErrorMsg(), 
					"System","文件管理", reqIP));
			throw e;
		}

	}
	
	public void deleteFileByPublishor(ModelEntity entity) {
		String fileUrl = entity.getUrl();
		//如果是压缩的数据，则不删除
		if (StringUtils.isNotBlank(entity.getDestPath())) {
			return;
		}
		File file = new File(fileUrl);
		if (!file.exists()) return;
		String prexName = entity.getPublishor() + "_" + entity.getServerName();
		for (File subFile : file.listFiles()) {
			if (!subFile.isDirectory() && (subFile.getName().equals(prexName + ".pyt") || 
										   subFile.getName().equals(prexName + ".lfp") || 
										   subFile.getName().equals(prexName + ".scp"))) {
				subFile.delete();
			}
		}
	}
	
	public void createFlpFile(ModelEntity entity, double x, double y, double z) {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("DataDefine");
		root.addElement("Version").setText("0");
		root.addElement("Name").setText("model");
		root.addElement("Position").setText(x + "," + y + "," + z);
		root.addElement("AltitudeMode").setText("1");
		root.addElement("OffsetMeters").setText("0,0,0");
		root.addElement("Rotation").setText("0.00,0.00,0.00");
		root.addElement("Scale").setText("1.000000,1.000000,1.000000");
		root.addElement("LocalPath").setText("./");
		Element netPath = root.addElement("NetPath");
		netPath.addElement("Type").setText("path");
		netPath.addElement("url").setText("");
		Element rangeDom = root.addElement("Range");
		rangeDom.addElement("West").setText(x + "");
		rangeDom.addElement("East").setText(x + "");
		rangeDom.addElement("South").setText(y + "");
		rangeDom.addElement("North").setText(y + "");
		rangeDom.addElement("MinZ").setText(z + "");
		rangeDom.addElement("MaxZ").setText(z + "");
		Element nodeList = root.addElement("NodeList");
		loadLfpUrl(entity,nodeList,false);
		createXmlDocument(entity,document,".lfp");
	}
	
	/**
	 * 加载lfp的url路径
	 * @param savePath 文件路径
	 * @param serverName 服务名称
	 * @param nodelist
	 * @throws UnsupportedEncodingException
	 */
	@SuppressWarnings("unchecked")
	private void loadLfpUrl(ModelEntity entity, Element nodeList, boolean isEdit) {
		File file = new File(entity.getUrl());
		String url = entity.getRequesturl() + "model" + "/"  + entity.getServerName() + "/" + file.getName() + "/" + file.getName();
		int index = 0;
		List<Element> nodeLists = null;
		int length = 0;
		if (isEdit) {
			nodeLists = nodeList.elements("Node");
			length = nodeLists.size();
		}
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				String fileName = getFirstFileName(f,entity.getDestPath());
				String nodeUrl = url + "?subName=" + fileName;
				if (isEdit) {
					//如果当前的下标已经超过元素的节点，则需要增加节点
					if (index > length - 1) {
						nodeList.addElement("Node").setText(nodeUrl);
					}else {
						nodeLists.get(index).setText(nodeUrl);
					}
				}else {
					nodeList.addElement("Node").setText(nodeUrl);
				}
				index++;
			}
		}
	}
	
	/**
	 * 
	 * 生成对应的xml文档
	 * @param savePath  保存路径
	 * @param publishor 当前发布人
	 * @param document  保存文档
	 * @param fileType  文件类型
	 * @throws  
	 */
	private static void createXmlDocument(ModelEntity entity,Document document,String fileType) {
		StringBuilder sb = new StringBuilder(entity.getUrl());
		sb.append(File.separator).append(entity.getPublishor()).append("_").append(entity.getServerName()).append(fileType);
		XMLWriter writer;
		try {
			writer = new XMLWriter(new FileOutputStream(new File(sb.toString())),
					OutputFormat.createPrettyPrint());
			writer.setEscapeText(false);
			writer.write(document);
			writer.close();
		} catch (IOException e) {
			log.error("ModelFileUtil createXmlDocument has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "倾斜摄影" + fileType + "配置文件"));
		}
	}
	
	/**
	 * 创建scp文件
	 * 
	 * @param savePath 倾斜射影文件选择路径
	 * @param serverName 服务名称
	 * @param x
	 * @param y
	 * @param z
	 */
	public void createScpFile(ModelEntity entity, double x, double y, double z) {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("SuperMapCache_Unicode");
		root.addNamespace("sml", "http://www.supermap.com/SuperMapCache/vectorltile");
		root.addElement("sml:Version").setText("1.000000");
		Element position = root.addElement("sml:Position");
		position.addElement("sml:X").setText(x + "");
		position.addElement("sml:Y").setText(y + "");
		position.addElement("sml:Z").setText(z + "");
		//生成OSGfile的标签
		Element fileDom = root.addElement("sml:OSGFiles");
		loadScpUrl(entity,fileDom,false);
		createXmlDocument(entity,document,".scp");
	}
	
	
	/**
	 * 根据文件url获取文件信息
	 * 
	 * @param filePath 文件信息
	 * @param type 文件类型
	 * @return 返回文件数组信息
	 * @throws FileNotFoundException 
	 */
	public byte[] getFileInfo(String filePath) {
		File file = new File(filePath);
		byte[] fileData = null;
		InputStream in = null;
		if (file.exists()) {
			try {
				in = new FileInputStream(file);
				fileData = new byte[(int) file.length()];
				in.read(fileData);
			} catch (IOException e) {
				log.error("ModelFileUtil getFileInfo has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, file.getName()));
			} finally {
				// 关闭输入流
				FileUtil.closeStream(in);
			}
		} else {
			throw new ResourceException(ExceptionsUtil.ERROR_400,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_NOT_FOUND, file.getName()));
		}
		return fileData;
	}
	
	
	public byte[] getFileBytesByUncompress(String zipPath, String entryName) {
		BufferedInputStream fis = null;
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(zipPath + ".tdb");
			ZipEntry entry = zipFile.stream().filter(entry1 -> entry1.getName().endsWith(entryName)).findFirst().get();
			fis = new BufferedInputStream(zipFile.getInputStream(entry));
			byte[] buf = new byte[(int) entry.getSize()];
			fis.read(buf);
			return buf;
		} catch (IOException e) {
			log.error("ModelFileUtil getFileBytesByUncompress has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, entryName));
		} finally {
			FileUtil.closeStream(fis,zipFile);
		}
	}
	
	/**
	 * 
	 * @param savePath
	 * @param fileDom
	 * @param ServreName
	 * @param isEdit
	 * @throws UnsupportedEncodingException
	 */
	@SuppressWarnings("unchecked")
	private void loadScpUrl(ModelEntity entity, Element fileDom, boolean isEdit) {
		File file = new File(entity.getUrl());
		String url = entity.getRequesturl() + "model" + "/" + entity.getServerName() + "/" + file.getName() + "/" + file.getName();
		int index = 0;
		int length = 0;
		List<Element> elements = null;
		if (isEdit) {
			elements = fileDom.elements("FileName");
			length = elements.size();
		}
		
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				String fileName = getFirstFileName(f,entity.getDestPath());
				String nodeUrl = url + "?subName=" + fileName;
				Element node = null;
				if (isEdit) {
					if (index > length - 1) {
						node = fileDom.addElement("sml:FileName");
					}else {
						node = elements.get(index);
					}
				}else {
					node = fileDom.addElement("sml:FileName");
				}
				node.setText(nodeUrl);
				index++;
			}
		}
	}
	
	
	/**
	 * @param savePath
	 * @param serverName
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public void createPytFile(ModelEntity entity, double x, double y, double z) {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("tilt");
		Element name = root.addElement("name");
		name.addAttribute("name", "model");
		Element position = root.addElement("position");
		position.addAttribute("lon", Double.toString(x));
		position.addAttribute("lat", Double.toString(y));
		position.addAttribute("hei", Double.toString(z));
		Element offset = root.addElement("offset");
		offset.addAttribute("lon", X_LON);
		offset.addAttribute("lat", Y_lat);
		offset.addAttribute("hei", Z_HEI);
		Element rotate = root.addElement("rotate");
		rotate.addAttribute("x", X_LON);
		rotate.addAttribute("y", Y_lat);
		rotate.addAttribute("z", Z_HEI);
		Element scale = root.addElement("scale");
		scale.addAttribute("x", X);
		scale.addAttribute("y", Y);
		scale.addAttribute("z", Z);
		Element nodelist = root.addElement("nodelist");
		loadPytUrlList(entity,nodelist,false);
		createXmlDocument(entity,document,".pyt");
	}

	/**
	 * @param path 为倾斜射影所在路径，路径到图层文件夹位置
	 * @param url
	 * @param contextPath
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void loadPytUrlList(ModelEntity entity, Element nodelist, boolean isEdit) {
		File file = new File(entity.getUrl());
		String url = entity.getRequesturl() + "model" + "/" + entity.getServerName() + "/" + file.getName() + "/" + file.getName();
		int index = 0;
		int length = 0;
		String destPath = entity.getDestPath();
		List<Element> nodeLists = null;
		if (isEdit) {
			nodeLists = nodelist.elements("node");
			length = nodeLists.size();
		}
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				String fileName = getFirstFileName(f,destPath);
				String nodeUrl = url + "?subName=" + fileName;
				Element node = null;
				if (isEdit) {
					if (index > length - 1) {
						node = nodelist.addElement("node");
						node.addAttribute("node", nodeUrl);
					}else {
						node = nodeLists.get(index);
						node.setAttributeValue("node", nodeUrl);
					}
				}else {
					node = nodelist.addElement("node");
					node.addAttribute("node", nodeUrl);
				}
				index++;
			}
		}
	}

	private String getFirstFileName(File file,String destPath) {
		String fileName = "";
		boolean flag = StringUtils.isNotBlank(destPath);
		for (File f : file.listFiles()) {
			if(f.isDirectory()) {
				getFirstFileName(f,destPath);
			}else {
				String name = flag ? f.getName().split("\\.")[0] + ".tob" : f.getName();
				if (file.getName().equals(name.substring(0, name.lastIndexOf(".")))) {
					fileName = f.getParentFile().getName() + "/" + name;
					break;
				}
			}
		}
		return fileName.replace("+", "%2B");
	}

	/**
	 * 获取需要解析的文件名称
	 * 
	 * @param fileUrl  文件url
	 * @return
	 */
	private String getAnalysisFileName(String fileUrl,String currentName) {
		//首先寻找文件夹下是否有当前用户生成的文件信息
		File file = new File(fileUrl);
		String[] fileArray = null;
		//先查当前用户相关的配置文件
		if (file.exists() && file.isDirectory()) {
			fileArray = file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if ((name.equalsIgnoreCase("metadata.xml") 
							|| name.endsWith(".scp")
							|| name.endsWith(".lfp")
							|| name.endsWith(".pyt"))
						) {
						return true;
					}
					return false;
				}
			});
		}
		
		if (fileArray == null || fileArray.length == 0) {
			return null;
		}
		if (fileArray != null && fileArray.length == 1) {
			return fileArray[0];
		}
		
		String[] fileName = new String[2]; 
		//获取以当前用户名开头的文件，如果没有获取以admin开头的文件，如果没有获取任意一个
		Arrays.asList(fileArray).stream().forEach((name) -> {
			if (name.startsWith(currentName)) {
				fileName[0] = name;
			}else if (name.startsWith("admin")) {
				fileName[1] = name;
			}
		});
		
		if (StringUtils.isNotBlank(fileName[0])) {
			return fileName[0];
		}else if (StringUtils.isNotBlank(fileName[1])){
			return fileName[1];
		}
		return fileArray[0];
	}

	/**
	 * 解析当前文件夹下的原始数据获取对应的position
	 * @param fileUrl
	 * @return
	 */
	public Map<String, Double> analysisXmlFile(String fileUrl,String user) {
		// 首先判断是否存在metadata.xml文件
		String fileName = getAnalysisFileName(fileUrl,user);
		// 根据文件名称分别解析不同的文件，获取对应的位置信息
		return analysisDocument(fileUrl + File.separator + fileName);
	}

	/**
	 * 解析pyt文件
	 * 
	 * @param 文件路径
	 */
	private Map<String, Double> analysisDocument(String filePath) {
		File file = new File(filePath);
		if (file.exists()) {
			SAXReader read = new SAXReader();
			Document document = null;
			try {
				document = read.read(file);
			} catch (DocumentException e) {
				log.error("ModelFileUtil analysisDocument has error:" + ExceptionsUtil.stackTraceToStr(e));
				throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, file.getName()));
			}
			Map<String, Double> param = new HashMap<String, Double>();
			// 获取根节点
			Element root = document.getRootElement();
			// 通过element对象的elementIterator方法获取迭代器
			Iterator it = root.elementIterator();
			// 根据不同的文件类型，解析不同的元素类型
			if (file.getName().endsWith(".xml")) {
				analysisxmlElement(it,param);
			}
			if (file.getName().endsWith(".scp")) {
				analysisScpElement(it,param);
			}
			if (file.getName().endsWith(".lfp")) {
				analysislfpElemnt(it,param);
			}
			if (file.getName().endsWith(".pyt")) {
				analysispytElement(it,param);
			}
			return param;
		}
		return null;
	}

	/**
	 * 
	 * @param position
	 * @param param
	 */
	private void analysisxmlElement(Iterator it,Map<String, Double> param) {
		while (it.hasNext()) {
			Element position = (Element) it.next();
			if ("SRS".equalsIgnoreCase(position.getName())) {
				String coordinate = position.getText();
				if (StringUtils.isNotBlank(coordinate)) {
					String[] tempArray = coordinate.split(":");
					if (tempArray != null && tempArray.length > 0) {
						if (tempArray[1] != null) {
							String[] coorArray = tempArray[1].split(",");
							param.put("lat", Double.parseDouble(coorArray[0]));
							param.put("lon", Double.parseDouble(coorArray[1]));
							param.put("hei", 0.0);
							break;
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * 解析lfp
	 * @param position
	 * @param param
	 */
	private void analysislfpElemnt(Iterator it,Map<String, Double> param) {
		while (it.hasNext()) {
		Element position = (Element) it.next();
			if ("Position".equalsIgnoreCase(position.getName())) {
				String coordinate = position.getText();
				if (StringUtils.isNotBlank(coordinate)) {
					String[] coorArray = coordinate.split(",");
					if (coorArray != null) {
						param.put("lon", Double.parseDouble(coorArray[0]));
						param.put("lat", Double.parseDouble(coorArray[1]));
						param.put("hei", Double.parseDouble(coorArray[2] == null ? "0.0" : coorArray[2]));
						break;
					}
				}
			}
		}
	}
	
	
	private void analysispytElement(Iterator it,Map<String, Double> param) {
		while (it.hasNext()) {
			Element position = (Element) it.next();
			if ("Position".equalsIgnoreCase(position.getName())) {
				param.put("lon", Double.parseDouble(position.attribute("lon").getText()));
				param.put("lat", Double.parseDouble(position.attribute("lat").getText()));
				param.put("hei", Double.parseDouble(position.attribute("hei").getText()));
				break;
			}
		}
	}
	
	
	
	/**
	 * 解析scp文件
	 * @param position
	 */
	private void analysisScpElement(Iterator it,Map<String, Double> param) {
		while (it.hasNext()) {
			Element position = (Element) it.next();
			if ("Position".equalsIgnoreCase(position.getName())) {
				Iterator itt = position.elementIterator();
				while (itt.hasNext()) {
					Element ele = (Element) itt.next();
					switch (ele.getName()) {
					case "X":
						param.put("lon", Double.parseDouble(ele.getText()));
						break;
					case "Y":
						param.put("lat", Double.parseDouble(ele.getText()));
						break;
					case "Z":
						param.put("hei", Double.parseDouble(ele.getText()));
						break;
					default:
						break;
					}
				}
			}
		}
	}
}
