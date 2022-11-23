package org.pygisserver.common.ui.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.Data;

/**
 * @Description: 图层组信息 
 * @author: 
 * @date: 2020-05-11
 */
@Data
public class LayerGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 图层组id
	 */
	private int LayerGroupId;
	
	/**
	 * 远程url
	 */
	private String remoteUrl;
	
	/**
	 * 远程服务类型  eg:supermap
	 */
	private String serverType;
	
	/**
	 * OGC协议标准
	 */
	private String ogcProtocal;
	
	/**
	 * 图层组名称
	 */
	private String layerGroupName;
	
	/**
	 * 图层名称
	 */
	private String layerName;
	
	/**
	 * 样式名称
	 */
	private String styleName;
	
	/**
	 * 最小级别
	 */
	private int minLevel;
	
	/**
	 * 最大级别
	 */
	private int maxlevel;

	/**
	 * 源文件路径
	 */
	private String fileUrl;

	/**
	 * 数据类型
	 */
	private int dataType;

	/**
	 * 本地坐标
	 */
	private String nativeCrs;

	/**
	 *本地bbox
	 */
	private String nativeBbox;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LayerGroup that = (LayerGroup) o;
		return minLevel == that.minLevel &&
				maxlevel == that.maxlevel &&
				layerName.equals(that.layerName) &&
				styleName.equals(that.styleName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(layerName, styleName, minLevel, maxlevel, fileUrl);
	}
}
