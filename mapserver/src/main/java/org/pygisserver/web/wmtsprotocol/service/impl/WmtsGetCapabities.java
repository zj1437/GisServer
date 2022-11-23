package org.pygisserver.web.wmtsprotocol.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ServerContact;
import org.pygisserver.common.entity.ServerProtocolInfo;
import org.pygisserver.common.enumm.DataType;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.mapper.MapServerDataMapper;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.io.XMLBuilder;
import org.geowebcache.layer.TileLayer;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @Description
 * @author: 刘洋
 * @date: 2019-12-09
 */
@Component
@Slf4j
public class WmtsGetCapabities {

	@Autowired
	private MapServerDataMapper mapServerDataMapper;

	@Autowired
	private MapServerMapper mapServerMapper;

	private String baseUrl;

	private static final String BASEPATH = "pytc/service/wmts";

	private static final String supportFormat = "image/png;image/jpeg;image/tiff;osgb;image/terrain";
	
	private static final String VectorSupportFormat = "application/json;type=geojson,application/x-protobuf;type=mapbox-vector";
	
	private static final String supportInfoFormat = "text/plain;application/vnd.ogc.gml;text/xml;text/html;application/json";

	
	private List<ServerProtocolInfo> protocols = null;

	public byte[] getCapabitiesByte(String baseUrlPrefix) {
		this.baseUrl = MessageFormat.format(String.valueOf(new StringBuffer(baseUrlPrefix).append("{0}")),BASEPATH);
		final Charset encoding = StandardCharsets.UTF_8;
		return generateGetCapabilities(encoding).getBytes(encoding);
	}

