package org.pygisserver.common.pytc.entity;

import org.pygisserver.common.entity.SeedTaskInstance;

import lombok.Data;

/**
 * 
 * @author Administrator
 *
 */
@Data
public class RenderResouce {
	
	private SeedTaskInstance taskInstance;
	
	private byte[] buf;
	
	private int[] tileIndex;
	
	private String styleName;
	
	private String format;

	public RenderResouce(SeedTaskInstance taskInstance, byte[] buf, int[] tileIndex,String styleName,String format) {
		super();
		this.taskInstance = taskInstance;
		this.buf = buf;
		this.tileIndex = tileIndex;
		this.styleName = styleName;
		this.format = format;
	}
	
}
