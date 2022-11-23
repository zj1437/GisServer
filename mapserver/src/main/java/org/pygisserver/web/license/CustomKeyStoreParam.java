package org.pygisserver.web.license;

import de.schlichtherle.license.AbstractKeyStoreParam;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * 自定义KeyStoreParam，用于将公私钥存储文件存放到其他磁盘位置而不是项目中
 *
 * @author zifangsky
 * @date 2018/4/26
 * @since 1.0.0
 */
@Component
public class CustomKeyStoreParam extends AbstractKeyStoreParam {

    /**
     * 公钥/私钥在磁盘上的存储路径
     */
    private String storePath;
    private String alias;
    private String storePwd;
    private String keyPwd;

    private CustomKeyStoreParam(){
        //需要无参构造 参数有问题
        super(LicenseVerify.class, "D:");

    }

    public CustomKeyStoreParam(Class clazz, String resource,String alias,String storePwd,String keyPwd) {
        super(clazz, resource);
        this.storePath = resource;
        this.alias = alias;
        this.storePwd = storePwd;
        this.keyPwd = keyPwd;
    }


    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getStorePwd() {
        return storePwd;
    }

    @Override
    public String getKeyPwd() {
        return keyPwd;
    }

    /**
     * 复写de.schlichtherle.license.AbstractKeyStoreParam的getStream()方法<br/>
     * 用于将公私钥存储文件存放到其他磁盘位置而不是项目中
     * @author zifangsky
     * @date 2018/4/26 18:28
     * @since 1.0.0
     * @param
     * @return java.io.InputStream
     */
    @Override
    public InputStream getStream() throws IOException {
        final InputStream in = new FileInputStream(new File(storePath));
        if (null == in){
            throw new FileNotFoundException(storePath);
        }

        return in;
    }
}
