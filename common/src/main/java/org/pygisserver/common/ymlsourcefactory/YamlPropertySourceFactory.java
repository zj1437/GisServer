package org.pygisserver.common.ymlsourcefactory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * 读取yml文件
 *
 * @author 张亨特
 * @since 2021-12-15
 */
@Slf4j
public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        Properties properties = loadYamlIntoPerperties(resource);
        String sourceName = StringUtils.isNotBlank(name) ? name : resource.getResource().getFilename();
        return new PropertiesPropertySource(sourceName, properties);

    }

    private Properties loadYamlIntoPerperties(EncodedResource resource) throws FileNotFoundException {
        try {
            YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
            factoryBean.setResources(resource.getResource());
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause();
            if (cause instanceof FileNotFoundException) {
                log.error("读取配置文件异常{}", e.getMessage());
                throw (FileNotFoundException)e.getCause();
            }
        }
        return null;
    }
}
