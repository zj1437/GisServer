package org.pygisserver.render.style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.geotools.styling.Style;
import org.opengis.feature.type.FeatureType;

/**
 * GetLegendGraphicEntity的总体结构由 GetLegendGraphicRequest 优化而来，相关解释可以参照该类
 * @author admin
 */
public class LegendGraphicEntity {
	/** Legend option to enable feature count matching */
    public static final String COUNT_MATCHED_KEY = "countMatched";

    /** Legend option to enable feature count matching */
    public static final String HIDE_EMPTY_RULES = "hideEmptyRules";

    public static class LegendDetailInfo {
        private String layer;
//        private Name layerName;
        private FeatureType featureType;
        private String styleName;
        private String title;

        /** Optional rule used to refine presentation of style */
//        private String rule;

        /** Style determined from a review of request parameters */
        private Style style;

//        /** Optional legend info (from layer info or style info) */
//        private LegendInfo legendInfo;
//
//        /** Optional layer info (if available) */
//        private LayerInfo layerInfo;
//
//        /** Optional layer group info (if available ) */
//        private LayerGroupInfo layerGroupInfo;

        /** LegendRequest for a style, no associated featureType. */
        public LegendDetailInfo() {
            this.layer = "";
            this.featureType = null;
//            this.layerName = new NameImpl("");
        }

        /**
         * LegendRequest for a feature type, additional details (title and legend graphic) provided
         * by MapLayerInfo.
         */
        public LegendDetailInfo(FeatureType featureType) {
            if (featureType == null) {
                throw new NullPointerException("FeatureType required for LegendRequest");
            }
            this.featureType = featureType;
//            this.layerName = featureType.getName();
        }
        
//        public LegendDetailInfo(FeatureType featureType /**, Name layerName */) {
//            if (featureType == null) {
//                throw new NullPointerException("FeatureType required for LegendRequest");
//            }
//            this.featureType = featureType;
////            this.layerName = layerName;
//        }

        public String getLayer() {
            return layer;
        }

        public void setLayer(String layerName) {
            this.layer = layerName;
        }

//        public Name getLayerName() {
//            return layerName;
//        }

        public FeatureType getFeatureType() {
            return featureType;
        }

        public void setFeatureType(FeatureType featureType) {
            this.featureType = featureType;
        }

//        public LayerGroupInfo getLayerGroupInfo() {
//            return layerGroupInfo;
//        }
//
//        public void setLayerGroupInfo(LayerGroupInfo layerGroupInfo) {
//            this.layerGroupInfo = layerGroupInfo;
//        }
//
//        public LayerInfo getLayerInfo() {
//            return layerInfo;
//        }
//
//        public void setLayerInfo(LayerInfo layerInfo) {
//            this.layerInfo = layerInfo;
//        }
        
//        public String getRule() {
//            if ("".equals(rule)) {
//                return null;
//            }
//            return rule;
//        }
//
//        public void setRule(String rule) {
//            this.rule = rule;
//        }

        public String getStyleName() {
            return styleName;
        }

        public void setStyleName(String styleName) {
            this.styleName = styleName;
        }
        
//        public String getTitle() {
//            if (title == null || "".equals(title)) {
//                title = getLayerName().getLocalPart();
//            }
//            return title;
//        }
        /**
         * Used to provide a legend title (from MapLayerInfo).
         *
         * <p>If the title is empty or null the layer name will be used.
         */
        public void setTitle(String title) {
            this.title = title;
        }
        
        public Style getStyle() {
            return style;
        }

        public void setStyle(Style style) {
            this.style = style;
            if (style != null) this.styleName = style.getName();
        }

//        public LegendInfo getLegendInfo() {
//            return legendInfo;
//        }
//
//        public void setLegendInfo(LegendInfo legendInfo) {
//            this.legendInfo = legendInfo;
//        }

        @Override
        public String toString() {
            return "LegendRequest [layer=" + layer + " styleName=" + styleName + ", title=" + title + "]";
        }
    }

    public static final String SLD_VERSION = "1.0.0";

    /** default legend graphic width, in pixels, to apply if no WIDTH parameter was passed */
    public static final int DEFAULT_WIDTH = 20;

