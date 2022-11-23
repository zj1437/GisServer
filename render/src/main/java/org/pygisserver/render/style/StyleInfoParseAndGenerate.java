package org.pygisserver.render.style;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;
import org.pygisserver.common.util.FileUtil;
import org.pygisserver.render.style.LegendGraphicEntity.LegendDetailInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.Parameter;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.process.Processors;
import org.geotools.process.function.ProcessFunction;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.renderer.lite.StyledShapePainter;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.renderer.style.Style2D;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.NumberRange;
import org.geotools.util.PreventLocalEntityResolver;
import org.geotools.xml.styling.SLDParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.style.GraphicLegend;
import org.springframework.stereotype.Component;
import org.xml.sax.EntityResolver;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StyleInfoParseAndGenerate {

	private final double MINIMUM_SYMBOL_SIZE = 3.0;
	
	
	public byte[] getSldImage(String pngPath,String styleName,String sldFilePath,String kvpInfo){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileOutputStream fos = null;
		LegendGraphicEntity lgEntity = null;
		BufferedImage image = null;
		File file = new File(pngPath);
		try {
			if(file.exists()) {
				image = ImageIO.read(file);
			} else {
				fos = new FileOutputStream(pngPath);
				lgEntity = getLGEntity(styleName,sldFilePath,kvpInfo);
				file.createNewFile();
				image = buildLegendGraphic(lgEntity);
				ImageIO.write(image, "png", fos);
			}
			ImageIO.write(image, "png", baos);
			return baos.toByteArray();
		} catch (IOException e) {
			log.error("StyleInfoParseAndGenerate getSldImage has error:" + ExceptionsUtil.stackTraceToStr(e));
			throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_THUMBAL, styleName));
		}
	}
	
	/**
	 * 解析SLD文件方法
	 * @return
	 * @throws IOException
	 */
	public Style getStyle(String sldFilePath) {
		//直接使用文件方式进行解析，可以扩展为文件名称、文件路径、直接传递File对象等（这里需要根据Context获取style路径）
		File file = new File(FileUtil.decodeFilePath(sldFilePath));
        EntityResolver entityResolver = PreventLocalEntityResolver.INSTANCE;
        SLDParser p = StyleUtils.createSld10Parser(file, null, entityResolver);
        StyledLayerDescriptor sld = p.parseSLD();
        Style ss = StyleUtils.style(sld);
		return ss;
	}
	
