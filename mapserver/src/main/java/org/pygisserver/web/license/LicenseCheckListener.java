package org.pygisserver.web.license;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * 在项目启动时安装证书
 *
 * @author zifangsky
 * @date 2018/4/24
 * @since 1.0.0
 */
@Component
@Data
public class LicenseCheckListener implements ApplicationListener<ContextRefreshedEvent> {
    private static Logger logger = LogManager.getLogger(LicenseCheckListener.class);

    /**
     * 证书subject
     */
    @Value("${license.subject}")
    private String subject;

    /**
     * 公钥别称
     */
    @Value("${license.publicAlias}")
    private String publicAlias;

    /**storePass
     * 访问公钥库的密码
     */
    @Value("${license.storePass}")
    private String storePass;

    /**
     * 证书生成路径
     */
    @Value("${license.licensePath}")
    private String licensePath;

    /**
     * 密钥库存储路径
     */
    @Value("${license.publicKeysStorePath}")
    private String publicKeysStorePath;

    /**
     * 替换存储路径
     */
    @Value("${license.replacePath}")
    private String replacePath;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        //root application context 没有parent
        ApplicationContext context = event.getApplicationContext().getParent();
        if(context == null){
            if(StringUtils.isNotBlank(licensePath)){
                logger.info("++++++++ 开始安装证书 ++++++++");


                LicenseVerifyParam param = new LicenseVerifyParam();
                param.setSubject(subject);
                param.setPublicAlias(publicAlias);
                param.setStorePass(storePass);
                String licensePathNew = Thread.currentThread().getContextClassLoader().getResource(licensePath).getFile();
                licensePathNew = licensePathNew.replaceAll(replacePath,"");
//                licensePathNew = licensePathNew.replaceFirst("/", "");
                String publicKeysStorePathNew = Thread.currentThread().getContextClassLoader().getResource(publicKeysStorePath).getFile();
//                publicKeysStorePathNew = publicKeysStorePathNew.replaceFirst("/", "");
                publicKeysStorePathNew = publicKeysStorePathNew.replaceAll(replacePath,"");
                param.setLicensePath(licensePathNew);
                param.setPublicKeysStorePath(publicKeysStorePathNew);

                LicenseVerify licenseVerify = new LicenseVerify();
                //安装证书
                try {
                    licenseVerify.install(param);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                logger.info("++++++++ 证书安装结束 ++++++++");
            }
        }
    }


}
