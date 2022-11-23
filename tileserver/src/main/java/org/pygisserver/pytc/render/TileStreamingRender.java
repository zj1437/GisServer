package org.pygisserver.pytc.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.TiledImage;
import javax.xml.parsers.ParserConfigurationException;

import org.pygisserver.common.exception.BaseException;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.sld.SLDConfiguration;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.jaitools.imageutils.ImageUtils;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 瓦片渲染类 
 * @author: 
 * @date: 2020-12-05
 */
@Slf4j
public class TileStreamingRender {

	private static final Interpolation NN_INTERPOLATION = new InterpolationNearest();
	
	private static final Interpolation BIL_INTERPOLATION = new InterpolationBilinear();

    private static final Interpolation BIC_INTERPOLATION = new InterpolationBicubic2(0);
	
	private MapContent map;
	
	private Rectangle paintArea;
	
	private ReferencedEnvelope mapArea;
	
	private int width;
	
	private int height;
	
	
	public TileStreamingRender(MapContent map,
							   Rectangle paintArea,
							   ReferencedEnvelope mapArea) {
		
		this.map = map;
		this.paintArea = paintArea;
		this.mapArea = mapArea;
		
	}

    public BufferedImage paintRender() {
    	StreamingRenderer renderer = new StreamingRenderer();
    	renderer.setMapContent(map);
    	Map<RenderingHints.Key, Object> hintsMap = getHintsMap();
        RenderingHints hints = new RenderingHints(hintsMap);
        renderer.setJava2DHints(hints);
    	BufferedImage image = createBufferedImage(width, height, true);
    	Graphics2D graphic2D = prepareTransparency(true,null,image,null);
    	graphic2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        		RenderingHints.VALUE_ANTIALIAS_ON);
        graphic2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        renderer.paint(graphic2D, paintArea, mapArea);
        return image;
    }
    
    private static Style getSldStyle() {
		Configuration config = new SLDConfiguration();
	    Parser parser = new Parser(config);
	    InputStream sld;
		try {
			sld = new FileInputStream("");
			StyledLayerDescriptor styleSLD;
			styleSLD = (StyledLayerDescriptor) parser.parse(sld);
			Style style = SLD.defaultStyle(styleSLD);
			style.getDefaultSpecification();
			return style;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException | SAXException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		return null;
	}
    
	/**
	 * 构造渲染参数
	 * @return
	 */
	public Map<RenderingHints.Key, Object> getHintsMap(){
		final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
		hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		hintsMap.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hintsMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        hintsMap.put(JAI.KEY_INTERPOLATION, NN_INTERPOLATION);
        hintsMap.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        return hintsMap;
	}
	
	
	public BufferedImage createBufferedImage(int width,int height,boolean transparent) {
		if (transparent) {
            return new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        }
        if (height == 0) {
            height = 1;
        }
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
	}
	
	 public Graphics2D prepareTransparency(final boolean transparent,
								           final Color bgColor,
								           final RenderedImage preparedImage,
								           final Map<RenderingHints.Key, Object> extraHints) {
        final Graphics2D graphic;
        if (preparedImage instanceof BufferedImage) {
            graphic = ((BufferedImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof TiledImage) {
            graphic = ((TiledImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof VolatileImage) {
            graphic = ((VolatileImage) preparedImage).createGraphics();
        } else {
            throw new BaseException("无法识别的图片类型");
        }
        Map<RenderingHints.Key, Object> hintsMap;
        if (extraHints == null) {
            hintsMap = new HashMap<RenderingHints.Key, Object>();
        } else {
            hintsMap = new HashMap<RenderingHints.Key, Object>(extraHints);
        }
        hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphic.setRenderingHints(hintsMap);
        if (transparent) {
            int type = AlphaComposite.SRC;
            graphic.setComposite(AlphaComposite.getInstance(type));

            Color c = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
            graphic.setBackground(bgColor);
            graphic.setColor(c);
            graphic.fillRect(0, 0, preparedImage.getWidth(), preparedImage.getHeight());
            type = AlphaComposite.SRC_OVER;
            graphic.setComposite(AlphaComposite.getInstance(type));
        } else {
            graphic.setColor(bgColor);
            graphic.fillRect(0, 0, preparedImage.getWidth(), preparedImage.getHeight());
        }
        return graphic;
    }
}
