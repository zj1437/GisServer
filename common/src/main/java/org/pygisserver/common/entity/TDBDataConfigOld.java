package org.pygisserver.common.entity;

import lombok.Data;

@Data
public class TDBDataConfigOld {
	    //版本号
		private int dwVersionNo;
		//是否是墨卡托，否则就是等经纬
		private boolean bIsMercator;
		//是否是高程
		private boolean bIsTex;
		//单个瓦片宽度
		private int nTleSizeX;
		//瓦片高度
		private int nTleSizeY;
		//最小级别
		private int nMinLod;
		//最大级别
		private int nMaxLod;
		//最小纬度
		private double dMinLat;
		//最小经度
		private double dMinLon;
		//最大纬度
		private double dMaxLat;
		//最大经度
		private double dMaxLon;
		//压缩格式，如果是高程时，该字段值为0
		private int extension;
		//单个文件保存的最大瓦片行数
		private int dMaxRowNum;
		//单个文件保存的最大瓦片列数,dMaxRowNum *  dMaxColNum就是一个文件保存的最大瓦片数
		private int dMaxColNum;
}
