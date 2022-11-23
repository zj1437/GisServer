package org.pygisserver.render.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.PyRenderRequestParam;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.enumm.DataTypeEnum;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.common.ui.entity.LayerGroup;
import org.pygisserver.common.util.GeometryUtil;
import org.pygisserver.common.util.ImageUtil;
import org.pygisserver.render.entity.PyInterpolationEnum;
import org.pygisserver.render.entity.PyMapContent;
import org.pygisserver.render.entity.PyRenderedParam;
import org.pygisserver.render.tiled.TiledInfoStrategy;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.media.jai.JAI;
import javax.media.jai.TiledImage;
import java.awt.*;
import java.awt.image.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * TODO: 上层增加一个渲染入口，哪些图层使用StreamingRender渲染  哪些图层使用DirectRasterRender
 * @Description: 
 * @author: 
 * @date: 2021-03-16
 */
@Component
@Slf4j
public class PyStreamingRender {

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    
    private static final int MAX_FILTER_RULES = 20;
    
    @Autowired
    private LayerBaseInfoUtil layerBaseInfoUtil;
    
    @Autowired
	private ObjectFactory<TiledInfoStrategy> factory;
    
    public RenderedImage renderedImage(PyRenderedParam pyRenderedParam,PyRenderRequestParam requestParam) {
    	
    		//构造基础图像
        	BufferedImage bufferedImage = createBufferedImage(pyRenderedParam.getRenderWidth(),
        													  pyRenderedParam.getRenderHeight(),
        													  null,    //需要构造调色板参数
        													  pyRenderedParam.isTransparent());
        	final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
        	//创建基础画布
        	Graphics2D graphics2D = prepareTransparency(pyRenderedParam.isTransparent(),
    									    			pyRenderedParam.getBgcolor(),
    									    			bufferedImage,
    									    			hintsMap);
        	createRenderingHints(bufferedImage,pyRenderedParam,hintsMap);
        	graphics2D.setRenderingHints(hintsMap);
            RenderingHints hints = new RenderingHints(hintsMap);
            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setThreadPool(threadPool);
            renderer.setJava2DHints(hints);
        	//设置renderhints
            renderer.setRendererHints(createRenderHints(pyRenderedParam));
            //增加渲染异常监听
            PyRenderException renderException = new PyRenderException(renderer);
            renderer.addRenderListener(renderException);
            PyRenderingTimeout timeout = new PyRenderingTimeout(0, renderer, graphics2D);  //0 表示最大渲染时间，目前不配置
            PyMapContent pyMapContent = (PyMapContent) pyRenderedParam.getMapcontent();
            timeout.start();
            try {
            	//如果是图层组，且图层组中有数据是瓦片或者在线数据，则需要处理
            	if (requestParam.isLayerGroup() && requestParam.isTiledLayer()) {
            		//获取到集合后，挨个渲染
            		List<Layer> layers = pyRenderedParam.getMapcontent().layers();
            		String layerName = requestParam.getLayerName();
            		int index = 0;
            		for (Layer layer : layers) {
            			String name = layer.getTitle();
            			LayerBaseInfo layerBaseInfo = layerBaseInfoUtil.queryServerBaseInfo(name);
            			if (layerBaseInfo.getDataType() == DataTypeEnum.TILED.getFileCode() 
            					|| layerBaseInfo.getDataType() == DataTypeEnum.ONLINE.getFileCode()) {
                			BufferedImage img = null;
                			//查看时否为图层组信息
                			List<LayerGroup> layerGroup = layerBaseInfoUtil.getLayerGroups(layerName);
                			int maxLevel = -1;
                			int minLevel = -1;
                			if (layerGroup != null) {
                				for (LayerGroup layerGroup2 : layerGroup) {
    								if (layerGroup2.getLayerName().equals(name)) {
    									maxLevel = layerGroup2.getMaxlevel();
    									minLevel = layerGroup2.getMinLevel();
    									break;
    								}
    							}
                			}
							String requestSrs = requestParam.getRequestSrs();
							if (maxLevel != -1 && minLevel != -1) {
								boolean isWms = StringUtils.isNotBlank(requestParam.getService()) && StringUtils.equals("WMS", requestParam.getService()) ? true : false;
								double maxScale;
								double minScale;
								if (isWms) {
									maxScale = GeometryUtil.calculateScale(minLevel, requestSrs,true);
									minScale = GeometryUtil.calculateScale(maxLevel, requestSrs,false);
								} else {
									maxScale = GeometryUtil.calculateScale(minLevel, requestSrs, true, null);
									minScale = GeometryUtil.calculateScale(maxLevel, requestSrs, false, null);
								}
                				double resolution = (pyRenderedParam.getEnvelope().getMaxX() - pyRenderedParam.getEnvelope().getMinX()) / pyMapContent.getMapWidth();
                				double scale = GeometryUtil.calculateScale(resolution);
                				if (scale >= maxScale || scale < minScale) {
                        			img = ImageUtil.createTransparentImg(pyRenderedParam.getRenderWidth(), pyRenderedParam.getRenderHeight());
                        		}else {
                        			img = getTileImg(pyRenderedParam, name,requestParam.getRequestSrs());
                        		}
                			}else {
                    			img = getTileImg(pyRenderedParam, name,requestParam.getRequestSrs());
                    		}
            				graphics2D.drawImage(img, 0, 0, null);
                		}else {
                			MapContent result = new MapContent();
                        	result.setViewport(pyMapContent.getViewport());
                        	result.layers().add(pyMapContent.layers().get(index));
                			renderer.setMapContent(result);
                        	renderer.paint(graphics2D,
                        			       pyRenderedParam.getRectangle(),
                        			       pyRenderedParam.getEnvelope(),
                        			       pyRenderedParam.getAffineTransform());
                		}
                		index++;
    				}
            	}else {
            		renderer.setMapContent(pyRenderedParam.getMapcontent());
            		renderer.paint(graphics2D, pyRenderedParam.getRectangle(), pyRenderedParam.getEnvelope(), pyRenderedParam.getAffineTransform());
            	}
	            timeout.stop();
	            //检查渲染是否超时
	            if (timeout.isTimedOut()) {
	                log.error("This request used more time than allowed {} and has been forcefully stopped ",0); //0 表示最大渲染时间
	            }
	            if (renderException.exceptionOccurred()) {
//	                Exception renderError = renderException.getRenderException();
	                log.error("Rendering process failed {}","internalError");
	                return null;
	            }
			} finally {
				timeout.stop();
				graphics2D.dispose();
			}
            return bufferedImage;
    }
    
