package org.pygisserver.web.entity;

import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.Data;

/**
 * @Description: 实时系统资源记录 
 * @author:
 * @date: 2018-11-26
 */
@Data
public class SysResourceRecord {

	private int id;

	private double cpuRate;
	
	private double totalSize;
	
	private double useSize;
	
	private double useRate;
	
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
//	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date recordTime;

	public SysResourceRecord(double cpuRate, double totalSize, double useSize, double useRate, Date recordTime) {
		super();
		this.cpuRate = cpuRate;
		this.totalSize = totalSize;
		this.useSize = useSize;
		this.useRate = useRate;
		this.recordTime = recordTime;
	}
}
