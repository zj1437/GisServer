package org.pygisserver.web.license.controller;

import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.web.license.*;
import org.pygisserver.web.license.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * License在线激活
 */
@RestController
@RequestMapping("/license")
public class LicenseClientController {

    @Autowired
    LicenseCheckListener licenseCheckListener;

    /**
     * 证书生成路径
     */
    @Value("${license.licensePath}")
    private String licensePath;

    /**
     * 替换存储路径
     */
    @Value("${license.replacePath}")
    private String replacePath;

    /**
     * 获取服务器硬件信息
     * @author zifangsky
     * @date 2018/4/26 13:13
     * @since 1.0.0
     * @param osName 操作系统类型，如果为空则自动判断
     * @return com.ccx.models.license.LicenseCheckModel
     */
    @RequestMapping(value = "/getServerInfos",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ReponseEntity<LicenseCheckModel> getServerInfos(@RequestParam(value = "osName",required = false) String osName) {
        //操作系统类型

        if(StringUtils.isBlank(osName)){
            osName = System.getProperty("os.name");
        }
        osName = osName.toLowerCase();

        AbstractServerInfos abstractServerInfos = null;

        //根据不同操作系统类型选择不同的数据获取方法
        if (osName.startsWith("windows")) {
            abstractServerInfos = new WindowsServerInfos();
        } else if (osName.startsWith("linux")) {
            abstractServerInfos = new LinuxServerInfos();
        }else{//其他服务器类型
            abstractServerInfos = new LinuxServerInfos();
        }
        return new ReponseEntity<>(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),abstractServerInfos.getServerInfos());
    }


    /**
     * 在线证书激活
     */
    @RequestMapping(value = "/activateLicense",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ReponseEntity<String> activateLicense(HttpServletRequest request, HttpServletResponse response) {
        try {
            LicenseVerifyParam param = new LicenseVerifyParam();
            param.setSubject(licenseCheckListener.getSubject());
            param.setPublicAlias(licenseCheckListener.getPublicAlias());
            param.setStorePass(licenseCheckListener.getStorePass());
            String licensePath =  Thread.currentThread().getContextClassLoader().getResource(licenseCheckListener.getLicensePath()).getFile();
            licensePath = licensePath.replaceAll(replacePath,"");
            param.setLicensePath(licensePath);
            param.setPublicKeysStorePath( Thread.currentThread().getContextClassLoader().getResource(licenseCheckListener.getPublicKeysStorePath()).getFile().replaceAll(replacePath,""));
            LicenseVerify licenseVerify = new LicenseVerify();
            //安装证书
            licenseVerify.install(param);
            return new ReponseEntity(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),"证书安装结束");
        } catch (Exception e) {
            e.printStackTrace();
            return new ReponseEntity(ReponseStatus.SUCCESS.getCode(), ReponseStatus.ERROR.getResult(),"证书安装失败");
        }

    }
}
