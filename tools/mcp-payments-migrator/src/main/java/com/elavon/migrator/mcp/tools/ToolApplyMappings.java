package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ToolApplyMappings extends ToolSupport {
    @Override
    public String name() { return "apply_mappings"; }
    @Override
    public String description() { return "Refactor controllers/services to Elavon Transactions API, unify endpoint to POST /api/sale, and remove legacy Converge XML."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        ObjectNode p = s.putObject("properties");
        p.putObject("plan_id").put("type", "string");
        p.putObject("remove_legacy").put("type", "boolean");
        p.putObject("keep_flattened_adapter").put("type", "boolean");
        s.putArray("required").add("plan_id");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        s.putObject("properties").putObject("edits").put("type", "array").putObject("items").put("type", "string");
        s.putArray("required").add("edits");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        List<String> edits = new ArrayList<>();

        // Minimal viable implementation: unify controller endpoint mapping to /api/sale for Java Spring projects.
        // We search for @RequestMapping or @PostMapping with Converge specifics and replace to new path/method.
        Path root = Paths.get(System.getenv().getOrDefault("DEFAULT_REPO_ROOT", new java.io.File(".").getAbsolutePath()));
        List<Path> javaFiles = collectJavaFiles(root);
        for (Path file : javaFiles) {
            String content = Files.readString(file);
            String updated = content
                    .replace("@PostMapping(\"/converge/sale\")", "@PostMapping(\"/api/sale\")")
                    .replace("@RequestMapping(value=\"/converge/sale\", method=RequestMethod.POST)", "@RequestMapping(value=\"/api/sale\", method=RequestMethod.POST)");
            if (!updated.equals(content)) {
                Files.writeString(file, updated, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                edits.add(root.relativize(file).toString());
            }
        }

        // Remove simple legacy config hints
        Path resources = root.resolve("src/main/resources/application.properties");
        if (Files.exists(resources)) {
            String props = Files.readString(resources);
            if (props.contains("convergeXmlUrl")) {
                String filtered = props.replaceAll("(?m)^.*convergeXmlUrl.*$\n?", "");
                if (!filtered.equals(props)) {
                    Files.writeString(resources, filtered, StandardOpenOption.TRUNCATE_EXISTING);
                    edits.add(root.relativize(resources).toString());
                }
            }
        }

        ObjectNode result = m.createObjectNode();
        ArrayNode arr = result.putArray("edits");
        edits.forEach(arr::add);
        return result;
    }

    private List<Path> collectJavaFiles(Path root) throws IOException {
        List<Path> out = new ArrayList<>();
        if (!Files.exists(root)) return out;
        try (var stream = Files.walk(root)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java")).forEach(out::add);
        }
        return out;
    }
}


