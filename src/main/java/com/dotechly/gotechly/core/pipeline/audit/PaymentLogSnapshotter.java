package com.dotechly.gotechly.core.pipeline.audit;

import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Utility for creating JSON snapshots of {@link PaymentLogSPI} and computing
 * field-level diffs between two snapshots. Used by the audit pipeline listener
 * to detect what each stage changed.
 */
@Component
public class PaymentLogSnapshotter {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentLogSnapshotter.class);

    private final ObjectMapper objectMapper;

    public PaymentLogSnapshotter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Serialize a PaymentLogSPI to a JSON string for snapshotting.
     * Returns "{}" on serialization failure (audit should never halt the pipeline).
     */
    public String snapshot(PaymentLogSPI request) {
        if (request == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to snapshot PaymentLogSPI: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Compute the field-level diff between two JSON snapshots.
     * Returns a map of dotted field paths to {@link FieldChange} (old value, new value).
     * Only fields that actually changed are included.
     */
    public Map<String, FieldChange> computeDiff(String beforeJson, String afterJson) {
        Map<String, FieldChange> changes = new LinkedHashMap<>();
        try {
            JsonNode beforeTree = objectMapper.readTree(beforeJson != null ? beforeJson : "{}");
            JsonNode afterTree = objectMapper.readTree(afterJson != null ? afterJson : "{}");

            Map<String, String> beforeFlat = new LinkedHashMap<>();
            Map<String, String> afterFlat = new LinkedHashMap<>();
            flatten("", beforeTree, beforeFlat);
            flatten("", afterTree, afterFlat);

            Set<String> allKeys = new LinkedHashSet<>();
            allKeys.addAll(beforeFlat.keySet());
            allKeys.addAll(afterFlat.keySet());

            for (String key : allKeys) {
                String oldVal = beforeFlat.get(key);
                String newVal = afterFlat.get(key);
                if (!Objects.equals(oldVal, newVal)) {
                    changes.put(key, new FieldChange(oldVal, newVal));
                }
            }
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to compute diff: {}", e.getMessage());
        }
        return changes;
    }

    /**
     * Serialize a diff map to JSON for storage in AuditLog.changedFields.
     */
    public String changedFieldsToJson(Set<String> fieldNames) {
        try {
            return objectMapper.writeValueAsString(fieldNames);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * Serialize old/new value maps to JSON for storage in AuditLog.oldValue / newValue.
     */
    public String valuesToJson(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void flatten(String prefix, JsonNode node, Map<String, String> result) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flatten(key, entry.getValue(), result);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flatten(prefix + "[" + i + "]", node.get(i), result);
            }
        } else {
            result.put(prefix, node.isNull() ? null : node.asText());
        }
    }

    /**
     * Represents a single field change with its old and new values.
     */
    public record FieldChange(String oldValue, String newValue) {}
}
