#!/usr/bin/env python3
"""Validate the Release-1 placeholder brand set in docs/branding/ (internal work item 1415, plan task N02).

This is the checked-in, deterministic validation command the plan requires. Every check fails
closed, and the command exits non-zero on any failure.

Checks (select one with --only; all run by default):
  surface      docs/branding holds exactly the bounded file set (EXPECTED_FILES)
  sources      the SVGs are plain, editable XML built only from ALLOWED_TAGS and ALLOWED_ATTRS (an
               allowlist, so script, style, animation, raster images and in-document external
               references are refused by construction); no DOCTYPE/ENTITY; no processing instruction
               other than the leading XML declaration (an <?xml-stylesheet?> sits OUTSIDE the element
               tree, so the allowlist alone cannot see it; it is refused explicitly); the icon has no
               text; each lockup has a live, non-empty <text id="wordmark"> and <text id="tagline">
  exports      exactly one PNG per canonical size (32/64/128/256/512). Each is checked against
               ITS OWN size and against a fresh render of icon.svg at that size.
  consistency  every colour in every SVG is a palette colour; each lockup embeds icon.svg's exact mark
  contrast     every colour pair in palette.json meets its WCAG minimum
  small        the committed 32 px export keeps every layer distinct: probe pixels hold the expected
               palette colours, and adjacent layers keep the declared contrast. It reads the export
               file; that the export is a faithful render of icon.svg is the exports check's job, so
               run both (the default) to know the source itself is legible at 32 px
  claims       no trademark sign or trademark/registration wording in the assets, and no positive
               trademark/ownership/marketing claim (official logo, is a trademark, (c), superlatives)
               in the assets or README.md (internal work item 1940 decides)

The exports check needs the pinned renderer (RENDERER_PIN). Without it, or with a
different version, the command exits 3 and prints NOT_MEASURED. It never passes an unchecked set.

Exit codes: 0 all checks pass · 1 a check failed · 2 usage error · 3 renderer unavailable.

Stdlib only, apart from the renderer. The PNG decoder below handles the 8-bit RGB/RGBA,
non-interlaced PNGs that the renderer writes, and refuses anything else.

@author Navaneeth Mysore G
"""
import argparse
import html
import json
import re
import struct
import sys
import unicodedata
import zlib
from pathlib import Path
import xml.etree.ElementTree as ET

RENDERER_PIN = "resvg-py==0.5.0"
SIZES = (32, 64, 128, 256, 512)
MAX_CHANNEL_DELTA = 2  # tolerance for a re-render; anti-aliasing is deterministic, so this is headroom only
EXPECTED_FILES = {
    "README.md", "palette.json", "icon.svg", "lockup-light.svg", "lockup-dark.svg",
    "export.py", "validate.py",
    *(f"exports/icon-{s}.png" for s in SIZES),
}
SVGS = ("icon.svg", "lockup-light.svg", "lockup-dark.svg")
SVG_NS = "{http://www.w3.org/2000/svg}"
# An ALLOWLIST, not a denylist: a denylist of <script>/on* was bypassed in review by SMIL
# (<set attributeName="onmouseover" .../>) and by style="fill:..." (off-palette, unchecked).
ALLOWED_TAGS = {"svg", "title", "g", "rect", "text"}
ALLOWED_ATTRS = {
    "svg": {"width", "height", "viewBox"},
    "title": set(),
    "g": {"id", "transform"},
    "rect": {"id", "x", "y", "width", "height", "rx", "ry", "fill"},
    "text": {"id", "x", "y", "font-family", "font-size", "font-weight", "fill"},
}
CLAIM_PATTERNS = [r"™", r"®", r"\(tm\)", r"\(r\)", r"trade\s*mark", r"registered"]
# These unambiguously assert or mark trademark status, so they apply to prose as well as assets.
# Bare "trademark" remains asset-only because the guide must be able to explain that it makes no
# trademark claim and that internal work item 1940 owns the eventual decision.
UNIVERSAL_TRADEMARK_PATTERNS = [r"™", r"®", r"\(tm\)", r"\(r\)", r"registered\s+trademark"]
# Claim-SHAPED phrases: a positive trademark, ownership or marketing claim. Applied to the assets AND
# to README.md, whose prose must be able to say "no trademark claim" without tripping the bare words.
#
# Matching runs on the text as written AND on its NFKC normalisation, so compatibility forms such as a
# fullwidth "（ｃ）" are caught without losing the raw signs (NFKC turns "™" into "TM", which "\bTM\b"
# would miss when the sign is attached to the name). "ᴄ" (U+1D04) has no NFKC decomposition, so the
# copyright pattern names it explicitly.
#
# KNOWN FALSE POSITIVES (R-2502-8) — this is a denylist, it fails closed, and it does not parse
# negation. An editor who trips one of these should rephrase rather than widen an exception:
#   - negated claims: "Appbase is not trademarked", "This is not a trademark of X" — write
#     "makes no trademark claim" instead, which matches nothing below;
#   - an enumeration item "(c)" followed by a year or a Capitalised word, e.g. "(c) Configure ..." —
#     write "c)", "(iii)" or a list. "(c)" followed by a lowercase word, punctuation or nothing, as in
#     "(a), (b) or (c)" and "step (c)", is NOT treated as a copyright notice and passes.
POSITIVE_CLAIM_PATTERNS = [
    r"™", r"®", r"℠", r"©", r"\bcopyright\b", r"\bcopr\.", r"all rights reserved",
    # "(c)", "( c )" or "(ᴄ)" as a copyright notice: followed by a year or a Capitalised name.
    r"\(\s*[cᴄ]\s*\)\s*(?:\d{4}\b|(?-i:[A-Z]))",
    r"\b(TM|SM)\b", r"\bis (a|an|the) (registered )?trade[\s-]*mark", r"\btrade[\s-]*marks?\s+of\b",
    r"\btrade[\s-]*marked\b", r"\bservice[\s-]*marks?\b",
    r"\bregistered\s+(?:(?:trade|service)[\s-]*)?marks?\b",
    r"\bofficial\w*\s+(?:\w+\s+)?(logo|brand|mark|icon|wordmark)",
    r"\b(best|leading|world-class|best-in-class|industry-leading|fastest|#1)\b",
]
CHECKS = ("surface", "sources", "exports", "consistency", "contrast", "small", "claims")


