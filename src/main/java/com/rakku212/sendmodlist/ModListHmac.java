package com.rakku212.sendmodlist;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class ModListHmac {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static byte[] utf8SignedPayload(byte[] sessionKey, String jsonBodyUtf8) {
        String hex = hmacSha256Hex(sessionKey, jsonBodyUtf8.getBytes(StandardCharsets.UTF_8));
        String signed = hex + '\n' + jsonBodyUtf8;
        return signed.getBytes(StandardCharsets.UTF_8);
    }

    public static String hmacSha256Hex(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return bytesToHex(mac.doFinal(message));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            result[i * 2] = HEX_CHARS[v >>> 4];
            result[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(result);
    }
}