	private String generateGetCapabilities(Charset encoding) {
		StringBuilder str = new StringBuilder();
		XMLBuilder xml = new XMLBuilder(str);

		try {
			xml.header("1.0", encoding);
			xml.indentElement("Capabilities");
			xml.attribute("xmlns", "http://www.opengis.net/wmts/1.0");
			xml.attribute("xmlns:ows", "http://www.opengis.net/ows/1.1");
			xml.attribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
			xml.attribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			xml.attribute("xmlns:gml", "http://www.opengis.net/gml");
			StringBuilder schemasLocations = new StringBuilder("http://www.opengis.net/wmts/1.0 ");
			schemasLocations.append("http://schemas.opengis.net/wmts/1.0/wmtsGetCapabilities_response.xsd ");
			schemasLocations.delete(schemasLocations.length() - 1, schemasLocations.length());
			xml.attribute("xsi:schemaLocation", schemasLocations.toString());
			xml.attribute("version", "1.0.0");
			// DataSourceContextHolder.setDataSource("default");
			ServerContact serviceInformation = mapServerDataMapper.queryServerContact();
			serviceIdentification(xml, serviceInformation);
			serviceProvider(xml, serviceInformation);
			operationsMetadata(xml);
			contents(xml);
			xml.indentElement("ServiceMetadataURL")
					.attribute("xlink:href", baseUrl + "?REQUEST=getcapabilities&VERSION=1.0.0").endElement();
			xml.endElement("Capabilities");
			return str.toString();
		} catch (IOException e) {
			log.error("WmtsGetCapabities generateGetCapabilities has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "WMTS服务能力值文件"));
		}
	}

	private void serviceIdentification(XMLBuilder xml, ServerContact servInfo) throws IOException {
		xml.indentElement("ows:ServiceIdentification");
		if (servInfo != null) {
			appendTag(xml, "ows:Title", servInfo.getTitleName(), "Web Map Tile Service - GeoWebCache");
			appendTag(xml, "ows:Abstract", servInfo.getAbstractName(), null);

			if (servInfo != null && servInfo.getKeywords() != null) {
				xml.indentElement("ows:Keywords");
				appendTag(xml, "ows:Keyword", servInfo.getKeywords(), null);
				xml.endElement();
			}
		} else {
			xml.simpleElement("ows:Title", "Web Map Tile Service - GeoWebCache", true);
		}
		xml.simpleElement("ows:ServiceType", "OGC WMTS", true);
		xml.simpleElement("ows:ServiceTypeVersion", "1.0.0", true);
		xml.endElement("ows:ServiceIdentification");
	}

	private void serviceProvider(XMLBuilder xml, ServerContact servInfo) throws IOException {
		xml.indentElement("ows:ServiceProvider");
		if (servInfo != null) {
			appendTag(xml, "ows:ProviderName", servInfo.getOrganization(), null);
			if (servInfo.getWebSite() != null) {
				xml.indentElement("ows:ProviderSite").attribute("xlink:href", servInfo.getWebSite()).endElement();
			}
			xml.indentElement("ows:ServiceContact");
			appendTag(xml, "ows:IndividualName", servInfo.getContactPerson(), null);
			appendTag(xml, "ows:PositionName", servInfo.getPrimary(), null);
			xml.indentElement("ows:ContactInfo");
			xml.indentElement("ows:Phone");
			appendTag(xml, "ows:Voice", servInfo.getTelephone(), null);
			appendTag(xml, "ows:Facsimile", servInfo.getFax(), null);
			xml.endElement();
			xml.indentElement("ows:Address");
			appendTag(xml, "ows:DeliveryPoint", servInfo.getAddress(), null);
			appendTag(xml, "ows:City", servInfo.getCity(), null);
			appendTag(xml, "ows:PostalCode", servInfo.getPostcode(), null);
			appendTag(xml, "ows:Country", servInfo.getCountry(), null);
			appendTag(xml, "ows:ElectronicMailAddress", servInfo.getEmail(), null);
			xml.endElement("ows:Address");
			xml.endElement();
			xml.endElement();
		} else {
			appendTag(xml, "ows:ProviderName", baseUrl, null);
			xml.indentElement("ows:ProviderSite").attribute("xlink:href", baseUrl).endElement();
			xml.indentElement("ows:ServiceContact");
			appendTag(xml, "ows:IndividualName", "GeoWebCache User", null);
			xml.endElement();
		}
		xml.endElement("ows:ServiceProvider");
	}

	private void operationsMetadata(XMLBuilder xml) throws IOException {
		xml.indentElement("ows:OperationsMetadata");
		operation(xml, "GetCapabilities", baseUrl);
		operation(xml, "GetTile", baseUrl);
		operation(xml, "GetFeatureInfo", baseUrl);
		xml.endElement("ows:OperationsMetadata");
	}

	private void operation(XMLBuilder xml, String operationName, String baseUrl) throws IOException {
		xml.indentElement("ows:Operation").attribute("name", operationName);
		xml.indentElement("ows:DCP");
		xml.indentElement("ows:HTTP");
		xml.indentElement("ows:Get").attribute("xlink:href", baseUrl + "?");
		xml.indentElement("ows:Constraint").attribute("name", "GetEncoding");
		xml.indentElement("ows:AllowedValues");
		xml.simpleElement("ows:Value", "KVP", true);
		xml.endElement();
		xml.endElement();
		xml.endElement();
		xml.endElement();
		xml.endElement();
		xml.endElement("ows:Operation");
	}

	// TODO 该方法需要使用我们的数据进行查询
	private void contents(XMLBuilder xml) throws IOException {
		xml.indentElement("Contents");
		RequestQueryCondition condition = new RequestQueryCondition();
		condition.setProtocolName("wmts");
		List<LayerBaseInfo> list = mapServerMapper.queryLayerBaseInfo(condition);
		for (LayerBaseInfo layer : list) {
			try {
				layer(xml, layer, baseUrl);
			} catch (MismatchedDimensionException | NumberFormatException | FactoryException e) {
				e.printStackTrace();
			}
		}
		List<String> supportSrs = GeometryUtil.supportSrs;
		for (String srs : supportSrs) {
			tileMatrixSet(xml, srs);
		}
		xml.endElement("Contents");
	}

	private void layer(XMLBuilder xml, LayerBaseInfo layer, String baseurl) throws IOException, MismatchedDimensionException, NumberFormatException, NoSuchAuthorityCodeException, FactoryException {
		xml.indentElement("Layer");
		appendTag(xml, "ows:Title", layer.getLayerName(), null);
		//增加abstract字段，用来适配其他客户端
		appendTag(xml, "ows:Abstract", layer.getDescription(), layer.getLayerName());
		layerWGS84BoundingBox(xml, layer);
		appendTag(xml, "ows:Identifier", layer.getLayerName(), null);
		layerStyles(xml, layer);
		layerFormats(xml, layer);
		layerInfoFormats(xml, layer);
		layerDescriptions(xml, layer);
		layerGridSubSets(xml, layer);
		xml.endElement("Layer");
	}

	private void layerWGS84BoundingBox(XMLBuilder xml, LayerBaseInfo layer) throws IOException {
		String nativeBbox = layer.getNativaBbox();
		if (StringUtils.isNotBlank(nativeBbox)) {
			String[] bboxArray = nativeBbox.split(";");
			double[] coords = {Double.parseDouble(bboxArray[0]),Double.parseDouble(bboxArray[1]),Double.parseDouble(bboxArray[2]),Double.parseDouble(bboxArray[3])};
			xml.indentElement("ows:WGS84BoundingBox");
			xml.simpleElement("ows:LowerCorner", coords[0] + " " + coords[1], true);
			xml.simpleElement("ows:UpperCorner", coords[2] + " " + coords[3], true);
			xml.endElement("ows:WGS84BoundingBox");
			return;
		}
		String resourceBbox = layer.getResourceBbox();
		if (StringUtils.isNotBlank(resourceBbox)) {
			String[] bboxArray = resourceBbox.split(";");
			double[] coords = {Double.parseDouble(bboxArray[0]),Double.parseDouble(bboxArray[1]),Double.parseDouble(bboxArray[2]),Double.parseDouble(bboxArray[3])};
			double originShift = 2 * Math.PI * 6378137 / 2.0;
			double mx = coords[0];
			double my = coords[1];
			double lon = (mx / originShift) * 180.0;
			double lat = (my / originShift) * 180.0;

			lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
			xml.indentElement("ows:WGS84BoundingBox");
			xml.simpleElement("ows:LowerCorner", lon + " " + lat, true);

			mx = coords[2];
			my = coords[3];
			lon = (mx / originShift) * 180.0;
			lat = (my / originShift) * 180.0;

			lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);

			xml.simpleElement("ows:UpperCorner", lon + " " + lat, true);
			xml.endElement("ows:WGS84BoundingBox");
			return;
		}
	}

