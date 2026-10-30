#!/usr/bin/env python3
"""Re-export the placeholder icon PNGs from icon.svg (internal work item 1415, plan task N02, export route (a)).

Writes exports/icon-{32,64,128,256,512}.png, each rendered directly from the vector source at its own
size (never downscaled from a larger PNG). Uses the renderer pinned in validate.py, so the output is
exactly what `validate.py` re-renders and compares.

    pip install resvg-py==0.5.0
    python3 docs/branding/export.py && python3 docs/branding/validate.py

@author Navaneeth Mysore G
@author Claude
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from validate import SIZES, RendererUnavailable, render  # noqa: E402


def main():
    out = HERE / "exports"
    out.mkdir(exist_ok=True)
    try:
        for s in SIZES:
            (out / f"icon-{s}.png").write_bytes(render(HERE / "icon.svg", s))
            print(f"wrote exports/icon-{s}.png")
    except RendererUnavailable as e:
        print(f"export: {e}", file=sys.stderr)
        return 3
    return 0


if __name__ == "__main__":
    sys.exit(main())
