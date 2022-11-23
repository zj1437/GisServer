package org.pygisserver.render.vector;

import com.google.common.base.Charsets;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.render.entity.PyMapContent;
import org.pygisserver.render.util.GeoJSONBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.CoordinatePrecisionReducerFilter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import si.uom.SI;

import javax.measure.Unit;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

public class GeoJsonWMSBuilder implements VectorTileBuilder {

    private Writer writer;

    private CoordinatePrecisionReducerFilter precisionReducerFilter;

    private DeferredFileOutputStream out;

    private GeoJSONBuilder jsonWriter;

    public GeoJsonWMSBuilder(Rectangle mapSize, ReferencedEnvelope mapArea) {

        final int memotyBufferThreshold = 8096;
        out = new DeferredFileOutputStream(memotyBufferThreshold, "geojson", ".geojson", null);
        writer = new OutputStreamWriter(out, Charsets.UTF_8);
        jsonWriter = new GeoJSONBuilder(writer);
        jsonWriter.object(); // start root object
        jsonWriter.key("type").value("FeatureCollection");
        jsonWriter.key("totalFeatures").value("unknown");
        jsonWriter.key("features");
        jsonWriter.array();

        CoordinateReferenceSystem mapCrs = mapArea.getCoordinateReferenceSystem();
        jsonWriter.setAxisOrder(CRS.getAxisOrder(mapCrs));

        Unit<?> unit = mapCrs.getCoordinateSystem().getAxis(0).getUnit();
        Unit<?> standardUnit = unit.getSystemUnit();

        PrecisionModel pm = null;
        if (SI.RADIAN.equals(standardUnit)) {
            pm = new PrecisionModel(1e6); // truncate coords at 6 decimals
        } else if (SI.METRE.equals(standardUnit)) {
            pm = new PrecisionModel(100); // truncate coords at 2 decimals
        }
        if (pm != null) {
            precisionReducerFilter = new CoordinatePrecisionReducerFilter(pm);
        }
    }

    @Override
    public void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry aGeom,
            Map<String, Object> properties) {

        if (precisionReducerFilter != null) {
            aGeom.apply(precisionReducerFilter);
        }

        jsonWriter.object();
        jsonWriter.key("type").value("Feature");

        jsonWriter.key("id").value(featureId);

        jsonWriter.key("geometry");

        // Write the geometry, whether it is a null or not
        jsonWriter.writeGeom(aGeom);
        jsonWriter.key("geometry_name").value(geometryName);

        jsonWriter.key("properties");
        jsonWriter.object();

        for (Map.Entry<String, Object> e : properties.entrySet()) {
            String attributeName = e.getKey();
            Object value = e.getValue();

            jsonWriter.key(attributeName);
            if (value == null) {
                jsonWriter.value(null);
            } else {
                jsonWriter.value(value);
            }
        }

        jsonWriter.endObject(); // end the properties
        jsonWriter.endObject(); // end the feature
    }

    @Override
    public byte[] build(PyMapContent mapContent) throws IOException {
        jsonWriter.endArray(); // end features
        jsonWriter.endObject(); // end root object
        writer.flush();
        writer.close();
        out.close();
        File f = out.getFile();
        try {
        	if (out.isInMemory()) {
            	return out.getData();
            } else {
                return FileUtil.getFileInfo(f.getAbsolutePath());
            }
		} finally {
			if (f != null && f.exists()) {
				f.delete();
			}
		}
    }
}
