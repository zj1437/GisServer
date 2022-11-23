package org.pygisserver.terrainanalysis.entity;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class TilePixelEntity implements Serializable {


    /**
     * 瓦片所在行数
     * */
    private int tileRow;


    /**
     * 瓦片所在列数
     * */
    private int tileCol;


    /**
     * 瓦片顶点(左上角)x轴坐标
     * */
    private double tileX;


    /**
     * 瓦片顶点y轴坐标
     * */
    private double tileY;


    /**
     * 像素点二维坐标x
     * */
    private int x_pixel;


    /**
     * 像素点二维坐标y
     * */
    private int y_pixel;


    /**
     * 单个像素点在x轴变化量
     * */
    private double x_singlePixel;


    /**
     * 单个像素点在y轴变化量
     * */
    private double y_singlePixel;


    /**
     * minX(4326/3857整张图层坐标范围，不是bbox)
     * */
    private double minX;


    /**
     * maxX
     * */
    private double maxX;


    /**
     * minY
     * */
    private double minY;


    /**
     * maxY
     * */
    private double maxY;


    /**
     * 当前瓦片所在级别
     * */
    private int zoom;
}
