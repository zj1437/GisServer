package org.pygisserver.web.restprotocol.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.checkerframework.checker.units.qual.A;
import org.pygisserver.common.entity.PostGisConnectEntity;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.mapper.MapServerDataMapper;
import org.pygisserver.web.dao.MapServerMapper;
import org.pygisserver.web.entity.DataServerEntity;
import org.pygisserver.web.entity.RestServerInfoEntity;
import org.pygisserver.web.restprotocol.entity.RestRequestParam;
import org.pygisserver.web.restprotocol.service.RestProtocolService;
import org.pygisserver.web.service.MapServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class RestProtocolServiceImpl implements RestProtocolService {
    @Autowired
    MapServerDataMapper mapServerDataMapper;
    @Resource
    MapServerMapper mapServerMapper;
    @Autowired
    MapServerService mapServerService;
    @Override
    public ReponseEntity getRestFeatureInfo(RestRequestParam requestParam) {
        ReponseEntity reponseEntity = new ReponseEntity(200,"",null);
        Connection c = null;
        Connection c1 = null;
        Statement stmt = null;
        Statement stmt1 = null;
        try {

            String restServerName = requestParam.getRestServerName();
            boolean flag = mapServerService.ispublisedRest(restServerName);
            if(!flag){
                reponseEntity.setMsg("未找到该服务");
                return reponseEntity;
            }
            DataServerEntity dataServerEntity = mapServerMapper.queryRestInfoByServerName(restServerName);
            if(dataServerEntity!=null){
                if(dataServerEntity.getStatus()!=1){
                    reponseEntity.setMsg("已暂停该服务");
                    return reponseEntity;
                }
            }
            String fileUrl = dataServerEntity.getDataUrl();
            String[] split = fileUrl.split(";");
            String sourceName = split[0];
            String restName = split[1];
            PostGisConnectEntity postGisConnectEntity = mapServerDataMapper.queryPostGisBySourceName(sourceName);
            c = this.connection(postGisConnectEntity);
            c1 = this.connection(postGisConnectEntity);
            stmt = c.createStatement();
            stmt1 = c1.createStatement();
            stmt.setFetchSize(100000);
            stmt1.setFetchSize(100000);
            StringBuilder sb = new StringBuilder();
            sb.append("select * from "+restName+"");
            //查询总数
            ResultSet cs = stmt1.executeQuery("select  count(1) from "+restName);

            int limit = requestParam.getPageSize();
            Integer pageNum = requestParam.getPageNum();
            if(pageNum>=1){
                int offset = (requestParam.getPageNum()-1)*limit;
                sb.append(" limit  "+limit+"  offset  "+offset+"");
            }
            //查询sql
            ResultSet rs = stmt.executeQuery(sb.toString());
            Integer resultGetCount = resultGetCount(cs);
            System.out.println("查询数据成功！");
            JSONArray jsonArray = resultSetToJson(rs);
            ///todo
            rs.close();
            cs.close();
            stmt.close();
            stmt1.close();
            c.close();
            c1.close();
            reponseEntity.setMsg("查询成功");
            reponseEntity.setItems(jsonArray);
            reponseEntity.setTotalNum(resultGetCount);
            return reponseEntity;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

    /**
     * 获取数据库链接
     * @param postGisConnectEntity
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public Connection connection(PostGisConnectEntity postGisConnectEntity) throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        Connection c = DriverManager.getConnection(
                "jdbc:postgresql://"+postGisConnectEntity.getDatabaseIp()+":"+postGisConnectEntity.getPort()+"/"+ postGisConnectEntity.getDatabaseName()+"", ""+postGisConnectEntity.getUserName()+"",
                ""+new String(Base64.getDecoder().decode(postGisConnectEntity.getPassword()))+"");
        c.setAutoCommit(false);
        System.out.println("连接数据库成功！");
        return c;
    }


    /**
     * 结果转jsonArray
     * @param rs
     * @return
     * @throws SQLException
     * @throws JSONException
     */
    public JSONArray resultSetToJson(ResultSet rs) throws SQLException, JSONException {
        // json数组
        JSONArray array = new JSONArray();

        // 获取列数
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 遍历ResultSet中的每条数据
        while (rs.next()) {
            JSONObject jsonObj = new JSONObject();

            // 遍历每一列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                String value = rs.getString(columnName);
                jsonObj.put(columnName, value);
            }
            array.add(jsonObj);
        }

        return array;
    }

    /**
     * 获取总量
     * @param rs
     * @return
     * @throws SQLException
     * @throws JSONException
     */
    public Integer resultGetCount(ResultSet rs) throws SQLException, JSONException {
        // 获取列数
        ResultSetMetaData metaData = rs.getMetaData();
        Integer count=0;
        while (rs.next()) {
            String columnName = metaData.getColumnLabel(1);
            count = rs.getInt(columnName);
        }
        return  count;


    }

}
