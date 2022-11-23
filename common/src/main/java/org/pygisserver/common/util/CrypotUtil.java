package org.pygisserver.common.util;

import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.pygisserver.common.exception.ExceptionsUtil;
import org.pygisserver.common.exception.ResourceException;

import lombok.extern.slf4j.Slf4j;

/**
 * 加密处理工具类
 * @author 
 *
 */
@Slf4j
public class CrypotUtil {

    public static Key DEFAULT_KEY = null;
    public static final String DEFAULT_SECRET_KEY1 = "?:P)(OL><KI*&UJMNHY^%TGBVFR$#EDCXSW@!QAZ";
    public static final String DEFAULT_SECRET_KEY2 = "b2894177167db0192222561aa2dbf1";
    public static final String DEFAULT_SECRET_KEY3 = "3bca5b9f6d50a734a66a21a99864e4";
    public static final String DEFAULT_SECRET_KEY4 = "1qaz@WSX3edc$RFV5tgb^YHN7ujm*IK<9ol.)P:?";
    public static final String DEFAULT_SECRET_KEY5 = "!QAZ2wsx#EDC4rfv%TGB6yhn&UJM8ik,(OL>0p;/";
    public static final String DEFAULT_SECRET_KEY6 = "1qaz2wsx3edc4rfv5tgb^YHN&UJM*IK<(OL>)P:?";
    public static final String DEFAULT_SECRET_KEY = DEFAULT_SECRET_KEY1;

    public static final String DES = "DES";
    
    public static final String SHA1PRNG = "SHA1PRNG";

    public static final Base32 base32 = new Base32();

    static {
        DEFAULT_KEY = obtainKey(DEFAULT_SECRET_KEY);
    }

    /**
     * 获得key
     **/
    public static Key obtainKey(String key) {
        if (key == null) {
            return DEFAULT_KEY;
        }
        KeyGenerator generator = null;
        SecureRandom secureRandom = null;
        try {
        	generator = KeyGenerator.getInstance(DES);
        	if(StringUtils.isNoneBlank(key)) {
                secureRandom = SecureRandom.getInstance(SHA1PRNG);
                secureRandom.setSeed(key.getBytes("UTF-8"));
        	} else {
        		secureRandom = SecureRandom.getInstance(SHA1PRNG);
        	}
        } catch (Exception e) {
        	log.error("CrypotUtil obtainKey has error:" + ExceptionsUtil.stackTraceToStr(e));
        	throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_GET, key));
        }
        generator.init(secureRandom);
        Key key1 = generator.generateKey();
//        generator = null;
        return key1;
    }

    /**
     * 加密<br>
     * String明文输入,String密文输出
     */
    public static String encode(String str) {
        return encode64(null, str);
    }

    /**
     * 加密<br>
     * String明文输入,String密文输出
     */
    public static String encode64(String key, String str) {
        return Base64.encodeBase64URLSafeString(obtainEncode(key, str.getBytes()));
    }

    /**
     * 加密<br>
     * String明文输入,String密文输出
     */
    public static String encode32(String key, String str) {
        return base32.encodeAsString(obtainEncode(key, str.getBytes())).replaceAll("=", "");
    }

    /**
     * 加密<br>
     * String明文输入,String密文输出
     */
    public static String encode16(String key, String str) {
        return Hex.encodeHexString(obtainEncode(key, str.getBytes()));
    }

    /**
     * 解密<br>
     * 以String密文输入,String明文输出
     */
    public static String decode(String str) {
        return decode64(null, str);
    }

    /**
     * 解密<br>
     * 以String密文输入,String明文输出
     */
    public static String decode64(String key, String str) {
        return new String(obtainDecode(key, Base64.decodeBase64(str)));
    }

    /**
     * 解密<br>
     * 以String密文输入,String明文输出
     */
    public static String decode32(String key, String str) {
        return new String(obtainDecode(key, base32.decode(str)));
    }

    /**
     * 解密<br>
     * 以String密文输入,String明文输出
     */
    public static String decode16(String key, String str) {
        try {
            return new String(obtainDecode(key, Hex.decodeHex(str.toCharArray())));
        } catch (DecoderException e) {
        	log.error("CrypotUtil decode16 has error:" + ExceptionsUtil.stackTraceToStr(e));
        	throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SECRET, "解密"));
        }
    }

    /**
     * 加密<br>
     * 以byte[]明文输入,byte[]密文输出
     */
    private static byte[] obtainEncode(String key, byte[] str) {
        byte[] byteFina = null;
        Cipher cipher;
        try {
            Key key1 = obtainKey(key);
            cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.ENCRYPT_MODE, key1);
            byteFina = cipher.doFinal(str);
        } catch (Exception e) {
        	log.error("CrypotUtil obtainEncode has error:" + ExceptionsUtil.stackTraceToStr(e));
        	throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SECRET, "加密"));
        } finally {
            cipher = null;
        }
        return byteFina;
    }

    /**
     * 解密<br>
     * 以byte[]密文输入,以byte[]明文输出
     */
    private static byte[] obtainDecode(String key, byte[] str) {
        Cipher cipher;
        byte[] byteFina = null;
        try {
            Key key1 = obtainKey(key);
            cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.DECRYPT_MODE, key1);
            byteFina = cipher.doFinal(str);
        } catch (Exception e) {
        	log.error("CrypotUtil obtainDecode has error:" + ExceptionsUtil.stackTraceToStr(e));
        	throw new ResourceException(ExceptionsUtil.ERROR_500,ExceptionsUtil.formatInfo(ExceptionsUtil.RESOURCE_SECRET, "解密"));
        } finally {
            cipher = null;
        }
        return byteFina;
    }

    /**
     * 加密
     * @param val
     * @return
     */
    public static String encrypt(String val){
        String m = encode64(DEFAULT_SECRET_KEY2, val);
        String m1 = encode32(DEFAULT_SECRET_KEY3, m);
        return m1;
    }

    /**
     * 解密
     * @param val
     * @return
     */
    public static String decrypt(String val){
        String n = decode32(DEFAULT_SECRET_KEY3, val);
        String n1 = decode64(DEFAULT_SECRET_KEY2, n);
        return n1;
    }
}
