#!/usr/bin/env python3
"""Offline acceptance check for the appbase-core Javadoc leaf."""

import argparse
import importlib.util
import json
from pathlib import Path
import re
import sys
from collections import Counter


ROOT = Path(__file__).resolve().parents[1]
INVENTORY = ROOT / "docs/api-inventory/inventory.json"
GENERATOR = ROOT / "scripts/api-inventory.py"
DEFAULT_PACKAGES = (
    "com.viveworks.appbase.core",
    "com.viveworks.appbase.core.bootstrap",
    "com.viveworks.appbase.core.di.guice",
    "com.viveworks.appbase.core.jmx",
    "com.viveworks.appbase.core.microprofile",
    "com.viveworks.appbase.core.service",
    "com.viveworks.appbase.core.service.configuration",
)

BANNED_PROSE = (
    "null arguments are not supported unless the corresponding parameter is explicitly documented as optional.",
    "the returned value represents the state visible when this method is invoked.",
    "the instance is not ready for use until all required collaborators have been supplied.",
)
BANNED_PATTERNS = (
    re.compile(r"creates a .+ with the collaborators and initial state supplied by the caller\.", re.I),
    re.compile(r"resolves .+ from the current service state\.", re.I),
)
# Existing package prose retained by this comment-only leaf.  The ceiling is deliberate: scanning
# final sources remains commit-independent, while one more copy still fails the check.
REPEATED_BASELINE_CEILINGS = {
    "x it does not exist returns the x that is passed as argument": 12,
    "x there is a matching x loaded using x this will take higher precedence": 12,
    "x the builder used to build this object": 5,
}


def normalized_javadoc_sentences(text):
    """Return comparable prose sentences while ignoring tags and markup."""
    blocks = re.findall(r"/\*\*(.*?)\*/", text, flags=re.DOTALL)
    sentences = []
    for block in blocks:
        prose = []
        for line in block.splitlines():
            line = re.sub(r"^\s*\*\s?", "", line).strip()
            if not line or line.startswith("@"):
                continue
            prose.append(line)
        joined = " ".join(prose)
        joined = re.sub(r"\{@(?:link|linkplain|code|literal)\s+([^}]+)\}", " X ", joined)
        joined = re.sub(r"<[^>]+>", " ", joined)
        for sentence in re.split(r"(?<=[.!?])\s+", joined):
            plain = re.sub(r"[^A-Za-z0-9]+", " ", sentence).strip()
            if len(plain.split()) < 8:
                continue
            plain = re.sub(r"\b(?:[a-z]+[A-Z][A-Za-z0-9]*|[A-Z][A-Za-z0-9]+)\b", "X", plain)
            sentences.append(plain.lower())
    return sentences


def flattened_prose(text):
    """Javadoc/comment text with line-wrap markers removed and whitespace collapsed, so a banned phrase
    cannot dodge the check by being wrapped across comment lines."""
    unwrapped = re.sub(r"\s*\n\s*(?:\*+/?|//+)?\s*", " ", text)
    return re.sub(r"\s+", " ", unwrapped)


def check_no_boilerplate(texts):
    combined = "\n".join(flattened_prose(text) for text in texts)
    failures = []
    lowered = combined.lower()
    for phrase in BANNED_PROSE:
        if phrase in lowered:
            failures.append(f'banned phrase: "{phrase}"')
    for pattern in BANNED_PATTERNS:
        if pattern.search(combined):
            failures.append(f'banned phrase pattern: "{pattern.pattern}"')
    counts = Counter(
        sentence
        for text in texts
        for sentence in normalized_javadoc_sentences(text)
    )
    failures.extend(
        f'repeated {count} times: "{sentence}"'
        for sentence, count in counts.most_common()
        if count > REPEATED_BASELINE_CEILINGS.get(sentence, 3)
    )
    return failures, counts


def load_generator():
    spec = importlib.util.spec_from_file_location("api_inventory", GENERATOR)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Check class and public-member Javadoc for selected appbase-core packages.")
    parser.add_argument(
        "--package", action="append", dest="packages",
        help="exact package to check; repeat for each whole package")
    parser.add_argument(
        "--boilerplate-fixture", type=Path,
        help="run EXTRA_no_boilerplate against this fixture instead of package sources")
    parser.add_argument(
        "--show-repeated", action="store_true",
        help="print the five most common normalized prose sentences")
    args = parser.parse_args(argv)
    selected = tuple(args.packages or DEFAULT_PACKAGES)

    if args.boilerplate_fixture:
        prose_inputs = [args.boilerplate_fixture.read_text(encoding="utf-8")]
    else:
        source_root = ROOT / "appbase-java/appbase-core/src/main/java"
        prose_inputs = []
        for package_name in selected:
            package_dir = source_root / package_name.replace(".", "/")
            prose_inputs.extend(
                path.read_text(encoding="utf-8")
                for path in sorted(package_dir.glob("*.java"))
            )
    boilerplate_failures, sentence_counts = check_no_boilerplate(prose_inputs)
    if args.show_repeated:
        for sentence, count in sentence_counts.most_common(5):
            print(f"EXTRA_no_boilerplate: COUNT {count}: {sentence}")
    if boilerplate_failures:
        for failure in boilerplate_failures:
            print(f"EXTRA_no_boilerplate: FAIL: {failure}", file=sys.stderr)
        return 1
    print("EXTRA_no_boilerplate: PASS: no banned or over-repeated prose sentences")

    if args.boilerplate_fixture:
        return 0

    recorded = json.loads(INVENTORY.read_text(encoding="utf-8"))
    artifact = next(
        item for item in recorded["artifacts"]
        if item.get("artifact_id") == "appbase-core")
    recorded_packages = {item["package"]: item for item in artifact["packages"]}

    generator = load_generator()
    fresh, _ = generator.build(str(ROOT))
    fresh_artifact = next(
        item for item in fresh["artifacts"]
        if item.get("artifact_id") == "appbase-core")
    fresh_packages = {item["package"]: item for item in fresh_artifact["packages"]}

    failures = []
    checked_types = 0
    checked_members = 0
    for package_name in selected:
        baseline = recorded_packages.get(package_name)
        current = fresh_packages.get(package_name)
        if baseline is None or baseline["classification"] != "supported-public":
            failures.append(f"{package_name}: not an inventory-listed supported-public package")
            continue
        if current is None:
            failures.append(f"{package_name}: absent from the fresh source scan")
            continue
        if current["public_types"] != baseline["public_types"]:
            failures.append(f"{package_name}: published type list drifted from inventory.json")
        checked_types += current["public_type_count"]
        checked_members += current["public_members"]
        if current["undocumented_types"]:
            failures.append(
                f"{package_name}: {current['undocumented_types']} published type(s) lack Javadoc")
        if current["undocumented_members"]:
            failures.append(
                f"{package_name}: {current['undocumented_members']} public member(s) lack Javadoc")

    if failures:
        for failure in failures:
            print(f"AC1402-1: FAIL: {failure}", file=sys.stderr)
        return 1
    print(
        "AC1402-1: PASS: inventory-listed published types and public members "
        f"are documented ({len(selected)} package(s), {checked_types} types, {checked_members} members)")
    print("AC1402-2: AUTHORED_NOT_EXECUTED: strict Maven Javadoc generation is a host/CI check")
    return 0


if __name__ == "__main__":
    sys.exit(main())
