package org.pygisserver.render.vector;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.pygisserver.render.entity.PyMapContent;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

import no.ecc.vectortile.VectorTileEncoder;

/**
 * @Description: MapBox瓦片构造
 * @author: 刘洋 
 * @date: 2021-03-22
 */
public class MapBoxTileBuilder implements VectorTileBuilder {
    private static final Logger LOGGER = Logging.getLogger(MapBoxTileBuilder.class);

    private VectorTileEncoder encoder;

    public MapBoxTileBuilder(Rectangle mapSize, ReferencedEnvelope mapArea) {
        final int extent = Math.max(mapSize.width, mapSize.height);
        final int polygonClipBuffer = extent / 32;
        final boolean autoScale = false;
        this.encoder = new VectorTileEncoderNoClip(extent, polygonClipBuffer, autoScale);
    }

    @Override
    public void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry geometry,
            Map<String, Object> properties) {
        int id = -1;
        if (featureId.matches(".*\\.[0-9]+")) {
            try {
                id = Integer.parseInt(featureId.split("\\.")[1]);
            } catch (NumberFormatException e) {
            }
        }

        if (id < 0) {
            LOGGER.warning("Cannot obtain numeric id from featureId: " + featureId);
        }

        encoder.addFeature(layerName, properties, geometry, id);
    }

    @Override
    public byte[] build(PyMapContent mapContent) throws IOException {
        return encoder.encode();
    }
}