class RendererUnavailable(Exception):
    pass


# ── colour maths (WCAG 2.x relative luminance) ────────────────────────────────────────────────────────
def hex_rgb(h):
    h = h.lstrip("#")
    if not re.fullmatch(r"[0-9A-Fa-f]{6}", h):
        raise ValueError(f"not a #RRGGBB colour: #{h}")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def luminance(rgb):
    def lin(c):
        c = c / 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (lin(c) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a, b):
    la, lb = luminance(a), luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


# ── minimal PNG reader ────────────────────────────────────────────────────────────────────────────────
def png_size(data):
    if (len(data) < 33 or data[:8] != b"\x89PNG\r\n\x1a\n"
            or data[8:12] != b"\x00\x00\x00\r" or data[12:16] != b"IHDR"):
        raise ValueError("not a PNG")
    (stored_crc,) = struct.unpack(">I", data[29:33])
    if zlib.crc32(data[12:29]) & 0xffffffff != stored_crc:
        raise ValueError("bad PNG IHDR CRC")
    return struct.unpack(">II", data[16:24])


def png_decode(data, expected_dimensions=None):
    """Return (width, height, channels, bytearray of pixels) for 8-bit RGB/RGBA non-interlaced PNGs."""
    w, h = png_size(data)
    if expected_dimensions is not None and (w, h) != expected_dimensions:
        raise ValueError(f"PNG dimensions {w}x{h}, expected {expected_dimensions[0]}x{expected_dimensions[1]}")
    if not (1 <= w <= max(SIZES) and 1 <= h <= max(SIZES)):
        raise ValueError(f"PNG dimensions {w}x{h} exceed the supported 1..{max(SIZES)} range")
    depth, ctype, comp, filt, interlace = data[24], data[25], data[26], data[27], data[28]
    if depth != 8 or ctype not in (2, 6) or interlace != 0:
        raise ValueError(f"unsupported PNG (depth={depth} type={ctype} interlace={interlace})")
    if comp != 0:
        raise ValueError(f"unsupported PNG compression method {comp}")
    if filt != 0:
        raise ValueError(f"unsupported PNG filter method {filt}")
    ch = 3 if ctype == 2 else 4
    pos, chunks, saw_idat, ended_idat, saw_iend = 8, [], False, False, False
    while pos < len(data):
        if len(data) - pos < 12:
            raise ValueError("truncated PNG chunk")
        (n,) = struct.unpack(">I", data[pos:pos + 4])
        kind = data[pos + 4:pos + 8]
        end = pos + 12 + n
        if end > len(data):
            raise ValueError(f"truncated PNG {kind.decode('ascii', 'replace')} chunk")
        payload = data[pos + 8:pos + 8 + n]
        (stored_crc,) = struct.unpack(">I", data[pos + 8 + n:end])
        if zlib.crc32(kind + payload) & 0xffffffff != stored_crc:
            raise ValueError(f"bad PNG {kind.decode('ascii', 'replace')} CRC")
        if pos == 8 and (kind != b"IHDR" or n != 13):
            raise ValueError("PNG must begin with one 13-byte IHDR chunk")
        if pos != 8 and kind == b"IHDR":
            raise ValueError("duplicate PNG IHDR chunk")
        if kind == b"IDAT":
            if ended_idat:
                raise ValueError("PNG IDAT chunks must be consecutive")
            saw_idat = True
            chunks.append(payload)
        elif saw_idat and kind != b"IEND":
            ended_idat = True
        if kind == b"IEND":
            if n != 0 or not saw_idat:
                raise ValueError("invalid PNG IEND chunk")
            saw_iend = True
            pos = end
            break
        if kind not in (b"IHDR", b"IDAT") and not (kind[0] & 0x20):
            raise ValueError(f"unsupported critical PNG chunk {kind.decode('ascii', 'replace')}")
        pos = end
    if not saw_iend or pos != len(data):
        raise ValueError("PNG has no terminal IEND or has trailing data")
    stride = w * ch
    expected_bytes = h * (stride + 1)
    decompressor = zlib.decompressobj()
    raw = decompressor.decompress(b"".join(chunks), expected_bytes + 1)
    if (len(raw) != expected_bytes or not decompressor.eof or decompressor.unconsumed_tail
            or decompressor.unused_data):
        raise ValueError(f"PNG decompressed data is not exactly {expected_bytes} bytes")
    out = bytearray(h * stride)
    prev = bytearray(stride)
    i = 0
    for y in range(h):
        f = raw[i]; i += 1
        line = bytearray(raw[i:i + stride]); i += stride
        for x in range(stride):
            a = line[x - ch] if x >= ch else 0
            b = prev[x]
            c = prev[x - ch] if x >= ch else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + ((a + b) >> 1)) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
            elif f != 0:
                raise ValueError(f"bad PNG filter {f}")
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return w, h, ch, out


