package org.pygisserver.web.tmsprotocol.service;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.pytc.entity.TileRequestInfo;

/**
 * @Title : TmsProtocolService
 * @Description：
 * @Author ：柳强
 * @Date ：Created in 2022/8/2 10:02
 * @Modified By：
 * @Version: $
 * Copyright: Copyright (c) 2019
 * Company: 恒歌科技
 */
public interface TmsProtocolService {
    /**
     * 获取tms能力值
     * @param
     */
    byte[] getTmsCapabilities(String layerInfo,String baseUrl);

    byte[] getTmsTile(LayerBaseInfo baseInfo, TileRequestInfo tileRequestInfo, String reqIP);
}
