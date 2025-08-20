package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.elavon.migrator.mcp.util.SimpleSchemaValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public abstract class ToolSupport {
    protected final ObjectMapper mapper = Json.mapper();

    public abstract String name();
    public abstract String description();
    public abstract ObjectNode inputSchema();
    public abstract ObjectNode outputSchema();
    protected abstract ObjectNode execute(JsonNode validatedParams) throws Exception;

    public final ObjectNode handle(JsonNode params) {
        try {
            List<String> inErrors = SimpleSchemaValidator.validate(inputSchema(), params == null ? mapper.createObjectNode() : params);
            if (!inErrors.isEmpty()) {
                ObjectNode e = mapper.createObjectNode();
                e.put("error", "Invalid parameters");
                var arr = e.putArray("details");
                inErrors.forEach(arr::add);
                return e;
            }
            ObjectNode result = execute(params == null ? mapper.createObjectNode() : params);
            List<String> outErrors = SimpleSchemaValidator.validate(outputSchema(), result);
            if (!outErrors.isEmpty()) {
                ObjectNode e = mapper.createObjectNode();
                e.put("error", "Tool produced invalid output");
                var arr = e.putArray("details");
                outErrors.forEach(arr::add);
                return e;
            }
            return result;
        } catch (Exception ex) {
            ObjectNode e = mapper.createObjectNode();
            e.put("error", ex.getMessage() == null ? ex.toString() : ex.getMessage());
            return e;
        }
    }
}


