package org.pygisserver.web.startegy;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverageio.gdal.erdasimg.ErdasImgReader;
import org.geotools.gce.geotiff.GeoTiffReader;

/**
 * @Description: 栅格reader的工厂方法
 * @author: 
 * @date: 2019-07-18
 */
public class CoverageReaderFactory {

	public static GridCoverage2DReader getCoverageReader(String fileUrl,String fileType) throws IOException {
		switch (fileType) {
			case "GeoTIFF":
				return new GeoTiffReader(new File(fileUrl));
			case "MBTiles":
				return new PyMbtileReader(new File(fileUrl));
			case "ERDASImg":
				return new ErdasImgReader(new File(fileUrl));
			default:
				return new GeoTiffReader(new File(fileUrl));
		}
	}
}
