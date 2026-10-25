package com.dotechly.gotechly.core.domain;

import com.dotechly.gotechly.core.util.JsonUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, String>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        return JsonUtil.mapToJson(attribute);
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        return JsonUtil.jsonToMap(dbData);
    }
}
