package org.pygisserver.render.service.impl;

import static org.geotools.renderer.lite.PyVectorRenderUtils.buildTransform;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.enumm.DataType;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.raster.PyRasterSymbolizerVisitor;
import org.pygisserver.common.util.ImageUtil;
import org.pygisserver.render.entity.MapBaseDataInfo;
import org.pygisserver.render.entity.PyMapContent;
import org.pygisserver.render.entity.PyRenderedParam;
import org.pygisserver.render.service.PyRenderImage;
import org.pygisserver.render.tiled.GdalReadData;
import org.pygisserver.render.util.PyDirectRasterRender;
import org.pygisserver.render.util.PyParseStyle;
import org.pygisserver.render.util.PyRenderedUtil;
import org.pygisserver.render.util.PyStreamingRender;
import org.pygisserver.render.util.GeojsonToImageUtils;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.Processors;
import org.geotools.process.function.ProcessFunction;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;
import no.ecc.vectortile.VectorTileDecoder;
import vector_tile.VectorTile;

/**
 * @Description: 图片渲染实现类
 * @author:
 * @date: 2021-03-19
 */
@Service
@Slf4j
public class PyRenderImageImpl extends PyRenderImpl implements PyRenderImage {

	@Autowired
    private PyParseStyle parseStyle;
	
	@Autowired
	private PyDirectRasterRender directRasterRender;
	
	@Autowired
	private PyStreamingRender streamingRender;
	
	private Rectangle paintArea = new Rectangle(0,0,4096,4096);
	
	private MathTransform screenToWorld;

	@Autowired
    private CustomDataLoadImpl customDataLoad;
	
	@Override
	public byte[] renderImageByRequestParam(PyRenderRequestParam requestParam) {
		constructRenderRequestParam(requestParam);
		PyRenderedParam pyRenderedParam = constructRenderParam(requestParam);
		RenderedImage image = null;
		//如果是栅格数据
		Style[] styles = null;
		if (requestParam.getDataType() == DataType.RASTER.getTypeCode() && !requestParam.isLayerGroup()) {
			Style style = null;
			image = directRasterRender(requestParam, pyRenderedParam,style);
			if (style != null) {
				styles = new Style[1];
				styles[0] = style;
			}
		}

		// 瓦片矢量数据
		if (requestParam.getDataType() == DataTypeEnum.TILED.getFileCode()
                && StringUtils.equalsAny(requestParam.getDataFormat(), "geojson", "protobuf", "pbf")) {
			image = dealGeoserverThumbal(requestParam);
			if(image == null) {
				return null;
			}
		}

		//图层组和矢量数据使用流式渲染
		if (image == null) {
			PyMapContent mapContent = null;
			try {
				mapContent = constructMapContent(requestParam, pyRenderedParam.getEnvelope());
				pyRenderedParam.setMapcontent(mapContent);
				image = streamingRender.renderedImage(pyRenderedParam,requestParam);
			} catch (IOException | FactoryException e) {
				log.error("PYRenderImageImpl renderImageByRequestParam has error:{}", e.getMessage());
				return null;
			}finally {
				if (mapContent != null) {
					mapContent.dispose();
				}
			}
		}
		//如果是Img数据，且设置了透明，则需要处理
		if (DataTypeEnum.ERDASIMG.getFileType().equals(requestParam.getDataFormat())
				&& StringUtils.isNotBlank(requestParam.getInputTransparentColor())){
			image = ImageUtil.makeTransparent(image, requestParam.getInputTransparentColor().replaceFirst("#", ""));
		}
		return formatMapImage(pyRenderedParam.getFormat(),image,styles);
	}

	
	private void transformGrid2World(ReferencedEnvelope envelope,String requestSrs) throws FactoryException, NoninvertibleTransformException {
		AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(envelope, paintArea);
		MathTransform targetToScreen = ProjectiveTransform.create(worldToScreen);
		MathTransform sourceToTargetCrs = buildTransform(CRS.decode(requestSrs,true),CRS.decode(requestSrs,true));
		MathTransform sourceToScreen = ConcatenatedTransform.create(sourceToTargetCrs, targetToScreen);
		this.screenToWorld = sourceToScreen.inverse();
	}
	
