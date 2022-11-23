package org.pygisserver.web.startegy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.entity.TDBDataConfigOld;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.web.entity.FlatGisEntity;
import org.pygisserver.render.service.CustomDataLoad;
import org.pygisserver.render.service.impl.CustomDataLoadImpl;
import org.pygisserver.render.util.TdbUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Description: 自定义数据类型获取bbox
 * @author:  
 * @date: 2019-07-18
 */
@Slf4j
public class CustomDataInfo extends AbstractReadData {
	private CustomDataLoad customDataLoad = new CustomDataLoadImpl();
	
	private static final String oldFlat = "tilemap";
	private static final String lx = "minx";
	private static final String ly = "miny";
	private static final String gx = "maxx";
	private static final String gy = "maxy";
	private static final String tFormat = "tileformat";
	private static final String w = "width";
	private static final String h = "height";
	private static final String m = "mime-type";
	private static final String e = "extension";
	private static final String tType = "tiletype";
	private static final String PNG = "png";
	private static final String ORIGIN = "origin";
	
	
	
	private String filePath;
	private String fileType;
	private int imgWidth;
	private int imgHeight;
	private String tileExtension;
	String result = null;
	private String srs;
	private int maxLevel;
	private int origin;
	
	
	public CustomDataInfo(String fileUrl, String fileType) {
		this.filePath = fileUrl;
		this.fileType = fileType;
		if(this.fileType != null) {
			parserTileConfigFile();
		}
	}
	
	private void parserTileConfigFile() {
		switch (fileType) {
		case "Texx":
			break;
		case "FlatGis":
		case "TMS":
			FlatGisEntity entity;
			entity = parseXml(filePath);
			result = entity.getBoundingBox();
			imgWidth = entity.getWidth();
			imgHeight = entity.getHeight();
			tileExtension = entity.getExtension();
			srs = entity.getSrs();
			break;
		case "TDB":
			StringBuilder bbox = new StringBuilder();
			TDBDataConfig tdbDataConfig = customDataLoad.analysisNewTdbConfig(filePath,null);
			if(tdbDataConfig != null) {
				bbox.append(tdbDataConfig.getDMinLon()).append(";")
				.append(tdbDataConfig.getDMaxLon()).append(";")
				.append(tdbDataConfig.getDMinLat()).append(";")
				.append(tdbDataConfig.getDMaxLat());
				result = bbox.toString();
				imgWidth = tdbDataConfig.getNTleSizeX();
				imgHeight = tdbDataConfig.getNTleSizeY();
				srs = tdbDataConfig.getSrsType().toString();
				maxLevel = tdbDataConfig.getNMaxLod();
				int tileType = (int)tdbDataConfig.getCSourceDataType();
				//目前服务在实现支持高程功能时，是基于tiff的，根据studio的规划，该逻辑后续可能要改动
				if (tileType == 1) {
					tileExtension = "jpeg";
				} else if (tileType == 2) {
					tileExtension = PNG;
				} else if (tileType == 3) {
					tileExtension = "tiff";
				} else if (tileType == 16) {
					//当前约定TDB高程的数据格式为heightarray
					tileExtension = TdbUtils.TDB_DEM_TYPE;
				} else if (tileType == 4) {
					tileExtension = "protobuf";
				} else if (tileType == 5) {
					tileExtension = "geojson";
				} else if (tileType == 6) {
                    tileExtension = "terrain";
                }
			} else {
				TDBDataConfigOld oldConfig = customDataLoad.analysisOldTdbConfig(filePath,null);
				bbox.append(oldConfig.getDMinLon()).append(";")
				.append(oldConfig.getDMaxLon()).append(";")
				.append(oldConfig.getDMinLat()).append(";")
				.append(oldConfig.getDMaxLat());
				result = bbox.toString();
				imgWidth = oldConfig.getNTleSizeX();
				imgHeight = oldConfig.getNTleSizeY();
				tileExtension = PNG;
				if (oldConfig.getExtension() == TdbUtils.TDB_IMAGECODETYPE_JPG) {
					tileExtension = "jpeg";
				}
				
				if (oldConfig.getExtension() == TdbUtils.TDB_IMAGECODETYPE_PNG) {
					tileExtension = PNG;
				}
				
				File file = new File(this.filePath);
				if(file.isFile()) {
					file = file.getParentFile();
				}
				if (TdbUtils.getTdbFileType(file.getAbsolutePath(),null).equals(TdbUtils.TDB_FILE_DEM_SUFFIX)) {
					tileExtension = TdbUtils.TDB_DEM_TYPE;
				}
			}
			break;
		}
	}
	
