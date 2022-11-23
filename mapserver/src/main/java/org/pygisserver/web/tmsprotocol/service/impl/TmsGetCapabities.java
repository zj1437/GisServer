package org.pygisserver.web.tmsprotocol.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.OgcEpsg;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.pytc.entity.RequestQueryCondition;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.utils.MapMimeType;
import org.pygisserver.render.util.TdbUtils;
import org.pygisserver.web.entity.GuassCoordinates;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.io.XMLBuilder;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Description: 
 * @author: 刘洋 
 * @date: 2019-12-09
 */
@Component
@Slf4j
public class TmsGetCapabities {
    
	
    private String baseUrl;
    
    private String layerName;
    
    private Charset encoding = Charset.forName("UTF-8");

    public final static String TILEMAPSERVICE_LEADINGPATH = "tms/1.0.0";

    public final static String SERVICE_PATH = "pytc/service/" + TILEMAPSERVICE_LEADINGPATH;

    @Autowired
	private MapServerMapper mapServerMapper;
    
    @Autowired
    private GuassCoordinates guassCoordinates;
    

    public byte[] getCapabitiesByte(String layerInfo,String baseUrl) {
    	String[] layerSRSFormatExtension = layerInfo.split("@");
    	RequestQueryCondition condition = new RequestQueryCondition();
    	layerName = layerSRSFormatExtension[0];
		condition.setServerName(layerSRSFormatExtension[0]);
		List<LayerBaseInfo> layerBaseInfo = mapServerMapper.queryLayerBaseInfo(condition);
    	LayerBaseInfo tl = layerBaseInfo.get(0);
//        String srsName = tl.getResourceSrs();
        String srsName = layerSRSFormatExtension[1];
        MimeType mimeType = createMimeType(layerSRSFormatExtension[2]);
        this.baseUrl = baseUrl;
    	String capabitiesXml = getTileMapDoc(tl,srsName,mimeType);
        return capabitiesXml.getBytes(StandardCharsets.UTF_8);
    }

	public MimeType createMimeType(String mType) {
		MimeType mimeType = null;
        if(mType.equals("osgb")) {
        	mimeType = new MapMimeType("image/osgb","osgb",null,"osgb",false);
        } else if (mType.equals("terrain")) {
        	mimeType = new MapMimeType("image/terrain","terrain",null,"terrain",false);
        } else if (mType.equals(TdbUtils.TDB_DEM_TYPE)) {
        	mimeType = new MapMimeType("image/" + TdbUtils.TDB_DEM_TYPE,TdbUtils.TDB_DEM_TYPE,null,TdbUtils.TDB_DEM_TYPE,false);
        } else {
        	try {
				mimeType = MimeType.createFromExtension(mType);
			} catch (MimeException e) {
				e.printStackTrace();
			}
        }
		return mimeType;
	}

