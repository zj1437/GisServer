package org.pygisserver.render.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.label.LabelCacheImpl;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Title : GeojsonToImageUtils
 * @Description: geojson矢量数据渲染
 * @author: 张亨特
 * @date: 2022-05-23
 */
public class GeojsonToImageUtils {
    /**
     * 获取GeoJSON图片
     * @return
     * @throws IOException
     */
    public static BufferedImage geojsonStr2img(String json, Style layerStyle, int mapWidth, int mapHeight) {
        BufferedImage bufferedImage = null;
        try {
//            String geoJson = removeGeoJsonProperties(json);
            bufferedImage = geojson2img(json, layerStyle, mapWidth, mapHeight);
        }catch (Exception e){
            e.printStackTrace();
        }
        return bufferedImage;
    }
    /**
     * 生成图片
     * @param json
     * @throws IOException
     */
    public static BufferedImage geojson2img(String json, Style layerStyle, int mapWidth, int mapHeight) throws IOException {
        FeatureJSON featureJSON = new FeatureJSON();
        FeatureCollection features = featureJSON.readFeatureCollection(json);
        MapContent mapContent = new MapContent();
        mapContent.setTitle("Quickstart");
        Layer layer = new FeatureLayer(features, layerStyle);
        mapContent.addLayer(layer);
        ReferencedEnvelope bounds = features.getBounds();
        BufferedImage bufferedImage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        mapContent.getViewport().setMatchingAspectRatio(true);
        mapContent.getViewport().setScreenArea(new Rectangle(Math.round(mapWidth), Math.round(mapHeight)));
        mapContent.getViewport().setBounds(bounds);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle outputArea = new Rectangle(210, 150);
        GTRenderer renderer = new StreamingRenderer();
        LabelCacheImpl labelCache = new LabelCacheImpl();
        Map<Object, Object> hints = renderer.getRendererHints();
        if (hints == null) {
            hints = new HashMap<>();
        }
        hints.put(StreamingRenderer.LABEL_CACHE_KEY, labelCache);
        renderer.setRendererHints(hints);
        renderer.setMapContent(mapContent);
        renderer.paint(g2d, outputArea, bounds);
        return bufferedImage;
    }

//    /**
//     * 生成样式
//     * @param features
//     * @param json
//     * @return
//     */
//    public static Style getStyle(FeatureCollection features, String json){
//        Integer geojsonType = getGeojsonType(json);
//        if (geojsonType == 1){
//            Style point = SLD.createSimpleStyle(features.getSchema(), Color.GRAY);
//            return point;
//        }else if (geojsonType ==2){
//            Style lineStyle = SLD.createLineStyle(Color.GRAY, 1);
//            return lineStyle;
//        }else if(geojsonType == 3){
//            Style polygonStyle = SLD.createPolygonStyle(Color.white, Color.GRAY, 1);
//            return polygonStyle;
//        }
//        return null;
//    }

    /**
     * 去除properties属性
     * @param jsonstr
     * @return
     */
    public static String removeGeoJsonProperties(String jsonstr){
        JSONObject json = (JSONObject) JSONObject.parse(jsonstr);
        JSONArray features = (JSONArray) json.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            feature.remove("properties");
        }
        return json.toJSONString();
    }
    /**
     * 获取GeoJSON类型
     * @param strJson
     * @return
     */
    public static Integer getGeojsonType(String strJson){
        JSONObject json = (JSONObject) JSONObject.parse(strJson);
        JSONArray features = (JSONArray) json.get("features");
        JSONObject feature0 = features.getJSONObject(0);
        String strType = ((JSONObject)feature0.get("geometry")).getString("type").toString();
        Integer geoType = null;
        if ("Point".equals(strType)) {
            geoType = 1;
        } else if ("MultiPoint".equals(strType)) {
            geoType = 1;
        } else if ("LineString".equals(strType)) {
            geoType = 2;
        } else if ("MultiLineString".equals(strType)) {
            geoType = 2;
        } else if ("Polygon".equals(strType)) {
            geoType = 3;
        } else if ("MultiPolygon".equals(strType)) {
            geoType = 3;
        }
        return geoType;
    }
}
