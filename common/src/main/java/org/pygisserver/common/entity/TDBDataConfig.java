package org.pygisserver.common.entity;

import lombok.Data;

@Data
public class TDBDataConfig {
	//version id，char格式，4字符
	private String dwVersionNo;
	//srs，是EPSG:4326或EPSG:3857
	private String srsType;
	//tile format,mixed-0,jpeg-1,png-2,tiff-3,heightfiled-16  //切片类型，具体是指影像是0，高程是1，法线是2
	private char cBlkType;
	//compress type,unzip-0,mixed-1,zlib-2   //压缩类型，,unzip-0,mixed-1,zlib-2
	private char cCompressType;
	//datatype，影像是0，高程是1，法线是2  //瓦片类型，jpeg、png
	private char cSourceDataType;
	//tile width
	private int nTleSizeX;
	//tile height
	private int nTleSizeY;
	//min level
	private int nMinLod;
	//max level
	private int nMaxLod;
	//min latitude
	private double dMinLat;
	//min longtitude
	private double dMinLon;
	//max latitude
	private double dMaxLat;
	//max longtitude
	private double dMaxLon;
	//max row number
	private int nPerFileBlkNumX;
	//max col number
	private int nPerFileBlkNumY;
	//description, 100char
	private String strDes;
}
