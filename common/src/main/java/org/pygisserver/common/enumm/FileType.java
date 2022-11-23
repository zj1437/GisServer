package org.pygisserver.common.enumm;

import java.util.Arrays;

public enum FileType{
	
	SHAPE("Shape",".shp"),
	
	TIFF("GeoTIFF",".tif"),
	
	MBTILESDB("MBTiles",".db"),
	
	MBTILES("MBTiles",".mbtiles"),
	
	TDB("TDB",".tdb"),
	
	TMS("TMS",".tms"),
	
	FlatGIS("FlatGis",".flatgis"),
	
	PostGIS("PostGIS",".postgis"),
	
	KML("KML",".kml"),
	
	IMAGEMOSAIC("ImageMosaic",""),

	REST("REST",".rest"),

	/*新增IMG数据类型*/
	IMG("ERDASImg",".img");
	
	private String filterType;
	
	private String fileType;
	
	/**
	 * @return the filterType
	 */
	public String getFilterType() {
		return filterType;
	}

	/**
	 * @return the fileType
	 */
	public String getFileType() {
		return fileType;
	}

	FileType(String filterType,String fileType) {
		this.filterType = filterType;
		this.fileType = fileType;
	}
	
	public static String getFileType(String fileNameSuffix) {
		for (FileType fileType : FileType.values()) {
			if (fileNameSuffix.equals(fileType.getFileType())) {
				return fileType.getFilterType();
			}
		}
		return TIFF.getFilterType();
	}
	
	
	public static String getFileNameFilter(String filterType) {
		return Arrays.asList(FileType.values()).stream().filter(type -> filterType.equals(type.getFilterType())).findFirst().get().getFileType();
	}
}
