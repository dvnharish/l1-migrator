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
        String specPath = com.elavon.migrator.mcp.util.SpecLocator.resolveElavonSpec().toString();
        String dtoPackage = System.getProperty("JAVA_DTO_PACKAGE", System.getenv().getOrDefault("JAVA_DTO_PACKAGE", "com.elavon.transactions.model"));
        Path repoRoot = Paths.get(System.getenv().getOrDefault("DEFAULT_REPO_ROOT", new File(".").getAbsolutePath()));
        Path outDir = repoRoot.resolve("generated/elavon-models");
        Files.createDirectories(outDir);

        List<String> written = new ArrayList<>();

        // Attempt to run OpenAPI Generator via Maven plugin
        String[] cmd = new String[]{
                "mvn", "-q",
                "org.openapitools:openapi-generator-maven-plugin:7.6.0:generate",
                "-DgeneratorName=java",
                "-DinputSpec=" + specPath,
                "-Doutput=" + outDir.toAbsolutePath(),
                "-DmodelPackage=" + dtoPackage,
                "-DapiPackage=com.elavon.transactions.api",
                "-Dlibrary=webclient",
                "-DconfigOptions=sourceFolder=src/main/java,useJakartaEe=true,dateLibrary=java8"
        };
        try {
            Process p = new ProcessBuilder().command(cmd).directory(repoRoot.toFile()).redirectErrorStream(true).start();
            p.waitFor();
        } catch (Exception ignored) {
            // Fallback: create stubs for requested models
            ArrayNode models = (ArrayNode) params.get("models");
            Path stubDir = outDir.resolve("src/main/java/" + dtoPackage.replace('.', '/'));
            Files.createDirectories(stubDir);
            for (JsonNode model : models) {
                String name = model.asText();
                Path file = stubDir.resolve(name + ".java");
                if (!Files.exists(file)) {
                    String content = "package " + dtoPackage + ";\n\npublic class " + name + " {}\n";
                    Files.writeString(file, content);
                }
            }
        }

        // Collect written files
        if (Files.exists(outDir)) {
            try (var stream = Files.walk(outDir)) {
                stream.filter(Files::isRegularFile).forEach(p -> written.add(p.toString()));
            }
        }

        ObjectNode result = m.createObjectNode();
        ArrayNode arr = result.putArray("written_files");
        written.forEach(arr::add);
        return result;
    }
}


