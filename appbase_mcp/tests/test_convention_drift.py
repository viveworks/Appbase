import json
from pathlib import Path
import tempfile
import unittest

from appbase_mcp.conventions import ConventionError, load_and_validate_conventions


ROOT = Path(__file__).resolve().parents[2]
SEED = ROOT / "appbase_mcp" / "conventions.json"


class ConventionDriftTest(unittest.TestCase):
    def test_repository_seed_matches_current_java_and_maven_semantics(self):
        conventions = load_and_validate_conventions(SEED, ROOT)
        self.assertEqual(
            {
                "live-test-system-property",
                "stacktest-default-skip-fast-allowlist",
            },
            {item["id"] for item in conventions},
        )

    def test_missing_evidence_is_rejected(self):
        with self.assertRaisesRegex(ConventionError, "evidence path"):
            load_and_validate_conventions(SEED, ROOT / "does-not-exist")

    def mutated_seed(self, mutation):
        value = json.loads(SEED.read_text(encoding="utf-8"))
        mutation(value)
        temp = tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False)
        self.addCleanup(Path(temp.name).unlink, missing_ok=True)
        with temp:
            json.dump(value, temp)
        return Path(temp.name)

    def test_empty_or_wrong_typed_public_fields_are_rejected(self):
        mutations = (
            lambda value: value["conventions"][0].update(title=""),
            lambda value: value["conventions"][0].update(keywords="live"),
            lambda value: value["conventions"][0].update(examples=[]),
        )
        for mutation in mutations:
            with self.subTest(mutation=mutation), self.assertRaises(ConventionError):
                load_and_validate_conventions(self.mutated_seed(mutation), ROOT)

    def test_live_public_example_must_bind_to_validated_evidence(self):
        def mutate(value):
            value["conventions"][0]["examples"][0]["annotation"] = "@Disabled"

        with self.assertRaisesRegex(ConventionError, "public example"):
            load_and_validate_conventions(self.mutated_seed(mutate), ROOT)

    def test_maven_public_examples_must_bind_to_both_validated_controls(self):
        def mutate(value):
            value["conventions"][1]["examples"][1]["execution"] = "another-execution"

        with self.assertRaisesRegex(ConventionError, "public examples"):
            load_and_validate_conventions(self.mutated_seed(mutate), ROOT)


if __name__ == "__main__":
    unittest.main()
