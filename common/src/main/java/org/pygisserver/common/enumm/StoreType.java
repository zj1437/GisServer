package org.pygisserver.common.enumm;

/**
 * @Description: 存储类型枚举 
 * @author:  
 * @date: 2019-12-09
 */
public enum StoreType {

	POSTGRRSQL(1,"postgresql"),
	
	
	SQLITE(2,"sqlite"),
	
	
	MONGODB(3,"mongodb"),
	
	
	TDB(4,"tdb"),
	
	TMS(5,"tms");
	
	
	private int typeId;
	
	private String typeStr;
	
	/**
	 * @return the typeId
	 */
	public int getTypeId() {
		return typeId;
	}

	/**
	 * @param typeId the typeId to set
	 */
	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	/**
	 * @return the typeStr
	 */
	public String getTypeStr() {
		return typeStr;
	}

	/**
	 * @param typeStr the typeStr to set
	 */
	public void setTypeStr(String typeStr) {
		this.typeStr = typeStr;
	}

	StoreType(int typeId,String typeStr) {
		this.typeId = typeId;
		this.typeStr = typeStr;
	}
	
	public static StoreType getStoreType(int typeId) {
		for (StoreType storeType : StoreType.values()) {
			if (typeId == storeType.typeId) {
				return storeType;
			}
		}
		return StoreType.SQLITE;
	}
}
