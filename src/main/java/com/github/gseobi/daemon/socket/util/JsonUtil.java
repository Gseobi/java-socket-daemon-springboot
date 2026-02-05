package com.github.gseobi.daemon.socket.util;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@Slf4j
public final class JsonUtil {

    private JsonUtil() {}

    public static JSONObject parseObject(String json) {
        if (json == null || json.isBlank()) return new JSONObject();
        try {
            return (JSONObject) new JSONParser().parse(json);
        } catch (Exception e) {
            log.warn("[JsonUtil] Failed to parse JSONObject. json={}", safeShort(json), e);
            return new JSONObject();
        }
    }

    public static JSONArray parseArray(String json) {
        if (json == null || json.isBlank()) return new JSONArray();
        try {
            return (JSONArray) new JSONParser().parse(json);
        } catch (Exception e) {
            log.warn("[JsonUtil] Failed to parse JSONArray. json={}", safeShort(json), e);
            return new JSONArray();
        }
    }

    public static String getString(JSONObject obj, String key, String defaultValue) {
        if (obj == null || key == null) return defaultValue;
        try {
            Object v = obj.get(key);
            return (v == null) ? defaultValue : String.valueOf(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String safeShort(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