    protected String getTileMapDoc(LayerBaseInfo layer, String srsName, MimeType mimeType) {
        StringBuilder str = new StringBuilder();
        XMLBuilder xml = new XMLBuilder(str);
        try {
            xml.header("1.0", encoding);
            xml.indentElement("TileMap")
            .attribute("tilemapservice", baseUrl+SERVICE_PATH)
            .attribute("version", "1.0");
            xml.simpleElement("Title", tileMapTitle(), true);
            xml.simpleElement("SRS", srsName, true);
            String resourceBbox = layer.getResourceBbox();
            String resourceSrs = layer.getResourceSrs();
            Double[] bBox = new Double[4];
            bBox[0] = Double.parseDouble(resourceBbox.split(";")[0]);
            bBox[1] = Double.parseDouble(resourceBbox.split(";")[1]);
            bBox[2] = Double.parseDouble(resourceBbox.split(";")[2]);
            bBox[3] = Double.parseDouble(resourceBbox.split(";")[3]);
            if (!StringUtils.equals(srsName,resourceSrs)){
                ReferencedEnvelope referencedEnvelope = GeometryUtil.transformCoordinateSystem(new ReferencedEnvelope(bBox[0], bBox[2], bBox[1], bBox[3], CRS.decode(resourceSrs, true)), srsName);
                bBox[0] = referencedEnvelope.getMinX();
                bBox[1] = referencedEnvelope.getMinY();
                bBox[2] = referencedEnvelope.getMaxX();
                bBox[3] = referencedEnvelope.getMaxY();
            }
            xml.boundingBox(null, bBox[0], bBox[1], bBox[2], bBox[3]);
            xml.indentElement("Origin")
                .attribute("x", Double.toString(bBox[0]))
                .attribute("y", Double.toString(bBox[1]))
                .endElement();

            int tileHeight, tileWidth;
            if (("GeoTIFF".equals(layer.getFileType()) && "terrain".equals(mimeType.getFormat())) || ("TDB".equals(layer.getFileType()) && "terrain".equals(mimeType.getFormat()))) {
                tileHeight = 65;
                tileWidth = 65;
            } else {
                tileHeight = layer.getTileHeight();
                tileWidth = layer.getTileWidth();
            }
            // Can we have multiple formats? NO
            xml.indentElement("TileFormat")
                .attribute("extension", mimeType.getFileExtension())
                .attribute("height", Integer.toString(tileHeight))
                .attribute("mime-type", mimeType.getMimeType())
                .attribute("width", Integer.toString(tileWidth))
                .endElement();
            String desc = StringUtils.isBlank(layer.getDescription()) ? "" : layer.getDescription();
    		xml.simpleElement("Description", desc, true);
            xml.indentElement("TileSets")
                .attribute("profile", profileForGridSet(srsName));
            Double[] resolutions = null;
            int supportMaxLevel = 0;
            if (guassCoordinates.getGuassCoordinates().contains(srsName)){
                List<String> tilecount = guassCoordinates.getTileParam().get(srsName.replace(":", "")).get("tilecount");
                supportMaxLevel = Math.min(layer.getSupportMaxLevel(), tilecount.size() - 1);
                resolutions = GeometryUtil.getResolutions(tilecount,supportMaxLevel,tileWidth);
            } else if (GeometryUtil.supportSrs.contains(srsName) || OgcEpsg.MERCATOR_CODE == OgcEpsg.getOgcEpsgCodeType(srsName)){
                String tempSrs = OgcEpsg.getOgcEpsgCodeType(srsName) == OgcEpsg.MERCATOR_CODE
                                && !srsName.equals(OgcEpsg.Mercator_3857.getOgcCode())
                                ? OgcEpsg.Mercator_3857.getOgcCode() : srsName;
                supportMaxLevel = Math.min(layer.getSupportMaxLevel(), GeometryUtil.getMaxSupportLevel(tempSrs));;
                resolutions = GeometryUtil.getResolutions(tempSrs,supportMaxLevel,tileWidth);
            } else {
                xml.indentElement("info");
                xml.attribute("exception"," does not support Srs : " + srsName);
                xml.endElement();
                xml.endElement();
                xml.endElement();
                return str.toString().toLowerCase();
            }
            for(int zoom = 0 ; zoom <= supportMaxLevel; zoom++) {
                xml.indentElement("TileSet");
                xml.attribute("href", tileMapUrl(srsName, mimeType, zoom, baseUrl));
                xml.attribute("units-per-pixel", Double.toString(resolutions[zoom]));
                xml.attribute("order", Integer.toString(zoom));
                xml.endElement();
            }
            
            xml.endElement();
            xml.endElement();
            
            return str.toString().toLowerCase();
        } catch (Exception e) {
        	log.error("TmsGetCapabities getTileMapDoc has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_CREATE, "TMS服务能力值文件"));
        }
    }
    
    protected String profileForGridSet(String srsName) {
        if(srsName.equals(GeometryUtil.supportSrs.get(0))) {
            return "global-geodetic";
        } else if(srsName.equals(GeometryUtil.supportSrs.get(1))) {
            return "global-mercator";
        } else {
            return "local";
        }
    }
    
    protected String tileMapUrl(String srsName, MimeType mimeType,
            String baseUrl) {
        return new StringBuilder().append(baseUrl).append(SERVICE_PATH).append("/").append(tileMapName(srsName,mimeType)).toString();
    }

    protected String tileMapUrl(String srsName, MimeType mimeType, int z,
            String baseUrl) {
        return tileMapUrl(srsName, mimeType, baseUrl) + "/" + z;
    }

    protected String tileMapName(String srsName, MimeType mimeType) {
    	String name =  null;
        try {
            name = URLEncoder.encode(layerName, "UTF-8");
            String gridSubset = URLEncoder.encode(srsName, "UTF-8");
            return name + "@" + gridSubset + "@" + mimeType.getFileExtension();
        } catch (UnsupportedEncodingException e) {
        	log.error("TmsGetCapabities tileMapName has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_PARSE, name + "服务信息"));
        }
    }

    protected String tileMapTitle() {
        return layerName;
    }

}
