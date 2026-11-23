import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
REVISION = "a" * 40


class StdioProtocolTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.artifact = Path(self.temp.name) / "api-surface.json"
        self.artifact.write_text(
            json.dumps(
                {
                    "schema_version": 1,
                    "source_revision": REVISION,
                    "symbols": [
                        {
                            "id": "com.viveworks.appbase.core.AppbaseCore#start()",
                            "qualified_name": "com.viveworks.appbase.core.AppbaseCore",
                            "kind": "method",
                            "signature": "void start()",
                            "source_path": "appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/AppbaseCore.java",
                        }
                    ],
                }
            ),
            encoding="utf-8",
        )

    def run_server(self, messages, *, revision=REVISION):
        command = [
            sys.executable,
            "-m",
            "appbase_mcp",
            "--api-artifact",
            str(self.artifact),
            "--repository-root",
            str(ROOT),
            "--expected-revision",
            revision,
        ]
        result = subprocess.run(
            command,
            cwd=ROOT,
            input="".join(json.dumps(message) + "\n" for message in messages),
            text=True,
            capture_output=True,
            timeout=10,
            check=False,
        )
        responses = [json.loads(line) for line in result.stdout.splitlines()]
        return result, responses

    @staticmethod
    def initialize(identifier=1):
        return {
            "jsonrpc": "2.0",
            "id": identifier,
            "method": "initialize",
            "params": {
                "protocolVersion": "2025-06-18",
                "capabilities": {},
                "clientInfo": {"name": "protocol-test", "version": "1"},
            },
        }

    def test_real_stdio_handshake_lists_and_calls_registered_tools(self):
        messages = [
            self.initialize(),
            {"jsonrpc": "2.0", "method": "notifications/initialized"},
            {"jsonrpc": "2.0", "id": 2, "method": "tools/list"},
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {"name": "lookup_api", "arguments": {"query": "AppbaseCore start"}},
            },
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {"name": "lookup_convention", "arguments": {"query": "live test gate"}},
            },
            {
                "jsonrpc": "2.0",
                "id": 5,
                "method": "tools/call",
                "params": {"name": "list_conventions", "arguments": {}},
            },
        ]
        result, responses = self.run_server(messages)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("", result.stderr)
        self.assertEqual("2025-06-18", responses[0]["result"]["protocolVersion"])
        self.assertEqual(
            {"lookup_api", "lookup_convention", "list_conventions"},
            {tool["name"] for tool in responses[1]["result"]["tools"]},
        )
        self.assertEqual("void start()", responses[2]["result"]["structuredContent"]["results"][0]["signature"])
        self.assertEqual("live-test-system-property", responses[3]["result"]["structuredContent"]["results"][0]["id"])
        self.assertEqual(2, responses[4]["result"]["structuredContent"]["count"])

    def test_protocol_and_tool_errors_fail_closed_without_tracebacks(self):
        messages = [
            {"jsonrpc": "2.0", "id": 1, "method": "tools/list"},
            self.initialize(2),
            {
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {"name": "not_registered", "arguments": {}},
            },
            {
                "jsonrpc": "2.0",
                "id": 4,
                "method": "tools/call",
                "params": {"name": "lookup_api", "arguments": {"query": ""}},
            },
        ]
        result, responses = self.run_server(messages)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(-32002, responses[0]["error"]["code"])
        self.assertEqual(-32602, responses[2]["error"]["code"])
        self.assertTrue(responses[3]["result"]["isError"])
        self.assertNotIn("Traceback", result.stderr)

    def test_stale_artifact_refuses_startup_before_protocol_serving(self):
        result, responses = self.run_server([self.initialize()], revision="b" * 40)
        self.assertEqual(2, result.returncode)
        self.assertEqual([], responses)
        self.assertIn("refusing startup", result.stderr)
        self.assertIn("does not match expected", result.stderr)


if __name__ == "__main__":
    unittest.main()
