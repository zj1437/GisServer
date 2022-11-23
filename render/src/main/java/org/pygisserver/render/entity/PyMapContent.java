package org.pygisserver.render.entity;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.IndexColorModel;
import java.util.HashMap;
import java.util.Map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;

import lombok.Data;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.renderer.lite.RendererUtilities;

@Data
public class PyMapContent extends MapContent{

    private int mapWidth;

    private int mapHeight;

    private Color bgColor = Color.white;

    private boolean transparent;

    private int tileSize = -1;

    private double angle;

    private int buffer;

    private IndexColorModel icm;

    private ReferencedEnvelope envelope;

    public double getScaleDenominator(boolean considerDPI) {
        Map<String, Object> hints = new HashMap<>();
        AffineTransform at = getRenderingTransform();
        if (Math.abs(XAffineTransform.getRotation(at)) != 0.0) {
            return RendererUtilities.calculateOGCScaleAffine(
                    getCoordinateReferenceSystem(), at, hints);
        } else {
            return RendererUtilities.calculateOGCScale(
                    getViewport().getBounds(), getMapWidth(), hints);
        }
    }

    public AffineTransform getRenderingTransform() {
        Rectangle paintArea = new Rectangle(0, 0, getMapWidth(), getMapHeight());
        ReferencedEnvelope dataArea = getViewport().getBounds();
        AffineTransform tx;
        if (getAngle() != 0.0) {
            tx = new AffineTransform();
            tx.translate(paintArea.width / 2, paintArea.height / 2);
            tx.rotate(Math.toRadians(getAngle()));
            tx.translate(-paintArea.width / 2, -paintArea.height / 2);
            tx.concatenate(RendererUtilities.worldToScreenTransform(dataArea, paintArea));
        } else {
            tx = RendererUtilities.worldToScreenTransform(dataArea, paintArea);
        }
        return tx;
    }

    public ReferencedEnvelope getRenderingArea() {
        ReferencedEnvelope dataArea = getViewport().getBounds();
        if (getAngle() == 0) return dataArea;

        AffineTransform tx = new AffineTransform();
        double offsetX = dataArea.getMinX() + dataArea.getWidth() / 2;
        double offsetY = dataArea.getMinY() + dataArea.getHeight() / 2;
        tx.translate(offsetX, offsetY);
        tx.rotate(Math.toRadians(getAngle()));
        tx.translate(-offsetX, -offsetY);
        Rectangle2D dataAreaShape =
                new Rectangle2D.Double(
                        dataArea.getMinX(),
                        dataArea.getMinY(),
                        dataArea.getWidth(),
                        dataArea.getHeight());
        Rectangle2D transformedBounds = tx.createTransformedShape(dataAreaShape).getBounds2D();
        return new ReferencedEnvelope(transformedBounds, dataArea.getCoordinateReferenceSystem());
    }
}
