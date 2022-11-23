package org.pygisserver.pytc.entity;

import java.io.Serializable;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;

import lombok.Data;

@Data
public class TileRangeInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1069559973753047159L;

	/**
	 * 起始瓦片
	 */
	private long[] startIndex;
	
	private List<long[]> relatedTiles;
	
	private ReferencedEnvelope envelope;
}
