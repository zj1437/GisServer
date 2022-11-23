package org.pygisserver.web.wmsprotocol.service;

import org.pygisserver.web.wmsprotocol.entity.WmsRequestParam;

/**
 * @Description: wms协议service
 * @author: 张亨特
 * @date: 2020/07/19
 */
public interface WmsProtocolService {

    byte[] getWmsCapabilities(String baseUrl, String version);
    
    byte[] getWmsLegendGraphic(WmsRequestParam requestParam, String reqIP);

    byte[] getWmsImageByte(WmsRequestParam requestParam, String reqIP);

    void getWmsFeatureInfo(WmsRequestParam requestParam);
}
