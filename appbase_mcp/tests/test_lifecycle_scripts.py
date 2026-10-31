import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest


ROOT = Path(__file__).resolve().parents[2]


class LifecycleScriptsTest(unittest.TestCase):
    def test_start_generates_serves_and_cleans_pid_file(self):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            artifact = work / "api.json"
            pidfile = work / "server.pid"
            environment = {
                **os.environ,
                "APPBASE_MCP_API_ARTIFACT": str(artifact),
                "APPBASE_MCP_PID_FILE": str(pidfile),
                "APPBASE_MCP_PYTHON": sys.executable,
            }
            process = subprocess.Popen(
                ["bash", "scripts/start-appbase-developer-mcp.sh"],
                cwd=ROOT,
                env=environment,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
            )
            def cleanup():
                if process.poll() is None:
                    process.kill()
                    process.wait()
                for stream in (process.stdin, process.stdout, process.stderr):
                    if stream is not None and not stream.closed:
                        stream.close()

            self.addCleanup(cleanup)
            deadline = time.monotonic() + 20
            while not pidfile.is_file() and process.poll() is None and time.monotonic() < deadline:
                time.sleep(0.05)
            if process.poll() is not None:
                self.fail(process.stderr.read())
            self.assertTrue(artifact.is_file())
            self.assertRegex(pidfile.read_text(encoding="utf-8").strip(), r"^[0-9]+$")
            process.stdin.write(
                '{"jsonrpc":"2.0","id":1,"method":"initialize","params":'
                '{"protocolVersion":"2025-06-18","capabilities":{},'
                '"clientInfo":{"name":"lifecycle-test","version":"1"}}}\n'
            )
            process.stdin.flush()
            response = process.stdout.readline()
            if not response:
                self.fail(process.stderr.read())
            self.assertIn('"protocolVersion":"2025-06-18"', response)
            process.stdin.close()
            self.assertEqual(0, process.wait(timeout=10), process.stderr.read())
            self.assertFalse(pidfile.exists())

    def test_stop_without_pid_file_is_idempotent(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run(
                ["bash", "scripts/stop-appbase-developer-mcp.sh"],
                cwd=ROOT,
                env={**os.environ, "APPBASE_MCP_PID_FILE": str(Path(directory) / "missing.pid")},
                text=True,
                capture_output=True,
                check=False,
            )
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertIn("not running", result.stdout)


if __name__ == "__main__":
    unittest.main()
