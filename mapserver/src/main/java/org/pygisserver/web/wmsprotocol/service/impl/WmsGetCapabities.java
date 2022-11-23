package org.pygisserver.web.wmsprotocol.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.entity.ServerContact;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.mapper.MapServerDataMapper;
import org.pygisserver.common.service.MapServerDataService;
import org.pygisserver.web.entity.OgcProtocol;
import org.geotools.referencing.CRS;
import org.geowebcache.io.XMLBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description wms协议GetCapabities接口
 * @author: 张亨特
 * @date: 2022/07/19
 */
@Component
@Slf4j
public class WmsGetCapabities {

    @Autowired
    private MapServerDataMapper mapServerDataMapper;

    @Autowired
    private MapServerDataService mapServerDataService;

    @Autowired
    private OgcProtocol ogcProtocol;

    private static final String NAMESPACE = "http://www.opengis.net/wms";

    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private static final String XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String WMS_CAPS_MIME = "text/xml";

    private static final String XLINK_HREF_130 = "http://localhost:8088/pygisserver/ows?SERVICE=WMS&";

    private static final String XLINK_HREF_111 = "http://localhost:8088/pygisserver/wms?SERVICE=WMS&amp;";

    private static final String[] EXCEPTION_FORMATS_130 = {"XML", "INIMAGE", "BLANK", "JSON"};

    private static final String[] EXCEPTION_FORMATS_111 = {"application/vnd.ogc.se_xml", "application/vnd.ogc.se_inimage", "application/vnd.ogc.se_blank", "application/json"};

    private static final String[] GET_FEATUREINFO_FOORMAT = {"text/plain", "application/vnd.ogc.gml", "text/xml", "application/vnd.ogc.gml/3.1.1", "text/xml; subtype=gml/3.1.1", "text/html", "application/json"};

    private String baseUrl;

    private String version;

