package org.pygisserver.render.vector;

import org.locationtech.jts.geom.Geometry;

import no.ecc.vectortile.VectorTileEncoder;

/*
 * provides VectorTileEncoder that doesn't do any clipping.
 * Our clipping system is "better" (more robust, faster, and maintainable here).
 */
public class VectorTileEncoderNoClip extends VectorTileEncoder {

    public VectorTileEncoderNoClip(int extent, int polygonClipBuffer, boolean autoScale) {
        super(extent, polygonClipBuffer, autoScale);
    }

    /*
     * returns original geometry - no clipping. Assume upstream has already clipped!
     */
    protected Geometry clipGeometry(Geometry geometry) {
        return geometry;
    }
}
