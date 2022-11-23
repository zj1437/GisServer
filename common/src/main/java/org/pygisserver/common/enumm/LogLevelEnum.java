package org.pygisserver.common.enumm;

public enum LogLevelEnum {

	SERIOUS(0,"严重"),
	
	WARN(1,"警告"),
	
	INFO(2,"信息"),
	
	DEBUG(3,"调试");
	
	private int level;
	
	private String levelStr;
	
	LogLevelEnum(int level,String levelStr){
		this.level = level;
		this.levelStr = levelStr;
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return the levelStr
	 */
	public String getLevelStr() {
		return levelStr;
	}
	
}
