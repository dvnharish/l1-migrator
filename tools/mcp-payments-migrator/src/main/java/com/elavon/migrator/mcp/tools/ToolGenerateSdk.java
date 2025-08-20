package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ToolGenerateSdk extends ToolSupport {
    @Override
    public String name() { return "generate_sdk"; }
    @Override
    public String description() { return "Generate multi-language SDKs from the target OpenAPI spec into sdk/<lang>."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        ObjectNode p = s.putObject("properties");
        p.putObject("plan_id").put("type", "string");
        p.putObject("languages").put("type", "array").putObject("items").put("type", "string");
        p.putObject("out_dir").put("type", "string");
        s.putArray("required").add("plan_id").add("languages").add("out_dir");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        s.putObject("properties").putObject("sdk_paths").put("type", "array").putObject("items").put("type", "string");
        s.putArray("required").add("sdk_paths");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        String outDir = params.path("out_dir").asText();
        Files.createDirectories(Paths.get(outDir));
        ArrayNode langs = (ArrayNode) params.get("languages");
        List<String> paths = new ArrayList<>();
        for (JsonNode lang : langs) {
            String l = lang.asText();
            Path sdkPath = Paths.get(outDir).resolve("sdk").resolve(l);
            Files.createDirectories(sdkPath);
            Files.writeString(sdkPath.resolve("README.md"), "SDK placeholder for " + l + " generated from OpenAPI.\n");
            paths.add(sdkPath.toString());
        }
        ObjectNode result = m.createObjectNode();
        ArrayNode arr = result.putArray("sdk_paths");
        paths.forEach(arr::add);
        return result;
    }
}


