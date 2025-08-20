package com.elavon.migrator.mcp.core;

import com.elavon.migrator.mcp.tools.ToolRegistry;
import com.elavon.migrator.mcp.util.Json;
import com.elavon.migrator.mcp.util.LogRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class JsonRpcServer {
    private final ObjectMapper mapper = Json.mapper();
    private final ToolRegistry toolRegistry = new ToolRegistry();
    private volatile boolean shutdownRequested = false;

    public void runEventLoop() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                handleLine(line, writer);
            }
        } catch (IOException e) {
            // best effort shutdown
        }
    }

    private void handleLine(String line, PrintWriter writer) {
        try {
            String safeLine = LogRedactor.redactSensitive(line);
            JsonNode req = mapper.readTree(line);
            String method = req.path("method").asText();
            JsonNode idNode = req.get("id");
            JsonNode params = req.get("params");

            if ("initialize".equals(method) || "server/initialize".equals(method)) {
                ObjectNode result = mapper.createObjectNode();
                result.put("protocolVersion", "2024-11-05");
                ObjectNode caps = mapper.createObjectNode();
                ObjectNode tools = mapper.createObjectNode();
                tools.put("listChanged", true);
                caps.set("tools", tools);
                result.set("capabilities", caps);
                ObjectNode serverInfo = mapper.createObjectNode();
                serverInfo.put("name", "payments-migrator");
                serverInfo.put("version", "0.1.0-SNAPSHOT");
                result.set("serverInfo", serverInfo);
                writeResponse(writer, idNode, result, null);
                // Send initialized notification
                sendNotification(writer, "initialized", mapper.createObjectNode());
                return;
            }

            if ("shutdown".equals(method)) {
                shutdownRequested = true;
                writeResponse(writer, idNode, mapper.createObjectNode(), null);
                return;
            }

            if ("exit".equals(method)) {
                // Best-effort clean exit
                writeResponse(writer, idNode, mapper.createObjectNode(), null);
                System.exit(shutdownRequested ? 0 : 1);
                return;
            }

            if ("tools/list".equals(method)) {
                ObjectNode result = toolRegistry.listToolsSchema();
                writeResponse(writer, idNode, result, null);
                return;
            }

            if (method != null && method.startsWith("tools/call")) {
                String toolName = method.replace("tools/call ", "").replace("tools/call", "").trim();
                if (toolName.isEmpty()) {
                    toolName = params != null && params.has("name") ? params.get("name").asText() : "";
                    params = params != null && params.has("arguments") ? params.get("arguments") : params;
                }
                ObjectNode result = toolRegistry.invoke(toolName, params);
                writeResponse(writer, idNode, result, null);
                return;
            }

            // Unknown method
            writeResponse(writer, idNode, null, error(-32601, "Method not found: " + method));
        } catch (Exception ex) {
            String err = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            writeResponse(writer, null, null, error(-32603, LogRedactor.redactSensitive(err)));
        }
    }

    private ObjectNode error(int code, String message) {
        ObjectNode e = mapper.createObjectNode();
        e.put("code", code);
        e.put("message", message);
        return e;
    }

    private void writeResponse(PrintWriter writer, JsonNode idNode, ObjectNode result, ObjectNode error) {
        ObjectNode resp = mapper.createObjectNode();
        resp.put("jsonrpc", "2.0");
        if (idNode == null || idNode.isMissingNode()) {
            resp.put("id", UUID.randomUUID().toString());
        } else if (idNode.isTextual()) {
            resp.put("id", idNode.asText());
        } else if (idNode.isNumber()) {
            resp.put("id", idNode.asLong());
        } else {
            resp.put("id", idNode.toString());
        }
        if (error != null) {
            resp.set("error", error);
        } else {
            resp.set("result", result == null ? mapper.createObjectNode() : result);
        }
        writer.println(resp.toString());
        writer.flush();
    }

    private void sendNotification(PrintWriter writer, String method, ObjectNode params) {
        ObjectNode notif = mapper.createObjectNode();
        notif.put("jsonrpc", "2.0");
        notif.put("method", method);
        if (params != null) {
            notif.set("params", params);
        }
        writer.println(notif.toString());
        writer.flush();
    }
}


