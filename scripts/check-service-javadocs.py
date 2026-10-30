#!/usr/bin/env python3
"""Check Javadoc completeness and non-boilerplate prose for service API packages.

Detector limits (internal work item 1403 F8), all covered by the manual review gate and not by this script:
* a Javadoc block is attached to the nearest following declaration only, so a stacked or dangling
  earlier block is not reported;
* an @Override callable is exempt from the block-tag check, because it may inherit its documentation;
* overloads are checked one declaration at a time, so prose copied between overloads is only caught
  by the repeated-sentence rule (more than three copies);
* identifier splitting treats distinct class-name words as prose, so copied wrapper prose that differs
  only in a class name can normalize differently;
* the script proves presence, tag coverage and scope, never that the prose is accurate.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import tempfile
from collections import Counter
from pathlib import Path

DEFAULT_PACKAGES = (
    "com.viveworks.appbase.service",
    "com.viveworks.appbase.service.appbase.data",
    "com.viveworks.appbase.service.authorization",
    "com.viveworks.appbase.service.cluster",
    "com.viveworks.appbase.service.exchange",
)
SOURCE_ROOT = Path("appbase-java/appbase-service")
BANNED = (
    "Null arguments are not supported unless",
    "The returned value represents the state visible when this method is invoked.",
    "Creates a X with the collaborators and initial state supplied by the caller.",
    "The instance is not ready for use until",
    "Resolves <x> from the current service state.",
)
DECL_RE = re.compile(r"\b(?:public|protected)\s+")
COMMENT_RE = re.compile(r"/\*\*(.*?)\*/", re.S)
SENTENCE_RE = re.compile(r"(?<=[.!?])\s+")


def java_files(root: Path, package: str) -> list[Path]:
    suffix = Path(*package.split("."))
    return sorted(root.glob(f"*/src/main/java/{suffix}/*.java"))


def expected_types(inventory: Path, packages: tuple[str, ...]) -> dict[str, set[str]]:
    """Read the selected package/type boundary from the generated N01 inventory."""
    data = json.loads(inventory.read_text(encoding="utf-8"))
    matches = [artifact for artifact in data["artifacts"]
               if artifact.get("artifact_id") == "appbase-service-core"]
    if len(matches) != 1:
        raise ValueError("inventory must contain exactly one appbase-service-core artifact")
    package_rows = {row["package"]: set(row["public_types"])
                    for row in matches[0]["packages"]}
    absent = sorted(set(packages) - package_rows.keys())
    if absent:
        raise ValueError("inventory lacks selected package(s): " + ", ".join(absent))
    return {package: package_rows[package] for package in packages}


def code_mask(text: str) -> str:
    """Blank comments and literals while preserving offsets and newlines."""
    pattern = re.compile(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])*'", re.S)
    return pattern.sub(lambda match: re.sub(r"[^\n]", " ", match.group()), text)


def has_attached_javadoc(text: str, position: int) -> bool:
    comments = list(COMMENT_RE.finditer(text, 0, position))
    if not comments:
        return False
    comment = comments[-1]
    # An annotation before the comment means the Javadoc is attached too late.
    previous_line = text[:comment.start()].rstrip().split("\n")[-1].strip()
    if previous_line.startswith("@"):
        return False
    gap = text[comment.end():position]
    gap = re.sub(r"(?m)^[ \t]*// CheckStyle: Builder start[ \t]*$", "", gap)
    annotation = r"@[\w.]+(?:\s*\((?:[^()]|\([^)]*\))*\))?"
    return re.fullmatch(rf"(?:\s|{annotation})*", gap) is not None


def declaration_end(text: str, start: int) -> int:
    parens = 0
    angle = 0
    for index in range(start, len(text)):
        char = text[index]
        if char == "(": parens += 1
        elif char == ")": parens = max(0, parens - 1)
        elif char == "<": angle += 1
        elif char == ">": angle = max(0, angle - 1)
        elif char in "{;" and parens == 0 and angle == 0:
            return index + 1
    return len(text)


def prose_sentences(text: str) -> list[str]:
    sentences: list[str] = []
    for comment in COMMENT_RE.findall(text):
        prose_lines = []
        for line in comment.splitlines():
            line = re.sub(r"^\s*\*?\s?", "", line)
            if line.startswith("@author") or line.startswith("@since"):
                continue
            # A tag's description is prose too. Discard only tag syntax and its identifier/type.
            line = re.sub(r"^@(param|throws|exception)\s+\S+\s*", "", line)
            line = re.sub(r"^@return\s*", "", line)
            prose_lines.append(line)
        cleaned = re.sub(r"\{@\w+\s+([^}]+)\}", r"\1", " ".join(prose_lines))
        cleaned = re.sub(r"<[^>]+>", " ", cleaned)
        for sentence in SENTENCE_RE.split(cleaned):
            # Split identifiers so HTTPService, serviceName, service_name, service.name, and
            # service$name compare like their prose spellings.
            sentence = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", " ", sentence)
            sentence = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", sentence)
            sentence = re.sub(r"[._$]+", " ", sentence)
            normalized = " ".join(re.findall(r"[a-z0-9]+", sentence.lower()))
            if normalized:
                sentences.append(normalized)
    return sentences


def tag_issues(text: str, position: int, end: int, type_name: str) -> list[str]:
    """Return missing block tags for a documented, non-override callable declaration."""
    signature = text[position:end]
    if "(" not in signature:
        return []
    comments = list(COMMENT_RE.finditer(text, 0, position))
    if not comments:
        return []
    comment = comments[-1]
    if "@Override" in text[comment.end():position]:
        return []
    doc = comment.group(1)
    parameters = signature[signature.find("(") + 1:signature.rfind(")")]
    names = []
    parts, start, depth = [], 0, 0
    for index, char in enumerate(parameters):
        if char in "<([":
            depth += 1
        elif char in ">)]":
            depth = max(0, depth - 1)
        elif char == "," and depth == 0:
            parts.append(parameters[start:index])
            start = index + 1
    parts.append(parameters[start:])
    for parameter in parts:
        cleaned = re.sub(r"@[\w.]+(?:\([^)]*\))?", "", parameter)
        words = re.findall(r"[A-Za-z_$][\w$]*", cleaned)
        if words:
            names.append(words[-1])
    issues = [f"@param {name}" for name in names if not re.search(rf"@param\s+{re.escape(name)}\b", doc)]
    before_paren = signature[:signature.find("(")]
    callable_name = re.findall(r"[A-Za-z_$][\w$]*", before_paren)[-1]
    if callable_name != type_name and not re.search(r"\bvoid\s+" + re.escape(callable_name) + r"\s*$", before_paren):
        if "@return" not in doc:
            issues.append("@return")
    throws = signature[signature.rfind(")") + 1:]
    match = re.search(r"\bthrows\s+([^;{]+)", throws)
    if match:
        for exception in re.findall(r"[A-Za-z_$][\w.$]*", match.group(1)):
            simple = exception.rsplit(".", 1)[-1]
            if not re.search(rf"@throws\s+(?:[\w.]+\.)?{re.escape(simple)}\b", doc):
                issues.append(f"@throws {simple}")
    return issues


def check(root: Path, packages: tuple[str, ...], scope: dict[str, set[str]] | None = None) -> tuple[bool, Counter[str]]:
    ok = True
    repeated: Counter[str] = Counter()
    for package in packages:
        missing: list[str] = []
        files = java_files(root, package)
        if scope is not None:
            actual = {path.stem for path in files}
            for name in sorted(scope[package] - actual):
                missing.append(f"inventory type absent from source: {name}")
            for name in sorted(actual - scope[package]):
                missing.append(f"source type absent from inventory: {name}")
        for path in files:
            text = path.read_text(encoding="utf-8")
            declarations = list(DECL_RE.finditer(code_mask(text)))
            # Interface fields and abstract methods are public even without an explicit modifier.
            if re.search(r"(?m)^public\s+interface\s+", text):
                implicit = re.compile(
                    r"(?m)^    (?!public\b|protected\b|private\b|static\b|default\b)"
                    r"(?=[A-Za-z][^\n]*(?:\(|=))")
                declarations.extend(implicit.finditer(text))
            declarations.sort(key=lambda match: match.start())
            if not declarations:
                continue
            for declaration in declarations:
                end = declaration_end(text, declaration.start())
                signature = " ".join(text[declaration.start():end].split())[:120]
                if not has_attached_javadoc(text, declaration.start()):
                    missing.append(f"{path.name}:{signature}")
                else:
                    for issue in tag_issues(text, declaration.start(), end, path.stem):
                        missing.append(f"{path.name}:{signature} missing {issue}")
            repeated.update(prose_sentences(text))
        if not files:
            missing.append("no Java sources found")
        state = "GREEN" if not missing else "RED"
        print(f"AC1403-1 {state} {package}: {len(files)} source files, {len(missing)} missing")
        for item in missing:
            print(f"  missing: {item}")
        ok &= not missing

    violations: list[str] = []
    joined = "\n".join(p.read_text(encoding="utf-8") for pkg in packages for p in java_files(root, pkg))
    for phrase in BANNED:
        if phrase.lower() in joined.lower():
            violations.append(f'banned phrase: "{phrase}"')
    for sentence, count in repeated.items():
        if count > 3:
            violations.append(f"repeated {count} times: {sentence}")
    state = "GREEN" if not violations else "RED"
    print(f"EXTRA_no_boilerplate {state}: {len(violations)} violation(s)")
    for violation in violations:
        print(f"  {violation}")
    return ok and not violations, repeated


def fixture() -> bool:
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        package = "fixture.service"
        src = root / "module/src/main/java/fixture/service"
        src.mkdir(parents=True)
        good = """/** Documented fixture type. */
