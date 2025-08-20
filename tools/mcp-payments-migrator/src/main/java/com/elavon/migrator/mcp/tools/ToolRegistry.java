package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

public class ToolRegistry {
    private final ObjectMapper mapper = Json.mapper();
    private final Map<String, ToolSupport> tools = new LinkedHashMap<>();

    public ToolRegistry() {
        register(new ToolPlanMigration());
        register(new ToolFetchSpecs());
        register(new ToolGenerateDtos());
        register(new ToolGenerateSdk());
        register(new ToolApplyMappings());
        register(new ToolBuildAndTest());
        register(new ToolCreateBranchAndPr());
        register(new ToolRevertChanges());
    }

    private void register(ToolSupport tool) {
        tools.put(tool.name(), tool);
    }

    public ObjectNode listToolsSchema() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode list = mapper.createArrayNode();
        for (ToolSupport t : tools.values()) {
            ObjectNode entry = mapper.createObjectNode();
            entry.put("name", t.name());
            entry.put("description", t.description());
            entry.set("input_schema", t.inputSchema());
            entry.set("output_schema", t.outputSchema());
            list.add(entry);
        }
        result.set("tools", list);
        return result;
    }

    public ObjectNode invoke(String name, JsonNode params) {
        ToolSupport t = tools.get(name);
        if (t == null) {
            ObjectNode err = mapper.createObjectNode();
            err.put("error", "Unknown tool: " + name);
            return err;
        }
        return t.handle(params);
    }
}


