package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ToolBuildAndTest extends ToolSupport {
    @Override
    public String name() { return "build_and_test"; }
    @Override
    public String description() { return "Build and test projects (Java/TS/C#/Python) with provided commands; returns success and logs."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        var p = s.putObject("properties");
        p.putObject("plan_id").put("type", "string");
        p.putObject("java_cmd").put("type", "string");
        p.putObject("ts_cmd").put("type", "string");
        p.putObject("csharp_cmd").put("type", "string");
        p.putObject("python_cmd").put("type", "string");
        s.putArray("required").add("plan_id");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        var p = s.putObject("properties");
        p.putObject("success").put("type", "boolean");
        p.putObject("logs").put("type", "string");
        s.putArray("required").add("success").add("logs");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        String javaCmd = params.path("java_cmd").asText("mvn -DskipTests package");
        Process proc = new ProcessBuilder()
                .command(javaCmd.split(" "))
                .redirectErrorStream(true)
                .start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        int code = proc.waitFor();
        ObjectNode result = m.createObjectNode();
        result.put("success", code == 0);
        result.put("logs", sb.toString());
        return result;
    }
}


