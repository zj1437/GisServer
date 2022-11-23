package org.pygisserver.web.wmsprotocol.entity;

import org.pygisserver.web.wmsprotocol.identifier.impl.PyVectorRenderingLayerIdentifier;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.styling.*;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;
import org.opengis.style.GraphicalSymbol;

import javax.swing.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PyDynamicBufferEstimator extends AbstractStyleVisitor {

    static final Logger LOGGER = Logging.getLogger(PyVectorRenderingLayerIdentifier.class);

    Feature feature;

    double buffer;

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public int getBuffer() {
        return (int) Math.ceil(buffer);
    }

    @Override
    public void visit(Stroke stroke) {
        Expression width = stroke.getWidth();
        if (width != null) {
            Double w = width.evaluate(feature, Double.class);
            if (w != null && w > buffer) {
                buffer = w;
            }
        }
    }

    @Override
    public void visit(Graphic gr) {
        try {
            Expression grSize = gr.getSize();
            if (grSize != null) {
                Double size = grSize.evaluate(feature, Double.class);
                if (size != null) {
                    buffer = Math.max(buffer, size);
                }
                return;
            }
            for (GraphicalSymbol gs : gr.graphicalSymbols()) {
                if (gs instanceof ExternalGraphic) {
                    ExternalGraphic eg = (ExternalGraphic) gs;
                    Icon icon = null;
                    if (eg.getInlineContent() != null) {
                        icon = eg.getInlineContent();
                    } else {
                        String location = eg.getLocation().toExternalForm();
                        Expression expanded = ExpressionExtractor.extractCqlExpressions(location);

                        Iterator<ExternalGraphicFactory> it =
                                DynamicSymbolFactoryFinder.getExternalGraphicFactories();
                        while (it.hasNext()) {
                            try {
                                icon = it.next().getIcon(feature, expanded, eg.getFormat(), -1);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.FINE,
                                        "Error occurred evaluating external graphic",
                                        e);
                            }
                        }
                    }
                    if (icon != null) {
                        int size = Math.max(icon.getIconHeight(), icon.getIconWidth());
                        if (size > buffer) {
                            buffer = size;
                        }
                        return;
                    }
                } else if (gs instanceof Mark) {
                    if (SLDStyleFactory.DEFAULT_MARK_SIZE > buffer) {
                        buffer = SLDStyleFactory.DEFAULT_MARK_SIZE;
                    }
                }
            }
        } catch (ClassCastException e) {
            LOGGER.info("Could not parse graphic size, " + "it's a literal but not a Number...");
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Error occured during the graphic size estimation, "
                            + "meta buffer estimate cannot be performed",
                    e);
        }
    }
}
