package org.pygisserver.web.tmsprotocol.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.pygisserver.common.entity.LayerBaseInfo;
import org.pygisserver.common.pytc.entity.TileRequestInfo;
import org.pygisserver.web.wmtsprotocol.service.WmtsProtocolService;
import org.pygisserver.web.tmsprotocol.service.TmsProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Title : TmsProtocolServiceImpl
 * @Description：
 * @Author ：柳强
 * @Date ：Created in 2022/8/2 10:03
 * @Modified By：
 * @Version: $
 * Copyright: Copyright (c) 2019
 * Company: 恒歌科技
 */
@Service
@Slf4j
public class TmsProtocolServiceImpl implements TmsProtocolService {
    @Autowired
    private TmsGetCapabities tmsGetCapabities;

    @Autowired
    private WmtsProtocolService wmtsProtocolService;

    @Override
    public byte[] getTmsCapabilities(String layerInfo,String baseUrl) {
        // 移植tileserver中tms的getcapabilities代码
        return tmsGetCapabities.getCapabitiesByte(layerInfo,baseUrl);
    }

    @Override
    public byte[] getTmsTile(LayerBaseInfo baseInfo, TileRequestInfo tileRequestInfo, String reqIP) {

        return wmtsProtocolService.getWmtsTile(baseInfo, tileRequestInfo, reqIP);
    }
}
