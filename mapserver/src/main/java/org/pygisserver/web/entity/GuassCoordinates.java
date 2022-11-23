package org.pygisserver.web.entity;

import lombok.Data;
import org.pygisserver.common.ymlsourcefactory.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 高斯投影坐标配置文件实体类
 *
 * @author 张亨特
 * @since 2021-12-15
 */

@Data
@Component
@ConfigurationProperties(prefix = "guasscoordinates")
@PropertySource(value = {"classpath:tilecoordinates/guasscoordinates.yml"}, factory = YamlPropertySourceFactory.class)
public class GuassCoordinates {
    private Map<String, Map<String, List<String>>> tileParam;

    private List<String> guassCoordinates;
}