	private void layerStyles(XMLBuilder xml, LayerBaseInfo layer) throws IOException {
		String defStyle = layer.getStyleName();
		xml.indentElement("Style");
		xml.attribute("isDefault", "true");
		if (defStyle == null) {
			xml.simpleElement("ows:Identifier", "", true);
		} else {
			xml.simpleElement("ows:Identifier", TileLayer.encodeDimensionValue(defStyle), true);
		}
		xml.endElement("Style");
	}

	private void layerFormats(XMLBuilder xml, LayerBaseInfo layer) throws IOException {
//		 xml.indentElement("OutputFormat");
		String wformats = supportFormat;
		boolean isDem = false;
		boolean isOsgbFile = "osgb".equalsIgnoreCase(layer.getResourceFormat());
		if (!isOsgbFile) {
			wformats = wformats.replace(";osgb", "");
    	}
		if(StringUtils.isNotBlank(layer.getStyleName())) {
			isDem = layer.getStyleName().equals("高程");
		}
		if(!isDem) {
			wformats = wformats.replace(";image/terrain", "");
		}
		Set<String> formatSet = covertStrToList(wformats);
		Iterator<String> mimeIter = formatSet.iterator();
		while (mimeIter.hasNext()) {
			xml.simpleElement("Format", mimeIter.next(), true);
		}
		if (DataType.getDataTypeByTypeCode(layer.getDataType()).equals(DataType.VECTOR)) {
			String[] strArray = VectorSupportFormat.split(",");
			Set<String> vectorFormatSet = new HashSet<String>();
			for (String string : strArray) {
				vectorFormatSet.add(string);
			}
			Iterator<String> mimeIterator = vectorFormatSet.iterator();
			while (mimeIterator.hasNext()) {
				xml.simpleElement("Format", mimeIterator.next(), true);
			}
		}

//		xml.endElement("OutputFormat");
	}
	
