"""Pinned MCP stdio transport for the Appbase developer lookup catalog.

@author Navaneeth Mysore G
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from typing import Any, TextIO

from .catalog import CatalogError, DeveloperCatalog
from .handlers import LookupHandlers


PROTOCOL_VERSION = "2025-06-18"
SERVER_NAME = "appbase-developer"
SERVER_VERSION = "0.1.0"

TOOLS = (
    {
        "name": "lookup_api",
        "description": "Find source-derived Appbase Java API symbols and method signatures.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "minLength": 1},
                "limit": {"type": "integer", "minimum": 1, "maximum": 50, "default": 10},
            },
            "required": ["query"],
            "additionalProperties": False,
        },
    },
    {
        "name": "lookup_convention",
        "description": "Find repository-validated Appbase development conventions and examples.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "minLength": 1},
                "limit": {"type": "integer", "minimum": 1, "maximum": 50, "default": 10},
            },
            "required": ["query"],
            "additionalProperties": False,
        },
    },
    {
        "name": "list_conventions",
        "description": "List every repository-validated Appbase development convention.",
        "inputSchema": {"type": "object", "properties": {}, "additionalProperties": False},
    },
)


class ProtocolError(ValueError):
    """A JSON-RPC request is invalid at the MCP transport boundary."""

    def __init__(self, code: int, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


class StdioMcpServer:
    """Small dependency-free MCP 2025-06-18 server over newline-delimited stdio."""

    def __init__(self, handlers: LookupHandlers):
        self._handlers = handlers
        self._initialized = False

    @staticmethod
    def _result(request_id: object, result: dict[str, Any]) -> dict[str, Any]:
        return {"jsonrpc": "2.0", "id": request_id, "result": result}

    @staticmethod
    def _error(request_id: object, code: int, message: str) -> dict[str, Any]:
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {"code": code, "message": message},
        }

    @staticmethod
    def _arguments(params: object) -> tuple[str, dict[str, Any]]:
        if not isinstance(params, dict):
            raise ProtocolError(-32602, "tools/call params must be an object")
        name = params.get("name")
        arguments = params.get("arguments", {})
        if not isinstance(name, str) or not name:
            raise ProtocolError(-32602, "tools/call name must be a non-empty string")
        if not isinstance(arguments, dict):
            raise ProtocolError(-32602, "tools/call arguments must be an object")
        return name, arguments

    def _call_tool(self, params: object) -> dict[str, Any]:
        name, arguments = self._arguments(params)
        allowed = {
            "lookup_api": {"query", "limit"},
            "lookup_convention": {"query", "limit"},
            "list_conventions": set(),
        }
        if name not in allowed:
            raise ProtocolError(-32602, f"unknown tool: {name}")
        extra = set(arguments) - allowed[name]
        if extra:
            raise ProtocolError(-32602, f"unexpected arguments for {name}: {sorted(extra)}")
        try:
            if name == "lookup_api":
                value = self._handlers.lookup_api(
                    arguments.get("query"), arguments.get("limit", 10)
                )
            elif name == "lookup_convention":
                value = self._handlers.lookup_convention(
                    arguments.get("query"), arguments.get("limit", 10)
                )
            else:
                value = self._handlers.list_conventions()
        except (CatalogError, TypeError, ValueError) as error:
            return {
                "content": [{"type": "text", "text": str(error)}],
                "isError": True,
            }
        return {
            "content": [{"type": "text", "text": json.dumps(value, sort_keys=True)}],
            "structuredContent": value,
            "isError": False,
        }

    def dispatch(self, request: object) -> dict[str, Any] | None:
        if not isinstance(request, dict) or request.get("jsonrpc") != "2.0":
            raise ProtocolError(-32600, "request must be a JSON-RPC 2.0 object")
        request_id = request.get("id")
        method = request.get("method")
        if not isinstance(method, str):
            raise ProtocolError(-32600, "request method must be a string")
        if request_id is None:
            if method == "notifications/initialized":
                return None
            return None
        if method == "initialize":
            self._initialized = True
            return self._result(
                request_id,
                {
                    "protocolVersion": PROTOCOL_VERSION,
                    "capabilities": {"tools": {"listChanged": False}},
                    "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION},
                },
            )
        if not self._initialized:
            raise ProtocolError(-32002, "server is not initialized")
        if method == "ping":
            return self._result(request_id, {})
        if method == "tools/list":
            return self._result(request_id, {"tools": [dict(tool) for tool in TOOLS]})
        if method == "tools/call":
            return self._result(request_id, self._call_tool(request.get("params")))
        raise ProtocolError(-32601, f"method not found: {method}")

    def serve(self, input_stream: TextIO, output_stream: TextIO) -> int:
        for line in input_stream:
            request_id: object = None
            try:
                request = json.loads(line)
                if isinstance(request, dict):
                    request_id = request.get("id")
                response = self.dispatch(request)
            except json.JSONDecodeError:
                response = self._error(None, -32700, "parse error")
            except ProtocolError as error:
                response = self._error(request_id, error.code, error.message)
            except Exception:
                response = self._error(request_id, -32603, "internal error")
            if response is not None:
                output_stream.write(json.dumps(response, separators=(",", ":")) + "\n")
                output_stream.flush()
        return 0


def parser() -> argparse.ArgumentParser:
    value = argparse.ArgumentParser(description="Serve the Appbase developer MCP over stdio")
    value.add_argument("--api-artifact", required=True, type=Path)
    value.add_argument("--conventions", type=Path, default=Path(__file__).with_name("conventions.json"))
    value.add_argument("--repository-root", required=True, type=Path)
    value.add_argument("--expected-revision", required=True)
    return value


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    try:
        catalog = DeveloperCatalog.from_files(
            args.api_artifact,
            args.conventions,
            expected_revision=args.expected_revision,
            repository_root=args.repository_root,
        )
    except CatalogError as error:
        print(f"appbase-developer-mcp: refusing startup: {error}", file=sys.stderr)
        return 2
    return StdioMcpServer(LookupHandlers(catalog)).serve(sys.stdin, sys.stdout)


if __name__ == "__main__":
    raise SystemExit(main())
