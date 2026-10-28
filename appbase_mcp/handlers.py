"""Dependency-free handlers that a FastMCP or other transport adapter can register.

@author Navaneeth Mysore G
"""

from __future__ import annotations

from .catalog import DeveloperCatalog


class LookupHandlers:
    """Thin handler boundary; construction validates every served artifact first."""

    def __init__(self, catalog: DeveloperCatalog):
        self._catalog = catalog

    def lookup_api(self, query: str, limit: int = 10):
        return self._catalog.lookup_api(query, limit=limit)

    def lookup_convention(self, query: str, limit: int = 10):
        return self._catalog.lookup_convention(query, limit=limit)

    def list_conventions(self):
        return self._catalog.list_conventions()
