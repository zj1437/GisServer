package org.pygisserver.web.entity;

import java.util.Date;

import lombok.Data;

/**
 * @Description: 符号化样式 
 * @author:  
 * @date: 2019-03-12
 */
@Data
public class StyleInfo {
	//普通影像栅格
	public static final int RASTER = 1;

	//栅格高程、染色图
	public static final int RASTER_dem = 2;

	public static final int POINT = 3;

	public static final int LINE = 4;

	public static final int POLYGON = 5;

	public static final int DEFAULT = 6;

	public enum SldType{

		RASTER("RASTER",StyleInfo.RASTER),

		DEM("DEM",StyleInfo.RASTER_dem),

		POINT("POINT",StyleInfo.POINT),

		LINE("LINE",StyleInfo.LINE),

		POLYGON("POLYGON",StyleInfo.POLYGON),

		DEFAULT("DEFAULT",StyleInfo.DEFAULT);

		private String type;

		private int typeId;

		SldType(String type,int typeId) {
			this.type = type;
			this.typeId = typeId;
		}

		public String getType() {
			return type;
		}

		public int getTypeId() {
			return typeId;
		}

		public static int getTypeIdByType(String type) {
			for (SldType sldType : SldType.values()) {
				if (type.equals(sldType.getType())) {
					return sldType.getTypeId();
				}
			}
			return SldType.RASTER.getTypeId();
		}
	}

	private int id;

	private String styleId;

	private int serverId;

	private String sldName;

	private String fileName;

	private String url;

	private int isColor;

	private int typeId;

	private int isDefault;

	/**
	 * 样式可以适配的图层名称
	 */
	private String layerName;

	/**
	 * 上传时间
	 */
	private Date uploadTime;

	/**
	 * 上传者
	 */
	private String uploader;


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StyleInfo other = (StyleInfo) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (sldName == null) {
			if (other.sldName != null)
				return false;
		} else if (!sldName.equals(other.sldName))
			return false;
		if (styleId == null) {
			if (other.styleId != null)
				return false;
		} else if (!styleId.equals(other.styleId))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((sldName == null) ? 0 : sldName.hashCode());
		result = prime * result + ((styleId == null) ? 0 : styleId.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}
}
