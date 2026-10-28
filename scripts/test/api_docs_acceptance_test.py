#!/usr/bin/env python3
"""Behavioural tests for the internal work item 2541 API-documentation acceptance groundwork.

@author Navaneeth Mysore G
@author Claude
"""

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
CHECKER = ROOT / "scripts" / "api-docs-acceptance.py"
REQUIRED = ("catalogue", "supported-modules", "supported-adapters", "known-limitations")


class AcceptanceGroundworkTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / "scripts").mkdir()
        (self.root / "docs").mkdir()
        self._gate("api-inventory.py", 0)
        self._gate("catalogue-check.py", 0)
        (self.root / "README.md").write_text(
            "[Catalogue](docs/catalogue.md)\n"
            "[Modules](docs/modules.md#supported)\n"
            "[Adapters][adapters]\n"
            "[Limits](docs/limits.md)\n\n"
            "[adapters]: docs/adapters.md\n",
            encoding="utf-8",
        )
        for name, text in {
            "catalogue.md": "# Catalogue\n",
            "modules.md": "# Modules\n\n## Supported\n",
            "adapters.md": "# Adapters\n",
            "limits.md": "# Limits\n",
        }.items():
            (self.root / "docs" / name).write_text(text, encoding="utf-8")
        self.matrix = self.root / "matrix.json"
        self.report = self.root / "report.json"
        self._matrix()

    def tearDown(self):
        self.temp.cleanup()

    def _gate(self, name, rc):
        (self.root / "scripts" / name).write_text(
            "import sys\nprint('fixture gate')\nsys.exit(%d)\n" % rc,
            encoding="utf-8",
        )

    def _matrix(self, rows=None, required=None):
        if rows is None:
            rows = [
                {"role": "catalogue", "source": "README.md", "target": "docs/catalogue.md"},
                {"role": "supported-modules", "source": "README.md", "target": "docs/modules.md#supported"},
                {"role": "supported-adapters", "source": "README.md", "target": "docs/adapters.md"},
                {"role": "known-limitations", "source": "README.md", "target": "docs/limits.md"},
            ]
        self.matrix.write_text(
            json.dumps({"schema": 1, "required_roles": required or list(REQUIRED), "links": rows}),
            encoding="utf-8",
        )

    def run_checker(self, cwd=None):
        return subprocess.run(
            [sys.executable, str(CHECKER), "--root", str(self.root), "--matrix", str(self.matrix),
             "--report", str(self.report)],
            cwd=cwd or self.root,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            check=False,
        )

    def test_valid_static_matrix_is_deterministic(self):
        first = self.run_checker()
        self.assertEqual(0, first.returncode, first.stdout)
        body = self.report.read_bytes()
        second = self.run_checker(cwd=self.root / "docs")
        self.assertEqual(0, second.returncode, second.stdout)
        self.assertEqual(body, self.report.read_bytes())
        report = json.loads(body)
        self.assertEqual("pass", report["status"])
        self.assertEqual(list(REQUIRED), [row["role"] for row in report["links"]])

    def test_existing_gate_failure_propagates(self):
        self._gate("api-inventory.py", 7)
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("api-inventory", result.stdout)
        self.assertEqual("fail", json.loads(self.report.read_text())["status"])

    def test_missing_target_and_fragment_fail_with_source_context(self):
        (self.root / "docs" / "limits.md").unlink()
        (self.root / "docs" / "modules.md").write_text("# Modules\n", encoding="utf-8")
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("README.md", result.stdout)
        self.assertIn("docs/limits.md", result.stdout)
        self.assertIn("#supported", result.stdout)

    def test_heading_inline_code_text_participates_in_fragment(self):
        (self.root / "docs" / "modules.md").write_text(
            "# Modules\n\n## Supported `modules`\n", encoding="utf-8"
        )
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("#supported", result.stdout)
        (self.root / "README.md").write_text(
            self.root.joinpath("README.md").read_text().replace("#supported", "#supported-modules"),
            encoding="utf-8",
        )
        rows = json.loads(self.matrix.read_text())["links"]
        rows[1]["target"] = "docs/modules.md#supported-modules"
        self._matrix(rows)
        result = self.run_checker()
        self.assertEqual(0, result.returncode, result.stdout)

    def test_fake_links_in_code_and_comments_do_not_satisfy_role(self):
        (self.root / "README.md").write_text(
            "```md\n[Catalogue](docs/catalogue.md)\n```\n"
            "<!-- [Modules](docs/modules.md#supported) -->\n"
            "`[Catalogue](docs/catalogue.md)`\n"
            "`\n[Catalogue](docs/catalogue.md)\n`\n"
            "    [Modules](docs/modules.md#supported)\n"
            "\\[Catalogue](docs/catalogue.md)\n"
            "[Adapters](docs/adapters.md)\n[Limits](docs/limits.md)\n",
            encoding="utf-8",
        )
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("catalogue", result.stdout)
        self.assertIn("supported-modules", result.stdout)

    def test_empty_matrix_cannot_pass(self):
        self.matrix.write_text(json.dumps({"schema": 1, "required_roles": [], "links": []}), encoding="utf-8")
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("required_roles must declare", result.stdout)

    def test_non_object_matrix_replaces_stale_report_with_failure(self):
        self.matrix.write_text("[]", encoding="utf-8")
        self.report.write_text('{"status":"pass"}\n', encoding="utf-8")
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        report = json.loads(self.report.read_text())
        self.assertEqual("fail", report["status"])
        self.assertIn("matrix must be a JSON object", report["problems"])

    def test_duplicate_reference_definition_cannot_credit_last_target(self):
        (self.root / "README.md").write_text(
            "[Catalogue][catalogue]\n[Modules](docs/modules.md#supported)\n"
            "[Adapters](docs/adapters.md)\n[Limits](docs/limits.md)\n\n"
            "[catalogue]: docs/wrong.md\n[catalogue]: docs/catalogue.md\n",
            encoding="utf-8",
        )
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("duplicate reference definition", result.stdout)

    def test_html_source_link_and_fragment_are_checked(self):
        (self.root / "index.html").write_text(
            '<link rel="canonical" href="docs/catalogue.md">', encoding="utf-8"
        )
        rows = json.loads(self.matrix.read_text())["links"]
        rows[0] = {"role": "catalogue", "source": "index.html", "target": "docs/catalogue.md"}
        self._matrix(rows)
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        (self.root / "index.html").write_text(
            '<a href="docs/catalogue.md">Catalogue</a>', encoding="utf-8"
        )
        result = self.run_checker()
        self.assertEqual(0, result.returncode, result.stdout)

    def test_pending_generated_link_is_explicitly_incomplete(self):
        data = json.loads(self.matrix.read_text())
        data["links"][0] = {
            "role": "catalogue", "source": "target/site/index.html",
            "target": "docs/catalogue.md", "status": "pending",
            "reason": "generated Javadoc is unavailable until internal work item 1405",
        }
        self.matrix.write_text(json.dumps(data), encoding="utf-8")
        result = self.run_checker()
        self.assertEqual(2, result.returncode, result.stdout)
        self.assertIn("NOT MEASURED", result.stdout)
        self.assertEqual("incomplete", json.loads(self.report.read_text())["status"])

    def test_duplicate_or_missing_required_role_fails(self):
        data = json.loads(self.matrix.read_text())
        data["links"].append(dict(data["links"][0]))
        data["links"] = [row for row in data["links"] if row["role"] != "known-limitations"]
        self.matrix.write_text(json.dumps(data), encoding="utf-8")
        result = self.run_checker()
        self.assertEqual(1, result.returncode, result.stdout)
        self.assertIn("duplicate role", result.stdout)
        self.assertIn("missing required role", result.stdout)

    def test_traversal_and_symlink_escape_are_refused(self):
        outside = self.root.parent / (self.root.name + "-outside.md")
        outside.write_text("# Outside\n", encoding="utf-8")
        try:
            (self.root / "docs" / "escape.md").symlink_to(outside)
            (self.root / "README.md").write_text(
                "[Catalogue](docs/escape.md)\n[Modules](../outside.md)\n"
                "[Adapters](docs/adapters.md)\n[Limits](docs/limits.md)\n",
                encoding="utf-8",
            )
            data = json.loads(self.matrix.read_text())
            data["links"][0]["target"] = "docs/escape.md"
            data["links"][1]["target"] = "../outside.md"
            self.matrix.write_text(json.dumps(data), encoding="utf-8")
            result = self.run_checker()
            self.assertEqual(1, result.returncode, result.stdout)
            self.assertIn("outside --root", result.stdout)
        finally:
            outside.unlink(missing_ok=True)

    def test_percent_encoding_query_and_relative_parent_resolve(self):
        nested = self.root / "docs" / "guide"
        nested.mkdir()
        (self.root / "docs" / "known limits.md").write_text("# Build Limits\n", encoding="utf-8")
        (nested / "index.md").write_text("[Limits](../known%20limits.md?view=all#build-limits)\n", encoding="utf-8")
        rows = json.loads(self.matrix.read_text())["links"]
        rows[-1] = {"role": "known-limitations", "source": "docs/guide/index.md",
                    "target": "../known%20limits.md?view=all#build-limits"}
        self._matrix(rows)
        result = self.run_checker(cwd=self.root / "scripts")
        self.assertEqual(0, result.returncode, result.stdout)


if __name__ == "__main__":
    unittest.main()
