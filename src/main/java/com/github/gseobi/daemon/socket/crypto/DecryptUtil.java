package com.github.gseobi.daemon.socket.crypto;

import com.github.gseobi.daemon.socket.crypto.constant.EncryptionType;
import com.github.gseobi.daemon.socket.crypto.constant.OutputFormat;
import com.github.gseobi.daemon.socket.crypto.util.HexUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

@Slf4j
public class DecryptUtil {

    public static String decrypt(
            String cipherText,
            EncryptionType encType,
            String key,
            String iv
    ) throws Exception {

        if (cipherText == null || encType == null) return null;

        if (!(encType == EncryptionType.AES_128 || encType == EncryptionType.AES_192 || encType == EncryptionType.AES_256)) {
            throw new IllegalArgumentException("Decrypt is only supported for AES types.");
        }

        byte[] decoded;
        try {
            if (HexUtil.isHexString(cipherText)) decoded = HexUtil.hexToBytes(cipherText);
            else decoded = Base64.getDecoder().decode(cipherText);
        } catch (Exception e) {
            log.warn("CipherText format parse failed. Trying Base64 decode fallback...");
            decoded = Base64.getDecoder().decode(cipherText);
        }

        return AesCrypto.decrypt(decoded, key, iv);
    }
}
