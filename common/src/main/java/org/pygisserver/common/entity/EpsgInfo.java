package org.pygisserver.common.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @Description: EPSG投影信息 
 * @author: 
 * @date: 2021-06-10
 */
@Data
public class EpsgInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4574236094551757692L;

	private int id;
	
	private int epsgNo;
	
	private String prjInfo;
	
}
