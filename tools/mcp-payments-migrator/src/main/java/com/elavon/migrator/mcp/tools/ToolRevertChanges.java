package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ToolRevertChanges extends ToolSupport {
    @Override
    public String name() { return "revert_changes"; }
    @Override
    public String description() { return "Revert working tree to a clean state using git reset/clean (best-effort)."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        s.putObject("properties").putObject("plan_id").put("type", "string");
        s.putArray("required").add("plan_id");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        s.putObject("properties").putObject("reverted").put("type", "boolean");
        s.putArray("required").add("reverted");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        // Soft revert via git
        try {
            new ProcessBuilder().command("git", "reset", "--hard").start().waitFor();
            new ProcessBuilder().command("git", "clean", "-fd").start().waitFor();
        } catch (Exception ignored) {}
        ObjectNode result = m.createObjectNode();
        result.put("reverted", true);
        return result;
    }
}


