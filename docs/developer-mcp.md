# Appbase developer MCP

[User Guide](user-guide/README.md)

Appbase includes a local, read-only Model Context Protocol (MCP) server for coding assistants. It
answers questions from the checked-out source rather than from a separately maintained API list. Use
it when an agent needs to find a Java API signature, locate a repository convention, or enumerate the
available conventions.

The server exposes three tools:

| Tool | Input | Result |
|---|---|---|
| `lookup_api` | `query` (required string), `limit` (optional integer, 1–50; default 10) | Matching public Java symbols, signatures and source paths |
| `lookup_convention` | `query` (required string), `limit` (optional integer, 1–50; default 10) | Matching repository conventions and their source-backed evidence |
| `list_conventions` | `{}` | The complete convention catalogue |

The catalog is local to the checkout. No Appbase credential, hosted service, or network connection is
required.

## Prerequisites

- A Git checkout of Appbase.
- JDK 25 or later. Startup compiles and runs the source-catalog generator.
- Python 3. The server uses only the Python standard library.
- Bash and Git.

In the examples below, replace `/absolute/path/to/Appbase` with the absolute path to your checkout.
An absolute path is important because clients do not all start MCP processes from the repository
directory.

## Configure a client

Each client must run `scripts/start-appbase-developer-mcp.sh` directly. The wrapper locates the
repository from its own path, regenerates the API catalog for the current commit, and then becomes a
transparent stdio transport. Do not configure clients to invoke `python -m appbase_mcp` directly;
that bypasses catalog generation and lifecycle supervision.

### Claude Code

From the Appbase checkout, add a project-scoped stdio server:

```bash
claude mcp add --scope project appbase-developer -- /absolute/path/to/Appbase/scripts/start-appbase-developer-mcp.sh
claude mcp list
```

The equivalent project `.mcp.json` entry is:

```json
{
  "mcpServers": {
    "appbase-developer": {
      "command": "/absolute/path/to/Appbase/scripts/start-appbase-developer-mcp.sh",
      "args": []
    }
  }
}
```

Claude Code asks you to approve a project-scoped server when it first reads the configuration.

### Codex CLI

Add this to `~/.codex/config.toml`:

```toml
[mcp_servers.appbase-developer]
command = "/absolute/path/to/Appbase/scripts/start-appbase-developer-mcp.sh"
args = []
```

Then verify that Codex sees the entry:

```bash
codex mcp list
```

Codex CLI and the Codex IDE extension share this configuration.

### GitHub Copilot in VS Code

Add `.vscode/mcp.json` to the workspace:

```json
{
  "servers": {
    "appbase-developer": {
      "type": "stdio",
      "command": "/absolute/path/to/Appbase/scripts/start-appbase-developer-mcp.sh",
      "args": []
    }
  }
}
```

Open that file in VS Code, select **Start**, then enable `appbase-developer` from Copilot Chat's tools
picker in Agent mode. A repository configuration is trusted code: review the command before starting
it.

### Cursor

Add this entry to `~/.cursor/mcp.json` (or merge it into an existing `mcpServers` object):

```json
{
  "mcpServers": {
    "appbase-developer": {
      "command": "/absolute/path/to/Appbase/scripts/start-appbase-developer-mcp.sh",
      "args": []
    }
  }
}
```

Restart Cursor after changing its MCP configuration.

## Use it

After the client reports that the server is connected, try prompts such as:

- `Use lookup_api to find the public AppbaseClient constructors.`
- `Use lookup_convention to find the repository convention for client health checks.`
- `Use list_conventions and summarize the conventions relevant to a new client module.`

Tool results include structured JSON as well as text content. Invalid arguments and unknown tools are
reported as MCP tool errors rather than silently returning an empty result.

## Operator lifecycle

Normally the MCP client owns the process: it starts the wrapper, keeps stdin open while connected,
and ends the process by closing stdin. The wrapper writes the child PID to
`target/appbase-mcp/server.pid` and removes that file on a normal exit. Protocol frames are written
only to stdout; diagnostics go to stderr.

To start the server manually for protocol debugging, run it in the foreground from any directory:

```bash
/absolute/path/to/Appbase/scripts/start-appbase-developer-mcp.sh
```

Press `Ctrl-C` to stop that foreground process. If a client wedges or detaches while leaving the
child alive, use the guarded stop command:

```bash
/absolute/path/to/Appbase/scripts/stop-appbase-developer-mcp.sh
```

The stop command is idempotent when no PID file exists. It removes a stale PID file, but refuses to
signal a live PID unless its command line identifies this checkout's Appbase MCP server. If startup
says the server is already running, either reconnect through the owning client or run the stop command
before retrying.

### Optional path overrides

Most users should keep the defaults. Operators can set these environment variables in a client's MCP
configuration when its filesystem policy requires different paths:

| Variable | Default | Purpose |
|---|---|---|
| `APPBASE_MCP_API_ARTIFACT` | `target/appbase-mcp/api-surface.json` | Generated API catalog path |
| `APPBASE_MCP_PID_FILE` | `target/appbase-mcp/server.pid` | Supervised child PID path |
| `APPBASE_MCP_PYTHON` | `python3` | Python executable |
| `APPBASE_MCP_GENERATOR` | `scripts/generate-mcp-api-surface.sh` | Catalog generator command |

Startup fails closed if generation fails or if the artifact is absent, malformed, duplicated, or
does not identify the checkout's exact Git revision. That failure is intentional: fix the reported
JDK, Python, Git, filesystem, or catalog problem instead of bypassing the wrapper.

## Verify the implementation

The fast server and lifecycle tests do not require Maven or external services:

```bash
python3 -m unittest discover -s appbase_mcp/tests -p 'test_*.py'
```

Maintainers can independently compare a generated catalog with compiled reactor classes after a full
Java build:

```bash
bash scripts/generate-mcp-api-surface.sh /tmp/appbase-api-surface.json
bash scripts/verify-mcp-api-surface.sh /tmp/appbase-api-surface.json
```

This document is the public protocol, trust-boundary, and operator reference for the developer MCP.
