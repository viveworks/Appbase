import json
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest

from appbase_mcp.catalog import DeveloperCatalog
ROOT = Path(__file__).parents[2]
REVISION = "1" * 40
VERIFY_SPEC = importlib.util.spec_from_file_location(
    "verify_mcp_api_surface", ROOT / "scripts/verify-mcp-api-surface.py"
)
VERIFY_MODULE = importlib.util.module_from_spec(VERIFY_SPEC)
VERIFY_SPEC.loader.exec_module(VERIFY_MODULE)
parameter_count = VERIFY_MODULE.parameter_count


class ApiSurfaceGeneratorTest(unittest.TestCase):
    def test_real_source_generation_is_deterministic_and_loadable(self):
        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory) / "first.json"
            second = Path(directory) / "second.json"
            environment = {"MCP_API_SOURCE_REVISION": REVISION}
            for output in (first, second):
                subprocess.run(
                    ["bash", str(ROOT / "scripts/generate-mcp-api-surface.sh"), str(output)],
                    cwd=ROOT,
                    env={**__import__("os").environ, **environment},
                    check=True,
                )
            self.assertEqual(first.read_bytes(), second.read_bytes())
            value = json.loads(first.read_text(encoding="utf-8"))
            self.assertEqual(1, value["schema_version"])
            self.assertEqual(REVISION, value["source_revision"])
            self.assertGreater(len(value["symbols"]), 100)
            self.assertEqual(
                sorted(symbol["id"] for symbol in value["symbols"]),
                [symbol["id"] for symbol in value["symbols"]],
            )
            kinds = {symbol["kind"] for symbol in value["symbols"]}
            self.assertTrue({"interface", "annotation", "method"}.issubset(kinds))
            DeveloperCatalog.from_files(
                first,
                ROOT / "appbase_mcp/conventions.json",
                expected_revision=REVISION,
                repository_root=ROOT,
            )

    def test_parameter_count_handles_generics_and_empty_lists(self):
        self.assertEqual(0, parameter_count("void stop()"))
        self.assertEqual(2, parameter_count("void put(Map<String, Integer> values, String key)"))

    def test_compiled_oracle_accepts_real_bytecode_and_rejects_drift(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/demo/Service.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                "package demo; public interface Service { void start(); String get(int index); }\n",
                encoding="utf-8",
            )
            classes = root / "classes"
            classes.mkdir()
            subprocess.run(["javac", "-d", str(classes), str(source)], check=True)
            artifact = root / "api.json"
            value = {
                "schema_version": 1,
                "source_revision": REVISION,
                "symbols": [
                    {
                        "id": "demo.Service",
                        "qualified_name": "demo.Service",
                        "kind": "interface",
                        "signature": "public interface Service",
                        "source_path": "src/demo/Service.java",
                    },
                    {
                        "id": "demo.Service#start()",
                        "qualified_name": "demo.Service",
                        "kind": "method",
                        "signature": "void start()",
                        "source_path": "src/demo/Service.java",
                    },
                ],
            }
            artifact.write_text(json.dumps(value), encoding="utf-8")
            self.assertEqual(0, VERIFY_MODULE.verify(artifact, str(classes), "javap"))
            value["symbols"][1]["id"] = "demo.Service#missing()"
            value["symbols"][1]["signature"] = "void missing()"
            artifact.write_text(json.dumps(value), encoding="utf-8")
            with self.assertRaisesRegex(RuntimeError, "compiled method missing"):
                VERIFY_MODULE.verify(artifact, str(classes), "javap")


if __name__ == "__main__":
    unittest.main()