def pixel(img, x, y):
    w, _h, ch, px = img
    o = (y * w + x) * ch
    return tuple(px[o:o + ch]) if ch == 4 else tuple(px[o:o + 3]) + (255,)


# ── renderer (the only non-stdlib dependency) ─────────────────────────────────────────────────────────
def render(svg_path, size):
    try:
        from importlib.metadata import version
        import resvg_py
    except ImportError:
        raise RendererUnavailable(f"renderer not installed: pip install {RENDERER_PIN}")
    want = RENDERER_PIN.split("==")[1]
    have = version("resvg-py")
    if have != want:
        raise RendererUnavailable(f"renderer version {have} != pinned {want}: pip install {RENDERER_PIN}")
    return bytes(resvg_py.svg_to_bytes(svg_path=str(svg_path), width=size, height=size,
                                       skip_system_fonts=True))


# ── checks ────────────────────────────────────────────────────────────────────────────────────────────
def load_palette(root):
    return json.loads((root / "palette.json").read_text(encoding="utf-8"))


def check_surface(root, fail):
    found = {p.relative_to(root).as_posix() for p in root.rglob("*") if p.is_file()
             and "__pycache__" not in p.parts}
    for f in sorted(EXPECTED_FILES - found):
        fail(f"surface: missing {f}")
    for f in sorted(found - EXPECTED_FILES):
        fail(f"surface: unexpected file {f} (the placeholder set is bounded; see README.md)")


class _PIRecordingBuilder(ET.TreeBuilder):
    """A TreeBuilder that records every processing-instruction target the parser reports."""

    def __init__(self):
        super().__init__()
        self.pis = []

    def pi(self, target, data=None):
        self.pis.append(f"<?{target}?>")
        return super().pi(target, data)


