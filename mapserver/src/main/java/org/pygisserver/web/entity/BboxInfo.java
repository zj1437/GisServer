package org.pygisserver.web.entity;

public class BboxInfo {

	/**
	 * 最小纬度
	 */
	private double minLatitude;
	
	/**
	 * 最大纬度
	 */
	private double maxLatitude;

	/**
	 * 最小经度
	 */
    private double minLongitude;
	
    /**
     * 最大经度
     */
	private double maxLongitude;


	public BboxInfo(double minLatitude,double maxLatitude,double minLongitude,double maxLongitude){
		this.minLatitude = minLatitude;
		this.maxLatitude = maxLatitude;
		this.minLongitude = minLongitude;
		this.maxLongitude = maxLongitude;
	}
	
	public double getMinLatitude() {
		return minLatitude;
	}

	public void setMinLatitude(double minLatitude) {
		this.minLatitude = minLatitude;
	}

	public double getMaxLatitude() {
		return maxLatitude;
	}

	public void setMaxLatitude(double maxLatitude) {
		this.maxLatitude = maxLatitude;
	}

	public double getMinLongitude() {
		return minLongitude;
	}

	public void setMinLongitude(double minLongitude) {
		this.minLongitude = minLongitude;
	}

	public double getMaxLongitude() {
		return maxLongitude;
	}

	public void setMaxLongitude(double maxLongitude) {
		this.maxLongitude = maxLongitude;
	}
}
