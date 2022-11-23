package org.pygisserver.render.service;

import java.util.Map;

import org.pygisserver.common.entity.TDBDataConfig;
import org.pygisserver.common.entity.TDBDataConfigOld;
import org.pygisserver.common.pytc.entity.TileResourceInfo;

/**
 * @Description: 自定义数据的加载 
 * @author: 刘洋 
 * @date: 2020-01-14
 */
public interface CustomDataLoad {
    
    /**
     * 存入瓦片信息
     * @param tileResourceInfo 行
     * @param config 列
     * @param tdbParams 级别
     */
    public void insertTileImage(TileResourceInfo tileResourceInfo, TDBDataConfig config, Map<String,String> tdbParams, String rowNum);
    
    /**
          * 获取当前tdb数据的配置信息
     * @param filepath
     * @return
     */
    public TDBDataConfigOld analysisOldTdbConfig(String filepath,String serverName);
    
    /**
       * 获取当前tdb数据的配置信息
    * @param filepath
    * @return
    */
    public TDBDataConfig analysisNewTdbConfig(String filepath,String serverName);

    /**读取tdb影像数据接口
    * @param tileRow 行
    * @param tileCol 列
    * @param zoomLevel 界别
    * @return 返回图像的字节数组
    */
    public byte[] getImageInfo(int tileRow, int tileCol, int zoomLevel,String dataPath,String serverName);
    
    /**
     * tdb高程接口
     * @param tileRow 行
     * @param tileCol 列
     * @param zoomLevel 界别
     * @return 返回高程数据
     */
     public byte[] getElvationInfo(int tileRow, int tileCol, int zoomLevel,String dataPath,String serverName);
}
