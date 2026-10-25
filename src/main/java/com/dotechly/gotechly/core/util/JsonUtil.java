package com.dotechly.gotechly.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {
    }

    public static String mapToJson(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error converting map to JSON string", e);
            return null;
        }
    }

    public static Map<String, String> jsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON string to map", e);
            return new HashMap<>();
        }
    }

    public static String addField(String json, String key, String value) {
        Map<String, String> map = jsonToMap(json);
        map.put(key, value);
        return mapToJson(map);
    }

    public static String removeField(String json, String key) {
        Map<String, String> map = jsonToMap(json);
        map.remove(key);
        return mapToJson(map);
    }

    public static String getField(String json, String key) {
        Map<String, String> map = jsonToMap(json);
        return map.get(key);
    }
}
