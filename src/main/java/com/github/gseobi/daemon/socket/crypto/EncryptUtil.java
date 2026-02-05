package com.github.gseobi.daemon.socket.crypto;

import com.github.gseobi.daemon.socket.crypto.constant.EncryptionType;
import com.github.gseobi.daemon.socket.crypto.constant.OutputFormat;
import com.github.gseobi.daemon.socket.crypto.util.OutputEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EncryptUtil {

    public static String encrypt(
            String plainText,
            EncryptionType encType,
            String key,
            String iv,
            OutputFormat outFormat
    ) throws Exception {

        if (plainText == null || encType == null) return null;

        byte[] resultBytes = switch (encType) {
            case AES_128, AES_192, AES_256 -> AesCrypto.encrypt(plainText, key, iv);
            case SHA_256 -> ShaCrypto.sha256(plainText);
            case SHA_512 -> ShaCrypto.sha512(plainText);
        };

        return OutputEncoder.encode(resultBytes, outFormat);
    }
}
