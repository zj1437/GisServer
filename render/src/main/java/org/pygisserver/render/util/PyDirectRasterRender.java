package org.pygisserver.render.util;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;

import org.pygisserver.render.entity.PyInterpolationEnum;
import org.pygisserver.render.entity.PyRenderedParam;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ColorUtilities;
import org.geotools.renderer.lite.gridcoverage2d.ChannelSelectionUpdateStyleVisitor;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.styling.RasterSymbolizer;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.stereotype.Component;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.vectorbin.ROIGeometry;
import it.geosolutions.rendered.viewer.RenderedImageBrowser;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PyDirectRasterRender {

	private static final int MAX_TILE_SIZE = 1024;

	public RenderedImage renderRasterImage(PyRenderedParam pyRenderedParam) throws TransformException, NoninvertibleTransformException, FactoryException, IOException {
			// 插值方法
			Interpolation interpolation = PyInterpolationEnum
					.getInterpolationByInterStr(pyRenderedParam.getInterpolation());
			RasterSymbolizer symbolizer = pyRenderedParam.getSymbolizer();
			final int[] bandIndices = pyRenderedParam.getTransformation() == null ? symbolizer == null ? null
					: ChannelSelectionUpdateStyleVisitor.getBandIndicesFromSelectionChannels(symbolizer) : null;
			// 是否支持透明，默认支持
		final boolean transparent = pyRenderedParam.isTransparent() && pyRenderedParam.getFormat().toLowerCase().endsWith("png");
			Color bgColor = pyRenderedParam.getBgcolor();
			if (transparent) {
				bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
			} else {
				bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 255);
			}
			final Color readerBgColor = transparent ? null : bgColor;
			int mapWidth = pyRenderedParam.getRenderWidth() == 0 ? 256 : pyRenderedParam.getRenderWidth();
			int mapHeight = pyRenderedParam.getRenderHeight() == 0 ? 256 : pyRenderedParam.getRenderHeight();;
			// 瓦片大小可以设置在配置参数中
			int tileSizeX = -1;
			int tileSizeY = -1;
			if (mapWidth < MAX_TILE_SIZE && mapHeight < MAX_TILE_SIZE) {
				tileSizeX = mapWidth;
				tileSizeY = mapHeight;
			}
			GeneralParameterValue[] readParameters = PyRenderedUtil.getReadParameters(pyRenderedParam.getGeneralParam(),
					null, null, interpolation, readerBgColor, bandIndices);
			final GridCoverageRenderer renderer = new GridCoverageRenderer(pyRenderedParam.getCrs(),
					pyRenderedParam.getEnvelope(), pyRenderedParam.getRectangle(), pyRenderedParam.getAffineTransform(),
					null);
			renderer.setAdvancedProjectionHandlingEnabled(true);
			renderer.setWrapEnabled(true);
			// 开始渲染
			RenderedImage renderImage = renderer.renderImage(pyRenderedParam.getReader(), readParameters, symbolizer,
					interpolation, null, tileSizeX, tileSizeY);
			// 处理渲染结果
		return dealRenderImage(renderImage, pyRenderedParam.getRectangle(), mapWidth, mapHeight, bgColor,
					transparent, tileSizeX, tileSizeY);
	}

	private RenderedImage dealRenderImage(RenderedImage image, Rectangle mapRasterArea, int mapWidth, int mapHeight,
			Color bgColor, boolean transparent, int tileSizeX, int tileSizeY) {
		if (image == null) {
			return null;
		}
		// 检查渲染出来的图像与请求的交集
		final Rectangle imageBounds = PlanarImage.wrapRenderedImage(image).getBounds();
		Rectangle intersection = imageBounds.intersection(mapRasterArea);
		if (intersection.isEmpty()) {
			return null;
		}

		//构造layout  用来做裁切或者扩展
		final ImageLayout layout = new ImageLayout();
		layout.setMinX(0);
		layout.setMinY(0);
		layout.setWidth(mapWidth);
		layout.setHeight(mapHeight);
		if (tileSizeX > 0 && tileSizeY > 0) {
			layout.setTileGridXOffset(0);
			layout.setTileGridYOffset(0);
			layout.setTileWidth(tileSizeX);
			layout.setTileHeight(tileSizeY);
		}

		//获取图像的颜色分量模型
		ColorModel cm = image.getColorModel();
		double[] bgValues = null;
		PlanarImage[] alphaChannels = null;
		final ImageWorker worker = new ImageWorker(image);
		final int transparencyType = cm.getTransparency();
		if (cm instanceof IndexColorModel) {
			IndexColorModel icm = (IndexColorModel) cm;
			final int bgColorIndex;
			if (transparent) {
				bgColorIndex = icm.getTransparentPixel();
			} else {
				if (icm.hasAlpha() && icm.isAlphaPremultiplied()) {
					bgColorIndex = -1;
				} else {
					if (icm.getTransparency() != Transparency.OPAQUE) {
						icm = ColorUtilities.applyBackgroundColor(icm, bgColor);
						cm = icm;
						ImageLayout ilColorModel = new ImageLayout(image);
						ilColorModel.setColorModel(icm);
						RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, ilColorModel);
						worker.setRenderingHints(hints);
						worker.format(image.getSampleModel().getDataType());
						image = worker.getRenderedImage();
					}
					bgColorIndex = ColorUtilities.findColorIndex(bgColor, icm);
				}
			}

			//找不到背景色的颜色分量索引的情况下，使用rgb
			if (bgColorIndex == -1) {
				bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),transparent ? 0 : 255 };
				worker.setBackground(bgValues);
				image = worker.forceComponentColorModel().getRenderedImage();
				if (transparent && !image.getColorModel().hasAlpha()) {
					image = addAlphaChannel(image);
					worker.setImage(image);
				}
				cm = image.getColorModel();
			} else {
				//如果找到，就设设置背景色
				bgValues = new double[] { bgColorIndex };
			}
			//使用alpha通道
			if (cm.hasAlpha() && bgColorIndex == -1) {
				worker.forceComponentColorModel();
				final RenderedImage alpha = worker.retainLastBand().getRenderedImage();
				alphaChannels = new PlanarImage[] { PlanarImage.wrapRenderedImage(alpha) };
			}
		}
		boolean noDataTransparencyApplied = false;
		if (cm instanceof ComponentColorModel) {
			ComponentColorModel ccm = (ComponentColorModel) cm;
			boolean hasAlpha = cm.hasAlpha();
			//灰度图像扩展到RGB
			if (ccm.getNumColorComponents() == 1) {
				if ((!isLevelOfGray(bgColor) && !transparent) || (ccm.getTransferType() == DataBuffer.TYPE_DOUBLE
						|| ccm.getTransferType() == DataBuffer.TYPE_FLOAT
						|| ccm.getTransferType() == DataBuffer.TYPE_UNDEFINED)) {
					final ImageWorker iw = new ImageWorker(image);
					if (hasAlpha) {
						final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
						final RenderedImage gray = new ImageWorker(image).retainFirstBand().getRenderedImage();
						image = new ImageWorker(gray).bandMerge(3).addBand(alpha, false).forceComponentColorModel()
								.forceColorSpaceRGB().getRenderedImage();
					} else {
						image = iw.bandMerge(3).forceComponentColorModel().forceColorSpaceRGB().getRenderedImage();
					}
				} else if (!hasAlpha) {
					if (transparent) {
						RenderedImage transparentImage = grayNoDataTransparent(image);
						if (transparentImage == null) {
							image = addAlphaChannel(image);
							bgValues = new double[] { mapToGrayColor(bgColor, ccm), 0 };
						} else {
							image = transparentImage;
							noDataTransparencyApplied = true;
						}
					} else {
						bgValues = new double[] { mapToGrayColor(bgColor, ccm) };
					}
				} else {
					final ImageWorker iw = new ImageWorker(image);
					final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
					alphaChannels = new PlanarImage[] { PlanarImage.wrapRenderedImage(alpha) };

					if (transparent) {
						bgValues = new double[] { mapToGrayColor(bgColor, ccm), 0 };
					} else {
						bgValues = new double[] { mapToGrayColor(bgColor, ccm), 255 };
					}
				}
				cm = image.getColorModel();
				ccm = (ComponentColorModel) cm;
				hasAlpha = cm.hasAlpha();
			}
			if (bgValues == null && !noDataTransparencyApplied) {
				if (hasAlpha) {
					final ImageWorker iw = new ImageWorker(image);
					final RenderedImage alpha = iw.retainLastBand().getRenderedImage();
					alphaChannels = new PlanarImage[] { PlanarImage.wrapRenderedImage(alpha) };

					if (transparent) {
						bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0 };
					} else {
						bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 255 };
					}
				} else {
					if (transparent) {
						RenderedImage imageTransparent = rgbNoDataTransparent(image);
						if (imageTransparent != null) {
							image = imageTransparent;
							noDataTransparencyApplied = true;
						} else {
							image = addAlphaChannel(image);
							bgValues = new double[] { 0, 0, 0, 0 };
						}
					} else {
						bgValues = new double[] { bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue() };
					}
				}
			}
		}
		ImageWorker iw = new ImageWorker(image);
		Object roiCandidate = image.getProperty("ROI");
		if (!(imageBounds.contains(mapRasterArea) || imageBounds.equals(mapRasterArea))
				|| transparencyType != Transparency.OPAQUE || iw.getNoData() != null || roiCandidate instanceof ROI) {
			image = applyBackgroundTransparency(mapRasterArea, image, intersection, layout, bgValues, alphaChannels,
					transparencyType, iw, roiCandidate, noDataTransparencyApplied);
		} else {
			//裁剪
			if (imageBounds.contains(mapRasterArea) && !imageBounds.equals(mapRasterArea)) {
				iw.setBackground(bgValues);
				iw.crop(0, 0, mapWidth, mapHeight);
				image = iw.getRenderedImage();
			}
		}
		if (image != null) {
			log.info("Direct rendering path produced the following image chain:\n"
					+ RenderedImageBrowser.dumpChain(image));
		}
		return image;
	}

	private RenderedImage applyBackgroundTransparency(final Rectangle mapRasterArea, RenderedImage image,
			Rectangle intersection, final ImageLayout layout, double[] bgValues, PlanarImage[] alphaChannels,
			final int transparencyType, ImageWorker iw, Object roiCandidate, boolean preProcessedWithTransparency) {
		ROI roi;
		if (roiCandidate instanceof ROI) {
			ROI imageROI = (ROI) roiCandidate;
			try {
				roi = new ROIGeometry(mapRasterArea).intersect(imageROI);
			} catch (IllegalArgumentException e) {
				log.error("Failed to intersect image ROI with target bounds, returning empty result");
				return null;
			}
		} else {
			roi = new ROIShape(!intersection.isEmpty() ? intersection : mapRasterArea);
		}
		ROI[] rois = (!preProcessedWithTransparency) ? new ROI[] { roi } : null;
		double[][] thresholds = (!preProcessedWithTransparency)
				? new double[][] { { ColorUtilities.getThreshold(image.getSampleModel().getDataType()) } }
				: null;
		iw.setRenderingHint(JAI.KEY_IMAGE_LAYOUT, layout);
		iw.setBackground(bgValues);
		iw.mosaic(new RenderedImage[] { image },
				alphaChannels != null && transparencyType == Transparency.TRANSLUCENT
						? MosaicDescriptor.MOSAIC_TYPE_BLEND
						: MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
				alphaChannels, rois, thresholds, null);
		image = iw.getRenderedImage();
		return image;
	}

	/**
	 * 将RGB数据的nodata数据设置为透明
	 * @param image
	 * @return
	 */
	private RenderedImage rgbNoDataTransparent(RenderedImage image) {
		return makeNoDataTransparent(image, 3);
	}

	/**
	 * 单通道灰度图像将nodata数据设置为透明
	 * @param image
	 * @return
	 */
	private RenderedImage grayNoDataTransparent(RenderedImage image) {
		return makeNoDataTransparent(image, 1);
	}

	private RenderedImage makeNoDataTransparent(RenderedImage image, final int numBands) {
		ImageWorker iw = new ImageWorker(image);
		Range noData = iw.getNoData();
		ColorModel cm = image.getColorModel();
		final int numColorBands = cm.getNumColorComponents();
		if (noData != null && image.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE && numColorBands == numBands
				&& cm instanceof ComponentColorModel) {
			int minValue = noData.getMin().intValue();
			int maxValue = noData.getMax().intValue();
			if (minValue == maxValue && minValue >= Byte.MIN_VALUE && minValue <= Byte.MAX_VALUE) {
				Color transparentColor = new Color(minValue, minValue, minValue);
				iw.makeColorTransparent(transparentColor);
				return iw.getRenderedImage();
			}
		}
		return null;
	}

	private RenderedImage addAlphaChannel(RenderedImage image) {
		final ImageLayout tempLayout = new ImageLayout(image);
		tempLayout.unsetValid(ImageLayout.COLOR_MODEL_MASK).unsetValid(ImageLayout.SAMPLE_MODEL_MASK);
		RenderedImage alpha = ConstantDescriptor.create(Float.valueOf(image.getWidth()),
				Float.valueOf(image.getHeight()), new Byte[] { Byte.valueOf((byte) 255) },
				new RenderingHints(JAI.KEY_IMAGE_LAYOUT, tempLayout));
		ImageWorker iw = new ImageWorker(image);
		iw.addBand(alpha, false, true, null);
		return iw.getRenderedImage();
	}

	/**
	 * 使用单波段颜色模型和灰度红色分量，是配出一个合适的背景色
	 * @param gray
	 * @param cm
	 * @return
	 */
	double mapToGrayColor(Color gray, ComponentColorModel cm) {
		double[] rescaleFactors = new double[DataBuffer.TYPE_UNDEFINED + 1];
		rescaleFactors[DataBuffer.TYPE_BYTE] = 1;
		rescaleFactors[DataBuffer.TYPE_SHORT] = 255;
		rescaleFactors[DataBuffer.TYPE_INT] = Integer.MAX_VALUE / 255;
		rescaleFactors[DataBuffer.TYPE_USHORT] = 512;
		rescaleFactors[DataBuffer.TYPE_DOUBLE] = 1 / 255.0;
		rescaleFactors[DataBuffer.TYPE_FLOAT] = 1 / 255.0;
		rescaleFactors[DataBuffer.TYPE_UNDEFINED] = 1;
		return gray.getRed() / rescaleFactors[cm.getTransferType()];
	}

	/**
	 * 判断是否为灰度图像
	 * @param color
	 * @return
	 */
	private boolean isLevelOfGray(Color color) {
		return color.getRed() == color.getBlue() && color.getRed() == color.getGreen();
	}

}
