package org.pygisserver.common.entity;

import lombok.Data;

@Data
public class TdbHeadEntity {
	//数据版本号  	4char=4byte
    private String dwVersionNo;
    //srs 20char=20byte
    private String srsType;
    //瓦片的长
    private int nTileSizeX;
    //瓦片的宽
    private int nTileSizeY;
    //X方向上的块数
    private int nBlkNumX;
    //Y方向上的块数
    private int nBlkNumY;
    //最小层级
    private int nLodMin;
    //最大层级
    private int nLodMax;
    
    public TdbHeadEntity() {
    }
}
