package org.pygisserver.web.restprotocol.service;

import com.alibaba.fastjson.JSONArray;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.web.restprotocol.entity.RestRequestParam;
import org.springframework.stereotype.Service;

/**
 * @Description: wms协议service
 * @author: 张亨特
 * @date: 2020/07/19
 */
@Service
public interface RestProtocolService {

    ReponseEntity getRestFeatureInfo(RestRequestParam requestParam);
}