	private ReferencedEnvelope createEnvelope(String requestEnvelope,String srs) throws MismatchedDimensionException, NumberFormatException, FactoryException {
		String[] envelopeArray = requestEnvelope.split(",");
		ReferencedEnvelope envelope = new ReferencedEnvelope(Double.parseDouble(envelopeArray[0]),Double.parseDouble(envelopeArray[2]),
				Double.parseDouble(envelopeArray[1]),Double.parseDouble(envelopeArray[3]),CRS.decode(srs,true));
		return envelope;
	}

	private RenderedImage dealGeoserverThumbal(PyRenderRequestParam requestParam) {
		try {
			transformGrid2World(createEnvelope(requestParam.getRequestBbox(),requestParam.getRequestSrs()),requestParam.getRequestSrs());
		} catch (MismatchedDimensionException | NumberFormatException | NoninvertibleTransformException
				| FactoryException e1) {
			log.error("dealGeoserverThumbal  has error {}",e1.getLocalizedMessage());
		}
		String jeoGsonString = "";
		if (StringUtils.equalsAnyIgnoreCase(requestParam.getDataFormat(), "protobuf", "pbf")) {
			try {
                byte[] data;
			    if ("TDB".equalsIgnoreCase(requestParam.getFileType())) {
					data = requestParam.getLeftOrRight() == 0
							? customDataLoad.getImageInfo(0,0,0, requestParam.getFileUrl(), requestParam.getLayerName())
							: OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(requestParam.getRequestSrs())
							? customDataLoad.getImageInfo(0,1,0, requestParam.getFileUrl(), requestParam.getLayerName())
							: null;
                } else {
                    data = VectorTile.Tile.parseFrom(new BufferedInputStream(new FileInputStream(requestParam.getFileUrl()))).toByteArray();
                }
				if (data == null) {
					return null;
				}
				VectorTileDecoder decoder = new VectorTileDecoder();
				decoder.setAutoScale(false);
				VectorTileDecoder.FeatureIterable featureIterable = decoder.decode(data);
				JSONArray geoJsonArray = new JSONArray();
				for (VectorTileDecoder.Feature mvtFeature : featureIterable) {
					Map<String, Object> attributes = mvtFeature.getAttributes();
					Geometry geometry = mvtFeature.getGeometry();
					//grid坐标转世界坐标
					geometry = JTS.transform(geometry, screenToWorld);
					StringWriter stringWriter = new StringWriter();
					GeometryJSON geometryJSON = new GeometryJSON(20);
					geometryJSON.write(geometry,stringWriter);
					String geometryJSONArray = stringWriter.toString();
					JSONObject parse = (JSONObject) JSON.parse(geometryJSONArray);
					if (parse == null || !parse.containsKey("coordinates")) {
						continue;
					}
					JSONObject geometryObject = new JSONObject();
					geometryObject.put("coordinates", parse.get("coordinates"));
					geometryObject.put("type", geometry.getGeometryType());
					JSONObject featuresObject = new JSONObject();
					featuresObject.put("geometry", geometryObject);
					featuresObject.put("properties", attributes);
					featuresObject.put("type", "Feature");
					geoJsonArray.add(featuresObject);
				}
				JSONObject geoJsonObject = new JSONObject();
				geoJsonObject.put("features",geoJsonArray);
				geoJsonObject.put("type", "FeatureCollection");
				jeoGsonString = geoJsonObject.toJSONString();
			} catch (IOException | MismatchedDimensionException | TransformException e) {
				log.error("mapbox数据转geojson数据异常!");
			}
		} else {
		    if ("TDB".equalsIgnoreCase(requestParam.getFileType())) {
                byte[] data = requestParam.getLeftOrRight() == 0
						? customDataLoad.getImageInfo(0,0,0, requestParam.getFileUrl(), requestParam.getLayerName())
						: OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(requestParam.getRequestSrs())
						? customDataLoad.getImageInfo(0,1,0, requestParam.getFileUrl(), requestParam.getLayerName())
						: null;
				if (data == null) {
					return null;
				}
                try {
                    jeoGsonString = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                StringBuilder geoJson = new StringBuilder();
                try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(requestParam.getFileUrl()))){
                    byte[] bytes = new byte[2048];
                    int n = -1;
                    while ((n = inputStream.read(bytes, 0, bytes.length)) != -1) {
                        geoJson = geoJson.append(new String(bytes, 0, n, "UTF-8"));
                    }
                } catch (IOException e) {
                    log.error("矢量瓦片数据读取异常!");
                }
                jeoGsonString = geoJson.toString();
            }
		}
		Integer geojsonType = GeojsonToImageUtils.getGeojsonType(jeoGsonString);
		Style layerStyle = null;
		if (geojsonType == 1) {
			layerStyle = parseStyle.parserStyleByStyleName("五角星");
		}else if (geojsonType == 2) {
			layerStyle = parseStyle.parserStyleByStyleName("默认线");
		}else if(geojsonType == 3) {
			layerStyle = parseStyle.parserStyleByStyleName("默认面");
		}
		return GeojsonToImageUtils.geojsonStr2img(jeoGsonString, layerStyle, requestParam.getMapWidth(), requestParam.getMapHeight());
	}

	private RenderedImage directRasterRender(PyRenderRequestParam requestParam, PyRenderedParam pyRenderedParam,Style style) {
		GridCoverage2DReader reader = null;
		try {
			//TODO 移除reader.dispose(),使用缓存是否可以
			reader = PyRenderedUtil.getGridCoverage2DReader(requestParam);
			pyRenderedParam.setReader(reader);
			PyRasterSymbolizerVisitor rasterSymbolizerVisitor = new PyRasterSymbolizerVisitor();
			final List<RasterSymbolizer> rasterSymbolizer = getSymbolizers(requestParam.getSldUrl(),requestParam.getStyleName(), rasterSymbolizerVisitor,style);
			Expression transformation = rasterSymbolizerVisitor.getRasterRenderingTransformation();
			// 构造最终的读取参数
			ParameterValueGroup readParametersDescriptor = reader.getFormat().getReadParameters();
			//构造输入输出透明色参数
			Map<String,Serializable> setUpParams = null;
			if (StringUtils.isNotBlank(requestParam.getInputTransparentColor())) {
				setUpParams = new HashMap<String,Serializable>();
				setUpParams.put("InputTransparentColor", requestParam.getInputTransparentColor());
			}
			
			if (StringUtils.isNotBlank(requestParam.getOutPutTransparentColor())) {
				if (setUpParams == null) {
					setUpParams = new HashMap<String,Serializable>();
				}
				setUpParams.put("OutputTransparentColor", requestParam.getInputTransparentColor());
			}
			
			GeneralParameterValue[] params = PyRenderedUtil.getParameters(readParametersDescriptor, setUpParams, false);
			pyRenderedParam.setGeneralParam(params);
			if (rasterSymbolizer != null && rasterSymbolizer.size() == 1 && !isVectorSource(transformation)) {
				pyRenderedParam.setSymbolizer(rasterSymbolizer.get(0));	
				RenderedImage image = directRasterRender.renderRasterImage(pyRenderedParam);
                return image;
			}
		} catch (Exception e) {
			log.error("PyRenderImageImpl renderImageByRequestParam has error: 获取栅格gridreader失败，原因{}",e.getMessage());
		} finally {
			try {
				reader.dispose();
			} catch (IOException e) {
				log.error("PyRenderImageImpl renderImageByRequestParam has error: 清除reader报错，原因{}",e.getMessage());
			}
		}
		return null;
	}
	
	
	private byte[] formatMapImage(String format,RenderedImage image,Style...styles) {
		if (image != null && format.endsWith("png")) {
			return ImageUtil.formatImage2Png(image,styles);
		}else if (image != null && format.endsWith("jpeg")) {
			return ImageUtil.formatImage2Jpeg(image);
		} else if (image != null && format.endsWith("tiff")) {
			return ImageUtil.formatImage2Tiff(image);
		}
		return null;
	}
	
	
	private boolean isVectorSource(Expression tranformation) {
        if (tranformation instanceof ProcessFunction) {
            ProcessFunction processFunction = (ProcessFunction) tranformation;
            Name processName = processFunction.getProcessName();
            Map<String, org.geotools.data.Parameter<?>> params =
                    Processors.getParameterInfo(processName);
            for (org.geotools.data.Parameter<?> param : params.values()) {
                if (SimpleFeatureCollection.class.isAssignableFrom(param.getType())) {
                    return true;
                }
            }
        }
        return false;
    }
	
	private List<RasterSymbolizer> getSymbolizers(String sldFile,
												  String styleName,
												  PyRasterSymbolizerVisitor visitor,
												  Style style) {
		List<RasterSymbolizer> rasterSymbolizer = new ArrayList<RasterSymbolizer>();
		style = getStyle(sldFile, styleName);
		style.accept(visitor);
		rasterSymbolizer = visitor.getRasterSymbolizers();
		return rasterSymbolizer;
	}

	public Style getStyle(String sldFile, String styleName) {
		final Style style;
		if (StringUtils.isBlank(sldFile)) {
			style = parseStyle.parserStyleByStyleName(styleName);
		}else {
			style = parseStyle.parserStyleBySldFilePath(sldFile);
		}
		return style;
	}

	@Override
	public RenderedImage renderImageByRequestParam(PyRenderedParam requestParam, MapBaseDataInfo mapBaseDataInfo) {
		return null;
	}
	@Override
	public byte[] renderedDem(PyRenderRequestParam requestParam) {
		TileRequestInfo tileInfo = new TileRequestInfo();
		tileInfo.setGridSet(requestParam.getRequestSrs());
		tileInfo.setLayerName(requestParam.getLayerName());
		String reqBbox = requestParam.getRequestBbox();
		tileInfo.setMineType(requestParam.getFormat());
		tileInfo.setTileWidth(requestParam.getMapWidth());
		tileInfo.setTileHeight(requestParam.getMapHeight());
		String[] reqBboxArray = reqBbox.split(",");
		BoundingBox bbox;
		try {
			bbox = new ReferencedEnvelope(Double.parseDouble(reqBboxArray[0]),
					Double.parseDouble(reqBboxArray[1]),
					Double.parseDouble(reqBboxArray[2]),
					Double.parseDouble(reqBboxArray[3]),
					CRS.decode(requestParam.getRequestSrs(), true));
			tileInfo.setBbox(bbox);
		} catch (MismatchedDimensionException | NumberFormatException | FactoryException e) {
			log.error("FeRenderImageImpl renderedDem has error:" + e.getLocalizedMessage());
		}
		return getDemBuf(tileInfo, requestParam.getFileUrl(), requestParam.getResourcesSrs());
	}
	
	
	private byte[] getDemBuf(TileRequestInfo tileInfo, String filePath, String sourcesSrs) {
		byte[] buff = null;
		GdalReadData gdalReadData = new GdalReadData(tileInfo, filePath, false, tileInfo.getTileWidth(), tileInfo.getTileHeight(), sourcesSrs);
		if (tileInfo.getMineType().endsWith("tiff")) {
			buff = gdalReadData.getTifResource();
		}
		if (tileInfo.getMineType().endsWith("terrain")) {
			float[] pixelArray = gdalReadData.getDataRasterPiexl();
			buff = convetDemPixelData(pixelArray);
		}
		return buff;
	}

	private byte[] convetDemPixelData(float[] pixelArray) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[65 * 65 * 2 + 2]);
		for (int j = 0; j < 65; j++) {
			for (int i = 0; i < 65; i++) {
				short pixel = (short) pixelArray[j * 65 + i];
				if (pixel < 0) {
					pixel = 0;
				}
				// 像素大于0，陆地，小于零则是海洋
				short demHigh = (short) ((pixel + 1000) * 5);
				byte[] buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(demHigh).array();
				byteBuffer.put(buf);
			}
		}
		// 切片到最后一级的时候，子瓦片数设置为0
		byte[] buf = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte) 15).array();
		byteBuffer.put(buf);
		// 根据高程和陆地来确定最后一位值
		// 全部是陆地
		byte[] land = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte) 255).array();
		byteBuffer.put(land);
		return byteBuffer.array();
	}
}
