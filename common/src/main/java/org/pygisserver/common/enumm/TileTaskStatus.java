package org.pygisserver.common.enumm;

/**
 * @Description:  切片任务的枚举状态
 * @author: 
 * @date: 2019-12-21
 */
public enum TileTaskStatus {

	 RUNNING(1,"进行中"),
	 
	 READY(2,"等待中"),
	 
	 DEAD(3,"暂停中"),
	 
	 DONE(4,"已完成");
	
	TileTaskStatus(int status,String msg){
		this.status = status;
		this.msg = msg;
	}
	
	private int status;
	
	private String msg;

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @return the msg
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * @param msg the msg to set
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}
}