    /** default legend graphic height, in pixels, to apply if no WIDTH parameter was passed */
    public static final int DEFAULT_HEIGHT = 20;

    public static final String DEFAULT_FORMAT = "image/png";

    /** The featuretype(s) of the requested LAYER(s) */
    private List<LegendDetailInfo> legends = new ArrayList<LegendDetailInfo>();

    private String featureType;

    private double scale = -1d;

    private String format;

    private int width = DEFAULT_WIDTH;

    private int height = DEFAULT_HEIGHT;

    /** mime type of the format in which to return exceptions information. */
    private String exceptionsFormat = "SE_XML";

    private Map<String, Object> legendOptions;

    /** Whether the legend graphic background shall be transparent or not. */
    private boolean transparent;

    private boolean strict = true;

    /** Optional locale to be used for text in output. */
    private Locale locale;

    /** Contains the parsed kvp items */
    private Map<String, Object> kvp;

    public String getExceptions() {
        return exceptionsFormat;
    }

    public void setExceptions(String exceptionsFormat) {
        this.exceptionsFormat = exceptionsFormat;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<LegendDetailInfo> getLegends() {
        return legends;
    }

    public List<FeatureType> getLayers() {
        List<FeatureType> types = new ArrayList<>(legends.size());
        for (LegendDetailInfo layer : legends) {
            FeatureType ft = layer.getFeatureType();
            if (ft != null) types.add(ft);
        }
        return types;
    }

//    public LegendDetailInfo getLegend(Name featureTypeName) {
//        for (LegendDetailInfo legend : legends) {
//            if (featureTypeName.equals(legend.getLayerName())) {
//                return legend;
//            }
//        }
//        return null; // not found!
//    }

    public void setLayer(FeatureType layer) {
        this.legends.clear();
        if (layer == null) {
            this.legends.add(new LegendDetailInfo());
        } else {
            this.legends.add(new LegendDetailInfo(layer));
        }
    }
    
    public void addLegend(LegendDetailInfo legend) {
        if (legends.isEmpty()) {
            legends.add(legend);
        } else if (!legends.contains(legend)) {
        	legends.add(legend);
        }
    }

//    public void setRule(String rule) {
//        // Will set rule for first LegendRequest
//        if (!legends.isEmpty()) {
//            legends.get(0).setRule(rule);
//        }
//    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setStyle(Style style) {
        // this will set only the first LegendRequest
        if (legends.isEmpty()) {
            return;
        }
        legends.get(0).setStyle(style);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLegendOptions() {
        return (Map<String, Object>)(legendOptions == null ? Collections.emptyMap() : legendOptions);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setLegendOptions(Map legendOptions) {
        this.legendOptions = legendOptions;
    }

    public void setTransparent(boolean transparentBackground) {
        this.transparent = transparentBackground;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /** SLD replacement */
//    private Map<String, Object> env = new HashMap<String, Object>();

//    public Map<String, Object> getEnv() {
//        return env;
//    }
//
//    /** Sets the SLD environment substitution */
//    @SuppressWarnings({"rawtypes", "unchecked"})
//    public void setEnv(Map enviroment) {
//        this.env = enviroment;
//    }

    /** Sets the optional Locale to be used for text in legend output. */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /** Gets the locale to be used for text in output (null to use default locale). */
    public Locale getLocale() {
        return this.locale;
    }

//    public <T> T getLegendOption(String key, Class<T> optionClass) {
//        if (legendOptions == null) {
//            return null;
//        }
//
//        Object value = legendOptions.get(key);
//        if (value == null) {
//            return null;
//        }
//
//        T converted = Converters.convert(value, optionClass);
//        if (converted == null) {
//            throw new RuntimeException("Invalid syntax for option " + key + ", cannot be convered to a " + optionClass.getSimpleName());
//        }
//        return converted;
//    }

    /** The parsed KVP map */
    public Map<String, Object> getKvp() {
        return kvp;
    }

    /** Sets the parsed KVP map */
    public void setKvp(Map<String, Object> kvp) {
        this.kvp = kvp;
    }
}
