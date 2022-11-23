package org.pygisserver.web.entity;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description: 模型服务的产品名称
 * @author: 
 * @date: 2018-11-05
 */
@ApiModel(value="modelProduct",description="模型产品")
@Data
public class ModelProduct implements Serializable {


	/**
	 *
	 */
	private static final long serialVersionUID = -3860634896128116130L;

	/**
	 * 自增id
	 */
	private int productId;


	/**
	 * 模型服务id
	 */

	private int modelServerId;
	/**
	 * 产品名称
	 */
	@ApiModelProperty("产品名称")
	private String productName;

	/**
	 * 文件类型
	 */
	@ApiModelProperty("文件类型")
	private String fileType;

	/**
	 * 文件url
	 */
	@ApiModelProperty("文件url")
	private String fileUrl;

	/**
	 * 文件夹还是文件  0是文件  1是文件夹
	 */
	@ApiModelProperty("是否为文件")
	private int isDirectory;

	public ModelProduct(int modelServerId, String productName, String fileType, String fileUrl) {
		super();
		this.modelServerId = modelServerId;
		this.productName = productName;
		this.fileType = fileType;
		this.fileUrl = fileUrl;
	}

	public ModelProduct(int modelServerId, String productName, String fileUrl) {
		super();
		this.modelServerId = modelServerId;
		this.productName = productName;
		this.fileUrl = fileUrl;
	}

	/**
	 * 重写equals 判断两个对象是否相等
	 */
	@Override
	public boolean equals(Object object) {

		if (this == object) {
			return true;
		}

		//只要文件url相等
		if (object instanceof ModelProduct) {
			if (this.getFileUrl().equals(((ModelProduct) object).getFileUrl())
					&& this.getFileType().equals(((ModelProduct) object).getFileType())){
				return true;
			}
		}

		return false;
	}

	/**
	 * 重写hashcode方法
	 */
	@Override
	public int hashCode() {
		return this.getFileUrl().hashCode();
	}
}
