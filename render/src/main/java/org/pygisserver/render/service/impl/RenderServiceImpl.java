package org.pygisserver.render.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.render.service.PyRenderImage;
import org.pygisserver.render.service.PyRenderVectoreTile;
import org.pygisserver.render.vector.GeoJsonBuilderFactory;
import org.pygisserver.render.vector.MapBoxTileBuilderFactory;
import org.pygisserver.render.vector.TopoJSONBuilderFactory;
import org.pygisserver.render.service.RenderService;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description: 引入dubbo实现 RPC调用
 * TODO 需要引入springboot和dubbo的包，使用dubbo注解实现
 * @author:  
 * @date: 2021-04-09
 */
//@DubboService(version="3.0.0")
@Service("renderService")
@Slf4j
public class RenderServiceImpl implements RenderService {

	@Autowired
	private PyRenderImage renderImage;

	private static final String TERRAIN_FORMT = "image/terrain";

	private static final String TIFF_FORMAT = "image/tiff";
	
	
	@Resource(name="mapbox")
	private PyRenderVectoreTile mapBoxRenderVectoreTile;
	
	
	@Resource(name="geojson")
	private PyRenderVectoreTile geojsonRenderVectoreTile;
	
	
	@Resource(name="topjson")
	private PyRenderVectoreTile topjsonRenderVectoreTile;
	
	@Override
	public byte[] renderImageByRequestParam(PyRenderRequestParam requestParam) {
		String mineType = requestParam.getFormat();
		String sldName = requestParam.getStyleName();
		if(OgcEpsg.DEGREE_CODE == OgcEpsg.getOgcEpsgCodeType(requestParam.getRequestSrs()) && OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(requestParam.getResourcesSrs())) {
			String[] bboxArray = requestParam.getRequestBbox().split(",");
			CoordinateReferenceSystem crs = null;
			try {
				crs = CRS.decode(requestParam.getRequestSrs(), true);
			} catch (Exception e) {
				log.error("RenderService renderImageByRequestParam decode CRS has error : " + e.getLocalizedMessage());
			}
			if (crs != null){
				ReferencedEnvelope envelope = new ReferencedEnvelope(Double.parseDouble(bboxArray[0]),
						Double.parseDouble(bboxArray[1]), Double.parseDouble(bboxArray[2]), Double.parseDouble(bboxArray[3]),
						crs);
				envelope = GeometryUtil.checkEnvelope(envelope);
				requestParam.setRequestBbox(new StringBuffer().append(envelope.getMinX()).append(",")
						.append(envelope.getMaxX()).append(",").append(envelope.getMinY()).append(",").append(envelope.getMaxY()).toString());
			}
		}
		byte[] buff = null;
		if (MapBoxTileBuilderFactory.MIME_TYPE.equalsIgnoreCase(mineType) || MapBoxTileBuilderFactory.LEGACY_MIME_TYPE.equalsIgnoreCase(mineType)){
			buff = mapBoxRenderVectoreTile.vectorTiles(requestParam);
		}else if(TopoJSONBuilderFactory.MIME_TYPE.equalsIgnoreCase(mineType)){
			buff = topjsonRenderVectoreTile.vectorTiles(requestParam);
		}else if (GeoJsonBuilderFactory.MIME_TYPE.equalsIgnoreCase(mineType)) {
			buff = geojsonRenderVectoreTile.vectorTiles(requestParam);
		}else if ((TERRAIN_FORMT.equalsIgnoreCase(mineType) || TIFF_FORMAT.equalsIgnoreCase(mineType)) && ("高程".equals(sldName) || "等高线".equals(sldName))){
			buff = renderImage.renderedDem(requestParam);
		} else {
			buff = renderImage.renderImageByRequestParam(requestParam);
		}
		return buff;
	}
}