    public BufferedImage getTileImg(PyRenderedParam pyRenderedParam,String layerName,String srs) {
		final BufferedImage img;
		LayerBaseInfo baseInfo = layerBaseInfoUtil.queryServerBaseInfo(layerName);
		TileRequestInfo requestInfo = new TileRequestInfo();
		requestInfo.setBbox(pyRenderedParam.getEnvelope());
		requestInfo.setImageWidth(pyRenderedParam.getRenderWidth());
		requestInfo.setImageHeight(pyRenderedParam.getRenderHeight());
		requestInfo.setMineType(pyRenderedParam.getFormat());
		requestInfo.setGridSet(srs);
		requestInfo.setTransparent(pyRenderedParam.isTransparent());
		TiledInfoStrategy tiledStrategy = factory.getObject();
		tiledStrategy.setTiledInfo(requestInfo, baseInfo);
		img = tiledStrategy.getWmsImg();
		return img;
	}

    
    
    /**
     * 设置渲染参数
     * @param pyRenderedParam
     * @return
     */
    private Map<Object, Object> createRenderHints(PyRenderedParam pyRenderedParam){
    	Map<Object, Object> rendererParams = new HashMap<Object, Object>();
        rendererParams.put("optimizedDataLoadingEnabled", Boolean.TRUE);
        rendererParams.put("renderingBuffer", /*Integer.valueOf(mapContent.getBuffer())*/ 0); //TODO 适配实际值
        rendererParams.put("maxFiltersToSendToDatastore", MAX_FILTER_RULES);
        rendererParams.put(StreamingRenderer.SCALE_COMPUTATION_METHOD_KEY,StreamingRenderer.SCALE_OGC);
        if (PyRenderedUtil.AA_NONE.equals(pyRenderedParam.getAntialias())) {
            rendererParams.put(StreamingRenderer.TEXT_RENDERING_KEY, StreamingRenderer.TEXT_RENDERING_STRING);
        } else {
            rendererParams.put(StreamingRenderer.TEXT_RENDERING_KEY, StreamingRenderer.TEXT_RENDERING_OUTLINE);
        }
        //设置是否启动投影处理
        rendererParams.put(StreamingRenderer.ADVANCED_PROJECTION_HANDLING_KEY, true);
    	rendererParams.put(StreamingRenderer.CONTINUOUS_MAP_WRAPPING, true);
    	return rendererParams;
    }
    
    
	/**
	 * 创建基础图像
	 * @param width 图形宽度
	 * @param height 图像盖度
	 * @param palette 
	 * @param transparent
	 * @return
	 */
	private BufferedImage createBufferedImage(int width, 
											  int height, 
											  IndexColorModel palette, 
											  boolean transparent) {
        if (palette != null) {
            final WritableRaster raster = Raster.createInterleavedRaster(palette.getTransferType(), width, height, 1, null);
            return new BufferedImage(palette, raster, false, null);
        }
        if (transparent) {
            BufferedImage image =  new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            image = image.createGraphics().getDeviceConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            return image;
        }
        if (height == 0) {
            height = 1;
        }
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
	}
	
	
	/**
	 * 创建基础画布
	 * @param transparent
	 * @param bgColor
	 * @param preparedImage
	 * @param extraHints
	 * @return
	 */
	private Graphics2D prepareTransparency(boolean transparent,
            							   Color bgColor,
            							   RenderedImage preparedImage,
            							   Map<RenderingHints.Key, Object> extraHints) {
        final Graphics2D graphic;
        if (preparedImage instanceof BufferedImage) {
            graphic = ((BufferedImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof TiledImage) {
            graphic = ((TiledImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof VolatileImage) {
            graphic = ((VolatileImage) preparedImage).createGraphics();
        } else {
            throw new RuntimeException("Unrecognized back-end image type");
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
            log.info("setting to transparent");
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
	
	/**
	 * 创建渲染hints
	 * @return
	 */
	private void createRenderingHints(RenderedImage preparedImage,
									  PyRenderedParam renderedParam,
									  Map<RenderingHints.Key, Object> hintsMap){
		String antialias = renderedParam.getAntialias();
		if (PyRenderedUtil.AA_NONE.equals(antialias)) {
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (preparedImage.getColorModel() instanceof IndexColorModel) {
                hintsMap.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            }
        } else if (PyRenderedUtil.AA_TEXT.equals(antialias)) {
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            hintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            if (antialias != null && !PyRenderedUtil.AA_FULL.equals(antialias)) {
                log.warn("Unrecognized antialias setting '" + antialias + "', valid values are " + PyRenderedUtil.AA_SETTINGS);
            }
            hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
		hintsMap.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hintsMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        String interpolation = renderedParam.getInterpolation();
        if (PyInterpolationEnum.NEAREST_INTERPOLATION.equals(interpolation)) {
            hintsMap.put(JAI.KEY_INTERPOLATION, PyInterpolationEnum.Nearest.getInterpolation());
            hintsMap.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else if (PyInterpolationEnum.BILINEAR_INTERPOLATION.equals(interpolation)) {
            hintsMap.put(JAI.KEY_INTERPOLATION, PyInterpolationEnum.Bilinear.getInterpolation());
            hintsMap.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        } else if (PyInterpolationEnum.BICUBIC_INTERPOLATION.equals(interpolation)) {
            hintsMap.put(JAI.KEY_INTERPOLATION, PyInterpolationEnum.Bicubic.getInterpolation());
            hintsMap.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }
	}
	
}
