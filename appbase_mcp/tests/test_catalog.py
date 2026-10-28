import json
from pathlib import Path
import tempfile
import unittest

from appbase_mcp.catalog import CatalogError, DeveloperCatalog
from appbase_mcp.handlers import LookupHandlers


REVISION = "a" * 40


def artifact(**overrides):
    value = {
        "schema_version": 1,
        "source_revision": REVISION,
        "symbols": [
            {
                "id": "com.viveworks.appbase.core.AppbaseCore#start()",
                "qualified_name": "com.viveworks.appbase.core.AppbaseCore",
                "kind": "method",
                "signature": "void start()",
                "source_path": "appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/AppbaseCore.java",
            },
            {
                "id": "com.viveworks.appbase.core.AppbaseCore#stop()",
                "qualified_name": "com.viveworks.appbase.core.AppbaseCore",
                "kind": "method",
                "signature": "void stop()",
                "source_path": "appbase-java/appbase-core/src/main/java/com/viveworks/appbase/core/AppbaseCore.java",
            },
        ],
    }
    value.update(overrides)
    return value


class DeveloperCatalogTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.artifact_path = Path(self.temp.name) / "api-surface.json"
        self.artifact_path.write_text(json.dumps(artifact()), encoding="utf-8")
        self.catalog = DeveloperCatalog.from_files(
            self.artifact_path,
            Path(__file__).parents[1] / "conventions.json",
            expected_revision=REVISION,
        )
        self.handlers = LookupHandlers(self.catalog)

    def test_lookup_api_matches_name_signature_and_limits_results(self):
        result = self.handlers.lookup_api("AppbaseCore start", limit=1)
        self.assertEqual(1, result["count"])
        self.assertEqual("void start()", result["results"][0]["signature"])
        self.assertNotIn("score", result["results"][0])

    def test_lookup_api_is_deterministic_and_reports_no_match(self):
        first = self.handlers.lookup_api("AppbaseCore")
        second = self.handlers.lookup_api("AppbaseCore")
        self.assertEqual(first, second)
        self.assertEqual([], self.catalog.lookup_api("does-not-exist")["results"])

    def test_lookup_api_rejects_partial_match_for_nonsense_multi_term_query(self):
        result = self.handlers.lookup_api("definitely-no-such-appbase-symbol", limit=10)
        self.assertEqual(0, result["count"])
        self.assertEqual([], result["results"])

    def test_lookup_convention_matches_seed_terms(self):
        result = self.handlers.lookup_convention("live backend test gate")
        self.assertEqual("live-test-system-property", result["results"][0]["id"])
        stack = self.catalog.lookup_convention("stacktest whitelist")
        self.assertEqual("stacktest-default-skip-fast-allowlist", stack["results"][0]["id"])

    def test_list_conventions_returns_summaries_without_internal_validation_fields(self):
        result = self.handlers.list_conventions()
        self.assertEqual(2, result["count"])
        self.assertNotIn("validation", result["results"][0])

    def test_invalid_queries_and_limits_are_rejected(self):
        for query in ("", "   ", None, ["start"]):
            with self.subTest(query=query), self.assertRaises(CatalogError):
                self.catalog.lookup_api(query)
        for limit in (0, 51, True, "1"):
            with self.subTest(limit=limit), self.assertRaises(CatalogError):
                self.catalog.lookup_api("start", limit=limit)


class ApiArtifactFailClosedTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "api-surface.json"
        self.conventions = Path(__file__).parents[1] / "conventions.json"

    def load(self, value, expected_revision=REVISION):
        self.path.write_text(value if isinstance(value, str) else json.dumps(value), encoding="utf-8")
        return DeveloperCatalog.from_files(
            self.path, self.conventions, expected_revision=expected_revision
        )

    def test_missing_artifact_fails_closed(self):
        with self.assertRaisesRegex(CatalogError, "missing"):
            DeveloperCatalog.from_files(
                self.path, self.conventions, expected_revision=REVISION
            )

    def test_expected_revision_is_mandatory(self):
        self.path.write_text(json.dumps(artifact()), encoding="utf-8")
        with self.assertRaises(TypeError):
            DeveloperCatalog.from_files(self.path, self.conventions)

    def test_malformed_artifact_fails_closed(self):
        with self.assertRaisesRegex(CatalogError, "valid JSON"):
            self.load("{")

    def test_unsupported_schema_fails_closed(self):
        with self.assertRaisesRegex(CatalogError, "schema_version"):
            self.load(artifact(schema_version=2))

    def test_duplicate_symbol_id_fails_closed(self):
        value = artifact()
        value["symbols"].append(dict(value["symbols"][0]))
        with self.assertRaisesRegex(CatalogError, "duplicate symbol id"):
            self.load(value)

    def test_empty_symbols_fail_closed(self):
        with self.assertRaisesRegex(CatalogError, "must not be empty"):
            self.load(artifact(symbols=[]))

    def test_duplicate_semantic_signature_fails_closed(self):
        value = artifact()
        duplicate = dict(value["symbols"][0])
        duplicate["id"] = "generator-chose-a-different-id"
        value["symbols"].append(duplicate)
        with self.assertRaisesRegex(CatalogError, "duplicate semantic symbol"):
            self.load(value)

    def test_stale_revision_fails_closed(self):
        with self.assertRaisesRegex(CatalogError, "source_revision"):
            self.load(artifact(), expected_revision="b" * 40)

    def test_missing_or_invalid_required_fields_fail_closed(self):
        mutations = [
            artifact(source_revision="not-a-sha"),
            artifact(symbols="not-a-list"),
            artifact(symbols=[{"id": "only-an-id"}]),
        ]
        for value in mutations:
            with self.subTest(value=value), self.assertRaises(CatalogError):
                self.load(value)


if __name__ == "__main__":
    unittest.main()
