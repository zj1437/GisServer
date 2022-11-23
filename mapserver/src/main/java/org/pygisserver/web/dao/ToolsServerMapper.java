package org.pygisserver.web.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ToolsServerMapper {

    /**
     * 动态创建数据表
     *
     * */
    void createPostGisTable(@Param("tableName") String tableName,
                            @Param("columns") List<Object> columns,
                            @Param("geomIndex") int geomIndex ,
                            @Param("geomType") String geomType,
                            @Param("srs") int srs);


    /**
     * 动态插入数据库
     *
     * */
    void insertPostGisTable(@Param("tableName") String tableName,
                            @Param("columns") List<Object> columns,
                            @Param("valuesList") List<Object[]> values,
                            @Param("geomIndex") int geomIndex,
                            @Param("srs") int srs);


    /**
     * 获取数据
     *
     * */
    List<Map<String,Object>> getValuesFromTableName(@Param("columns") List<Object> columns,
                                                    @Param("geomIndex") int geomIndex,
                                                    @Param("tableName") String tableName,
                                                    @Param("srsName") String srsName,
                                                    @Param("primaryKey") String primaryKey,
                                                    @Param("filters") String filters,
                                                    @Param("geom") String geom);

    /**
     * 获取数据(针对默认投影坐标为0的数据)
     *
     * */
    List<Map<String,Object>> getNoSrsValuesFromTableName(@Param("columns") List<Object> columns,
                                                    @Param("geomIndex") int geomIndex,
                                                    @Param("tableName") String tableName,
                                                    @Param("srsName") String srsName,
                                                    @Param("primaryKey") String primaryKey,
                                                    @Param("filters") String filters,
                                                    @Param("geom") String geom);


    /**
     * 分批获取数据
     *
     * */
    List<Map<String,Object>> getValuesFromTableByCondition(@Param("columns") List<Object> columns,
                                                           @Param("geomIndex") int geomIndex,
                                                           @Param("tableName") String tableName,
                                                           @Param("srsName") String srsName,
                                                           @Param("start") String start,
                                                           @Param("end") String end,
                                                           @Param("primaryKey") String primaryKey,
                                                           @Param("filters") String filters,
                                                           @Param("geom") String geom);

    /**
     * 分批获取数据(针对默认投影坐标为0的数据)
     *
     * */
    List<Map<String,Object>> getNoSrsValuesFromTableByCondition(@Param("columns") List<Object> columns,
                                                           @Param("geomIndex") int geomIndex,
                                                           @Param("tableName") String tableName,
                                                           @Param("srsName") String srsName,
                                                           @Param("start") String start,
                                                           @Param("end") String end,
                                                           @Param("primaryKey") String primaryKey,
                                                           @Param("filters") String filters,
                                                           @Param("geom") String geom);


    /**
     * 通过表名获取列名
     *
     * */
    List<Object> getColumnsFromTableName(String tableName);


    /**
     * 获取表格geometry属性类型
     *
     * */
    String getPostGisGeomType(@Param("geomName") String geomName,@Param("tableName") String tableName);


    /**
     * 查询导出列表
     *
     * */
    List<Map<String, String>> queryExportShpList();


    /**
     * 获取geometry字段名称
     *
     * */
    String getGeomName(String tableName);


    /**
     * 获取数据投影坐标
     *
     * */
    String getResourceSrid(@Param("geomName") String geomName,@Param("tableName") String tableName);


    /**
     * 校验表格是否存在
     *
     * */
    int existTable(String tableName);


    /**
     * 转义geom得到类型
     *
     * */
    String getGeomType(String geomText);


    /**
     * 删除数据表
     *
     * */
    void dropTable(String tableName);


    /**
     * 获取总记录数
     *
     * */
    int queryTableCount(@Param("primaryKey") String primaryKey , @Param("tableName") String tableName);

}
