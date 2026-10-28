from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]


class CiWiringTest(unittest.TestCase):
    def test_mcp_tests_are_run_in_existing_offline_shell_job(self):
        workflow = (ROOT / ".github/workflows/ci-verify.yml").read_text(encoding="utf-8")
        job = workflow.split("  offline-shell-suites:", 1)[1].split(
            "\n  config-wizard-console:", 1
        )[0]
        self.assertIn(
            "python3 -B -m unittest discover -s appbase_mcp/tests -v", job
        )
        selector = (ROOT / ".github/scripts/changed-modules.sh").read_text(
            encoding="utf-8"
        )
        selector_test = (ROOT / "scripts/test/ci_changed_modules_test.sh").read_text(
            encoding="utf-8"
        )
        self.assertIn("appbase_mcp/*", selector)
        self.assertIn("EXTRA_mcp_selector", selector_test)


if __name__ == "__main__":
    unittest.main()
