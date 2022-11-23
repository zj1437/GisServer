package org.pygisserver.web.wmtsprotocol.service;

import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.pytc.entity.TileRequestInfo;

/**
 * @Title : WmtsProtocolService
 * @Description：
 * @Author ：柳强
 * @Date ：Created in 2022/8/2 9:20
 * @Modified By：
 * @Version: $
 * Copyright: Copyright (c) 2019
 * Company: 恒歌科技
 */
public interface WmtsProtocolService {
    /**
     * 获取wmts能力值
     * @param
     */
    byte[] getWmtsCapabilities(String urlPrefix);

    byte[] getWmtsTile(LayerBaseInfo baseInfo, TileRequestInfo tileRequestInfo, String reqIP);
}
