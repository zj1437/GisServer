package org.pygisserver.web.startegy;

import java.util.Map;

import org.pygisserver.web.service.ReadDataInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class AbstractReadData implements ReadDataInfo {

	@Override
	public abstract ReferencedEnvelope getBboxFromFile();

	@Override
	public int getImgWidth() {
		return 256;
	};

	@Override
	public int getImgHeight() {
		return 256;
	}

	@Override
	public abstract CoordinateReferenceSystem getResourceSrs();

	@Override
	public int getDataType() {
  		return 1;
	}

	@Override
	public int featuresSize() {
		return 0;
	}

	@Override
	public String getTileExtension() {
		return null;
	}

	@Override
	public abstract int getMaxLevel(ReferencedEnvelope originalEnvelope);

	@Override
	public Map<Object, Object> scaleMap() {
		return null;
	}

	@Override
	public String getSeaChartIdentifier() {
		return null;
	}

	/**
	 * @return
	 */
	@Override
	public int getTileOriginFlag() {
		return 0;
	}
}
