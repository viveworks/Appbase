"""Semantic drift checks for the repository-derived Appbase convention seed.

@author Navaneeth Mysore G
"""

from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


class ConventionError(ValueError):
    """Raised when convention evidence no longer matches repository behavior."""


def _required_file(root: Path, relative: object) -> Path:
    if not isinstance(relative, str) or not relative or Path(relative).is_absolute() or ".." in Path(relative).parts:
        raise ConventionError(f"invalid evidence path: {relative!r}")
    path = root / relative
    if not path.is_file():
        raise ConventionError(f"evidence path is missing: {relative}")
    return path


def _validate_live_gate(validation: dict, root: Path) -> None:
    path = _required_file(root, validation.get("path"))
    property_name = validation.get("property")
    if not isinstance(property_name, str) or not property_name:
        raise ConventionError("live gate validation requires a property")
    source = path.read_text(encoding="utf-8")
    annotation = re.compile(
        r'@EnabledIfSystemProperty\s*\(\s*named\s*=\s*"'
        + re.escape(property_name)
        + r'"\s*,\s*matches\s*=\s*"true"\s*\)'
    )
    if annotation.search(source) is None:
        raise ConventionError(f"live gate annotation drifted in {validation['path']}")


def _bind_live_example(convention: dict) -> None:
    validation = convention["validation"]
    expected = {
        "path": validation["path"],
        "annotation": (
            '@EnabledIfSystemProperty(named = "'
            + validation["property"]
            + '", matches = "true")'
        ),
    }
    if expected not in convention["examples"]:
        raise ConventionError(
            f"convention {convention['id']} public example does not bind validated live evidence"
        )


def _validate_stacktest_selection(validation: dict, root: Path) -> None:
    root_pom = _required_file(root, validation.get("root_pom"))
    module_pom = _required_file(root, validation.get("module_pom"))
    root_tree = ET.parse(root_pom)
    module_tree = ET.parse(module_pom)
    root_values = [element.text.strip() for element in root_tree.iter() if element.tag.endswith("appbase-stacktest.skipTests") and element.text]
    if "true" not in root_values or root_values.count("false") < 2:
        raise ConventionError("stacktest skip default/profile overrides drifted")
    executions = []
    for execution in module_tree.iter():
        if not execution.tag.endswith("execution"):
            continue
        identifiers = [item.text.strip() for item in execution if item.tag.endswith("id") and item.text]
        if identifiers == [validation.get("execution_id")]:
            executions.append(execution)
    if len(executions) != 1:
        raise ConventionError("stacktest fast allowlist execution is missing or duplicated")
    execution = executions[0]
    skip_values = [item.text.strip() for item in execution.iter() if item.tag.endswith("skipTests") and item.text]
    includes = [item.text.strip() for item in execution.iter() if item.tag.endswith("include") and item.text]
    expected = validation.get("includes")
    if skip_values != ["false"] or not isinstance(expected, list) or includes != expected:
        raise ConventionError("stacktest fast allowlist selection drifted")
    java_files = {path.name for path in (root / "appbase-stacktest").glob("src/**/*.java")}
    missing = [include for include in expected if Path(include).name not in java_files]
    if missing:
        raise ConventionError(f"stacktest fast allowlist names missing tests: {missing}")


def _bind_stacktest_examples(convention: dict) -> None:
    validation = convention["validation"]
    expected = (
        {
            "path": validation["root_pom"],
            "property": "appbase-stacktest.skipTests",
        },
        {
            "path": validation["module_pom"],
            "execution": validation["execution_id"],
        },
    )
    if any(item not in convention["examples"] for item in expected):
        raise ConventionError(
            f"convention {convention['id']} public examples do not bind validated Maven controls"
        )


def _validate_public_fields(convention: dict, index: int) -> None:
    for field in ("id", "title", "summary"):
        if not isinstance(convention.get(field), str) or not convention[field].strip():
            raise ConventionError(f"convention {index} has invalid {field}")
    for field in ("keywords", "examples"):
        value = convention.get(field)
        if not isinstance(value, list) or not value:
            raise ConventionError(f"convention {index} has invalid {field}")
    if any(not isinstance(keyword, str) or not keyword.strip() for keyword in convention["keywords"]):
        raise ConventionError(f"convention {index} has invalid keywords")
    if any(not isinstance(example, dict) or not example for example in convention["examples"]):
        raise ConventionError(f"convention {index} has invalid examples")


def load_and_validate_conventions(seed_path: Path | str, repository_root: Path | str):
    path = Path(seed_path)
    try:
        document = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise ConventionError(f"convention seed is not valid JSON: {path}") from error
    if not isinstance(document, dict) or document.get("schema_version") != 1:
        raise ConventionError("convention seed schema_version must be 1")
    conventions = document.get("conventions")
    if not isinstance(conventions, list) or not conventions:
        raise ConventionError("convention seed conventions must be a non-empty list")
    seen = set()
    validators = {
        "java_enabled_if_system_property": (_validate_live_gate, _bind_live_example),
        "maven_stacktest_selection": (_validate_stacktest_selection, _bind_stacktest_examples),
    }
    for index, convention in enumerate(conventions):
        if not isinstance(convention, dict):
            raise ConventionError(f"convention {index} must be an object")
        for field in ("id", "title", "summary", "keywords", "examples", "validation"):
            if field not in convention:
                raise ConventionError(f"convention {index} is missing {field}")
        _validate_public_fields(convention, index)
        if convention["id"] in seen:
            raise ConventionError(f"duplicate convention id: {convention['id']}")
        seen.add(convention["id"])
        validation = convention["validation"]
        if not isinstance(validation, dict) or validation.get("type") not in validators:
            raise ConventionError(f"convention {convention['id']} has unsupported validation")
        validate_evidence, bind_examples = validators[validation["type"]]
        validate_evidence(validation, Path(repository_root))
        bind_examples(convention)
    return tuple(conventions)
