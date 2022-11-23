package org.pygisserver.web.entity;

import lombok.Data;
import org.pygisserver.common.ymlsourcefactory.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Description: OGC服务协议配置类
 * @author: 张亨特
 * @date: 2022-06-14
 */
@Data
@Component
@ConfigurationProperties(prefix = "ogcprotocol")
@PropertySource(value = {"classpath:ogcprotocol/ogcprotocol.yml"}, factory = YamlPropertySourceFactory.class)
public class OgcProtocol {
    private List<String> protocol;

    private List<String> wmtsInterface;

    private Map<String, Map<String, List<String>>> wmtsParameter;

    private List<String> wmsInterface;

    private Map<String, Map<String, List<String>>> wmsParameter;

    private List<String> wfsInterface;

    private Map<String, Map<String, List<String>>> wfsParameter;

    private List<String> wcsInterface;

    private Map<String, Map<String, List<String>>> wcsParameter;

    private  List<String> eColfFilter;

    private  Map<String, Map<String, String>> eColGeom;
}
