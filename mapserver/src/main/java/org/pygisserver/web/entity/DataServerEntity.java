package org.pygisserver.web.entity;

import com.alibaba.fastjson.annotation.JSONField;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.pygisserver.common.entity.SeaChartSettings;
import org.pygisserver.common.ui.entity.LayerGroup;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel(value="dataServerEntity",description="地图模型")
public class DataServerEntity implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5433538135989085100L;

	/**
	 * 服务id
	 */
	@ApiModelProperty(required=false)
	private int serverId;

	/**
	 * 文件类型TIFF、 Shape、MBTiles,tdb
	 */
	@ApiModelProperty(value="文件类型",required=true)
	private String fileType;

	/**
	 * 服务名称
	 */
	@ApiModelProperty(value="服务名称",required=true)
	private String serverName;

	/**
	 * 图层名称
	 */
	@ApiModelProperty(required=false)
	private String layerName;

	/**
	 * 状态
	 */
	@ApiModelProperty(required=false)
	private int status;

	/**
	 * 发布时间
	 */
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	@ApiModelProperty(value="发布时间",required=true)
	private Date publishTime;
	
	/**
	 * 发布人
	 */
	@ApiModelProperty(value="发布人",required=true)
	private String publishor;
	
	/**
	 * 更新时间
	 */
	@JSONField(format="yyyy-MM-dd HH:mm:ss")
	private Date updateTime;
	
	/**
	 * 数据类型（栅格、矢量的点线面）
	 */
	@ApiModelProperty(value="数据类型",required=false)
	private int dataType;
	
	/**
	 * 数据位置
	 */
	@ApiModelProperty(value="源数据url",required=true)
	private String dataUrl;
	
	
	/**
	 * 服务描述
	 */
	@ApiModelProperty(value="服务描述",required=true)
	private String description;
	
	/**
	 * 是否公开 0不公开  1公开
	 */
	private int isPublic;
	
	
	/**
	 * 是否开启自动缓存缓存 0未开启 1 开启
	 */
	private int openTiled;
	
	/**
	 * 服务访问的唯一token
	 */
	private String serverToken;
	
	/**
	 * 编码格式
	 */
	@ApiModelProperty(value="矢量数据编码格式",required=true)
	private String encoding;
	
	/**
	 * 用于创建styleInfo对象
	 */
	private StyleInfo styleInfo;
	
	/**
	 * 色带值
	 */
	private RasterColorMap rasterColor;
	
	/**
	 * 已发布的协议   协议间以都好隔开
	 */
	private String publisedProctol;
	
	/**
	 * 特征信息数据
	 */
	private FeatureInfo featureInfo;
	
	/**
	 * 图层组标识 0默认单图层  1是普通图层组  2是高程图层组
	 */
	private int layerGroupFlag;
	
	/**
	 * 失败原因
	 */
	private String failReason;
	
	/**
	 * 最优协议
	 */
	private String optimalServerName;
	
	/**
	 * 排序级别
	 */
	private int currentLevel;
	
	/**
	 * 图层组信息
	 */
	private List<LayerGroup> layerGroups;
	
	/**
	 * 是否开启金字塔缓存  0 是不开启  1是开启
	 */
	private int isPyamid;
	
	/**
	 * 海图参数设置
	 */
	private SeaChartSettings seaChartSetting;

	private List<ServerProtocolEntity> serverProtocols;
	/**
	 * 服务是否前台展示
	 * 0展示，1不展示
	 */
	private int serverShow;

	/**
	 * 输入透明色
	 */
	private String inputTransparentColor="";

	/**
	 * 输出透明色
	 */
	private String outputTransparentColor="";

	public DataServerEntity() {
	}
	public DataServerEntity(Date publishTime, String publishor, int openTiled, String encoding) {
		this.publishTime = publishTime;
		this.publishor = publishor;
		this.openTiled = openTiled;
		this.encoding = encoding;
	}

	public DataServerEntity(String serverName, String layerName, String dataUrl) {
		super();
		this.serverName = serverName;
		this.layerName = layerName;
		this.dataUrl = dataUrl;
	}

	public DataServerEntity(String serverName, String layerName, String dataUrl, String encoding, StyleInfo styleInfo,
			RasterColorMap rasterColor) {
		super();
		this.serverName = serverName;
		this.layerName = layerName;
		this.dataUrl = dataUrl;
		this.encoding = encoding;
		this.styleInfo = styleInfo;
		this.rasterColor = rasterColor;
	}


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
		DataServerEntity other = (DataServerEntity) obj;
		if (encoding == null) {
			if (other.encoding != null)
				return false;
		} else if (!encoding.equals(other.encoding))
			return false;
		if (layerName == null) {
			if (other.layerName != null)
				return false;
		} else if (!layerName.equals(other.layerName))
			return false;
		if ((rasterColor == null && other.rasterColor != null && other.rasterColor.getColorId() != 0) || (rasterColor != null && rasterColor.getColorId() != 0 && other.rasterColor == null)) {
			return false;
		} else if (rasterColor != null && other.rasterColor != null)
			if (rasterColor.getColorId() != other.rasterColor.getColorId()) {
				return false;
			}
		if (serverName == null) {
			if (other.serverName != null)
				return false;
		} else if (!serverName.equals(other.serverName))
			return false;
		if (styleInfo == null) {
			if (other.styleInfo != null)
				return false;
		} else if (!styleInfo.equals(other.styleInfo))
			return false;
		if (inputTransparentColor == null) {
			if (other.inputTransparentColor != null)
				return false;
		} else if (!inputTransparentColor.equals(other.inputTransparentColor))
			return false;
		if (outputTransparentColor == null) {
			if (other.outputTransparentColor != null)
				return false;
		} else if (!outputTransparentColor.equals(other.outputTransparentColor))
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
		result = prime * result + isPublic;
		result = prime * result + ((layerName == null) ? 0 : layerName.hashCode());
		result = prime * result + openTiled;
		result = prime * result + ((publishor == null) ? 0 : publishor.hashCode());
		result = prime * result + ((rasterColor == null) ? 0 : rasterColor.hashCode());
		result = prime * result + serverId;
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + status;
		result = prime * result + ((styleInfo == null) ? 0 : styleInfo.hashCode());
		result = prime * result + ((updateTime == null) ? 0 : updateTime.hashCode());
		result = prime * result + ((publisedProctol == null) ? 0 : publisedProctol.hashCode());
		return result;
	}

}
