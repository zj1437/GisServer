package org.pygisserver.render.vector;

import java.awt.Rectangle;
import java.util.Set;

import org.geotools.geometry.jts.ReferencedEnvelope;

import com.google.common.collect.ImmutableSet;

/**
 * @Description: 
 * @author: 刘洋 
 * @date: 2021-03-22
 */
public class MapBoxTileBuilderFactory implements VectorTileBuilderFactory {

    public static final String MIME_TYPE = "application/vnd.mapbox-vector-tile";
    public static final String LEGACY_MIME_TYPE = "application/x-protobuf;type=mapbox-vector";

    public static final Set<String> OUTPUT_FORMATS =
            ImmutableSet.of(MIME_TYPE, LEGACY_MIME_TYPE, "pbf");

    @Override
    public Set<String> getOutputFormats() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public MapBoxTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea) {
        return new MapBoxTileBuilder(screenSize, mapArea);
    }

    /**
     * For Mapbox tiles, since they are rendered in screen/tile space, oversampling produces more
     * consistent results when zooming. See this question here:
     *
     * <p>https://github.com/mapbox/vector-tiles/issues/45
     */
    @Override
    public boolean shouldOversampleScale() {
        return true;
    }

    /** Use 16x oversampling to match actual Mapbox tile extent, which is 4096 for 3857 tiles */
    @Override
    public int getOversampleX() {
        return 16;
    }

    /** Use 16x oversampling to match actual Mapbox tile extent, which is 4096 for 3857 tiles */
    @Override
    public int getOversampleY() {
        return 16;
    }
}
