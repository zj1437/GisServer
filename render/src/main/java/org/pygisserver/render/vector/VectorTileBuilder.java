package org.pygisserver.render.vector;

import java.io.IOException;
import java.util.Map;

import org.pygisserver.render.entity.PyMapContent;
import org.locationtech.jts.geom.Geometry;

/**
 * @Description: 收集矢量数据类型 
 * @author: 刘洋 
 * @date: 2021-03-22
 */
public interface VectorTileBuilder {

    /**
     * 瓦片增加特征数据
     * @param layerName
     * @param featureId
     * @param geometryName
     * @param geometry
     * @param properties
     */
    void addFeature(String layerName,
            		String featureId,
            		String geometryName,
            		Geometry geometry,
            		Map<String, Object> properties);
    
    /**
     * 构造瓦片
     * @param mapContent
     * @return
     * @throws IOException
     */
    byte[] build(PyMapContent mapContent) throws IOException;
}
