package com.github.gseobi.daemon.socket.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Slf4j
final class AesCrypto {

    private static final String AES = "AES";
    private static final String AES_ECB = "AES/ECB/PKCS5Padding";
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";

    private AesCrypto() {}

    static byte[] encrypt(String plainText, String key, String iv) throws Exception {
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    static String decrypt(byte[] encryptedBytes, String key, String iv) throws Exception {
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = cipher.doFinal(encryptedBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static Cipher initCipher(int mode, String key, String iv) throws Exception {
        String transformation = (iv == null || iv.isBlank()) ? AES_ECB : AES_CBC;

        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
        Cipher cipher = Cipher.getInstance(transformation);

        if (transformation.equals(AES_ECB)) {
            cipher.init(mode, keySpec);
        } else {
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            cipher.init(mode, keySpec, ivSpec);
        }
        return cipher;
    }
}
