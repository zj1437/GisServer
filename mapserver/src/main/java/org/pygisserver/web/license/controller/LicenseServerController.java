package org.pygisserver.web.license.controller;

import org.pygisserver.common.entity.ReponseEntity;
import org.pygisserver.common.entity.ReponseStatus;
import org.pygisserver.web.license.LicenseCheckListener;
import org.pygisserver.web.license.LicenseCreator;
import org.pygisserver.web.license.LicenseCreatorParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 用于生成证书文件，不能放在给客户部署的代码里
 * @author zifangsky
 * @date 2018/4/26
 * @since 1.0.0
 */
@RestController
@RequestMapping("/licenseServer")
public class LicenseServerController {
    @Autowired
    LicenseCheckListener licenseCheckListener;

    /**
     * 证书生成路径
     */
    @Value("${license.licensePath}")
    private String licensePath;


    /**
     * 生成证书
     * @author zifangsky
     * @date 2018/4/26 13:13
     * @since 1.0.0
     * @param param 生成证书需要的参数，如：{"subject":"ccx-models","privateAlias":"privateKey","keyPass":"5T7Zz5Y0dJFcqTxvzkH5LDGJJSGMzQ","storePass":"3538cef8e7","licensePath":"C:/Users/zifangsky/Desktop/license.lic","privateKeysStorePath":"C:/Users/zifangsky/Desktop/privateKeys.keystore","issuedTime":"2018-04-26 14:48:12","expiryTime":"2018-12-31 00:00:00","consumerType":"User","consumerAmount":1,"description":"这是证书描述信息","licenseCheckModel":{"ipAddress":["192.168.245.1","10.0.5.22"],"macAddress":["00-50-56-C0-00-01","50-7B-9D-F9-18-41"],"cpuSerial":"BFEBFBFF000406E3","mainBoardSerial":"L1HF65E00X9"}}
     * @return java.util.Map<java.lang.String,java.lang.Object>
     */
    @RequestMapping(value = "/generateLicense",produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ReponseEntity generateLicense(@RequestBody(required = true) LicenseCreatorParam param) {
        ///TODO 只对管理员开放
//        licensePath = "E:/license.lic";

        Map<Object,Object> resultMap = new HashMap<>(2);

        if(StringUtils.isBlank(param.getLicensePath())){
//            licensePath = Thread.currentThread().getContextClassLoader().getResource(param.getLicensePath()).getFile();
                param.setLicensePath(licensePath);
        }

        LicenseCreator licenseCreator = new LicenseCreator(param);
        boolean result = licenseCreator.generateLicense();

        if(result){
            resultMap.put("msg",param);
        }else{
            resultMap.put("msg","证书文件生成失败！");
        }
        return new ReponseEntity(ReponseStatus.SUCCESS.getCode(), ReponseStatus.SUCCESS.getResult(),resultMap);
    }




}