//	boolean hasVectorTransformation = false;
	boolean hasRasterTransformation = false;
	
    public BufferedImage buildLegendGraphic(LegendGraphicEntity entity) {
        List<RenderedImage> layersImages = new ArrayList<RenderedImage>();
        List<LegendDetailInfo> legends = entity.getLegends();
        for (LegendDetailInfo legend : legends) {

            Style gt2Style = legend.getStyle();
            if (gt2Style == null) {
                throw new NullPointerException("Style can not be null");
            }
            gt2Style = StyleUtils.resizeStyleDPI(entity, gt2Style);
//            String ruleName = legend.getRule(); // was null

            boolean strict = entity.isStrict();
            final boolean transparent = entity.isTransparent();

            checkForRenderingTransformations(gt2Style);
            final boolean buildRasterLegend = (!strict && StyleUtils.checkRasterSymbolizer(gt2Style)) || hasRasterTransformation;

            if (buildRasterLegend) {
                final StyleRasterLegendHelper rasterLegendHelper = new StyleRasterLegendHelper(entity, gt2Style, null);
                final BufferedImage image = rasterLegendHelper.getLegend();
                if (image != null) {
                    layersImages.add(image);
                }
            } else {
                final Feature sampleFeature = StyleUtils.createSampleFeature();
                final FeatureTypeStyle[] ftStyles = gt2Style.featureTypeStyles().toArray(new FeatureTypeStyle[0]);
                final double scaleDenominator = entity.getScale();

                Rule[] applicableRules;
//                if (ruleName != null) {
//                    Rule rule = StyleUtils.getRule(ftStyles, ruleName);
//                    if (rule == null) {
//                        throw new ServiceException("Specified style does not contains a rule named " + ruleName);
//                    }
//                    applicableRules = new Rule[] {rule};
//                } else {
//                    applicableRules = StyleUtils.getApplicableRules(ftStyles, scaleDenominator);
//                }
                applicableRules = StyleUtils.getApplicableRules(ftStyles, scaleDenominator);

                final NumberRange<Double> scaleRange = NumberRange.create(scaleDenominator, scaleDenominator);
                final int ruleCount = applicableRules.length;
                final List<RenderedImage> legendsStack = new ArrayList<RenderedImage>(ruleCount);
                final SLDStyleFactory styleFactory = new SLDStyleFactory();

                double minimumSymbolSize = MINIMUM_SYMBOL_SIZE;
                // calculate the symbols rescaling factor necessary for them to be drawn inside the icon box
                int defaultSize = Math.min(entity.getWidth(), entity.getHeight());
                double[] minMax = StyleUtils.calcSymbolSize(defaultSize,minimumSymbolSize,sampleFeature,applicableRules);
                double actualMin = minMax[0];
                double actualMax = minMax[1];
                boolean rescalingRequired = actualMin < minimumSymbolSize || actualMax > defaultSize;
                java.util.function.Function<Double, Double> rescaler = null;
                if (actualMax == actualMin || ((actualMin / actualMax) * defaultSize) > minimumSymbolSize) {
                    rescaler = size -> (size / actualMax) * defaultSize;
                } else {
                    double finalMinimumSymbolSize = minimumSymbolSize;
                    rescaler = size -> (size - actualMin) / (actualMax - actualMin) * (defaultSize - finalMinimumSymbolSize) + finalMinimumSymbolSize;
                }

                renderRules(
                        entity,
                        layersImages,
                        transparent,
                        sampleFeature,
                        scaleDenominator,
                        applicableRules,
                        scaleRange,
                        ruleCount,
                        legendsStack,
                        styleFactory,
                        minimumSymbolSize,
                        rescalingRequired,
                        rescaler);
            }
        }
        // all legend graphics are merged if we have a layer group
        BufferedImage finalLegend = mergeGroups(layersImages, null, entity);
        return finalLegend;
    }
    
    private BufferedImage mergeGroups(
            List<RenderedImage> imageStack,
            Rule[] rules,
            LegendGraphicEntity req) {
    	ServerLegendMerger.MergeOptions options = ServerLegendMerger.MergeOptions.createFromRequest(
                        imageStack, 0, 0, 0, 0, req);
        options.setLayout(StyleUtils.getGroupLayout(req));
        return ServerLegendMerger.mergeGroups(rules, options);
    }
    
    
    private LiteShape2 samplePoint;
    private static final GeometryFactory geomFac = new GeometryFactory();
    private static final StyledShapePainter shapePainter = new StyledShapePainter();
    
	private void renderRules(
			LegendGraphicEntity entity,
            List<RenderedImage> layersImages,
            final boolean transparent,
            final Feature sampleFeature,
            final double scaleDenominator,
            Rule[] applicableRules,
            final NumberRange<Double> scaleRange,
            final int ruleCount,
            final List<RenderedImage> legendsStack,
            final SLDStyleFactory styleFactory,
            double minimumSymbolSize,
            boolean rescalingRequired,
            java.util.function.Function<Double, Double> rescaler) {
        MetaBufferEstimator estimator = new MetaBufferEstimator(sampleFeature);
        int w = entity.getWidth();
        int h = entity.getHeight();
        for (int i = 0; i < ruleCount; i++) {

            final RenderedImage image = StyleUtils.createImage(w, h, (IndexColorModel) null, transparent);
            final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
            final Graphics2D graphics = StyleUtils.prepareTransparency(transparent, StyleUtils.getBackgroundColor(entity), image, hintsMap);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Feature sample = StyleUtils.getSampleFeatureForRule(sampleFeature, applicableRules[i]);

            final List<Symbolizer> symbolizers = applicableRules[i].symbolizers();
            final GraphicLegend graphic = applicableRules[i].getLegend();

            // If this rule has a legend graphic defined in the SLD, use it
            if (graphic != null) {
                if (this.samplePoint == null) {
                    Coordinate coord = new Coordinate(w / 2, h / 2);
                    try {
                        this.samplePoint = new LiteShape2(geomFac.createPoint(coord), null, null, false);
                    } catch (Exception e) {
                        this.samplePoint = null;
                    }
                }
                shapePainter.paint(graphics, this.samplePoint, graphic, scaleDenominator, false);
            } else {
                for (Symbolizer symbolizer : symbolizers) {
                    // skip raster symbolizers
                    if (!(symbolizer instanceof RasterSymbolizer)) {
                        // rescale symbols if needed
                        LiteShape2 shape = getSampleShape(symbolizer, w, h, w, h);
                        if (rescalingRequired && (symbolizer instanceof PointSymbolizer || symbolizer instanceof LineSymbolizer)) {
                            double size = StyleUtils.getSymbolizerSize(estimator, symbolizer, Math.min(w, h) - 4);
                            double newSize = rescaler.apply(size);
                            symbolizer = StyleUtils.rescaleSymbolizer(symbolizer, size, newSize);
                        } else if (symbolizer instanceof PolygonSymbolizer) {
                            // need to make room for the stroke in the symbol, thus, a smaller rect
                            double symbolizerSize = StyleUtils.getSymbolizerSize(estimator, symbolizer, 0);
                            int rescaledWidth = (int)Math.ceil(Math.max(minimumSymbolSize, w - symbolizerSize));
                            int rescaledHeight = (int)Math.ceil(Math.max(minimumSymbolSize, h - symbolizerSize));
                            shape = getSampleShape(symbolizer, rescaledWidth, rescaledHeight, w, h);
                            symbolizer = StyleUtils.rescaleSymbolizer(symbolizer, w, (double) rescaledWidth);
                        }

                        Style2D style2d = styleFactory.createStyle(sample, symbolizer, scaleRange);
                        if (style2d != null) {
                            shapePainter.paint(graphics, shape, style2d, scaleDenominator);
                        }
                    }
                }
            }
            legendsStack.add(image);
            graphics.dispose();
        }
        int labelMargin = 3;
        ServerLegendMerger.MergeOptions options = ServerLegendMerger.MergeOptions.createFromRequest(legendsStack,0,0,0,labelMargin,entity);
        if (ruleCount > 0) {
            BufferedImage image = ServerLegendMerger.mergeLegends(applicableRules, entity, options);
            if (image != null) {
                layersImages.add(image);
            }
        }
    }

	public LegendGraphicEntity getLGEntity(String styleName,String sldFilePath,String kvpInfo) {
		LegendGraphicEntity lge = new LegendGraphicEntity();
		LegendDetailInfo ldi = new LegendDetailInfo();
		ldi.setStyle(getStyle(sldFilePath));
		ldi.setStyleName(styleName);
		lge.addLegend(ldi);
		
		StyleUtils.parseKVPInfo(kvpInfo,lge);
		lge.setScale(-1.0);
		return lge;
	}
	
    protected void checkForRenderingTransformations(Style gt2Style) {
        // Check for rendering transformation
        List<FeatureTypeStyle> ftsList = gt2Style.featureTypeStyles();
        for (int i = 0; i < ftsList.size(); i++) {
            FeatureTypeStyle fts = ftsList.get(i);
            Expression exp = fts.getTransformation();
            if (exp != null) {
                ProcessFunction processFunction = (ProcessFunction) exp;
                Name processName = processFunction.getProcessName();
                Map<String, Parameter<?>> outputs = Processors.getResultInfo(processName, null);
                if (outputs.isEmpty()) {
                    continue;
                }
                Parameter<?> output = outputs.values().iterator().next(); // we assume there is only one output
                /** if (SimpleFeatureCollection.class.isAssignableFrom(output.getType())) {
                    hasVectorTransformation = true;
                    break;
                } else */if (GridCoverage2D.class.isAssignableFrom(output.getType())) {
                    hasRasterTransformation = true;
                    break;
                }
            }
        }
    }
    
    public static final float hpaddingFactor = 0.15f;
    /** top & bottom padding percentage factor for the legend */
    public static final float vpaddingFactor = 0.15f;
    private LiteShape2 sampleLine;
    public LiteShape2 getSampleShape(Symbolizer symbolizer,int legendWidth,int legendHeight,int areaWidth,int areaHeight) {
        LiteShape2 sampleShape;
        final float hpad = (areaWidth * hpaddingFactor) + (areaWidth - legendWidth) / 2f;
        final float vpad = (areaHeight * vpaddingFactor) + (areaHeight - legendHeight) / 2f;

        if (symbolizer instanceof LineSymbolizer) {
            if (this.sampleLine == null) {
                Coordinate[] coords = {
                    new Coordinate(hpad, legendHeight - vpad - 1),
                    new Coordinate(legendWidth - hpad - 1, vpad)
                };
                LineString geom = geomFac.createLineString(coords);
                try {
                    this.sampleLine = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleLine = null;
                }
            }
            sampleShape = this.sampleLine;
        } else if ((symbolizer instanceof PolygonSymbolizer) || (symbolizer instanceof RasterSymbolizer)) {
            final float w = areaWidth - (2 * hpad) - 1;
            final float h = areaHeight - (2 * vpad) - 1;

            Coordinate[] coords = {
                new Coordinate(hpad, vpad),
                new Coordinate(hpad, vpad + h),
                new Coordinate(hpad + w, vpad + h),
                new Coordinate(hpad + w, vpad),
                new Coordinate(hpad, vpad)
            };
            LinearRing shell = geomFac.createLinearRing(coords);
            Polygon geom = geomFac.createPolygon(shell, null);

            try {
                return new LiteShape2(geom, null, null, false);
            } catch (Exception e) {
                return null;
            }
        } else if (symbolizer instanceof PointSymbolizer || symbolizer instanceof TextSymbolizer) {
            if (this.samplePoint == null) {
                Coordinate coord = new Coordinate(legendWidth / 2d, legendHeight / 2d);

                try {
                    this.samplePoint = new LiteShape2(geomFac.createPoint(coord), null, null, false);
                } catch (Exception e) {
                    this.samplePoint = null;
                }
            }
            sampleShape = this.samplePoint;
        } else {
            throw new IllegalArgumentException("Unknown symbolizer: " + symbolizer);
        }
        return sampleShape;
    }
}
