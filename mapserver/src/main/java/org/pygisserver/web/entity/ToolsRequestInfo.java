package org.pygisserver.web.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;


@Data
@ApiModel(value = "toolsRequestInfo",description = "小工具请求模型")
public class ToolsRequestInfo implements Serializable {

    @ApiModelProperty(value = "数据库表名",required = true)
    private String tableName;

    @ApiModelProperty(value = "编码规则",required = true)
    private String chartSetName;

    @ApiModelProperty(value = "投影坐标系",required = true)
    private String srsName;

    @ApiModelProperty(value = "导入功能文件名称",required = false)
    private String fileName;

    @ApiModelProperty(value = "导入功能结果标志",required = false)
    private int result;

    @ApiModelProperty(value = "导入结果",required = false)
    private String resultInfo;

    @ApiModelProperty(value = "表格聚合字段",required = false)
    private String aggregateField;

    @ApiModelProperty(value = "矢量数据类型",required = false)
    private String dataType;

    @ApiModelProperty(value = "矢量数据导出路径",required = false)
    private String exportPath;

}