	private void layerDescriptions(XMLBuilder xml, LayerBaseInfo layer) throws IOException {
		// 只有矢量有infoformat
		String desc = StringUtils.isBlank(layer.getDescription()) ? "" : layer.getDescription();
		xml.simpleElement("Description", desc, true);
	}

	private void layerInfoFormats(XMLBuilder xml, LayerBaseInfo layer) throws IOException {
		// 只有矢量有infoformat
		if (DataType.getDataTypeByTypeCode(layer.getDataType()).equals(DataType.VECTOR)) {
			Set<String> set = covertStrToList(supportInfoFormat);
			Iterator<String> mimeIter = set.iterator();
			while (mimeIter.hasNext()) {
				xml.simpleElement("InfoFormat", mimeIter.next(), true);
			}
		}
	}

	private Set<String> covertStrToList(String convertStr) {
		String[] strArray = convertStr.split(";");
		Set<String> set = new HashSet<String>();
		for (String string : strArray) {
			set.add(string);
		}
		return set;
	}

	private ReferencedEnvelope constructEnvelope(String resourceBbox ,String reourceSrs) throws MismatchedDimensionException, NumberFormatException, NoSuchAuthorityCodeException, FactoryException {
		String[] bboxArray = resourceBbox.split(";");
		return new ReferencedEnvelope(Double.parseDouble(bboxArray[0]), Double.parseDouble(bboxArray[2]),
				Double.parseDouble(bboxArray[1]), Double.parseDouble(bboxArray[3]), CRS.decode(reourceSrs,true));
	}
	
	private void layerGridSubSets(XMLBuilder xml, LayerBaseInfo layer) throws IOException, MismatchedDimensionException, NumberFormatException, NoSuchAuthorityCodeException, FactoryException {
		if(protocols == null) {
			protocols = mapServerMapper.queryServerProtocolInfo();
		}
		Set<String> gridSubsets = covertStrToList(protocols.get(0).getTileGridSet());
		int maxLevel = layer.getSupportMaxLevel();
		String nativeSrs = layer.getNativaBbox();
		ReferencedEnvelope nativeEnvelope = constructEnvelope(nativeSrs,"EPSG:4326");
		for (String gridSetId : gridSubsets) {
			xml.indentElement("TileMatrixSetLink");
			xml.simpleElement("TileMatrixSet", gridSetId, true);
//			if (nativeEnvelope.equals(GeometryUtil.WHOLE_ENVELOPE_4326)) {
//				continue;
//			}
			String[] levelNames = constructLevelNames(gridSetId,maxLevel);
			if (OgcEpsg.getOgcEpsgCodeType(gridSetId) == OgcEpsg.MERCATOR_CODE) {
				nativeEnvelope = (ReferencedEnvelope) GeometryUtil.transformCoordinateSystem(nativeEnvelope, "EPSG:3857");
			}else {
				nativeEnvelope = (ReferencedEnvelope) GeometryUtil.transformCoordinateSystem(nativeEnvelope, "EPSG:4326");
			}
			//获取数据
			int[][] wmtsLimits = constructWmtsLimits(gridSetId,maxLevel,nativeEnvelope);
			xml.indentElement("TileMatrixSetLimits");
			for (int i = 0; i <= maxLevel; i++) {
				xml.indentElement("TileMatrixLimits");
				xml.simpleElement("TileMatrix", levelNames[i], true);
				xml.simpleElement("MinTileRow", Integer.toString(wmtsLimits[i][1]), true);
				xml.simpleElement("MaxTileRow", Integer.toString(wmtsLimits[i][3]), true);
				xml.simpleElement("MinTileCol", Integer.toString(wmtsLimits[i][0]), true);
				xml.simpleElement("MaxTileCol", Integer.toString(wmtsLimits[i][2]), true);
				xml.endElement();
			}
			xml.endElement();
			xml.endElement("TileMatrixSetLink");
		}
		
	}

