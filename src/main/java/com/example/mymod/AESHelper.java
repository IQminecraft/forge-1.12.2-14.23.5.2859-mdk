package com.example.mymod;

import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESHelper {
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";

    public AESHelper() {
    }

    public static byte[] Decrypt(byte[] data, String aesStr) {
        if (null == data) {
            return null;
        } else {
            byte[] keybyte = aesStr.getBytes();
            if (keybyte != null && keybyte.length == 32) {
                byte[] bytePre = Arrays.copyOfRange(keybyte, 0, 16);
                byte[] bytePost = Arrays.copyOfRange(keybyte, 16, 32);
                SecretKey key = new SecretKeySpec(bytePre, "AES");
                IvParameterSpec ivparameter = new IvParameterSpec(bytePost);
                byte[] byte_decode = null;

                try {
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                    cipher.init(2, key, ivparameter);
                    byte_decode = cipher.doFinal(Arrays.copyOfRange(data, 0, data.length));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return byte_decode;
            } else {
                return data;
            }
        }
    }
}
