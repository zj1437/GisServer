package org.pygisserver.common.enumm;

/**
 * @Description: 
 * @author: 
 * @date: 2020-09-18
 */
public enum OgcEpsg {

	DEGREE("EPSG:4326",OgcEpsg.DEGREE_CODE),
	
	Mercator_900913("EPSG:900913",OgcEpsg.MERCATOR_CODE),
	
	Mercator_41001("EPSG:41001",OgcEpsg.MERCATOR_CODE),

	Mercator_3395("EPSG:3395",OgcEpsg.MERCATOR_CODE),

	/*客户自定义的坐标系，暂时先按照web墨卡托处理*/
	Mercator_404000("EPSG:404000",OgcEpsg.MERCATOR_CODE), 
	
	Mercator_3857("EPSG:3857",OgcEpsg.MERCATOR_CODE);
	
	
	public static final int DEGREE_CODE = 0;
	
	/**
	 * web墨卡托
	 */
	public static final int MERCATOR_CODE = 1;

	/**
	 * 通用横轴墨卡托
	 */
	public static final int MERCATOR_UTM = 2;
	
	private String ogcCode;
	
	private int typeCode;
	
	
	/**
	 * @return the ogcCode
	 */
	public String getOgcCode() {
		return ogcCode;
	}

	/**
	 * @param ogcCode the ogcCode to set
	 */
	public void setOgcCode(String ogcCode) {
		this.ogcCode = ogcCode;
	}

	/**
	 * @return the typeCode
	 */
	public int getTypeCode() {
		return typeCode;
	}

	/**
	 * @param typeCode the typeCode to set
	 */
	public void setTypeCode(int typeCode) {
		this.typeCode = typeCode;
	}

	OgcEpsg(String ogcCode,int typeCode) {
		this.ogcCode = ogcCode;
		this.typeCode = typeCode;
	}
	
	public static int getOgcEpsgCodeType(String ogcCode) {
		for (OgcEpsg osgEpsg : OgcEpsg.values()) {
			if (osgEpsg.ogcCode.equalsIgnoreCase(ogcCode)) {
				return osgEpsg.typeCode;
			}
		}
		return OgcEpsg.DEGREE.typeCode;
	}
}