	private String[] constructLevelNames(String srs,int maxLevel) {
		String[] levelNames = new String[maxLevel+1];
		for (int i = 0; i <= maxLevel; i++) {
			levelNames[i] = srs+":"+i;
		}
		return levelNames;
	}
	
	private int[][] constructWmtsLimits(String srs,int maxLevel,ReferencedEnvelope envelope){
		int[][] wmtsLimits = new int[maxLevel+1][];
		for (int i = 0; i <= maxLevel; i++) {
			int[] range;
			if (OgcEpsg.getOgcEpsgCodeType(srs) == OgcEpsg.MERCATOR_CODE) {
				range = GeometryUtil.convert3857ToTiledRange(i, envelope);
			}else {
				range = GeometryUtil.convert4326ToTiledRange(i, envelope);
			}
			wmtsLimits[i] = range;
		}
		return wmtsLimits;
	}
	
	
	private void tileMatrixSet(XMLBuilder xml, String srsName) throws IOException {
		xml.indentElement("TileMatrixSet");
		xml.simpleElement("ows:Identifier", srsName, true);
		// If the following is not good enough, please get in touch and we will try to
		// fix it :)
		xml.simpleElement("ows:SupportedCRS", "urn:ogc:def:crs:EPSG::" + srsName.split(":")[1], true);
		// TODO detect these str.append("
		// <WellKnownScaleSet>urn:ogc:def:wkss:GlobalCRS84Pixel</WellKnownScaleSet>\n");
		for (int i = 0; i < GeometryUtil.getMaxSupportLevel(srsName); i++) {
			tileMatrix(xml,srsName,false,i);
        }
		xml.endElement("TileMatrixSet");
	}

	private void tileMatrix(XMLBuilder xml, String srsName, boolean scaleWarning,int level) throws IOException {
		xml.indentElement("TileMatrix");
		if (scaleWarning) {
			xml.simpleElement("ows:Abstract",
					"The grid was not well-defined, the scale therefore assumes 1m per map unit.", true);
		}
		xml.simpleElement("ows:Identifier", String.valueOf(new StringBuffer(srsName).append(":").append(level)), true);
		if (level == 0) {
			xml.simpleElement("ScaleDenominator", Double.toString(GeometryUtil.calculateScale(level, srsName, false, null)), true);
		}else {
			xml.simpleElement("ScaleDenominator", Double.toString(GeometryUtil.calculateScale(level, srsName, false, null)), true);
		}
		xml.indentElement("TopLeftCorner").text(Double.toString(GeometryUtil.getOrderedTopLeftCorner(srsName)[0])).text(" ")
				.text(Double.toString(GeometryUtil.getOrderedTopLeftCorner(srsName)[1])).endElement();
		xml.simpleElement("TileWidth", Integer.toString((int) GeometryUtil.DEFAULT_TILE_WIDTH), true);
		xml.simpleElement("TileHeight", Integer.toString((int) GeometryUtil.DEFAULT_TILE_HEIGHT), true);
		xml.simpleElement("MatrixWidth", Long.toString(GeometryUtil.getNumMatrixTile(srsName,level)[0]), true);
		xml.simpleElement("MatrixHeight", Long.toString(GeometryUtil.getNumMatrixTile(srsName,level)[1]), true);
		xml.endElement("TileMatrix");
	}

	private void appendTag(XMLBuilder xml, String tagName, String value, String defaultValue) throws IOException {
		if (value == null) {
			if (defaultValue == null)
				return;
			else
				value = defaultValue;
		}
		xml.simpleElement(tagName, value, true);
	}
}
