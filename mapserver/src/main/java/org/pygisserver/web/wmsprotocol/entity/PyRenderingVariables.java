//package org.pygisserver.web.wmsprotocol.entity;
//
//import org.pygisserver.render.entity.PyMapContent;
//import org.geotools.filter.function.EnvFunction;
//import org.geotools.util.logging.Logging;
//
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//public class FeRenderingVariables {
//
//    static final Logger LOGGER = Logging.getLogger(FeRenderingVariables.class);
//    /** The WMS GetMap BBOX, as a {@link org.geotools.geometry.jts.ReferencedEnvelope} */
//    public static final String WMS_BBOX = "wms_bbox";
//    /** The GetMap SRS, as a {@link org.opengis.referencing.crs.CoordinateReferenceSystem} object */
//    private static final String WMS_SRS = "wms_srs";
//    /** The GetMap width, as an {@link Integer} */
//    private static final String WMS_WIDTH = "wms_width";
//    /** The GetMap height, as an {@link Integer} */
//    private static final String WMS_HEIGHT = "wms_height";
//    /** The GetMap scale denominator, as an {@link Double} */
//    private static final String WMS_SCALE_DENOMINATOR = "wms_scale_denominator";
//
//    public static void setupEnvironmentVariables(PyMapContent mapContent) {
//        // setup some SLD variable substitution environment used by rendering transformations
//        EnvFunction.setLocalValue(WMS_BBOX, mapContent.getViewport().getBounds());
//        EnvFunction.setLocalValue(
//                "wms_crs", mapContent.getViewport().getBounds().getCoordinateReferenceSystem());
//        EnvFunction.setLocalValue(WMS_SRS, mapContent.getViewport().getBounds().getCoordinateReferenceSystem());
//        EnvFunction.setLocalValue(WMS_WIDTH, mapContent.getMapWidth());
//        EnvFunction.setLocalValue(WMS_HEIGHT, mapContent.getMapHeight());
//        try {
//            double scaleDenominator = mapContent.getScaleDenominator(true);
//            EnvFunction.setLocalValue(WMS_SCALE_DENOMINATOR, scaleDenominator);
//        } catch (Exception e) {
//            LOGGER.log(
//                    Level.SEVERE,
//                    "Failed to compute the scale denominator, wms_scale_denominator env variable is unset",
//                    e);
//        }
//    }
//}