    public String generateGetCapabilities(String baseUrl, String version) {
        this.baseUrl = baseUrl;
        this.version = version;
        StringBuilder str = new StringBuilder();
        XMLBuilder xml = new XMLBuilder(str);
        try {
            xml.indentElement("WMS_Capabilities");
            if ("1.3.0".equals(version)) {
                xml.attribute("xmlns", NAMESPACE);
                xml.attribute("xmlns:xlink", XLINK_NS);
                xml.attribute("xmlns:xsi", XML_SCHEMA_INSTANCE);
                xml.attribute("version", "1.3.0");
                StringBuilder schemasLocations = new StringBuilder("http://www.opengis.net/wms ");
                schemasLocations.append(baseUrl + "schemas/wms/1.3.0/capabilities_1_3_0.xsd");
                xml.attribute("xsi:schemaLocation", schemasLocations.toString());
            } else if ("1.1.1".equals(version)) {
                xml.attribute("version", "1.1.1");
            }
            handleService(xml);
            xml.indentElement("Capability");
            handleRequest(xml);
            handleException(xml);
            if ("1.1.1".equals(version)) {
                xml.indentElement("UserDefinedSymbolization")
                        .attribute("SupportSLD", "1")
                        .attribute("UserLayer", "1")
                        .attribute("UserStyle", "1")
                        .attribute("RemoteWFS", "1")
                        .endElement();
            }
            handleLayers(xml);
            xml.endElement("Capability");
            xml.endElement("WMS_Capabilities");
            return str.toString();
        } catch (IOException e) {
            log.error("WmsGetCapabities generateGetCapabilities has error:" + ExceptionsUtil.stackTraceToStr(e));
            throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "WMS服务能力值文件"));
        }
    }

    private void handleService(XMLBuilder xml) throws IOException {
        xml.indentElement("Service");
        appendTag(xml, "Name", "WMS", null);
        appendTag(xml, "Title", "", null);
        appendTag(xml, "Abstract", "", null);
        appendTag(xml, "KeywordList", "", null);
        xml.indentElement("OnlineResource");
        xml.attribute("xlink:type", "simple");
        xml.attribute("xlink:href", baseUrl);
        if ("1.1.1".equals(version)) {
            xml.attribute("xmlns:xlink", XLINK_NS);
        }
        xml.endElement("OnlineResource");
        handleContact(xml);
        appendTag(xml, "Fees", "none", null);
        appendTag(xml, "AccessConstraints", "none", null);
        xml.endElement("Service");
    }

    private void handleContact(XMLBuilder xml) throws IOException {
        ServerContact contact = mapServerDataMapper.queryServerContact();
        if (contact != null) {
            xml.indentElement("ContactInformation");
            xml.indentElement("ContactPersonPrimary");
            appendTag(xml, "ContactPerson", contact.getContactPerson(), null);
            appendTag(xml, "ContactOrganization", contact.getOrganization(), null);
            xml.endElement("ContactPersonPrimary");
            appendTag(xml, "ContactPosition", contact.getPrimary(), null);
            xml.indentElement("ContactAddress");
            appendTag(xml, "AddressType", null, null);
            appendTag(xml, "Address", contact.getAddress(), null);
            appendTag(xml, "City", contact.getCity(), null);
            appendTag(xml, "StateOrProvince", contact.getProvince(), null);
            appendTag(xml, "PostCode", contact.getPostcode(), null);
            appendTag(xml, "Country", contact.getCountry(), null);
            xml.endElement("ContactAddress");
            appendTag(xml, "ContactVoiceTelephone", contact.getTelephone(), null);
            appendTag(xml, "ContactFacsimileTelephone", contact.getFax(), null);
            appendTag(xml, "ContactElectronicMailAddress", contact.getEmail(), null);
            xml.endElement("ContactInformation");
        }
    }

    private void handleRequest(XMLBuilder xml) throws IOException {
        xml.indentElement("Request");
        List<String> https = new ArrayList<>();
        https.add("get");
        https.add("post");
        handleCapabilities(xml, https);
        https.remove("post");
        handleGetMap(xml, https);
        if ("1.1.1".equals(version)) {
            https.add("post");
        }
        handleGetFeatureInfo(xml, https);
        if ("1.1.1".equals(version)) {
            https.remove("post");
            handleDescribeLayer(xml, https);
            handleGetLegendGraphic(xml, https);
            handleGetStyles(xml, https);
        }
        xml.endElement("Request");
    }

    private void handleGetStyles(XMLBuilder xml, List<String> https) throws IOException {
        xml.indentElement("GetLegendGraphic");
        appendTag(xml, "Format", "application/vnd.ogc.sld+xml", null);
        xml.indentElement("DCPType");
        handleHttp(xml, https);
        xml.endElement("DCPType");
        xml.endElement("GetLegendGraphic");
    }

    private void handleGetLegendGraphic(XMLBuilder xml, List<String> https) throws IOException {
        xml.indentElement("GetLegendGraphic");
        appendTag(xml, "Format", "image/png", null);
        appendTag(xml, "Format", "image/jpeg", null);
        appendTag(xml, "Format", "application/json", null);
        appendTag(xml, "Format", "image/gif", null);
        xml.indentElement("DCPType");
        handleHttp(xml, https);
        xml.endElement("DCPType");
        xml.endElement("GetLegendGraphic");
    }

    private void handleDescribeLayer(XMLBuilder xml, List<String> https) throws IOException {
        xml.indentElement("DescribeLayer");
        // application/vnd.ogc.wms_xml
        appendTag(xml, "Format", "application/vnd.ogc.wms_xml", null);
        xml.indentElement("DCPType");
        handleHttp(xml, https);
        xml.endElement("DCPType");
        xml.endElement("DescribeLayer");
    }

    private void handleGetFeatureInfo(XMLBuilder xml, List<String> https) throws IOException {
        xml.indentElement("GetFeatureInfo");
        for (String format : GET_FEATUREINFO_FOORMAT) {
            appendTag(xml, "Format", format, null);
        }
        xml.indentElement("DCPType");
        handleHttp(xml, https);
        xml.endElement("DCPType");
        xml.endElement("GetFeatureInfo");
    }

    private void handleGetMap(XMLBuilder xml, List<String> https) throws IOException {
        xml.indentElement("GetMap");
        Map<String, List<String>> getMap = ogcProtocol.getWmsParameter().get("GetMap");
        for (String format : getMap.get("outPutFormat")) {
            appendTag(xml, "Format", format, null);
        }
        xml.indentElement("DCPType");
        handleHttp(xml, https);
        xml.endElement("DCPType");
        xml.endElement("GetMap");
    }

    private void handleCapabilities(XMLBuilder xml, List<String> https) throws IOException {
        xml.indentElement("GetCapabilities");
        appendTag(xml, "Format", WMS_CAPS_MIME, null);
        xml.indentElement("DCPType");
        handleHttp(xml, https);
        xml.endElement("DCPType");
        xml.endElement("GetCapabilities");
    }

    private void handleHttp(XMLBuilder xml, List<String> param) throws IOException {
        xml.indentElement("HTTP");
        for (String doc: param) {
            handleOnlineResource(xml, doc);
        }
        xml.endElement("HTTP");
    }

    private void handleOnlineResource(XMLBuilder xml, String doc) throws IOException{
        xml.indentElement(doc);
        xml.indentElement("OnlineResource");
        xml.attribute("xlink:type", "simple");
        if ("1.3.0".equals(version)) {
            xml.attribute("xlink:href", baseUrl + "ows?SERVICE=WMS&");
        } else if ("1.1.1".equals(version)) {
            xml.attribute("xlink:href", baseUrl + "wms?SERVICE=WMS&amp;");
            xml.attribute("xmlns:xlink", XLINK_NS);
        }
        xml.endElement();
        xml.endElement(doc);
    }

    private void handleException(XMLBuilder xml) throws IOException {
        xml.indentElement("Exception");
        String[] exceptions = "1.3.0".equals(version) ? EXCEPTION_FORMATS_130 : EXCEPTION_FORMATS_111;
        for (String exceptionFormat : exceptions) {
            appendTag(xml, "Format", exceptionFormat, null);
        }
        xml.endElement("Exception");
    }


    private void handleLayers(XMLBuilder xml) throws IOException {
        xml.indentElement("Layer");
        appendTag(xml, "Title", null, null);
        appendTag(xml, "Abstract", null, null);
        for (String code : CRS.getSupportedCodes("AUTO")) {
            if ("WGS84(DD)".equals(code)) {
                continue;
            }
            appendTag(xml, "CRS", "AUTO:" + code, null);
        }
        for (String code : CRS.getSupportedCodes("EPSG")) {
            appendTag(xml, "CRS", "EPSG:" + code, null);
        }
        if ("1.3.0".equals(version)) {
            appendTag(xml, "CRS", "CRS:84", null);
        }

        List<LayerBaseInfo> layerInfos = mapServerDataService.queryServerInfo("wms", "1.3.0", null);
        for (LayerBaseInfo layerInfo : layerInfos) {
            String desc = StringUtils.isBlank(layerInfo.getDescription())
                    ? layerInfo.getLayerName() : layerInfo.getDescription();
            handleLayer(xml, layerInfo, desc, true);
        }

        xml.endElement("Layer");
    }


    private void handleLayer(XMLBuilder xml, LayerBaseInfo layer, String desc, boolean isRoot) throws IOException {
        xml.indentElement("Layer")
                .attribute("queryable", "1")
                .attribute("opaque", "0");

        // 修改为图层名称，不加默认的workspace
        appendTag(xml, "Name", layer.getLayerName(), null);
        appendTag(xml, "Abstract", layer.getIsLayerGroup() == 1 ? "Layer-Group type layer: " + layer.getLayerName() : desc, null);
        appendTag(xml, "Description", desc, null);
        xml.indentElement("KeywordList");
        if (layer != null) {
            appendTag(xml, "Keyword", "WCS", null);
            appendTag(xml, "Keyword", layer.getFileType(), null);
        }
        xml.endElement("KeywordList");
        final String crs = layer.getResourceSrs();
        String[] split = layer.getResourceBbox().split(";");
        String[] splitNativaBbox = layer.getNativaBbox().split(";");
        if ("1.3.0".equals(version)) {
            appendTag(xml, "CRS", crs, null);
            appendTag(xml, "CRS", "CRS:84", null);
            xml.indentElement("EX_GeographicBoundingBox");
            appendTag(xml, "westBoundLongitude", split[0], null);
            appendTag(xml, "eastBoundLongitude", split[2], null);
            appendTag(xml,"southBoundLatitude", split[1], null);
            appendTag(xml,"northBoundLatitude", split[3], null);
            xml.endElement("EX_GeographicBoundingBox");
            xml.indentElement("BoundingBox")
                    .attribute("CRS", crs)
                    .attribute("minx", splitNativaBbox[0])
                    .attribute("miny", splitNativaBbox[1])
                    .attribute("maxx", splitNativaBbox[2])
                    .attribute("maxy", splitNativaBbox[3])
                    .endElement();
            xml.indentElement("BoundingBox")
                    .attribute("CRS", "CRS:84")
                    .attribute("minx", split[0])
                    .attribute("miny", split[1])
                    .attribute("maxx", split[2])
                    .attribute("maxy", split[3])
                    .endElement();
        } else if ("1.1.1".equals(version)) {
            appendTag(xml, "SRS", crs, null);
            xml.indentElement("LatLonBoundingBox")
                    .attribute("maxy", splitNativaBbox[3])
                    .attribute("maxx", splitNativaBbox[2])
                    .attribute("miny", splitNativaBbox[1])
                    .attribute("minx", splitNativaBbox[0])
                    .endElement();
            xml.indentElement("BoundingBox")
                    .attribute("maxy", split[3])
                    .attribute("maxx", split[2])
                    .attribute("miny", split[1])
                    .attribute("minx", split[0])
                    .attribute("SRS", crs)
                    .endElement();
        }
        xml.indentElement("Style");
        String styleName = layer.getIsLayerGroup() == 1 ? "default-style-" + layer.getLayerName() : layer.getStyleName();
        appendTag(xml, "Name", styleName, null);
        handleStyleLegendURL(xml, styleName);
        xml.endElement("Style");
        xml.endElement("Layer");
    }


    private void handleStyleLegendURL(XMLBuilder xml, String styleName)  throws IOException {
        xml.indentElement("LegendURL")
                .attribute("width", "30")
                .attribute("height", "30");
        appendTag(xml, "Format", "image/png", null);
        Map<String, String> params = params(
                        "service",
                        "WMS",
                        "request",
                        "GetLegendGraphic",
                        "format",
                        "image/png",
                        "width",
                        String.valueOf(20),
                        "height",
                        String.valueOf(20),
                        "style",
                        StringUtils.isBlank(styleName) ? "" : styleName);
        String legendURL = buildURL("ows", params);
        xml.indentElement("OnlineResource")
                .attribute("xmlns:xlink", XLINK_NS)
                .attribute("xlink:type", "simple")
                .attribute("xlink:href", legendURL)
                .endElement();
        xml.endElement("LegendURL");
    }

    private Map<String, String> params(String... parameters) {
        Map<String, String> result = new LinkedHashMap<>();
        if (parameters.length % 2 != 0){
            throw new IllegalArgumentException("The parameters sequence should be composed of key/value pairs, but the params passed are odd in number");
        }
        for (int i = 0; i < parameters.length; ) {
            String key = parameters[i++];
            String value = parameters[i++];
            result.put(key, value);
        }
        return result;
    }

    private String buildURL(String ows, Map<String, String> params) {
        StringBuilder legendURL = new StringBuilder(baseUrl);
        legendURL.append(File.separator)
                .append(ows)
                .append("?");
        params.forEach((key, value) -> {
            legendURL.append(key)
                    .append("=")
                    .append(value)
                    .append("&");
        });
        return legendURL.deleteCharAt(legendURL.length() - 1).toString();
    }

    private void appendTag(XMLBuilder xml, String tagName, String value, String defaultValue) throws IOException {
        if (value == null) {
            if (defaultValue == null) {
                return;
            } else {
                value = defaultValue;
            }
        }
        xml.simpleElement(tagName, value, true);
    }
}
