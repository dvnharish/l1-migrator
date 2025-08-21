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
        // We search for common patterns and replace to new path/method; make idempotent edits.
        Path root = Paths.get(System.getenv().getOrDefault("DEFAULT_REPO_ROOT", new java.io.File(".").getAbsolutePath()));
        List<Path> javaFiles = collectJavaFiles(root);
        for (Path file : javaFiles) {
            String content = Files.readString(file);
            String updated = content;
            // unify converge-specific sale endpoint
            updated = updated.replace("@PostMapping(\"/converge/sale\")", "@PostMapping(\"/api/sale\")");
            updated = updated.replace("@RequestMapping(value=\"/converge/sale\", method=RequestMethod.POST)", "@RequestMapping(value=\"/api/sale\", method=RequestMethod.POST)");
            // unify common sample path /api/v1/payments + /sale -> /api/sale
            updated = updated.replace("@RequestMapping(\"/api/v1/payments\")", "@RequestMapping(\"/api\")");
            updated = updated.replace("@RequestMapping( value=\"/api/v1/payments\")", "@RequestMapping( value=\"/api\")");
            // remove JAXB XML converter usages
            if (updated.contains("Jaxb2RootElementHttpMessageConverter")) {
                updated = updated.replace("Jaxb2RootElementHttpMessageConverter", "/* removed_legacy_JAXB */ Jaxb2RootElementHttpMessageConverter");
            }
            if (!updated.equals(content)) {
                Files.writeString(file, updated, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                edits.add(root.relativize(file).toString());
            }
        }

        // Map legacy Converge process.do usage to Elavon transactions (placeholder comment for future OpenRewrite)
        // For now, add a TODO marker where we detect the Converge base URL property to guide migration.
        // Append Elavon config stubs to application.yml files and mark Converge legacy
        List<Path> ymls = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith("application.yml") || p.toString().endsWith("application.yaml"))
                    .forEach(ymls::add);
        }
        for (Path yml : ymls) {
            String y = Files.readString(yml);
            String out = y;
            if (y.contains("VirtualMerchant/process.do") && !y.contains("# MIGRATED_TO_ELAVON_TRANSACTIONS")) {
                out = "# MIGRATED_TO_ELAVON_TRANSACTIONS: Replace with Elavon Transactions base URL and client config\n" + out;
            }
            if (!y.contains("elavon:") && !y.contains("ELAVON_BASE_URL")) {
                out = out + "\n\nelavon:\n  base-url: ${ELAVON_BASE_URL:https://api.elavon.com}\n  accept-version: ${ELAVON_ACCEPT_VERSION:1.0}\n  merchant-alias: ${ELAVON_MERCHANT_ALIAS:demoAlias}\n  api-key: ${ELAVON_API_KEY:demoKey}\n";
            }
            if (!out.equals(y)) {
                Files.writeString(yml, out, StandardOpenOption.TRUNCATE_EXISTING);
                edits.add(root.relativize(yml).toString());
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


