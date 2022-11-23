package org.pygisserver.common.entity;

import java.io.Serializable;

import lombok.Data;
/**
 * 态势服务管理类实体
 * @author 
 *
 */
@Data
public class PostureServerInfoEntity implements Serializable{
	private static final long serialVersionUID = 610L;
	//态势推演服务id
	private int postureId;
	//态势推演服务名称
	private String postureName;
	//态势推演服务状态 0 正常 1 异常
	private int postureState;
	//态势推演文件路径
	private String posturePath;
	//态势推演备注信息
	private String description;
	//态势推演文件上传后真实文件名称 文件在上传时如果有重复可能会被重命名
	private String realFileName;
	
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PostureServerInfoEntity entity = (PostureServerInfoEntity)obj;
		if (entity.getPostureId() != this.postureId) return false;
		if (!entity.getPostureName().equals(this.postureName)) return false;
		if (!entity.getDescription().equals(this.description)) return false;
		return true;
    }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + postureId;
		result = prime * result + ((postureName == null) ? 0 : postureName.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		return result;
	}
	
	public String toString() {
        return postureName + "@" + postureState;
    }
}
