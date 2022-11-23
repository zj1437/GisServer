package org.pygisserver.common.enumm;

/**
 * @Description: 数据类型格式
 * @author:  
 * @date: 2019-12-12
 */
public enum DataType {

	VECTOR(0),
	
	RASTER(1),
	
	REMOTE(2),
	
	TILED(5),
	
	FUTURE(6);
	
	
	DataType(int typeCode){
		this.typeCode = typeCode;
	}
	
	private int typeCode;
	
	/**
	 * @return the typeCode
	 */
	public int getTypeCode() {
		return typeCode;
	}

	public static DataType getDataTypeByTypeCode(int typeCode) {
		for (DataType dataTypeEnum : DataType.values()) {
			if (dataTypeEnum.getTypeCode() == typeCode) {
				return dataTypeEnum;
			}
		}
		return DataType.RASTER;
	}
}
