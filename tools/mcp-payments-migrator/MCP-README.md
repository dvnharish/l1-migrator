## payments-migrator MCP server

Console Spring Boot 3 app exposing MCP tools over JSON-RPC 2.0 via stdio. See root `README.md` for overview.

### Running
```
java -jar tools/mcp-payments-migrator/target/mcp-payments-migrator-0.1.0-SNAPSHOT.jar
```

### Tools
- plan_migration
- fetch_specs
- generate_dtos
- generate_sdk
- apply_mappings
- build_and_test
- create_branch_and_pr
- revert_changes

### MCP client configuration
```
{
  "name": "payments-migrator",
  "command": "java",
  "args": ["-jar", "tools/mcp-payments-migrator/target/mcp-payments-migrator-0.1.0-SNAPSHOT.jar"],
  "env": {"DEFAULT_REPO_ROOT": "${workspaceFolder}"}
}
```

### Example: tools/list
```
{"jsonrpc":"2.0","id":"1","method":"tools/list"}
```

### Example: tools/call
```
{"jsonrpc":"2.0","id":"2","method":"tools/call plan_migration","params":{"repo_roots":["."],"target_openapi":"tools/mcp-payments-migrator/specs/elavon.json"}}
```

### Example prompts
- Plan migration for this repo
- Fetch specs to tools/mcp-payments-migrator/specs
- Generate DTOs for TransactionInput, Card, Contact, Total, Transaction
- Apply mappings and unify controller to POST /api/sale
- Build and test; return logs
- Create branch feat/migrate-converge-to-elavon and open PR


