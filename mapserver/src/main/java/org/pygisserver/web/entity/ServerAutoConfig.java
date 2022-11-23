package org.pygisserver.web.entity;

import java.util.List;

import lombok.Data;

@Data

public class ServerAutoConfig {
	private int id;
	
	private int isOpen;
	
	private String monitorPath;
	
	private List<PublishedDataFormat> dataFormat;
}
