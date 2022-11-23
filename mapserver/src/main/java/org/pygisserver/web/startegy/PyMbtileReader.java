package org.pygisserver.web.startegy;

import java.io.File;
import java.io.IOException;

import javax.media.jai.ImageLayout;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.mbtiles.mosaic.MBTilesReader;
import org.geotools.util.factory.Hints;

public class PyMbtileReader extends MBTilesReader {

	private MBTilesMetadata metadata;
	
	public PyMbtileReader(File file) throws IOException {
		this(file, null);
		MBTilesFilesHelper mbTilesFilesHelper = new MBTilesFilesHelper(file);
	}
	
	public PyMbtileReader(Object source, Hints hints) throws IOException {
		super(source, hints);
		this.metadata = super.metadata;
	}
	
	/**
	 * 获取mbtiles瓦片格式
	 * @return
	 */
	public String getTileExtension() {
		return metadata.getFormatStr();
    }
	
	/**
	 * 获取瓦片高度
	 * @return
	 */
	public ReferencedEnvelope getTileEnvelope() {
		return this.bounds;
    }
	
	/**
	 * 获取图片的colorModel 目前mbtiles默认是栅格数据
	 * @return
	 */
	public ImageLayout getImageLayout() {
		return null;
	} 
}
