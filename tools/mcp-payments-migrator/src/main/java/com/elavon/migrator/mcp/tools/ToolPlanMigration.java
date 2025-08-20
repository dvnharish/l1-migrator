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
import java.util.*;
import java.util.stream.Collectors;

public class ToolPlanMigration extends ToolSupport {
    @Override
    public String name() { return "plan_migration"; }
    @Override
    public String description() { return "Analyze repo(s) to plan migration from Converge XML sale flows to Elavon Transactions API; detect languages and impacted files."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("repo_roots").put("type", "array").putObject("items").put("type", "string");
        props.putObject("source_openapi").put("type", "string");
        props.putObject("target_openapi").put("type", "string");
        props.putObject("languages").put("type", "array").putObject("items").put("type", "string");
        ObjectNode java = props.putObject("java").put("type", "object").putObject("properties");
        java.putObject("dto_package").put("type", "string");
        java.putObject("service_package").put("type", "string");
        java.putObject("controller_package").put("type", "string");
        java.putObject("config_package").put("type", "string");
        ObjectNode unify = props.putObject("endpoint_unify").put("type", "object").putObject("properties");
        unify.putObject("method").put("type", "string");
        unify.putObject("path").put("type", "string");
        unify.putObject("request_model").put("type", "string");
        unify.putObject("response_model").put("type", "string");
        ObjectNode defs = props.putObject("defaults").put("type", "object").putObject("properties");
        defs.putObject("currency_code").put("type", "string");
        defs.putObject("shopper_interaction_cnp").put("type", "string");
        defs.putObject("shopper_interaction_inperson").put("type", "string");
        defs.putObject("do_capture").put("type", "boolean");
        s.putArray("required").add("target_openapi");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        ObjectNode props = s.putObject("properties");
        props.putObject("plan_id").put("type", "string");
        props.putObject("summary").put("type", "string");
        props.putObject("impacted_files").put("type", "array").putObject("items").put("type", "string");
        s.putArray("required").add("plan_id");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        ArrayNode rootsNode = (ArrayNode) params.path("repo_roots");
        List<Path> roots = new ArrayList<>();
        if (rootsNode != null && rootsNode.isArray() && rootsNode.size() > 0) {
            for (JsonNode n : rootsNode) roots.add(Paths.get(n.asText()));
        } else {
            String defaultRoot = System.getenv().getOrDefault("DEFAULT_REPO_ROOT", new File(".").getAbsolutePath());
            roots.add(Paths.get(defaultRoot));
        }

        Set<String> languages = detectLanguages(roots);
        List<String> impacted = findConvergeUsages(roots);

        ObjectNode result = m.createObjectNode();
        result.put("plan_id", UUID.randomUUID().toString());
        result.put("summary", "Detected languages: " + String.join(", ", languages) + "; potential Converge references: " + impacted.size());
        ArrayNode arr = result.putArray("impacted_files");
        impacted.forEach(arr::add);
        return result;
    }

    private Set<String> detectLanguages(List<Path> roots) {
        Set<String> langs = new LinkedHashSet<>();
        for (Path root : roots) {
            if (Files.exists(root.resolve("pom.xml"))) langs.add("java");
            if (Files.exists(root.resolve("package.json"))) langs.add("typescript");
            try (var stream = Files.walk(root)) {
                if (stream.anyMatch(p -> p.toString().endsWith(".csproj"))) {
                    langs.add("csharp");
                }
            } catch (Exception ignored) {}
            if (Files.exists(root.resolve("pyproject.toml"))) langs.add("python");
        }
        return langs;
    }

    private List<String> findConvergeUsages(List<Path> roots) {
        List<String> hits = new ArrayList<>();
        List<String> needles = List.of(
                "Converge",
                "ssl_",
                "convergeXmlUrl",
                "/converge",
                "xmlMapper",
                "approval_code",
                "ssl_cvv2cvc2"
        );
        for (Path root : roots) {
            try {
                List<Path> files = Files.walk(root)
                        .filter(p -> Files.isRegularFile(p) &&
                                (p.toString().endsWith(".java") || p.toString().endsWith(".xml") || p.toString().endsWith(".yml") || p.toString().endsWith(".properties")))
                        .collect(Collectors.toList());
                for (Path f : files) {
                    String content = Files.readString(f);
                    for (String n : needles) {
                        if (content.contains(n)) {
                            hits.add(root.relativize(f).toString());
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return hits;
    }
}


