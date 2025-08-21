package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class ToolFetchSpecs extends ToolSupport {
    @Override
    public String name() { return "fetch_specs"; }
    @Override
    public String description() { return "Download Converge and Elavon OpenAPI specs (with manual placement fallback)."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        ObjectNode p = s.putObject("properties");
        p.putObject("converge_url").put("type", "string");
        p.putObject("elavon_url").put("type", "string");
        p.putObject("out_dir").put("type", "string");
        s.putArray("required").add("out_dir");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        ObjectNode p = s.putObject("properties");
        p.putObject("converge_spec_path").put("type", "string");
        p.putObject("elavon_spec_path").put("type", "string");
        p.putObject("note").put("type", "string");
        s.putArray("required").add("converge_spec_path").add("elavon_spec_path");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        String outDir = params.path("out_dir").asText();
        Path dir = Paths.get(outDir);
        Files.createDirectories(dir);
        Path converge = dir.resolve("converge.json");
        Path elavon = dir.resolve("elavon.json");
        String note = null;
        boolean ok1 = tryDownload(params.path("converge_url").asText(null), converge);
        boolean ok2 = tryDownload(params.path("elavon_url").asText(null), elavon);
        // If not downloadable, populate from classpath defaults (empty stub) so pipeline continues
        if (!ok1 && !Files.exists(converge)) Files.writeString(converge, "{}\n");
        if (!ok2 && !Files.exists(elavon)) Files.writeString(elavon, "{}\n");
        if (!ok1 || !ok2) {
            note = "If direct download fails due to auth/CORS, manually place converge.json and elavon.json in " + dir.toAbsolutePath();
            if (!Files.exists(converge)) Files.writeString(converge, "{}\n");
            if (!Files.exists(elavon)) Files.writeString(elavon, "{}\n");
        }
        ObjectNode result = m.createObjectNode();
        result.put("converge_spec_path", converge.toAbsolutePath().toString());
        result.put("elavon_spec_path", elavon.toAbsolutePath().toString());
        if (note != null) result.put("note", note);
        return result;
    }

    private boolean tryDownload(String url, Path dest) {
        if (url == null || url.isBlank()) return false;
        try {
            URL u = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            conn.setReadTimeout((int) Duration.ofSeconds(20).toMillis());
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                byte[] bytes = conn.getInputStream().readAllBytes();
                Files.write(dest, bytes);
                return true;
            }
        } catch (IOException ignored) {}
        return false;
    }
}


