package org.pygisserver.render.style;

import org.geotools.styling.Style;

import lombok.Data;

@Data
public class StInfo {

	private String name;
	
	private String sldFileName;
	
	private Style style;
}
