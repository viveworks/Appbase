"""Strict loading and deterministic lookup over generated Appbase API metadata.

@author Navaneeth Mysore G
"""

from __future__ import annotations

import json
from pathlib import Path
import re
from typing import Any

from .conventions import ConventionError, load_and_validate_conventions


SHA_PATTERN = re.compile(r"[0-9a-f]{40}")
SYMBOL_FIELDS = ("id", "qualified_name", "kind", "signature", "source_path")


class CatalogError(ValueError):
    """Raised when lookup input or repository-derived metadata is unsafe to serve."""


def _read_json(path: Path, label: str) -> dict[str, Any]:
    if not path.is_file():
        raise CatalogError(f"{label} is missing: {path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise CatalogError(f"{label} is not valid JSON: {path}") from error
    if not isinstance(value, dict):
        raise CatalogError(f"{label} root must be an object")
    return value


def _terms(value: str) -> tuple[str, ...]:
    return tuple(re.findall(r"[a-z0-9]+", value.lower()))


def _validate_query(query: object, limit: object) -> tuple[tuple[str, ...], int]:
    if not isinstance(query, str) or not query.strip():
        raise CatalogError("query must be a non-empty string")
    if isinstance(limit, bool) or not isinstance(limit, int) or not 1 <= limit <= 50:
        raise CatalogError("limit must be an integer from 1 through 50")
    return _terms(query), limit


def _rank(items: tuple[dict[str, Any], ...], query: object, limit: object, fields: tuple[str, ...]):
    query_terms, safe_limit = _validate_query(query, limit)
    ranked = []
    for item in items:
        haystack = " ".join(str(item[field]) for field in fields if field in item).lower()
        score = sum(1 for term in query_terms if term in haystack)
        # A partial-token hit is not a relevant result. In particular, every qualified name contains
        # "appbase", so accepting one of several query terms turned arbitrary nonsense containing
        # that word into a full result page. Require the candidate to satisfy the complete query;
        # ranking still breaks ties deterministically for terms that match multiple fields.
        if score == len(query_terms):
            ranked.append((-score, item["id"], item))
    ranked.sort(key=lambda row: (row[0], row[1]))
    return [dict(row[2]) for row in ranked[:safe_limit]]


class DeveloperCatalog:
    """Validated catalog used by transport-neutral MCP tool handlers."""

    def __init__(self, symbols, conventions):
        self._symbols = tuple(symbols)
        self._conventions = tuple(conventions)

    @classmethod
    def from_files(
        cls,
        api_artifact: Path | str,
        convention_seed: Path | str,
        *,
        expected_revision: str,
        repository_root: Path | str | None = None,
    ) -> "DeveloperCatalog":
        artifact = _read_json(Path(api_artifact), "generated API artifact")
        if artifact.get("schema_version") != 1:
            raise CatalogError("generated API artifact schema_version must be 1")
        revision = artifact.get("source_revision")
        if not isinstance(revision, str) or SHA_PATTERN.fullmatch(revision) is None:
            raise CatalogError("generated API artifact source_revision must be a lowercase 40-character SHA")
        if not isinstance(expected_revision, str) or SHA_PATTERN.fullmatch(expected_revision) is None:
            raise CatalogError("expected_revision must be a lowercase 40-character SHA")
        if revision != expected_revision:
            raise CatalogError(
                f"generated API artifact source_revision {revision} does not match expected {expected_revision}"
            )
        symbols = artifact.get("symbols")
        if not isinstance(symbols, list):
            raise CatalogError("generated API artifact symbols must be a list")
        if not symbols:
            raise CatalogError("generated API artifact symbols must not be empty")
        seen = set()
        semantic_symbols = set()
        for index, symbol in enumerate(symbols):
            if not isinstance(symbol, dict):
                raise CatalogError(f"symbol {index} must be an object")
            for field in SYMBOL_FIELDS:
                if not isinstance(symbol.get(field), str) or not symbol[field].strip():
                    raise CatalogError(f"symbol {index} has invalid required field {field}")
            if symbol["id"] in seen:
                raise CatalogError(f"duplicate symbol id: {symbol['id']}")
            seen.add(symbol["id"])
            semantic_key = (symbol["qualified_name"], symbol["signature"])
            if semantic_key in semantic_symbols:
                raise CatalogError(
                    "duplicate semantic symbol: "
                    f"{symbol['qualified_name']} {symbol['signature']}"
                )
            semantic_symbols.add(semantic_key)
        seed_path = Path(convention_seed)
        root = Path(repository_root) if repository_root is not None else seed_path.parent.parent
        try:
            conventions = load_and_validate_conventions(seed_path, root)
        except ConventionError as error:
            raise CatalogError(str(error)) from error
        return cls(symbols, conventions)

    def lookup_api(self, query: object, *, limit: object = 10) -> dict[str, Any]:
        results = _rank(
            self._symbols,
            query,
            limit,
            ("id", "qualified_name", "kind", "signature", "source_path"),
        )
        return {"count": len(results), "results": results}

    def lookup_convention(self, query: object, *, limit: object = 10) -> dict[str, Any]:
        results = _rank(
            self._conventions, query, limit, ("id", "title", "summary", "keywords")
        )
        for item in results:
            item.pop("validation", None)
        return {"count": len(results), "results": results}

    def list_conventions(self) -> dict[str, Any]:
        results = []
        for convention in sorted(self._conventions, key=lambda item: item["id"]):
            public = dict(convention)
            public.pop("validation", None)
            results.append(public)
        return {"count": len(results), "results": results}
