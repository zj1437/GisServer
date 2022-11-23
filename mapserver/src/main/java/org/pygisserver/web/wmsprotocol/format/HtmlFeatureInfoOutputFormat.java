package org.pygisserver.web.wmsprotocol.format;

import net.opengis.wfs.FeatureCollectionType;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.pygisserver.web.wmsprotocol.entity.PyGetFeatureInfoRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HtmlFeatureInfoOutputFormat extends PyGetFeatureInfoOutputFormat {

    private static final String htmlHeader = "<html>" +
            "<head>" +
            "<title>PYGISServer GetFeatureInfo output</title>" +
            "</head>" +
            "<style type=\"text/css\">" +
            "table.featureInfo,table.featureInfo td, table.featureInfo th {" +
            "border:1px solid #ddd;" +
            "border-collapse:collapse;" +
            "margin:0;" +
            "padding:0;" +
            "font-size: 90%;" +
            "padding:.2em .1em;" +
            "}" +
            "table.featureInfo th {" +
            "padding:.2em .2em;" +
            "font-weight:bold;" +
            "background:#eee;" +
            "}" +
            "table.featureInfo td{" +
            "background:#fff;" +
            "}" +
            "table.featureInfo tr.odd td{" +
            "background:#eee;" +
            "}" +
            "table.featureInfo caption{" +
            "text-align:left;" +
            "font-size:100%;" +
            "font-weight:bold;" +
            "padding:.2em .2em;" +
            "}" +
            "</style>" +
            "<body>";

    public HtmlFeatureInfoOutputFormat(String contentType) {
        super("text/html");
    }

    @Override
    public void write(FeatureCollectionType results, PyGetFeatureInfoRequest request, OutputStream out) throws ServiceException, IOException {
        Map<String,Integer> indexMap = new HashMap<>();
        StringBuilder result = new StringBuilder(htmlHeader);
        result.append("<table class=\"featureInfo\">");
        List<String> queryLayers = request.getQueryLayers();
        queryLayers.stream().forEach(layer ->{
            int index = 0;
            result.append("<caption class=\"featureInfo\">").append(layer).append("</caption>");
            SimpleFeatureType schema = ((SimpleFeatureCollection) results.getFeature().get(0)).getSchema();
            List<AttributeDescriptor> attributes = schema.getAttributeDescriptors();
            result.append("<tr><th>fid</th>");
            //fid处理
            indexMap.put("fid",index);
            attributes.stream().forEach(attributeDescriptor -> {
                if (schema.getGeometryDescriptor() != null &&
                        !schema.getGeometryDescriptor().getLocalName().equalsIgnoreCase(attributeDescriptor.getLocalName())){
                    //矢量过滤the_geom
                    result.append("<th>").append(attributeDescriptor.getLocalName()).append("</th>");
                } else if (schema.getGeometryDescriptor() == null){
                    result.append("<th>").append(attributeDescriptor.getLocalName()).append("</th>");
                }
            });
            result.append("</tr>");
            Iterator iterator = results.getFeature().iterator();
            while (iterator.hasNext()){
                result.append("<tr>");
                FeatureCollection next = (FeatureCollection) iterator.next();
                FeatureIterator features = next.features();
                while (features.hasNext()){
                    Feature feature = features.next();
                    List<Property> values = (List<Property>) feature.getValue();
                    if (feature.getIdentifier() != null && !StringUtils.isEmpty(feature.getIdentifier().getID())){
                        result.append("<td>").append(feature.getIdentifier().getID()).append("</td>");
                    } else {
                        //栅格的fid设置为空
                        result.append("<td></td>");
                    }
                    values.stream().forEach(value ->{
                        if (schema.getGeometryDescriptor() != null &&
                                !schema.getGeometryDescriptor().getLocalName().equalsIgnoreCase(value.getName().toString())){
                            result.append("<td>").append(((Property) value).getValue()).append("</td>");
                        } else if (schema.getGeometryDescriptor() == null){
                            result.append("<td>").append(((Property) value).getValue()).append("</td>");
                        }
                    });
                    result.append("</tr>");
                }
            }
        });
        result.append("</table><br/></body></html>");
        out.write(result.toString().getBytes(StandardCharsets.UTF_8));
    }
}
