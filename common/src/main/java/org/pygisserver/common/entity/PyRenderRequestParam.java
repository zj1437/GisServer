package org.pygisserver.common.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.awt.*;
import java.io.Serializable;
import java.util.List;

/**
 * @Description: 渲染请求参数，  客户端提供的渲染参数 ，
 * 其中(O)表示可选参数，不设置，不会影响渲染流程
 * 其中(M)表示必选参数，必须设置
 * @author:    
 * @date: 2021-03-19
 */
@Data
@ApiModel(value = "renderParam",description = "渲染请求参数")
public class PyRenderRequestParam implements Cloneable,Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1384272705095873592L;

	/**
	 * 图层名称(O)
	 */
	@ApiModelProperty(value = "图层名称",required = false)
	private String layerName;
	
	/**
	 * 样式名称(O)
	 */
	@ApiModelProperty(value = "样式名称",required = true)
	private String styleName;
	
	/**
	 * 数据格式(O)
	 */
	@ApiModelProperty(value = "数据格式",required = true)
	private String dataFormat;

	/**
	 * 是否为图层组（O），如果layerName不为空，则该值需要输入
	 */
	@ApiModelProperty(value = "图层组标志位",required = false)
	private boolean isLayerGroup;
	
	/**
	 * 图层组中是否有瓦片图层
	 */
	@ApiModelProperty(value = "是否含有瓦片图层标志位",required = false)
	private boolean isTiledLayer;
	
	/**
	 * 数据类型(O)
	 */
	@ApiModelProperty(value = "数据类型",required = true)
	private int dataType;
	
	/**
	 * 文件url(M)  图层组多个url中间使用，如果传递了服务名称，则url，没有必要一定传递
	 */
	@ApiModelProperty(value = "服务文件路径",required = true)
	private String fileUrl;
	
	/**
	 * 样式url(M) 图层组多个url中间使用；
	 */
	@ApiModelProperty(value = "渲染样式文件路径",required = true)
	private String sldUrl;
	
	/**
	 * 渲染宽度(M)
	 */
	@ApiModelProperty(value = "渲染宽度",required = false)
	private int mapWidth;

	/**
	 * 渲染高度(M)
	 */
	@ApiModelProperty(value = "渲染高度",required = false)
	private int mapHeight;
	
	/**
	 * 请求bbox示例（-180,180,-90,90）(M)
	 */
	@ApiModelProperty(value = "请求bbox实例",required = false)
	private String requestBbox;
	
	/**
	 * 请求投影(M)
	 */
	@ApiModelProperty(value = "投影坐标系",required = true)
	private String requestSrs;
	
	/**
	 * 是否支持透明（O）
	 */
	@ApiModelProperty(value = "是否支持透明",required = false)
	private boolean transparent = true;
	
	/**
	 * 渲染格式(M)
	 */
	@ApiModelProperty(value = "渲染格式",required = true)
	private String format;
	
	/**
	 * 渲染背景色(O)
	 */
	@ApiModelProperty(value = "渲染背景颜色",required = false)
	private Color bgcolor = Color.white;
	
	/**
	 * 抗锯齿设置(O)
	 */
	@ApiModelProperty(value = "抗锯齿设置",required = false)
	private String antialias;
	
	/**
	 * 插值算法(nearest,bilinear,bicubic)(O)
	 */
	@ApiModelProperty(value = "插值算法",required = false)
	private String interpolation;
	
	/**
	 * 旋转角度(O)
	 */
	@ApiModelProperty(value = "旋转角度",required = false)
	private double angle;
	
	/**
	 * 数据库连接参数（o）
	 */
	@ApiModelProperty(value = "数据库连接参数",required = false,dataType = "postgisEntity")
	private PostGisConnectEntity postgisConnection;

	@ApiModelProperty(value = "原始数据渲染最大级别",required = true)
	private String resourceMaxLevel;

	@ApiModelProperty(value = "渲染最大级别",required = true)
	private int zoomLevel;
	
	/**
	 * 输入透明色
	 */
	private String inputTransparentColor;
	
	/**
	 * 输出透明色
	 */
	private String outPutTransparentColor;
	
	/**
	 * 高斯瓦片数
	 */
	@ApiModelProperty(value = "高斯瓦片数",required = false)
	private List<String> tilecount;

	@ApiModelProperty(hidden = true)
	private boolean isCache;

	/**
	 * 文件类型
	 */
	private String fileType;

	private int leftOrRight;

	private String resourcesSrs;

	private String service;

	private String cqlFilter;

	@Override
	public PyRenderRequestParam clone() {
		try {
			return (PyRenderRequestParam) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return this;
	}
}
