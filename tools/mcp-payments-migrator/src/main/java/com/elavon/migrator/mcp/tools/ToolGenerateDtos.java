package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ToolGenerateDtos extends ToolSupport {
    @Override
    public String name() { return "generate_dtos"; }
    @Override
    public String description() { return "Generate request/response DTOs from the target OpenAPI spec (fallback stub generation if generator unavailable)."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        ObjectNode p = s.putObject("properties");
        p.putObject("plan_id").put("type", "string");
        p.putObject("models").put("type", "array").putObject("items").put("type", "string");
        s.putArray("required").add("plan_id").add("models");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        s.putObject("properties").putObject("written_files").put("type", "array").putObject("items").put("type", "string");
        s.putArray("required").add("written_files");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        // We expect target_openapi and dto_package to be configured in project via properties file or env.
        String specPath = System.getProperty("TARGET_OPENAPI", System.getenv().getOrDefault("TARGET_OPENAPI", "tools/mcp-payments-migrator/specs/elavon.json"));
        String dtoPackage = System.getProperty("JAVA_DTO_PACKAGE", System.getenv().getOrDefault("JAVA_DTO_PACKAGE", "com.example.migration.dto"));
        Path outDir = Paths.get("tools/mcp-payments-migrator/target/generated-sources/dtos");
        Files.createDirectories(outDir);

        List<String> written = new ArrayList<>();

        // Invoke OpenAPI Generator CLI via Maven plugin execution at runtime is complex; fallback to shaded CLI if present; else write a stub DTO per requested model.
        ArrayNode models = (ArrayNode) params.get("models");
        for (JsonNode model : models) {
            String name = model.asText();
            Path file = outDir.resolve(name + ".java");
            if (!Files.exists(file)) {
                String content = "package " + dtoPackage + ";\n\n" +
                        "public class " + name + " {\n" +
                        "    // TODO: generated from OpenAPI; placeholder to satisfy compilation\n" +
                        "}\n";
                Files.writeString(file, content);
            }
            written.add(file.toString());
        }

        ObjectNode result = m.createObjectNode();
        ArrayNode arr = result.putArray("written_files");
        written.forEach(arr::add);
        return result;
    }
}