public class Good {
    /** Stored fixture number. */ public int number;
    /** Builds the fixture. */ public Good() {}
    /** Returns the identifier. @return fixture identifier */ public String id() { return "id"; }
    /** Updates a value. @param value fixture value */ public void set(int value) {}
    /** Reads a value. @return fixture value @throws Exception on failure */
    public int read() throws Exception { return 1; }
}
"""
        (src / "Good.java").write_text(good)
        green, _ = check(root, (package,))
        reds = []
        cases = (
            ("method", "public void absent() {}"),
            ("constructor", "public Bad() {}"),
            ("field", "public int absent;"),
            ("param-tag", "/** Missing parameter tag. */\n    public void absent(int value) {}"),
            ("return-tag", "/** Missing return tag. */\n    public int absent() { return 1; }"),
            ("throws-tag", "/** Missing throws tag. */\n    public void absent() throws Exception {}"),
        )
        for name, declaration in cases:
            body = f"/** Documented type with one deliberately missing {name}. */\npublic class Bad {{\n    {declaration}\n}}\n"
            (src / "Bad.java").write_text(body)
            result, _ = check(root, (package,))
            reds.append(not result)
        ordered = """/** Ordering fixture. */
public class Bad {
 @Deprecated
 /** Misordered documentation. */
 public void wrong() {}
}
"""
        (src / "Bad.java").write_text(ordered)
        ordering, _ = check(root, (package,))
        reds.append(not ordering)
        inline = "/** Documented type. */ public class Bad { public void absent() {} }\n"
        (src / "Bad.java").write_text(inline)
        inline_result, _ = check(root, (package,))
        reds.append(not inline_result)
        (src / "Bad.java").unlink()
        scope = {package: {"Good", "Removed"}}
        removed_result, _ = check(root, (package,), scope)
        reds.append(not removed_result)
        short = "/** Same words. Same words. Same words. Same words. */\npublic class Bad {}\n"
        (src / "Bad.java").write_text(short)
        short_result, _ = check(root, (package,))
        reds.append(not short_result)
        tag_prose = """/** Tag fixture. */
