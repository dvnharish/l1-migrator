package com.elavon.migrator.mcp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SimpleSchemaValidator {
    private SimpleSchemaValidator() {}

    public static List<String> validate(ObjectNode schema, JsonNode data) {
        List<String> errors = new ArrayList<>();
        if (schema == null) return errors;
        String type = text(schema, "type");
        if ("object".equals(type)) {
            if (data == null || !data.isObject()) {
                errors.add("Expected object but got " + (data == null ? "null" : data.getNodeType()));
                return errors;
            }
            // required
            ArrayNode required = (ArrayNode) schema.get("required");
            if (required != null) {
                for (JsonNode r : required) {
                    String key = r.asText();
                    if (!data.has(key)) {
                        errors.add("Missing required property: " + key);
                    }
                }
            }
            ObjectNode props = (ObjectNode) schema.get("properties");
            if (props != null) {
                Iterator<String> it = props.fieldNames();
                while (it.hasNext()) {
                    String name = it.next();
                    JsonNode propSchema = props.get(name);
                    JsonNode value = data.get(name);
                    if (value != null) {
                        errors.addAll(validate((ObjectNode) propSchema, value));
                    }
                }
            }
        } else if ("array".equals(type)) {
            if (data == null || !data.isArray()) {
                errors.add("Expected array but got " + (data == null ? "null" : data.getNodeType()));
                return errors;
            }
            JsonNode items = schema.get("items");
            if (items != null && items.isObject()) {
                for (JsonNode v : data) {
                    errors.addAll(validate((ObjectNode) items, v));
                }
            }
        } else if ("string".equals(type)) {
            if (data == null || !data.isTextual()) {
                errors.add("Expected string but got " + (data == null ? "null" : data.getNodeType()));
            }
        } else if ("boolean".equals(type)) {
            if (data == null || !data.isBoolean()) {
                errors.add("Expected boolean but got " + (data == null ? "null" : data.getNodeType()));
            }
        } else if ("number".equals(type)) {
            if (data == null || !data.isNumber()) {
                errors.add("Expected number but got " + (data == null ? "null" : data.getNodeType()));
            }
        } else if (schema.has("type")) {
            // Unknown type: accept but note
        }
        return errors;
    }

    private static String text(ObjectNode node, String field) {
        return node.has(field) ? node.get(field).asText() : null;
    }
}


