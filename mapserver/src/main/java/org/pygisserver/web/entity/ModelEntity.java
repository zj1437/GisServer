package org.pygisserver.web.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.annotation.JSONField;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模型实体类
 * 
 * @author 
 * @since 2018-05-03
 */
@ApiModel(value="模型数据实体")
@Data
public class ModelEntity implements Serializable
{
    /**
     * 序列号
     */
    private static final long serialVersionUID = 411904097700088560L;

    /**
     * 自增id
     */
    private int id;
    
    /**
     * 服务名称
     */
    @ApiModelProperty(value="服务名称")
    private String serverName;
    
    /**
     * 服务状态  0是已暂停 1是进行中，2处理中
     */
    @ApiModelProperty(value="服务状态")
    private int status;
    
    /**
     * 发布日期
     */
    @JSONField(format="yyyy-MM-dd")
//    @JsonFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value="发布时间")
    private Date publishTime;
    
    /**
     * 更新时间
     */
    @JSONField(format="yyyy-MM-dd")
//    @JsonFormat(pattern="yyyy-MM-dd")
    @ApiModelProperty(value="更新时间")
    private Date updateTime;
    
       /**
     * 发布人
     */
    @ApiModelProperty(value="发布人")
    private String publishor;
    
    /**
     * 服务描述
     */
    @ApiModelProperty(value="服务描述")
    private String description;
    
    /**
     * 产品数量
     */
    @ApiModelProperty(value="模型数量")
    private int productNum;
    
    /**
     * 文件url,目前只适用于倾斜射影，因为倾斜射影每次只发布一个数据
     */
    @ApiModelProperty(value="文件url")
    private String url;
    
    /**
     * 文件名称
     */
    @ApiModelProperty(value="文件名称")
    private String productName;
    
    /**
     * 远程请求url
     */
    private String requesturl;

    @ApiModelProperty(value="倾斜射影位置经度")
    private Double lon;
    
    @ApiModelProperty(value="倾斜射影位置纬度")
    private double lat;
    
    @ApiModelProperty(value="倾斜射影位置高度")
    private double hei;

	@ApiModelProperty(value="偏移高度")
    private double offsetHeight;
    
    /**
     * 模型产品
     */
    private List<ModelProduct> products;
    
    /**
     * 服务类型0是模型服务  1是倾斜射影服务
     */
    @ApiModelProperty(value="服务类型")
    private int serverType;
    
    /**
     * 倾斜射影位置信息
     */
    private String tiltPositon;
    
    /**
     * 目标文件路径
     */
    private String destPath;
    
    /**
     * 是否公开  0不公开  1公开
     */
    private int isPublic = 1;
    
    
	public ModelEntity(String serverName, Date updateTime, String publishor) {
		super();
		this.serverName = serverName;
		this.updateTime = updateTime;
		this.publishor = publishor;
	}


	/**
	 * 重写equal方法
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModelEntity other = (ModelEntity) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (destPath == null) {
			if (StringUtils.isNotBlank(other.destPath) )
				return false;
		} else if (!destPath.equals(other.destPath))
			return false;
		if (Double.doubleToLongBits(hei) != Double.doubleToLongBits(other.hei))
			return false;
		if (id != other.id)
			return false;
		if (isPublic != other.isPublic)
			return false;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (lon == null) {
			if (other.lon != null)
				return false;
		} else if (!lon.equals(other.lon))
			return false;
		if (productName == null) {
			if (StringUtils.isNotBlank(other.productName))
				return false;
		} else if (!productName.equals(other.productName))
			return false;
		if (productNum != other.productNum)
			return false;
		if (serverName == null) {
			if (StringUtils.isNotBlank(other.serverName))
				return false;
		} else if (!serverName.equals(other.serverName))
			return false;
		if (status != other.status)
			return false;
		if (tiltPositon == null) {
			if (StringUtils.isNotBlank(other.tiltPositon))
				return false;
		} else if (!tiltPositon.equals(other.tiltPositon))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		if (Double.doubleToLongBits(offsetHeight) != Double.doubleToLongBits(other.offsetHeight))
			return false;
		return true;
	}


	/**
	 * 重写hashcode
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((destPath == null) ? 0 : destPath.hashCode());
		long temp;
		temp = Double.doubleToLongBits(hei);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + id;
		result = prime * result + isPublic;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((lon == null) ? 0 : lon.hashCode());
		result = prime * result + ((productName == null) ? 0 : productName.hashCode());
		result = prime * result + productNum;
		result = prime * result + ((products == null) ? 0 : products.hashCode());
		result = prime * result + ((publishTime == null) ? 0 : publishTime.hashCode());
		result = prime * result + ((publishor == null) ? 0 : publishor.hashCode());
		result = prime * result + ((requesturl == null) ? 0 : requesturl.hashCode());
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + serverType;
		result = prime * result + status;
		result = prime * result + ((tiltPositon == null) ? 0 : tiltPositon.hashCode());
		result = prime * result + ((updateTime == null) ? 0 : updateTime.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}
	
}
