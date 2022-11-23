package org.pygisserver.web.entity;

import java.util.Map;

import lombok.Data;
import org.pygisserver.common.entity.LayerBaseInfo;

@Data
public class AnalysisTaskParam
 {
	
	private String taskName;
	
	private int taskId;
	
	private int taskType;
	
	private String fileUrl;
	
	private String nativeBbox;
	
	private String format;
	
	private Map<String,Object> mapParam;
	
	private String resultUrl;

	 private LayerBaseInfo layerBaseInfo;
}
