package org.pygisserver.common.entity;

import java.util.List;

import lombok.Data;

/**
 * @Description: 海图设置信息 
 * @author: 
 * @date: 2020-11-03
 */
@Data
public class SeaChartSettings {
	
	/**
	 * 是否显示海图文字，默认不显示0  1显示
	 */
	private int seaChartText;
	
	/**
	 * 是否显示水深文字 默认不显示  1显示
	 */
	private int waterDepthText;
	
	/**
	 * 是否显示图形边框 0是不显示  1是显示
	 */
	private int graphicsFrame = 1;
	
	/**
	 * 本地文字  默认支持0是不显示 1是显示
	 */
	private int localText = 1;
	
	/**
	 * 图形样式  0代表简单符号 1代表纸质符号
	 */
	private int graphicStyle = 1;
	
	/**
	 * 颜色方案 0白昼模式 1晨昏模式 2夜晚模式
	 */
	private int colorPlan;
	
	/**
	 * 图形边界0水平边界 1符号边界
	 */
	private int graphicBoundary;
	
	/**
	 * 水深颜色0：2色 1：4色
	 */
	private int waterDepthColor;
	
	/**
	 * 潜水深度
	 */
	private float divingDepth;
	
	/**
	 * 安全水深
	 */
	private float safeWaterDepth;
	
	/**
	 * 深水水深
	 */
	private float deepWaterDepth;
	
	/**
	 * 显示标准 0基础 1标准 2自定义
	 */
	private int showStandard = 1;
	
	/**
	 * 自定义的时候传递
	 */
	private List<String> SeaChartFeature;
	
	/**
	 * 海图属性显示
	 */
	private List<SeaChartFeatureInfo> seaChartFeatureInfo;

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
		SeaChartSettings other = (SeaChartSettings) obj;
		if (SeaChartFeature == null) {
			if (other.SeaChartFeature != null)
				return false;
		} else if (!SeaChartFeature.equals(other.SeaChartFeature))
			return false;
		if (colorPlan != other.colorPlan)
			return false;
		if (Float.floatToIntBits(deepWaterDepth) != Float.floatToIntBits(other.deepWaterDepth))
			return false;
		if (Float.floatToIntBits(divingDepth) != Float.floatToIntBits(other.divingDepth))
			return false;
		if (graphicBoundary != other.graphicBoundary)
			return false;
		if (graphicStyle != other.graphicStyle)
			return false;
		if (graphicsFrame != other.graphicsFrame)
			return false;
		if (localText != other.localText)
			return false;
		if (Float.floatToIntBits(safeWaterDepth) != Float.floatToIntBits(other.safeWaterDepth))
			return false;
		if (seaChartFeatureInfo == null) {
			if (other.seaChartFeatureInfo != null)
				return false;
		} else if (!seaChartFeatureInfo.equals(other.seaChartFeatureInfo))
			return false;
		if (seaChartText != other.seaChartText)
			return false;
		if (showStandard != other.showStandard)
			return false;
		if (waterDepthColor != other.waterDepthColor)
			return false;
		if (waterDepthText != other.waterDepthText)
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
		result = prime * result + ((SeaChartFeature == null) ? 0 : SeaChartFeature.hashCode());
		result = prime * result + colorPlan;
		result = prime * result + Float.floatToIntBits(deepWaterDepth);
		result = prime * result + Float.floatToIntBits(divingDepth);
		result = prime * result + graphicBoundary;
		result = prime * result + graphicStyle;
		result = prime * result + graphicsFrame;
		result = prime * result + localText;
		result = prime * result + Float.floatToIntBits(safeWaterDepth);
		result = prime * result + ((seaChartFeatureInfo == null) ? 0 : seaChartFeatureInfo.hashCode());
		result = prime * result + seaChartText;
		result = prime * result + showStandard;
		result = prime * result + waterDepthColor;
		result = prime * result + waterDepthText;
		return result;
	}
	
	
	
}
