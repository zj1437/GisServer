package org.pygisserver.common.util;

import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.jasypt.util.text.BasicTextEncryptor;

public class JasyptEncryptorUtils {

	private static final String salt = "pygissoft";
	 
    private static BasicTextEncryptor basicTextEncryptor = new BasicTextEncryptor();
 
    static {
        basicTextEncryptor.setPassword(salt);
    }
 
    private JasyptEncryptorUtils(){}
 
    /**
     * 明文加密
     * @param plaintext
     * @return
     */
    public static String encode(String plaintext){
        String ciphertext = basicTextEncryptor.encrypt(plaintext);
        return ciphertext;
    }
 
    /**
     * 解密
     * @param ciphertext
     * @return
     */
    public static String decode(String ciphertext){
        ciphertext = "ENC(" + ciphertext + ")";
        if (PropertyValueEncryptionUtils.isEncryptedValue(ciphertext)){
            String plaintext = PropertyValueEncryptionUtils.decrypt(ciphertext,basicTextEncryptor);
            return plaintext;
        }
        return "";
    }
}
