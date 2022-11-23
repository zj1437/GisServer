package org.pygisserver.render.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.enumm.ServerStatusEnum;
import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.render.dao.RenderMapper;
import org.pygisserver.render.entity.PyGridReaderLayer;
import org.pygisserver.render.entity.PyMapContent;
import org.pygisserver.render.entity.PyRenderedParam;
import org.pygisserver.render.service.PyRender;
import org.pygisserver.render.util.LayerBaseInfoUtil;
import org.pygisserver.render.util.PyParseStyle;
import org.pygisserver.render.util.PyRenderedUtil;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.RasterLayer;
import org.geotools.referencing.CRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.*;

@Slf4j
@Service
public class PyRenderImpl implements PyRender {

	@Autowired
    private PyParseStyle parseStyle;
	
	@Autowired
	private LayerBaseInfoUtil layerBaseInfoUtil;

	@Autowired
	private RenderMapper renderMapper;

	@Autowired
	private org.pygisserver.render.mapper.RenderMapperTest renderMapperTest;

	/** filter factory */
	private FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);
	
	/**
	 * 构造请求参数
	 */
	@Override
	public void constructRenderRequestParam(PyRenderRequestParam requestParam) {
		// 如果既没有传递文件url,也没有传递图层名称，就提示错误
		String fileUrl = requestParam.getFileUrl();
		String layerName = requestParam.getLayerName();
		if (StringUtils.isBlank(fileUrl) && StringUtils.isBlank(layerName)) {
			throw new IllegalArgumentException("PYRenderImageImpl renderImageByRequestParam param is error: 文件路径必须输入");
		}
	}

	@Override
	public PyRenderedParam constructRenderParam(PyRenderRequestParam requestParam) {
		// 构造渲染参数
		PyRenderedParam pyRenderedParam = new PyRenderedParam(requestParam.isTransparent(), requestParam.getFormat(),
				requestParam.getBgcolor(), requestParam.getAntialias(), requestParam.getAngle(),
				requestParam.getMapWidth(), requestParam.getMapHeight());
		String requestBbox = requestParam.getRequestBbox();
		if (StringUtils.isBlank(requestBbox)) {
			log.error("PYRenderImageImpl renderImageByRequestParam has error: 请求的bbox为空");
		}
		String[] bboxArray = requestBbox.split(",");
		CoordinateReferenceSystem crs = null;
		try {
			//如果是墨卡托则按照3857处理
			String srs = requestParam.getRequestSrs();
			if (OgcEpsg.getOgcEpsgCodeType(requestParam.getRequestSrs()) == OgcEpsg.MERCATOR_CODE) {
				srs = "EPSG:3857";
			}
			crs = CRS.decode(srs, true);
		} catch (FactoryException e) {
			log.error("PYRenderImageImpl renderImageByRequestParam has error: 请求的坐标系无法获取到对应的坐标系统{}", e.getMessage());
		}
		pyRenderedParam.setCrs(crs);
		ReferencedEnvelope envelope = new ReferencedEnvelope(Double.parseDouble(bboxArray[0]),
				Double.parseDouble(bboxArray[1]), Double.parseDouble(bboxArray[2]), Double.parseDouble(bboxArray[3]),
				crs);
		pyRenderedParam.setEnvelope(envelope);
		Rectangle rectangle = new Rectangle(0, 0, requestParam.getMapWidth() == 0 ? 256 : requestParam.getMapWidth(), 
				requestParam.getMapHeight() == 0 ? 256 : requestParam.getMapHeight());
		pyRenderedParam.setRectangle(rectangle);
		pyRenderedParam.setAffineTransform(
				PyRenderedUtil.createRenderingTransform(rectangle, envelope, pyRenderedParam.getAngle()));
		return pyRenderedParam;
	}

	@Override
	public PyMapContent constructMapContent(PyRenderRequestParam requestParam, ReferencedEnvelope envelope) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		// 构造PyMapContent
		PyMapContent mapContent = new PyMapContent();
		mapContent.setMapWidth(requestParam.getMapWidth());
		mapContent.setMapHeight(requestParam.getMapHeight());
		mapContent.setEnvelope(envelope);
		mapContent.setAngle(requestParam.getAngle());
		// 设置viewpoint
		mapContent.getViewport().setBounds(envelope);
		// 如果是图层组，则需要构造多个layer
		if (requestParam.isLayerGroup()) {
			String[] fileUrlArray = getServerFilePaths(requestParam);
			String sldUrl = requestParam.getSldUrl();
			String[] sldUrlArray = null;
			String[] styleNameArray = null;
			String[] fileTypeArray = requestParam.getDataFormat().split(";");
			// String[] resourceMaxLevelArray = requestParam.getResourceMaxLevel().split(";");
			if (StringUtils.isNotBlank(sldUrl)) {
				sldUrlArray = sldUrl.split(";");
			}else {
				styleNameArray = requestParam.getStyleName().split(";");
			}
			int index = 0;
			for (String file : fileUrlArray) {
//				if(requestParam.getZoomLevel() > Integer.parseInt(resourceMaxLevelArray[index])) {
//					continue;
//				}
				PyRenderRequestParam renderParam = requestParam.clone();
				if (requestParam.getLayerName().contains(",")) {
					String[] split = requestParam.getLayerName().split(",");
					renderParam.setLayerName(split[index]);
				}
				renderParam.setFileUrl(file);
				if (sldUrlArray == null) {
					renderParam.setStyleName(styleNameArray[index]);
				}else {
					renderParam.setSldUrl(sldUrlArray[index]);
				}
				renderParam.setDataFormat(fileTypeArray[index]);
				int dataType = CommonUtil.getDataType(fileTypeArray[index]);
				if (DataTypeEnum.TILED.getFileCode() == dataType || 
						DataTypeEnum.ONLINE.getFileCode() == dataType) {
					requestParam.setTiledLayer(true);
				}
				renderParam.setDataType(dataType);
				Layer layer = createLayerByMapData(renderParam);
				if (layer != null) {
					mapContent.addLayer(createLayerByMapData(renderParam));
				}
				index++;
			}
		} else {
			if (StringUtils.isBlank(requestParam.getLayerName())) {
				File file = new File(requestParam.getFileUrl());
				requestParam.setLayerName(file.getName().split("\\.")[0]);
			}
			Layer layer = createLayerByMapData(requestParam);
			if (layer != null) {
				mapContent.addLayer(layer);
			}
		}
		return mapContent;
	}
	
	private String[] getServerFilePaths(PyRenderRequestParam requestParam) {
		int styles = requestParam.getStyleName().split(";").length;
		int urlNum = requestParam.getFileUrl().split(";").length;
		if (styles == urlNum && requestParam.getPostgisConnection() == null) {
			return requestParam.getFileUrl().split(";");
		}
		String postgisDBName = requestParam.getPostgisConnection().getDataSourceName() + ";";
		String replaceStr = "py_data";
		String serverUrl = requestParam.getFileUrl().replaceAll(postgisDBName, replaceStr);
		String[] urlArray = serverUrl.split(";");
		for (int i = 0; i < urlArray.length; i++) {
			String url = urlArray[i];
			if (url.startsWith(replaceStr)) {
				String temp = url.replace(replaceStr, postgisDBName);
				urlArray[i] = temp;
			}
		}
		return urlArray;
	}

	/**
     * 创建图层
     * @param requestParam
     * @return
     * @throws IOException 
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
     */
    private Layer createLayerByMapData(PyRenderRequestParam requestParam) throws IOException, NoSuchAuthorityCodeException, FactoryException {
    	Layer layer = null;
    	Style layerStyle = null;
    	if (StringUtils.isNotBlank(requestParam.getSldUrl())) {
    		layerStyle = parseStyle.parserStyleBySldFilePath(requestParam.getSldUrl());
    	}else {
    		layerStyle = parseStyle.parserStyleByStyleName(requestParam.getStyleName());
    	}
    	//如果是图层组，则需要控制其级别显示范围
    	String layerName = requestParam.getLayerName();
    	boolean isWms = StringUtils.isNotBlank(requestParam.getService()) && StringUtils.equals("WMS", requestParam.getService()) ? true : false;
		String requestSrs = requestParam.getRequestSrs();
    	if (requestParam.isLayerGroup()) {
			renderMapperTest.querySysStylePath();
 			renderMapper.querySysStylePath();
    		List<LayerGroup> layerGroup = layerBaseInfoUtil.getLayerGroups(layerName);
    		for (LayerGroup layerGroup2 : layerGroup) {
    			if (requestParam.getStyleName().equals(layerGroup2.getStyleName())) {
    				LayerBaseInfo baseInfo = layerBaseInfoUtil.queryServerBaseInfo(layerGroup2.getLayerName());
    				if (!baseInfo.getFilePath().equals(requestParam.getFileUrl())) {
    					continue;
    				}
					if (baseInfo.getStatus() != ServerStatusEnum.PUBLISHED.getCode()) {
						return null;
					}
    				layerName = layerGroup2.getLayerName();
    				requestParam.setLayerName(layerName);
    				List<FeatureTypeStyle> featureTypeStyle = layerStyle.featureTypeStyles();
					for (FeatureTypeStyle featureTypeStyle2 : featureTypeStyle) {
						//获取其中的每一个rule 设置scale
						List<Rule> rules = featureTypeStyle2.rules();
						for (Rule rule : rules) {
							if (isWms) {
								rule.setMaxScaleDenominator(GeometryUtil.calculateScale(layerGroup2.getMinLevel(), requestSrs,true));
								rule.setMinScaleDenominator(GeometryUtil.calculateScale(layerGroup2.getMaxlevel(), requestSrs,false));
							} else {
								rule.setMaxScaleDenominator(GeometryUtil.calculateScale(layerGroup2.getMinLevel(), requestSrs,true, requestParam.getTilecount()));
								rule.setMinScaleDenominator(GeometryUtil.calculateScale(layerGroup2.getMaxlevel(), requestSrs,false, requestParam.getTilecount()));
							}
						}
					}
					break;
    			}
			}
    	}else {
    		//获取其中的每一个rule 设置scale
    		FeatureTypeStyle featureTypeStyle = layerStyle.featureTypeStyles().get(0);
			List<Rule> rules = featureTypeStyle.rules();
			for (Rule rule : rules) {
				rule.setMinScaleDenominator(0.0);
				rule.setMaxScaleDenominator(Double.POSITIVE_INFINITY);
			}
    	}
    	switch (requestParam.getDataType()) {
		//栅格
    	case 1:
    		//获取reader
    		GridCoverage2DReader reader = PyRenderedUtil.getGridCoverage2DReader(requestParam);
    		//获取paramter
    		ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
    		GeneralParameterValue[] readParameters = PyRenderedUtil.getParameters(readParametersDescriptor, null, false);
    		//构造图层
    		layer = new PyGridReaderLayer(reader, layerStyle, readParameters);
    		break;
		//矢量
		case 0:
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
			String reqSrs = requestParam.getRequestSrs();
			//矢量数据适配，根据数据类型（矢量数据小类型）
			if (DataTypeEnum.SHAPE.getFileType().equals(requestParam.getDataFormat())){
				//构造featuresources
				ShapefileDataStoreFactory shapefileDataStoreFactory = new ShapefileDataStoreFactory();
				DataStore dataStore = shapefileDataStoreFactory.createDataStore(createVectorConnectionParam(requestParam));
				File file = new File(requestParam.getFileUrl());
				String shapeFileName = file.getName().substring(0,file.getName().indexOf("."));
				featureSource = dataStore.getFeatureSource(shapeFileName);
				SimpleFeatureType featureType = rebuildFeatureType(featureSource.getSchema());
				featureSource = new PyFeatureSource(featureSource, featureType, Filter.INCLUDE, featureType.getCoordinateReferenceSystem());
				
			}else if (DataTypeEnum.PostGIS.getFileType().equals(requestParam.getDataFormat())) {
				DataStore dataStore = DataStoreFinder.getDataStore(createVectorConnectionParam(requestParam));
				String typeName = requestParam.getFileUrl().split(";")[1];
				SimpleFeatureType featureType = (SimpleFeatureTypeImpl) dataStore.getSchema(typeName);
				featureType = rebuildFeatureType(featureType);
				SimpleFeatureSource simpleFeatureSource = dataStore.getFeatureSource(typeName);
				featureSource = new PyFeatureSource(simpleFeatureSource, featureType, Filter.INCLUDE, featureType.getCoordinateReferenceSystem());
			}
			FeatureLayer featureLayer = new FeatureLayer(featureSource, layerStyle);
			String cqlFilter = requestParam.getCqlFilter();
			if (StringUtils.isNotBlank(cqlFilter)) {
				List<Filter> filters = new ArrayList<>();
				try {
					Filter filter = ECQL.toFilter(cqlFilter);
					filters.add(filter);
				} catch (CQLException e) {
					log.error("FeRenderImpl Contrustor has error : unknow ecql filter");
					throw new BaseException(e.getLocalizedMessage());
				}
				Query definitionQuery = new Query(requestParam.getLayerName());
				definitionQuery.setFilter(filterFactory.and(filters));
				definitionQuery.setVersion("");
				definitionQuery.setSortBy(null);
				definitionQuery.setMaxFeatures(Integer.MAX_VALUE);

				featureLayer.setQuery(definitionQuery);
			}
			layer = featureLayer;
			break;
		case 6:  //瓦片数据
		case 7:  //在线数据
			//只是简单实现，没有其他业务逻辑
        	layer = new RasterLayer(layerStyle) {
				@Override
				public ReferencedEnvelope getBounds() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public SimpleFeatureCollection toFeatureCollection() {
					// TODO Auto-generated method stub
					return null;
				}
			};
			break;
		default:
			break;
		}
    	layer.setTitle(layerName);
    	return layer;
    }

	private SimpleFeatureType rebuildFeatureType(SimpleFeatureType featureType)
			throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sb = new SimpleFeatureTypeBuilder();
		sb.init(featureType);
		sb.setName(featureType.getName());
		sb.setAttributes((AttributeDescriptor[]) null);
		List<AttributeDescriptor> attributes = featureType.getAttributeDescriptors();
		for (AttributeDescriptor ad : attributes) {
			if (ad instanceof GeometryDescriptor) {
				GeometryDescriptor old = (GeometryDescriptor) ad;
				CoordinateReferenceSystem sourceCoordinate = old.getCoordinateReferenceSystem();
				String srsName = null;
				if (sourceCoordinate == null) {
					srsName = "EPSG:404000";
				} else {
					srsName = getSrs(sourceCoordinate);
				}
				AttributeTypeBuilder b = new AttributeTypeBuilder();
		        b.init(old);
		        b.setCRS(CRS.decode(srsName, true));
		        ad = b.buildDescriptor(old.getLocalName());
			}
			sb.add(ad);
		}
		featureType = sb.buildFeatureType();
		return featureType;
	}
	
	private String getSrs(CoordinateReferenceSystem resourceCrs) throws FactoryException {
		String srsName = null;
		Set<ReferenceIdentifier> refrences = resourceCrs.getIdentifiers();
		if (refrences != null && !refrences.isEmpty()) {
			srsName = refrences.stream().findFirst().get().toString();
			return srsName;
		}else {
			//如果proj中不自带srs，则查询获取
			Integer epsgCode = CRS.lookupEpsgCode(resourceCrs, false);
			if (epsgCode == null) {
				return "EPSG:4326";
			} else {
				return "EPSG:" + epsgCode;
			}
		}
	}
	
    private Map<String, Serializable> createVectorConnectionParam(PyRenderRequestParam mapBaseDataInfo) {
    	// 创建默认的连接信息
    	String fileUrl = mapBaseDataInfo.getFileUrl();
		Map<String, Serializable> connectionParameters = new HashMap<String, Serializable>();
		switch (mapBaseDataInfo.getDataFormat()) {
		case "Shape":
			LayerBaseInfo layerBaseInfo = layerBaseInfoUtil.queryServerBaseInfo(mapBaseDataInfo.getLayerName());
			connectionParameters.put("charset", layerBaseInfo.getEncoding());
			connectionParameters.put("filetype", "shapefile");
			connectionParameters.put("create spatial index", true);
			connectionParameters.put("memory mapped buffer", false);
			connectionParameters.put("enable spatial index", true);
			connectionParameters.put("cache and reuse memory maps", true);
			connectionParameters.put("url", "file://" + fileUrl);
			connectionParameters.put("namespace", "localhost");
			TimeZone zone = TimeZone.getDefault();
			connectionParameters.put("timezone", zone);
			connectionParameters.put("fstype", "shape");
			break;
		case "PostGIS":
			PostGisConnectEntity entity = mapBaseDataInfo.getPostgisConnection();
			//TODO: 获取当前数据对应的postgis的连接参数
			connectionParameters.put("host", entity.getDatabaseIp());
			connectionParameters.put("port", entity.getPort());
			connectionParameters.put("database", entity.getDatabaseName());
			connectionParameters.put("user", entity.getUserName());
			connectionParameters.put("passwd", new String(Base64.getDecoder().decode(entity.getPassword())));
			connectionParameters.put("dbtype", "postgis");
		default:
			break;
		}
		return connectionParameters;
    }
}
