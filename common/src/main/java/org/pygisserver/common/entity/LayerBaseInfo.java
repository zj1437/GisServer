package org.pygisserver.common.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @Description: 服务的基本图层信息
 * @author: 
 * @date: 2020-08-26
 */
@Data
public class LayerBaseInfo implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String layerName;

	private int serverId;
	
	/**
	 * 元数据投影格式
	 */
	private String resourceSrs;

	/**
	 * 本地投影格式
	 */
	private String nativeSrs;
	
	/**
	 * 元数据的瓦片格式
	 */
	private String resourceFormat;
	
	/**
	 * 瓦片宽度
	 */
	private int tileWidth;
	
	/**
	 * 瓦片高度
	 */
	private int tileHeight;
	
	/**
	 * 文件路径
	 */
	private String filePath;
	
	/**
	 * 文件类型
	 */
	private String fileType;
	
	/**
	 * 数据类型
	 */
	private int dataType;
	
	/**
	 *瓦片服务最大级别
	 */
	private int supportMaxLevel;
	
	/**
	 * 当前服务状态
	 */
	private int status;
	
	/**
	 * 服务支持的协议
	 */
	private String supportProtocal;
	
	/**
	 * 元数据bbox
	 */
	private String resourceBbox;
	
	/**
	 * 本地bbox
	 */
	private String nativaBbox;
	
	/**
	 * sld样式名称
	 */
	private String styleName;
	
	/**
	 * 海图参数设置
	 */
	private String seaChartSettingJson;
	
	/**
	 * 海图标识符
	 */
	private String seaChartIdentifier;
	
	/**
	 * 是否为图层组
	 */
	private int isLayerGroup;
	
	/**
	 * 矢量文件编码格式
	 */
	private String encoding;
	
	/**
	 * 服务描述信息
	 */
	private String description;

	private int origin;
	
	/**
	 * 输入透明色
	 */
	private String inputTransparentColor;
	
	/**
	 * 输出透明色
	 */
	private String outPutTransparentColor;

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
		LayerBaseInfo other = (LayerBaseInfo) obj;
		if (dataType != other.dataType)
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (encoding == null) {
			if (other.encoding != null)
				return false;
		} else if (!encoding.equals(other.encoding))
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (fileType == null) {
			if (other.fileType != null)
				return false;
		} else if (!fileType.equals(other.fileType))
			return false;
		if (isLayerGroup != other.isLayerGroup)
			return false;
		if (layerName == null) {
			if (other.layerName != null)
				return false;
		} else if (!layerName.equals(other.layerName))
			return false;
		if (nativaBbox == null) {
			if (other.nativaBbox != null)
				return false;
		} else if (!nativaBbox.equals(other.nativaBbox))
			return false;
		if (resourceBbox == null) {
			if (other.resourceBbox != null)
				return false;
		} else if (!resourceBbox.equals(other.resourceBbox))
			return false;
		if (resourceFormat == null) {
			if (other.resourceFormat != null)
				return false;
		} else if (!resourceFormat.equals(other.resourceFormat))
			return false;
		if (resourceSrs == null) {
			if (other.resourceSrs != null)
				return false;
		} else if (!resourceSrs.equals(other.resourceSrs))
			return false;
		if (seaChartIdentifier == null) {
			if (other.seaChartIdentifier != null)
				return false;
		} else if (!seaChartIdentifier.equals(other.seaChartIdentifier))
			return false;
		if (seaChartSettingJson == null) {
			if (other.seaChartSettingJson != null)
				return false;
		} else if (!seaChartSettingJson.equals(other.seaChartSettingJson))
			return false;
		if (status != other.status)
			return false;
		if (styleName == null) {
			if (other.styleName != null)
				return false;
		} else if (!styleName.equals(other.styleName))
			return false;
		if (supportMaxLevel != other.supportMaxLevel)
			return false;
		if (supportProtocal == null) {
			if (other.supportProtocal != null)
				return false;
		} else if (!supportProtocal.equals(other.supportProtocal))
			return false;
		if (tileHeight != other.tileHeight)
			return false;
		if (tileWidth != other.tileWidth)
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
		result = prime * result + dataType;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + ((fileType == null) ? 0 : fileType.hashCode());
		result = prime * result + isLayerGroup;
		result = prime * result + ((layerName == null) ? 0 : layerName.hashCode());
		result = prime * result + ((nativaBbox == null) ? 0 : nativaBbox.hashCode());
		result = prime * result + ((resourceBbox == null) ? 0 : resourceBbox.hashCode());
		result = prime * result + ((resourceFormat == null) ? 0 : resourceFormat.hashCode());
		result = prime * result + ((resourceSrs == null) ? 0 : resourceSrs.hashCode());
		result = prime * result + ((seaChartIdentifier == null) ? 0 : seaChartIdentifier.hashCode());
		result = prime * result + ((seaChartSettingJson == null) ? 0 : seaChartSettingJson.hashCode());
		result = prime * result + status;
		result = prime * result + ((styleName == null) ? 0 : styleName.hashCode());
		result = prime * result + supportMaxLevel;
		result = prime * result + ((supportProtocal == null) ? 0 : supportProtocal.hashCode());
		result = prime * result + tileHeight;
		result = prime * result + tileWidth;
		return result;
	}
	
}
