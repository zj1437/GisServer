package org.pygisserver.common.entity;

import lombok.Data;

import java.io.Serializable;

//网络分析接收请求参数实体
@Data
public class BufferAnalysisParameter implements Serializable {


	/**
     * 几何图形
     */
    private String geometry;



    /**
     * 检索半径（圆形区域或者矩形，线段的缓冲半径）
     */
    private double distance;


    /**
     * 缓冲描述参数quad_segs：四分之一圆的线段数,默认为8
     */
    private String quadSegs;


    /**
     * 缓冲描述参数endcap：endcap样式,默认为“round”
     */
    private String endCap;


    /**
     * 缓冲描述参数join：连接样式,默认为“round”
     */
    private String join;


    /**
     * 缓冲描述参数mitre_limit：斜接比率限制
     * 非空要判断，无默认值
     */
//    private String mitreLimit;


    /**
     * 缓冲描述参数side：“左”或“右”在几何图形上执行单面缓冲，缓冲的一面相对于线的方向。
     * 0代表左，1代表右,-1代表双面
     * 
     */
    private int side;


    /**
     * 输入图层名称
     */
    private String inputLayerName;

    /**
     * 输入文件的url，查询的时候传递给前端、
     * 前端在调用接口的时候传递回来
     */
    private String inputFileUrl;

    /**
     * 默认安装目录下tomcat下的temp下
     */
    private String outPutPath;

    /**
     * 是否自动发布（1  发布  0  不发布）
     */
    private int autoPublish;


    public BufferAnalysisParameter(String geometry, double distance, String quadSegs, String endCap, String join, int side) {
    	this.geometry = geometry;
        this.distance = distance;
        this.quadSegs = quadSegs;
        this.endCap = endCap;
        this.join = join;
        this.side = side;
    }

    public BufferAnalysisParameter() {
    }
}