def parse_svg(root, name, fail):
    path = root / name
    if not path.is_file():
        fail(f"sources: missing {name}")
        return None
    text = path.read_text(encoding="utf-8")
    if re.search(r"<!DOCTYPE|<!ENTITY", text, re.I):
        fail(f"sources: {name} declares a DOCTYPE/ENTITY")
        return None
    # ET.fromstring silently DROPS processing instructions, so an <?xml-stylesheet href=...?> (which a
    # browser fetches and applies) would never reach the element allowlist. Parse with a target that
    # records every PI, before or inside the root; the leading XML declaration is not a PI to expat.
    builder = _PIRecordingBuilder()
    try:
        parser = ET.XMLParser(target=builder)
        parser.feed(text)
        svg = parser.close()
    except ET.ParseError as e:
        fail(f"sources: {name} is not well-formed XML ({e})")
        return None
    if builder.pis:
        fail(f"sources: {name} contains processing instruction(s) {', '.join(builder.pis)}; only the "
             f"leading XML declaration is allowed (an xml-stylesheet PI loads an external stylesheet)")
        return None
    return svg


def check_sources(root, fail):
    for name in SVGS:
        svg = parse_svg(root, name, fail)
        if svg is None:
            continue
        for el in svg.iter():
            if not el.tag.startswith(SVG_NS):
                fail(f"sources: {name} has a non-SVG element {el.tag}")
                continue
            tag = el.tag[len(SVG_NS):]
            if tag not in ALLOWED_TAGS:
                fail(f"sources: {name} contains <{tag}>, which is not an allowed tag "
                     f"(sources must be self-contained static vector shapes)")
                continue
            for k in el.attrib:
                if k not in ALLOWED_ATTRS[tag]:
                    fail(f"sources: {name} <{tag}> has attribute {k}, which is not allowed")
        texts = {el.get("id"): el for el in svg.iter(f"{SVG_NS}text")}
        if name == "icon.svg":
            w, h = int(svg.get("width", 0)), int(svg.get("height", 0))
            if w < 512 or h < 512:
                fail(f"sources: icon.svg is {w}x{h}; the source must be at least 512x512")
            if texts:
                fail("sources: icon.svg contains text; the icon must stay name-independent")
        else:
            wm = texts.get("wordmark")
            if wm is None or not (wm.text or "").strip():
                fail(f"sources: {name} has no live <text id=\"wordmark\"> (the name must stay text-swappable)")
            tl = texts.get("tagline")
            if tl is None or not (tl.text or "").strip():
                fail(f"sources: {name} has no non-empty <text id=\"tagline\">")


def check_exports(root, fail, img_cache):
    # Phase 1, stdlib only: presence and each export's OWN dimensions, for every size, before any
    # render — so a missing or mis-sized file is reported even when the renderer is unavailable.
    present = []
    for s in SIZES:
        name = f"exports/icon-{s}.png"
        p = root / name
        if not p.is_file():
            fail(f"exports: missing {name}")
            continue
        data = p.read_bytes()
        try:
            w, h = png_size(data)
        except ValueError as e:
            fail(f"exports: {name}: {e}")
            continue
        if (w, h) != (s, s):
            fail(f"exports: {name} is {w}x{h}, expected {s}x{s}")
            continue
        present.append((s, name, data))
    # Phase 2, needs the pinned renderer: per-size content equivalence with the source.
    for s, name, data in present:
        got = png_decode(data, (s, s))
        ref = png_decode(render(root / "icon.svg", s), (s, s))
        worst = max(abs(a - b) for a, b in zip(to_rgba(got), to_rgba(ref)))
        if worst > MAX_CHANNEL_DELTA:
            fail(f"exports: {name} content differs from a fresh render of icon.svg at {s}px "
                 f"(max channel delta {worst} > {MAX_CHANNEL_DELTA}); re-run export.py")
        img_cache[s] = got


def to_rgba(img):
    w, h, ch, px = img
    if ch == 4:
        return px
    out = bytearray()
    for i in range(0, len(px), 3):
        out += px[i:i + 3] + b"\xff"
    return out


def svg_colours(svg):
    for el in svg.iter():
        for k in ("fill", "stroke", "stop-color", "color"):
            v = el.get(k)
            if v and v.lower() not in ("none", "currentcolor", "transparent"):
                yield v


def mark_signature(svg):
    mark = next((el for el in svg.iter(f"{SVG_NS}g") if el.get("id") == "mark"), None)
    if mark is None:
        return None
    return [(c.tag, sorted(c.attrib.items())) for c in mark.iter()]


