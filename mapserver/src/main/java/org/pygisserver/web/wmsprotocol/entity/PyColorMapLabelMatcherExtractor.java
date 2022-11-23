package org.pygisserver.web.wmsprotocol.entity;

import org.pygisserver.common.raster.PyRasterSymbolizerVisitor;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.RasterSymbolizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PyColorMapLabelMatcherExtractor extends PyRasterSymbolizerVisitor {

    static final String LABEL_IN_FEATURE_INFO = "labelInFeatureInfo";
    static final String LABEL_ATTRIBUTE_NAME = "labelAttributeName";

    List<PyColorMapLabelMatcher> colorMapLabelMatcherList;

    public PyColorMapLabelMatcherExtractor(double scaleDenominator) {
        super(scaleDenominator, null);
        this.colorMapLabelMatcherList = new ArrayList<>();
    }

    @Override
    public void visit(RasterSymbolizer raster) {
        super.visit(raster);
        ColorMap cm = raster.getColorMap();
        Map<String, String> vendorOptions = raster.getOptions();
        String labelIncluded = vendorOptions.get(LABEL_IN_FEATURE_INFO);
        String targetAttributeName = vendorOptions.get(LABEL_ATTRIBUTE_NAME);

        if (targetAttributeName == null) targetAttributeName = "Label";

        if (labelIncluded != null
                && !labelIncluded
                .toUpperCase()
                .equals(PyColorMapLabelMatcher.LabelInFeatureInfoMode.NONE.name())) {
            Integer channelName = extractChannelSelectionName(raster.getChannelSelection());
            PyColorMapLabelMatcher colorMapLabelMatcher =
                    new PyColorMapLabelMatcher(targetAttributeName, cm, labelIncluded, channelName);
            colorMapLabelMatcherList.add(colorMapLabelMatcher);
        }
    }

    public List<PyColorMapLabelMatcher> getColorMapLabelMatcherList() {
        return colorMapLabelMatcherList;
    }

    private Integer extractChannelSelectionName(ChannelSelection channelSelection) {
        Integer channelName = null;
        if (channelSelection != null && channelSelection.getGrayChannel() != null)
            channelName =
                    channelSelection
                            .getGrayChannel()
                            .getChannelName()
                            .evaluate(null, Integer.class);
        return channelName;
    }

}