	/**
	 * 解析FlatGis配置文件
	 * @param path
	 * @return
	 */
	public FlatGisEntity parseXml(String path) {
		File file = new File(path);
		if(file.isDirectory()) {
			File[] files = file.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if(name.contains(".xml")) return true;
					return false;
				}
			});
			file = files[0];
			path = file.getPath();
		}
		//FlatGis和TMS的xml标签稍有不同，需要区别处理
		String bbox = "BoundingBox";
		String srString = "SRS";
		if (path.endsWith("tms.xml")) {
			bbox = "boundingbox";
			srString = "srs";
		}
		SAXBuilder builder = null;
		FlatGisEntity entity = new FlatGisEntity();
		try {
			if (file.exists() && path.endsWith(".xml")) {
				builder = new SAXBuilder();
				InputStream iStream = new FileInputStream(file);
				Document document = builder.build(iStream);
				Element root = document.getRootElement();
				String fileType = root.getName();
				if (fileType.equalsIgnoreCase(oldFlat)) {
					//element bbox
					Element bboxElement = root.getChildren(bbox).get(0);
					StringBuilder sBuilder = new StringBuilder();
					Attribute minx = bboxElement.getAttribute(lx);
					Attribute miny = bboxElement.getAttribute(ly);
					Attribute maxx = bboxElement.getAttribute(gx);
					Attribute maxy = bboxElement.getAttribute(gy);
					sBuilder.append(minx.getValue()).append(";").append(maxx.getValue()).append(";")
					.append(miny.getValue()).append(";").append(maxy.getValue());
					entity.setBoundingBox(sBuilder.toString());
					
					List<Element> elements = root.getChildren(tFormat);
					Element tileformat = null;
					if (elements != null && !elements.isEmpty()) {
						//element tileformat
						tileformat = elements.get(0);
					}else {
						tileformat = root.getChildren("TileFormat").get(0);
					}
					List<Element> originElements = root.getChildren(ORIGIN);
					if (originElements == null || originElements.isEmpty()) {
						originElements = root.getChildren("Origin");
					}
					
					Element originElement = originElements.get(0);
					double y = originElement.getAttribute("y").getDoubleValue();
					//0是坐下角  1是左上角
					origin = y < 0 ? 0 : 1;
					List<Attribute> attributes = tileformat.getAttributes();
					if(attributes.size() < 4) {
						String extensionType = getExtemsionType(file.getParent(),"TMS");
						entity.setExtension(extensionType);
						entity.setMimeType("image/" + extensionType);
					} else {
						Attribute mimeType = tileformat.getAttribute(m);
						entity.setMimeType(mimeType.getValue());
						Attribute extension = tileformat.getAttribute(e);
						entity.setExtension(extension.getValue());
					}
					Attribute width = tileformat.getAttribute(w);
					entity.setWidth(Integer.valueOf(width.getValue()));
					Attribute height = tileformat.getAttribute(h);
					entity.setHeight(Integer.valueOf(height.getValue()));
					//element tileformat
					String srs = root.getChildren(srString).get(0).getValue();
					entity.setSrs(srs);
					if (!path.endsWith("tms.xml")) {
						List<Element> list = root.getChildren(tType);
						String tiletype = null; 
						if (list != null && !list.isEmpty()) {
							tiletype = list.get(0).getValue();
						}else {
							tiletype = root.getChildren("TileType").get(0).getValue();
						}
						entity.setTileType(tiletype == null ? null : tiletype);
					}
				} else if ("CacheInfo".equalsIgnoreCase(fileType)) {
					Element tileCacheInfo = root.getChildren("TileCacheInfo").get(0);
					Element spatialReference = tileCacheInfo.getChildren("SpatialReference").get(0);
					String latestWKID = spatialReference.getChildren("LatestWKID").get(0).getValue();
					String srcString = "EPSG:" + latestWKID;
					entity.setSrs(srcString);
					String tileCols = tileCacheInfo.getChildren("TileCols").get(0).getValue();
					entity.setHeight(Integer.parseInt(tileCols));
					String tileRows = tileCacheInfo.getChildren("TileRows").get(0).getValue();
					entity.setWidth(Integer.parseInt(tileRows));
					String cacheTileFormat = root.getChildren("TileImageInfo").get(0).getChildren("CacheTileFormat").get(0).getValue();
					entity.setExtension(cacheTileFormat);
					String cdiFilePath = file.getParent() + File.separator + "conf.cdi";
					File cdiFile = new File(cdiFilePath);
					StringBuilder sBuilder = new StringBuilder();
					if (cdiFile.exists()) {
						InputStream cdiFileStream = new FileInputStream(cdiFile);
						Document cdiEdocument = builder.build(cdiFileStream);
						Element cdiRoot = cdiEdocument.getRootElement();
						String xMin = cdiRoot.getChildren("XMin").get(0).getValue();
						String yMin = cdiRoot.getChildren("YMin").get(0).getValue();
						String xMax = cdiRoot.getChildren("XMax").get(0).getValue();
						String yMax = cdiRoot.getChildren("YMax").get(0).getValue();
						sBuilder.append(xMin).append(";").append(xMax).append(";").append(yMin).append(";").append(yMax);
					}
					entity.setBoundingBox(sBuilder.toString());
				} else {
					//root tilemap
					Element tilemap = root.getChildren().get(0); 
					//element boundingbox
					StringBuilder sBuilder = new StringBuilder();
					String a1 = tilemap.getChildren("boundingbox").get(0).getChildren(lx).get(0).getValue();
					String a2 = tilemap.getChildren("boundingbox").get(0).getChildren(ly).get(0).getValue();
					String a3 = tilemap.getChildren("boundingbox").get(0).getChildren(gx).get(0).getValue();
					String a4 = tilemap.getChildren("boundingbox").get(0).getChildren(gy).get(0).getValue();
						sBuilder.append(a1).append(";").append(a3).append(";").append(a2).append(";").append(a4);
					entity.setBoundingBox(sBuilder.toString());
					//element tileformat
					String width = tilemap.getChildren(tFormat).get(0).getChildren(w).get(0).getValue();
					String height = tilemap.getChildren(tFormat).get(0).getChildren(h).get(0).getValue();
					String mimeType = tilemap.getChildren(tFormat).get(0).getChildren(m).get(0).getValue();
					String extension = tilemap.getChildren(tFormat).get(0).getChildren(e).get(0).getValue();
					entity.setWidth(Integer.valueOf(width));
					entity.setHeight(Integer.valueOf(height));
					entity.setMimeType(mimeType);
					entity.setExtension(extension);
					//element srs and tiletype
					String srs = tilemap.getChildren("srs").get(0).getValue();
					entity.setSrs(srs);
					String tiletype = tilemap.getChildren(tType).get(0).getValue();
					entity.setTileType(tiletype);
				}
			}
			return entity;
		} catch (JDOMException | IOException e) {
			log.error("CustomDataInfo parseXml has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, path + "配置文件"));
		}
	}
	
	private String getExtemsionType(String path,String fileType) {
		if(fileType.equalsIgnoreCase("TMS")) {
			String path0 = path + File.separator + "0" + File.separator + "0";
			String path1 = path + File.separator + "0" + File.separator + "1";
			File file0 = new File(path0);
			if(file0.exists() && file0.isDirectory()) {
				File file01 = file0.listFiles()[0];
				if(file01.exists() && file01.isFile()) {
					String fileName = file01.getName();
					return fileName.substring(fileName.indexOf(".") + 1,fileName.length());
				}
			}
			File file1 = new File(path1);
			if(file1.exists() && file1.isDirectory()) {
				File file11 = file1.listFiles()[0];
				if(file11.exists() && file11.isFile()) {
					String fileName = file11.getName();
					return fileName.substring(fileName.indexOf(".") + 1,fileName.length());
				}
			}
		}
		return null;
	}
	
	@Override
	public ReferencedEnvelope getBboxFromFile() {
		if (StringUtils.isBlank(result)) {
		    return convertToEnvelope(null);
		} else {
			return convertToEnvelope(result);
		}
	}
	
	private ReferencedEnvelope convertToEnvelope(String bbox) {
		ReferencedEnvelope envelope = null;
		try {
			if (bbox == null) {
				envelope = new ReferencedEnvelope(-180,180,-90,90, CRS.decode("EPSG:4326",true));
			} else {
				String[] strings = bbox.split(";");
				double minLon = Double.valueOf(strings[0]);
				double maxLon = Double.valueOf(strings[1]);
				double minLat = Double.valueOf(strings[2]);
				double maxLat = Double.valueOf(strings[3]);
				envelope = new ReferencedEnvelope(minLon,maxLon,minLat,maxLat, getResourceSrs());
			}
			return envelope;
		} catch (MismatchedDimensionException | FactoryException e) {
			log.error("CustomDataInfo convertToEnvelope has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "EPSG:4326坐标系"));
		}
		
	}

	@Override
	public int getImgWidth() {
		return this.imgWidth;
	}

	@Override
	public CoordinateReferenceSystem getResourceSrs() {
		try {
			if (StringUtils.isNotBlank(srs)){
				if (srs.startsWith("OSGEO")) {
					srs = srs.replace("OSGEO", "EPSG");
				}
				return CRS.decode(srs,true);
			}
			return CRS.decode("EPSG:4326",true);
		} catch (FactoryException e) {
			log.error("CustomDataInfo getResourceSrs has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, "坐标系"));
		}
	}
	
	@Override
	public int getDataType() {
		//TDOD 根据文件格式设计
		return 1;
	}

	@Override
	public int getImgHeight() {
		return this.imgHeight;
	}

	@Override
	public int getMaxLevel(ReferencedEnvelope originalEnvelope) {
		if ("Tdb".equalsIgnoreCase(fileType)) {
			//调用TDB辅助方法读取对应的config文件
			int maxLod = TdbUtils.getMaxTDBLevel(filePath);
			if(maxLod == 0) maxLod = maxLevel;
			return maxLod;
		} else if ("FlatGis".equalsIgnoreCase(fileType) || "TMS".equalsIgnoreCase(fileType)) {
			//调用瓦片解析工具方法获取最大层级数
			return getTileDataMaxLevel(filePath);
		}
		return 21;
	}
	
	
	/**
	 * 获取FlatGIS或TMS最大级别
	 * @param path
	 * @return
	 */
	private int getTileDataMaxLevel(String path) {
		File file = new File(path);
		if (file.exists() && !file.isDirectory()) {
			file = file.getParentFile();
		}
		ArrayList<Integer> levels = new ArrayList<>();
		File[] files = file.listFiles(new FilenameFilter() {	
			@Override
			public boolean accept(File dir, String name) {
				String regex =  "(\\d)+";
				boolean match = name.matches(regex);
				File f = new File(dir,name);
				if (f.isDirectory() && match) {
					levels.add(Integer.valueOf(name));
					return true;
				} else {
					return false;
				}
			}
		});
		Collections.sort(levels);
		return levels.get(levels.size() - 1);
	}

	@Override
	public String getTileExtension() {
		return this.tileExtension;
	}
	
	/**
	 * @return
	 */
	@Override
	public int getTileOriginFlag() {
		return origin;
	}
}
