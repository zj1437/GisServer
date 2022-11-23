package org.pygisserver.render.tiled;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public interface TiledInfoRead {

	public RenderedImage getWmsImg();
	
	public byte[] getWmsResource();
	
	public byte[] getWmtsResource();
	
	public byte[] getTmsResource();
	
	public BufferedImage compressImg(int[] tileRanage, int realLevel);
	
}
