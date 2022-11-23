package org.pygisserver.web.wmsprotocol.format;

import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.data.util.TemporalUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.pygisserver.web.wmsprotocol.entity.PyGetFeatureInfoRequest;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class TextFeatureInfoOutputFormat extends PyGetFeatureInfoOutputFormat {

    public TextFeatureInfoOutputFormat() {
        super("text/plain");
    }

    @SuppressWarnings("PMD.CloseResource") // just a wrapper, actual output managed by servlet
    public void write(
            FeatureCollectionType results, PyGetFeatureInfoRequest request, OutputStream out) {
        Charset charSet = Charset.forName(getCharset());
        OutputStreamWriter osw = new OutputStreamWriter(out, charSet);
        PrintWriter writer = new PrintWriter(osw);
        int featuresPrinted = 0;
        int maxfeatures = request.getFeatureCount();
        FeatureIterator reader = null;

        try {
            final List collections = results.getFeature();
            FeatureCollection fr;
            SimpleFeature f;
            SimpleFeatureType schema;
            List<AttributeDescriptor> types;
            for (Object collection : collections) {
                fr = (FeatureCollection) collection;
                reader = fr.features();

                boolean startFeat = true;
                while (reader.hasNext()) {
                    Feature feature = reader.next();

                    if (startFeat) {
                        writer.println(
                                "Results for FeatureType '" + fr.getSchema().getName() + "':");
                        startFeat = false;
                    }

                    if (featuresPrinted < maxfeatures) {
//                        writer.println("--------------------------------------------");

                        if (feature instanceof SimpleFeature) {
                            f = (SimpleFeature) feature;
                            schema = f.getType();
                            types = schema.getAttributeDescriptors();

                            for (AttributeDescriptor descriptor : types) {
                                final Name name = descriptor.getName();
                                final Class<?> binding = descriptor.getType().getBinding();
                                if (Geometry.class.isAssignableFrom(binding)) {
                                    Geometry g = (Geometry) f.getAttribute(name);
                                    if (g != null) {
                                        writer.println(
                                                name
                                                        + " = [GEOMETRY ("
                                                        + g.getGeometryType()
                                                        + ") with "
                                                        + g.getNumPoints()
                                                        + " points]");
                                    } else {
                                        writer.println(name + " = null");
                                    }
                                } else if (Date.class.isAssignableFrom(binding)
                                        && TemporalUtils.isDateTimeFormatEnabled()) {
                                    String printValue =
                                            TemporalUtils.printDate((Date) f.getAttribute(name));
                                    writer.println(name + " = " + printValue);
                                } else {
                                    writer.println(name + " = " + f.getAttribute(name));
                                }
                            }

                        } else {
                            writer.println(feature.toString());
                        }
                    }

//                    writer.println("--------------------------------------------");
                    featuresPrinted++;
                }
            }
        } catch (Exception ife) {
            LOGGER.log(Level.WARNING, "Error generating getFeaturInfo, HTML format", ife);
            writer.println("Unable to generate information " + ife);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        if (featuresPrinted == 0) {
            writer.println("no features were found");
        }
        writer.flush();
    }

    @Override
    public String getCharset() {
        return "utf-8";
    }
}
