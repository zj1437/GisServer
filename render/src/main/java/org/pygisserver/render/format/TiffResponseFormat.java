package org.pygisserver.render.format;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import org.pygisserver.common.exception.BaseException;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.imageio.geotiff.CRS2GeoTiffMetadataAdapter;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffConstants;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataEncoder;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.image.io.ImageIOExt;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import it.geosolutions.imageio.plugins.tiff.TIFFImageWriteParam;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: tiff格式化处理，主要是将图像tiff增加地理信息数据
 * @author: 刘洋 
 * @date: 2022-06-22
 */
@Component
@Slf4j
public class TiffResponseFormat {

	
	private static final ImageWriterSpi writerSPI = new it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriterSpi();
	
	public void writeImage2OutPutformat(RenderedImage image, GridGeometry2D gg, String outPutFile) {
		try {
			this.writeImage2OutPutformat(image, gg, new FileOutputStream(new File(outPutFile)));
		} catch (IOException e) {
			log.error("TiffResponseFormat writeImage2OutPutformat has error {}",ExceptionsUtil.stackTraceToStr(e));
		}
	}
	
	
	/**
	 * 将tiff图像信息，增加地图信息
	 * @param image
	 * @param gg
	 * @param outPutFile
	 * @throws IOException
	 */
	public void writeImage2OutPutformat(RenderedImage image, GridGeometry2D gg, OutputStream os) throws IOException{
		GridEnvelope2D range = gg.getGridRange2D();
		AffineTransform tr = (AffineTransform) gg.getGridToCRS2D();
		CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem2D();
		CRS2GeoTiffMetadataAdapter adapter = new CRS2GeoTiffMetadataAdapter(crs);
		GeoTiffIIOMetadataEncoder geoTiffIIOMetadataEncoder = adapter.parseCoordinateReferenceSystem();
		setGeoReference(crs, geoTiffIIOMetadataEncoder, tr, range, false);
		GeoTiffWriteParams gtParams = new GeoTiffWriteParams();
		ImageWriteParam params = gtParams.getAdaptee();
		if (params instanceof TIFFImageWriteParam && gtParams instanceof GeoTiffWriteParams) {
			TIFFImageWriteParam param = (TIFFImageWriteParam) params;
			param.setForceToBigTIFF(((GeoTiffWriteParams) gtParams).isForceToBigTIFF());
		}
		TIFFImageWriter writer = (TIFFImageWriter) writerSPI.createWriterInstance();
		ImageOutputStream ioutstream = null;
		try {
			IIOMetadata metadata = createGeoTiffIIOMetadata(writer, ImageTypeSpecifier.createFromRenderedImage(image),
					geoTiffIIOMetadataEncoder, params);
			ioutstream = ImageIOExt.createImageOutputStream(image, os);
			writer.setOutput(ioutstream);
			writer.write(writer.getDefaultStreamMetadata(params), new IIOImage(image, null, metadata), params);
			ioutstream.flush();
		} finally {
			ioutstream.close();
			os.close();
			writer.dispose();
		}
	}

	/**
	 * 创建metadata对象
	 * @param writer
	 * @param type
	 * @param geoTIFFMetadata
	 * @param params
	 * @return
	 * @throws IIOException
	 */
	public final IIOMetadata createGeoTiffIIOMetadata(ImageWriter writer, ImageTypeSpecifier type,
			GeoTiffIIOMetadataEncoder geoTIFFMetadata, ImageWriteParam params) {
		IIOMetadata imageMetadata = writer.getDefaultImageMetadata(type, params);
		imageMetadata = writer.convertImageMetadata(imageMetadata, type, params);
		Element w3cElement = (Element) imageMetadata.getAsTree(GeoTiffConstants.GEOTIFF_IIO_METADATA_FORMAT_NAME);
		geoTIFFMetadata.assignTo(w3cElement);
		try {
			Node TIFFIFD = w3cElement.getChildNodes().item(0);
			final IIOMetadata iioMetadata = new TIFFImageMetadata(TIFFImageMetadata.parseIFD(TIFFIFD));
			imageMetadata = iioMetadata;
		} catch (IIOInvalidTreeException e) {
			log.error("TiffResponseFormat createGeoTiffIIOMetadata has error {}", e.getLocalizedMessage());
			throw new BaseException("创建tiff metadata信息失败", e.getCause());
		}
		return imageMetadata;
	}

	private void setGeoReference(final CoordinateReferenceSystem crs, final GeoTiffIIOMetadataEncoder metadata,
			final AffineTransform rasterToModel, GridEnvelope2D range, boolean retainAxesOrder) throws IOException {

		final AffineTransform modifiedRasterToModel = new AffineTransform(rasterToModel);
		modifiedRasterToModel.concatenate(CoverageUtilities.CENTER_TO_CORNER);
		int minx = range.getLow(0), miny = range.getLow(1);
		if (minx != 0 || miny != 0) {
			modifiedRasterToModel.concatenate(AffineTransform.getTranslateInstance(minx, miny));
		}
		metadata.addGeoShortParam(GeoTiffConstants.GTRasterTypeGeoKey, GeoTiffConstants.RasterPixelIsArea);
		boolean swapAxes = XAffineTransform.getSwapXY(modifiedRasterToModel) == -1
				|| CRS.getAxisOrder(crs).equals(AxisOrder.NORTH_EAST);
		swapAxes &= !retainAxesOrder;
		if (swapAxes) {
			modifiedRasterToModel.preConcatenate(CoverageUtilities.AXES_SWAP);
		}
		metadata.setModelTransformation(modifiedRasterToModel);
	}

}
