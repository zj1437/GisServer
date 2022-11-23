package org.pygisserver.web.wmsprotocol.entity;

import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PyColorMapLabelMatcher {

    static final String DEFAULT_ATTRIBUTE_NAME = "Label";

    String attributeName;
    ColorMap colorMap;
    String labelInclusion;
    Integer channel;

    public PyColorMapLabelMatcher(
            String attributeName, ColorMap colorMap, String labelInclusion, Integer channel) {

        String labelInclusionUpper = labelInclusion.toUpperCase();
        if (!PyColorMapLabelMatcher.LabelInFeatureInfoMode.getAsStringList().contains(labelInclusionUpper))
            throw new RuntimeException(
                    "Unsupported labelInFeatureInfo VendorOption value "
                            + labelInclusion
                            + ". It should be one of add, replace, none");

        this.attributeName = attributeName;
        this.colorMap = colorMap;
        this.labelInclusion = labelInclusionUpper;
        this.channel = channel;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Integer getChannel() {
        return channel;
    }

    /**
     * Give a pixel value match it to a label of a ColorMapEntry in the ColorMap
     *
     * @param pixel the pixel value
     * @return the label from the matched ColorMapEntry
     */
    public String getLabelForPixel(double pixel) {
        int type = colorMap.getType();
        ColorMapEntry[] entries = colorMap.getColorMapEntries();
        String label = null;
        switch (type) {
            case ColorMap.TYPE_RAMP:
                label = getLabelForPixelRamp(pixel, entries);
                break;
            case ColorMap.TYPE_INTERVALS:
                label = getLabelForPixelIntervals(pixel, entries);
                break;
            case ColorMap.TYPE_VALUES:
                label = getLabelForPixelValue(pixel, entries);
                break;
        }
        return label;
    }

    private String getLabelForPixelRamp(double pixel, ColorMapEntry[] entries) {
        String label = null;
        for (int i = 0; i < entries.length; i++) {
            ColorMapEntry current = entries[i];
            double currentVal = current.getQuantity().evaluate(null, Double.class);
            if (i == 0 && pixel <= currentVal) {
                label = current.getLabel();
                break;
            } else if (pixel <= currentVal) {
                // matching with the nearest value
                ColorMapEntry prev = entries[i - 1];
                double prevValue = prev.getQuantity().evaluate(null, Double.class);
                double diffWithCurr = Math.abs(pixel - currentVal);
                double diffWithPrev = Math.abs(pixel - prevValue);
                if (diffWithCurr < diffWithPrev) label = current.getLabel();
                else label = prev.getLabel();
                break;
            } else if (i == entries.length - 1 && pixel > currentVal) {
                label = current.getLabel();
            }
        }
        return label;
    }

    private String getLabelForPixelValue(double pixel, ColorMapEntry[] entries) {
        String label = null;
        for (ColorMapEntry entry : entries) {
            double quantity = entry.getQuantity().evaluate(null, Double.class);
            if (pixel == quantity) {
                label = entry.getLabel();
                break;
            }
        }
        return label;
    }

    private String getLabelForPixelIntervals(double pixel, ColorMapEntry[] entries) {
        String label = null;
        for (int i = 1; i < entries.length; i++) {
            ColorMapEntry current = entries[i];
            ColorMapEntry prev = entries[i - 1];
            double currentQuantity = current.getQuantity().evaluate(null, Double.class);
            double prevQuantity = entries[i - 1].getQuantity().evaluate(null, Double.class);
            if (i == 1 && pixel < prevQuantity) {
                label = prev.getLabel();
                break;
            } else if (pixel >= prevQuantity && pixel < currentQuantity) {
                label = current.getLabel();
                break;
            }
        }
        return label;
    }

    public String getLabelInclusion() {
        return labelInclusion;
    }

    public static int getLabelAttributeNameCount(List<PyColorMapLabelMatcher> colorMapLabelMatchers) {
        return Long.valueOf(
                colorMapLabelMatchers
                        .stream()
                        .filter(l -> l.getAttributeName() == "Label")
                        .count())
                .intValue();
    }

    public static boolean isLabelReplacingValue(List<PyColorMapLabelMatcher> colorMapLabelMatchers) {
        return !colorMapLabelMatchers.isEmpty()
                && colorMapLabelMatchers
                .stream()
                .allMatch(
                        l ->
                                l.getLabelInclusion()
                                        .equals(PyColorMapLabelMatcher.LabelInFeatureInfoMode.REPLACE.name()));
    }

    public enum LabelInFeatureInfoMode {
        ADD,
        REPLACE,
        NONE;

        static List<String> getAsStringList() {
            return Stream.of(PyColorMapLabelMatcher.LabelInFeatureInfoMode.values())
                    .map(v -> v.name())
                    .collect(Collectors.toList());
        }
    }
}
