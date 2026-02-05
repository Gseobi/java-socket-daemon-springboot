package com.github.gseobi.daemon.socket.crypto.util;

import com.github.gseobi.daemon.socket.crypto.constant.OutputFormat;

import java.util.Base64;

public final class OutputEncoder {
    private OutputEncoder() {}

    public static String encode(byte[] data, OutputFormat format) {
        return switch (format) {
            case HEX -> HexUtil.bytesToHex(data);
            case BASE64 -> Base64.getEncoder().encodeToString(data);
        };
    }

    public static byte[] decode(String text, OutputFormat format) {
        return switch (format) {
            case HEX -> HexUtil.hexToBytes(text);
            case BASE64 -> Base64.getDecoder().decode(text);
        };
    }
}