public class Bad {
 /** A. @return repeated tag prose */ public int a() { return 1; }
 /** B. @return repeated tag prose */ public int b() { return 1; }
 /** C. @return repeated tag prose */ public int c() { return 1; }
 /** D. @return repeated tag prose */ public int d() { return 1; }
}
"""
        (src / "Bad.java").write_text(tag_prose)
        tag_result, _ = check(root, (package,))
        reds.append(not tag_result)
        identifier_prose = ("/** Uses HTTPService. Uses HTTP_Service. Uses HTTP.Service. Uses HTTP$Service. */\n"
                            "public class Bad {}\n")
        (src / "Bad.java").write_text(identifier_prose)
        identifier_result, _ = check(root, (package,))
        reds.append(not identifier_result)
        repeated = "This deliberately repeated sentence contains at least eight normalized words."
        boiler = "/** " + " ".join([repeated] * 4) + " */\npublic class Bad {}\n"
        (src / "Bad.java").write_text(boiler)
        red_boilerplate, _ = check(root, (package,))
        success = green and all(reds) and not red_boilerplate
        print(f"FIXTURE {'GREEN' if success else 'RED'}: declaration, tag, ordering, scope, and prose arms rejected")
        return success


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, default=SOURCE_ROOT)
    parser.add_argument("--inventory", type=Path, default=Path("docs/api-inventory/inventory.json"))
    parser.add_argument("--packages", nargs="+", default=list(DEFAULT_PACKAGES))
    parser.add_argument("--fixture", action="store_true")
    args = parser.parse_args()
    if args.fixture:
        return 0 if fixture() else 1
    try:
        scope = expected_types(args.inventory, tuple(args.packages))
    except (OSError, KeyError, TypeError, ValueError, json.JSONDecodeError) as error:
        print(f"AC1403-1 RED inventory: {error}")
        return 1
    passed, repeated = check(args.source_root, tuple(args.packages), scope)
    print("TOP repeated normalized sentences:")
    for sentence, count in repeated.most_common(5):
        print(f"  {count} {sentence}")
    return 0 if passed else 1

if __name__ == "__main__":
    sys.exit(main())
