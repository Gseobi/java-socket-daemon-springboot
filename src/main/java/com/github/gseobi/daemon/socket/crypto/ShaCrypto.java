package com.github.gseobi.daemon.socket.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class ShaCrypto {

    private ShaCrypto() {}

    static byte[] sha256(String plainText) throws Exception {
        return digest("SHA-256", plainText);
    }

    static byte[] sha512(String plainText) throws Exception {
        return digest("SHA-512", plainText);
    }

    private static byte[] digest(String algorithm, String plainText) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        return md.digest(plainText.getBytes(StandardCharsets.UTF_8));
    }
}
