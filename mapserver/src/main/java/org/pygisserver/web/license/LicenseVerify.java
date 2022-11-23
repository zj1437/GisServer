package org.pygisserver.web.license;

import de.schlichtherle.license.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pygisserver.common.util.CommonUtil;
import org.pygisserver.common.util.CrypotUtil;
import org.pygisserver.common.util.DateUtil;
import org.pygisserver.common.util.FileUtil;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;


/**
 * License校验类
 *
 * @author zifangsky
 * @date 2018/4/20
 * @since 1.0.0
 */
@Component
public class LicenseVerify {
    private static Logger logger = LogManager.getLogger(LicenseVerify.class);

    /**
     * 安装License证书
     * @author zifangsky
     * @date 2018/4/20 16:26
     * @since 1.0.0
     */
    public synchronized LicenseContent install(LicenseVerifyParam param) throws Exception{
        LicenseContent result = null;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //1. 安装证书
            LicenseManager licenseManager = LicenseManagerHolder.getInstance(initLicenseParam(param));
            licenseManager.uninstall();
            File file = new File(param.getLicensePath());
            result = licenseManager.install(file);

        logger.info(MessageFormat.format("证书安装成功，证书有效期：{0} - {1}",format.format(result.getNotBefore()),format.format(result.getNotAfter())));

            //如果操作系统魏Windows 跟新注册表
            String osName = System.getProperty("os.name").toLowerCase();
            //根据不同操作系统类型选择不同的数据获取方法
            String format1 = format.format(new Date());
            if (osName.startsWith("windows")) {
                Date systemExpireTime = CommonUtil.getSystemLastTime();
                if(systemExpireTime==null){
                    CommonUtil.updateSystemLastTime(format1);
                }else {
                    if(systemExpireTime.after(new Date())){
                        throw new Exception("激活异常");
                    }
                }
            }

        if (osName.startsWith("linux")) {
            String path = System.getProperty("user.dir")+"/license.txt";
//            String path = this.getClass().getClassLoader().getResource("/").getPath()+"license.txt";
            File checkFile = new File(path);
            if(!checkFile.exists()){
                FileUtil.createFile(path,CrypotUtil.encrypt(format.format(new Date())));
            }else {
                String s = FileUtil.readTxt(path);
                if(StringUtils.isNotBlank(s)){
                    Date date = DateUtil.covertStringToDateHour(CrypotUtil.decrypt(s));
                    if(date.after(new Date())){
                        logger.error("最新激活时间晚于系统时间");
                    }else {
                        FileUtil.updateFile(path,CrypotUtil.encrypt(format.format(new Date()) ));
                    }
                }
            }
        }

        return result;
    }


    /**
     * 校验License证书
     * @author zifangsky
     * @date 2018/4/20 16:26
     * @since 1.0.0
     * @return boolean
     */
    public boolean verify(){
        LicenseManager licenseManager = LicenseManagerHolder.getInstance(null);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //2. 校验证书
        try {
            LicenseContent licenseContent = licenseManager.verify();
//            System.out.println(licenseContent.getSubject());
            //如果操作系统魏Windows 跟新注册表
            String osName = System.getProperty("os.name").toLowerCase();
            //根据不同操作系统类型选择不同的数据获取方法
            if (osName.startsWith("windows")) {
                //更新注册表最后使用时间
                Date systemExpireTime = CommonUtil.getSystemLastTime();
                if(systemExpireTime.after(new Date())){
                logger.error("windows证书注册表校验失败！");
                return false;
                }else {
//                    CommonUtil.updateSystemLastTime(format.format(new Date()));
                }
            }
            if(osName.startsWith("linux")){
                //更新最后注册使用时间
                String path = System.getProperty("user.dir")+"/license.txt";
//            String path = this.getClass().getClassLoader().getResource("/").getPath()+"license.txt";
                File checkFile = new File(path);
                if(!checkFile.exists()){
                    FileUtil.createFile(path,CrypotUtil.encrypt(format.format(new Date())));
                    return false;
                }else {
                    String s = FileUtil.readTxt(path);
                    if(StringUtils.isNotBlank(s)){
                        Date date = DateUtil.covertStringToDateHour(CrypotUtil.decrypt(s));
                        if(date.after(new Date())){
                            logger.error("linux证书注册时间校验失败");
                            return false;
                        }else {
//                            FileUtil.updateFile(path,CrypotUtil.encrypt(format.format(new Date()) ));
                        }
                    }else {
                        return false;
                    }
                }
            }

            logger.info(MessageFormat.format("证书校验通过，证书有效期：{0} - {1}",format.format(licenseContent.getNotBefore()),format.format(licenseContent.getNotAfter())));
            return true;
        }catch (Exception e){
            logger.error("证书校验失败！",e);
            return false;
        }
    }

    /**
     * 初始化证书生成参数
     * @author zifangsky
     * @date 2018/4/20 10:56
     * @since 1.0.0
     * @param param License校验类需要的参数
     * @return de.schlichtherle.license.LicenseParam
     */
    private LicenseParam initLicenseParam(LicenseVerifyParam param){
        Preferences preferences = Preferences.userNodeForPackage(LicenseVerify.class);

        CipherParam cipherParam = new DefaultCipherParam(param.getStorePass());

        KeyStoreParam publicStoreParam = new CustomKeyStoreParam(LicenseVerify.class
                ,param.getPublicKeysStorePath()
                ,param.getPublicAlias()
                ,param.getStorePass()
                ,null);

        return new DefaultLicenseParam(param.getSubject()
                ,preferences
                ,publicStoreParam
                ,cipherParam);
    }

}
