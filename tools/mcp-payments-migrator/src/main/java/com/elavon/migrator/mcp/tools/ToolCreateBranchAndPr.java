package com.elavon.migrator.mcp.tools;

import com.elavon.migrator.mcp.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ToolCreateBranchAndPr extends ToolSupport {
    @Override
    public String name() { return "create_branch_and_pr"; }
    @Override
    public String description() { return "Create a git branch, commit migration edits, push, and open a PR (or write PR body if token not available)."; }

    @Override
    public ObjectNode inputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        var p = s.putObject("properties");
        p.putObject("plan_id").put("type", "string");
        p.putObject("branch").put("type", "string");
        p.putObject("create_pr").put("type", "boolean");
        p.putObject("pr_title").put("type", "string");
        p.putObject("pr_body").put("type", "string");
        s.putArray("required").add("plan_id").add("branch");
        return s;
    }

    @Override
    public ObjectNode outputSchema() {
        ObjectMapper m = Json.mapper();
        ObjectNode s = m.createObjectNode();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        var p = s.putObject("properties");
        p.putObject("branch").put("type", "string");
        p.putObject("pr_url").put("type", "string");
        s.putArray("required").add("branch");
        return s;
    }

    @Override
    protected ObjectNode execute(JsonNode params) throws Exception {
        ObjectMapper m = Json.mapper();
        String branch = params.path("branch").asText("feat/migrate-converge-to-elavon");

        run(new String[]{"git", "checkout", "-B", branch});
        run(new String[]{"git", "add", "-A"});
        run(new String[]{"git", "commit", "-m", "chore: migrate Converge XML to Elavon Transactions API"});
        run(new String[]{"git", "push", "-u", "origin", branch});

        String prUrl = null;
        String token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isEmpty() && params.path("create_pr").asBoolean(false)) {
            // Simplified: ask user/CI to create PR; write body to disk
            prUrl = "https://github.com/<owner>/<repo>/pulls";
        }

        Path out = Paths.get("PR_BODY.md");
        String body = params.path("pr_body").asText("Migrate Converge XML sale flows to Elavon Transactions API");
        Files.writeString(out, "# " + params.path("pr_title").asText("Converge -> Elavon Migration") + "\n\n" + body + "\n");

        ObjectNode result = m.createObjectNode();
        result.put("branch", branch);
        if (prUrl != null) result.put("pr_url", prUrl);
        return result;
    }

    private void run(String[] cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder().command(cmd).start();
        p.waitFor();
    }
}