def check_consistency(root, fail):
    pal = {v.upper() for v in load_palette(root)["colors"].values()}
    sigs = {}
    for name in SVGS:
        svg = parse_svg(root, name, fail)
        if svg is None:
            continue
        for c in svg_colours(svg):
            if c.upper() not in pal:
                fail(f"consistency: {name} uses {c}, which is not in palette.json")
        sigs[name] = mark_signature(svg)
    icon = sigs.get("icon.svg")
    if icon is None:
        fail("consistency: icon.svg has no <g id=\"mark\">")
        return
    for name in SVGS[1:]:
        if name in sigs and sigs[name] != icon:
            fail(f"consistency: {name}'s <g id=\"mark\"> differs from icon.svg's")


def check_contrast(root, fail):
    pal = load_palette(root)
    cols = pal["colors"]
    for pair in pal["pairs"]:
        r = contrast(hex_rgb(cols[pair["fg"]]), hex_rgb(cols[pair["bg"]]))
        if r < pair["min"]:
            fail(f"contrast: {pair['fg']} on {pair['bg']} is {r:.2f}:1, below {pair['min']}:1")


def check_small(root, fail, img_cache):
    spec = load_palette(root)["small_size_probes"]
    cols = {k: hex_rgb(v) for k, v in load_palette(root)["colors"].items()}
    s = spec["size"]
    img = img_cache.get(s)
    if img is None:
        p = root / f"exports/icon-{s}.png"
        if not p.is_file():
            fail(f"small: missing exports/icon-{s}.png")
            return
        img = png_decode(p.read_bytes(), (s, s))
    seen = []
    for pr in spec["probes"]:
        r, g, b, a = pixel(img, pr["x"], pr["y"])
        want = cols[pr["expect"]]
        if a != 255 or max(abs(r - want[0]), abs(g - want[1]), abs(b - want[2])) > MAX_CHANNEL_DELTA:
            fail(f"small: {s}px pixel ({pr['x']},{pr['y']}) is rgba({r},{g},{b},{a}), "
                 f"expected {pr['expect']} {want}")
        seen.append((pr["expect"], (r, g, b)))
    for (na, ca), (nb, cb) in zip(seen, seen[1:]):
        if na != nb:
            cr = contrast(ca, cb)
            if cr < spec["min_adjacent_contrast"]:
                fail(f"small: {na} next to {nb} at {s}px measures {cr:.2f}:1, "
                     f"below {spec['min_adjacent_contrast']}:1")


def check_claims(root, fail):
    for name in SVGS + ("palette.json", "README.md"):
        p = root / name
        if not p.is_file():
            continue
        text = p.read_text(encoding="utf-8")
        # Markdown and XML prose may wrap phrases at arbitrary columns. Collapse the whitespace inside
        # each paragraph to single spaces, so a pattern written with a literal space ("all rights
        # reserved") still matches a phrase wrapped across lines. Paragraph breaks are kept as "\n\n",
        # which a literal-space pattern cannot cross; a \s+ pattern still can, which errs toward
        # failing and is therefore safe.
        text = "\n\n".join(" ".join(paragraph.split())
                           for paragraph in re.split(r"(?:\r?\n[ \t]*\r?\n)+", text))
        text = html.unescape(text)
        base_pats = UNIVERSAL_TRADEMARK_PATTERNS if name == "README.md" else CLAIM_PATTERNS
        pats = base_pats + POSITIVE_CLAIM_PATTERNS
        nfkc = unicodedata.normalize("NFKC", text)
        for pat in pats:
            if re.search(pat, text, re.I) or re.search(pat, nfkc, re.I):
                fail(f"claims: {name} matches /{pat}/ — the placeholder makes no trademark, "
                     f"ownership or marketing claim (internal work item 1940)")


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--root", default=str(Path(__file__).resolve().parent),
                    help="the branding directory (default: this script's directory)")
    ap.add_argument("--only", choices=CHECKS, help="run one check")
    args = ap.parse_args(argv)
    root = Path(args.root)
    failures = []
    fail = failures.append
    img_cache = {}
    checks = [args.only] if args.only else list(CHECKS)
    try:
        for c in checks:
            if c == "exports":
                check_exports(root, fail, img_cache)
            elif c == "small":
                check_small(root, fail, img_cache)
            else:
                globals()[f"check_{c}"](root, fail)
    except RendererUnavailable as e:
        print(f"NOT_MEASURED: {e}", file=sys.stderr)
        for f in failures:
            print(f"FAIL {f}")
        return 1 if failures else 3
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as e:
        fail(f"error: {type(e).__name__}: {e}")
    for f in failures:
        print(f"FAIL {f}")
    print(f"branding validate: {len(checks)} check(s), {len(failures)} failure(s)")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
