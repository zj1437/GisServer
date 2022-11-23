package org.pygisserver.common.entity;

import lombok.Data;

@Data
public class TdbHeadEntityOld {
	//数据版本号
    private int dwVersionNo;
    //X方向上的块数，单个文件最大限值是400
    private int nBlkNumX;
    //Y方向上的块数，单个文件最大限值是400
    private int nBlkNumY;
    //是否是墨卡托
    private boolean  bIsMercator;
    //瓦片的长
    private int nTileSizeX;
    //瓦片的宽
    private int nTileSizeY;
    //最小层级
    private int nMinZmlevel;
    //最大层级
    private int nMaxZmlevel;
    
    public TdbHeadEntityOld() {
    }
    
    public TdbHeadEntityOld(int dwVersionNo,int nBlkNumX,int nBlkNumY,boolean  bIsMercator,int nTileSizeX
    		,int nTileSizeY,int nMinZmlevel,int nMaxZmlevel) {
    	this.dwVersionNo = dwVersionNo;
    	this.nBlkNumX = nBlkNumX;
    	this.nBlkNumY = nBlkNumY;
    	this.bIsMercator = bIsMercator;
    	this.nTileSizeX = nTileSizeX;
    	this.nTileSizeY = nTileSizeY;
    	this.nMinZmlevel = nMinZmlevel;
    	this.nMaxZmlevel = nMaxZmlevel;
    }
}
