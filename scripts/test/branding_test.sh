#!/usr/bin/env bash
#
# branding_test.sh — acceptance arms for the Release-1 placeholder brand set (internal work item 1415, plan task N02).
#
# One arm group per acceptance item of internal work item 1415, numbered exactly as the issue numbers them
# (AC1415-1 … AC1415-6), plus EXTRA_ arms that prove the checked-in validator fails closed.
#
# The per-size content check re-renders the source SVG, so it needs the pinned renderer
# (resvg-py, version pinned in docs/branding/validate.py). Without it the validator exits 3 and
# this suite reports the content arms as FAILED, never as passed: "could not check" is not "checked".
#
# Hermetic otherwise: no network, no docker; mutation arms run on a temporary copy.
#
# @author Navaneeth Mysore G
#
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$HERE/../.." && pwd)"
BR="$ROOT/docs/branding"
GUIDE="$BR/README.md"
PASSED=0; FAILED=0
ok()  { PASSED=$((PASSED+1)); echo "  ok   $1"; }
bad() { FAILED=$((FAILED+1)); echo "  FAIL $1"; }
has() { grep -qiE -- "$2" "$1" 2>/dev/null; }

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
validate() { python3 "$BR/validate.py" --root "$1" "${@:2}" >"$TMP/out" 2>&1; }

echo "placeholder brand set (internal work item 1415)"; echo

# ── AC1415-1 · one placeholder direction; logo/icon/wordmark placements; light/dark usage ─────────────
[ -f "$BR/icon.svg" ] && [ "$(find "$BR" -maxdepth 1 -name '*icon*.svg' | wc -l)" -eq 1 ] \
  && ok "AC1415-1: exactly one icon source (one direction, not a set of options)" \
  || bad "AC1415-1: exactly one icon source docs/branding/icon.svg"
[ -f "$BR/lockup-light.svg" ] && [ -f "$BR/lockup-dark.svg" ] \
  && ok "AC1415-1: wordmark lockups exist for light and dark backgrounds" \
  || bad "AC1415-1: lockup-light.svg and lockup-dark.svg"
has "$GUIDE" 'prefers-color-scheme' && has "$GUIDE" 'lockup-light\.svg' && has "$GUIDE" 'lockup-dark\.svg' \
  && ok "AC1415-1: guide documents light/dark selection" \
  || bad "AC1415-1: guide must document light/dark usage (prefers-color-scheme, both lockups)"
has "$GUIDE" '^## .*[Ss]ign-off' && has "$GUIDE" 'internal work item 1142' \
  && ok "AC1415-1: guide records the owner sign-off status on internal work item 1142" \
  || bad "AC1415-1: guide must carry a sign-off section pointing at internal work item 1142"

# ── AC1415-2 · editable sources, reproducible exports, documented replacement points ──────────────────
if validate "$BR" --only sources; then ok "AC1415-2: sources are editable plain SVG (live wordmark text, no embedded raster/script)"
else bad "AC1415-2: sources check — $(tail -3 "$TMP/out" | tr '\n' ' ')"; fi
if validate "$BR" --only exports; then ok "AC1415-2: all five exports match their own size and re-render from the source"
else bad "AC1415-2: exports check — $(tail -3 "$TMP/out" | tr '\n' ' ')"; fi
has "$GUIDE" 'export\.py' && has "$GUIDE" 'validate\.py' && has "$GUIDE" 'resvg-py==' \
  && ok "AC1415-2: guide names the export command, the validator and the pinned renderer" \
  || bad "AC1415-2: guide must name export.py, validate.py and the pinned renderer"
for point in 'id="wordmark"' 'id="tagline"' 'palette\.json' 'id="mark"'; do
  has "$GUIDE" "$point" && ok "AC1415-2: replacement point documented: $point" \
    || bad "AC1415-2: replacement point not documented: $point"
done

# ── AC1415-3 · consistent use (owned files) + placements handed to the owning tasks ───────────────────
if validate "$BR" --only consistency; then ok "AC1415-3: every colour is a palette colour and the lockups embed the icon's exact mark"
else bad "AC1415-3: consistency check — $(tail -3 "$TMP/out" | tr '\n' ' ')"; fi
for owner in 'internal work item 2419' 'internal work item 1416' 'internal work item 1009'; do
  has "$GUIDE" "$owner" && ok "AC1415-3: placement handed to $owner is listed" \
    || bad "AC1415-3: guide must list the placement for $owner"
done

# ── AC1415-4 · contrast and small-size legibility ─────────────────────────────────────────────────────
if validate "$BR" --only contrast; then ok "AC1415-4: every declared colour pair meets its WCAG minimum"
else bad "AC1415-4: contrast check — $(tail -3 "$TMP/out" | tr '\n' ' ')"; fi

# Owner direction: the lockups use the documented Viveworks visual system, while the
# name-independent three-block icon remains unchanged.
missing_tokens=""
for token in '#FAF8F3' '#FFFFFF' '#1B1B18' '#4A4740' '#DDD6C4' '#1A1918' '#232120' '#F2EDE0' '#B3ADA0' '#38352F' '#0F5C56' '#38BAA0'; do
  has "$BR/palette.json" "$token" || missing_tokens="$missing_tokens $token"
done
[ -z "$missing_tokens" ] \
  && ok "AC1415-4: palette carries the documented Viveworks light/dark token set" \
  || bad "AC1415-4: palette is missing Viveworks tokens:$missing_tokens"
if has "$BR/lockup-light.svg" "ui-serif, Georgia, 'Iowan Old Style', 'Times New Roman', serif" \
  && has "$BR/lockup-dark.svg" "ui-serif, Georgia, 'Iowan Old Style', 'Times New Roman', serif" \
  && has "$BR/lockup-light.svg" "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" \
  && has "$BR/lockup-dark.svg" "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"; then
  ok "AC1415-4: both lockups use the Viveworks serif-heading and system-sans body stacks"
else
  bad "AC1415-4: both lockups must use the Viveworks serif-heading and system-sans body stacks"
fi
if has "$BR/lockup-dark.svg" 'id="icon-backing".*fill="#FAF8F3"' \
  && has "$GUIDE" 'teal tile.*light backing' && has "$GUIDE" 'backing.*actual dark surface'; then
  ok "AC1415-4: name-independent icon has an implemented and documented backing on actual dark surfaces"
else
  bad "AC1415-4: dark placement must implement and document the unchanged icon's light backing"
fi
if ! rg -q '#2F5BD3|#A7F3D0' "$BR/icon.svg" "$BR/lockup-light.svg" "$BR/lockup-dark.svg" "$BR/palette.json" \
  && has "$BR/icon.svg" 'id="tile".*fill="#0F5C56"' \
  && has "$BR/icon.svg" 'id="block-top".*fill="#F2EDE0"'; then
  ok "AC1415-4: icon uses Viveworks fills and contains no retired blue/mint palette"
else
  bad "AC1415-4: icon must use the measured Viveworks fills without retired blue/mint"
fi
if validate "$BR" --only small; then ok "AC1415-4: 32 px export keeps every layer distinct (probed pixels, measured contrast)"
else bad "AC1415-4: small-size check — $(tail -3 "$TMP/out" | tr '\n' ' ')"; fi

# ── AC1415-5 · no final trademark or marketing claim; internal work item 1940 stays the checkpoint ──────────────────────
if validate "$BR" --only claims; then ok "AC1415-5: no trademark marks or claims in the assets"
else bad "AC1415-5: claims check — $(tail -3 "$TMP/out" | tr '\n' ' ')"; fi
has "$GUIDE" 'internal work item 1940' && has "$GUIDE" 'placeholder' && ! grep -qE '™|®' "$GUIDE" \
  && ok "AC1415-5: guide defers name/trademark/publication to internal work item 1940 and uses no ™/®" \
  || bad "AC1415-5: guide must cite internal work item 1940, say placeholder, and carry no ™/®"

# ── AC1415-6 · bounded placeholder surface, not a full brand system ───────────────────────────────────
if validate "$BR" --only surface; then ok "AC1415-6: docs/branding holds exactly the bounded file set"
else bad "AC1415-6: surface check — $(tail -3 "$TMP/out" | tr '\n' ' ')"; fi
has "$GUIDE" 'Release 2' && has "$GUIDE" 'marketing' \
  && ok "AC1415-6: guide states what is deferred to Release 2" \
  || bad "AC1415-6: guide must state the Release-2 deferrals"

# ── Whole-validator run ───────────────────────────────────────────────────────────────────────────────
if validate "$BR"; then ok "EXTRA_full_validator: validate.py passes end to end (it is the checked-in gate the plan requires)"
else bad "EXTRA_full_validator: $(tail -5 "$TMP/out" | tr '\n' ' ')"; fi

# ── EXTRA mutation arms — the validator must fail CLOSED (plan N02: omit one size, mis-size one) ──────
mut() { rm -rf "$TMP/m"; cp -R "$BR" "$TMP/m"; }
sed_edit() {
  expression=$1; file=$2
  sed "$expression" "$file" >"$file.tmp" && mv "$file.tmp" "$file"
}

mut; rm -f "$TMP/m/exports/icon-64.png"
if ! validate "$TMP/m" --only exports && grep -q 'missing exports/icon-64.png' "$TMP/out"; then ok "EXTRA_mut_omit_size: a missing 64 px export is reported and fails"
else bad "EXTRA_mut_omit_size: validator did not report the deleted icon-64.png"; fi

mut; cp "$BR/exports/icon-64.png" "$TMP/m/exports/icon-128.png"
if ! validate "$TMP/m" --only exports && grep -q 'icon-128.png is 64x64, expected 128x128' "$TMP/out"; then ok "EXTRA_mut_missize: a 64 px image saved as icon-128.png is reported and fails"
else bad "EXTRA_mut_missize: validator did not report the mis-sized icon-128.png"; fi

# C14-B1/B2: PNG structure/header methods and decoded size are part of the export oracle.
mut; python3 - "$TMP/m/exports/icon-32.png" <<'PY'
import struct, sys, zlib
p = sys.argv[1]; d = bytearray(open(p, "rb").read()); d[26] = 1
d[29:33] = struct.pack(">I", zlib.crc32(d[12:29]) & 0xffffffff)
open(p, "wb").write(d)
PY
if ! validate "$TMP/m" --only small && grep -q 'compression method' "$TMP/out"; then ok "EXTRA_mut_png_compression: unsupported PNG compression fails the standalone small check"
else bad "EXTRA_mut_png_compression: unsupported PNG compression passed"; fi

mut; python3 - "$TMP/m/exports/icon-32.png" <<'PY'
import struct, sys, zlib
p = sys.argv[1]; d = bytearray(open(p, "rb").read()); d[27] = 1
d[29:33] = struct.pack(">I", zlib.crc32(d[12:29]) & 0xffffffff)
open(p, "wb").write(d)
PY
if ! validate "$TMP/m" --only small && grep -q 'filter method' "$TMP/out"; then ok "EXTRA_mut_png_filter_method: unsupported PNG filter method fails the standalone small check"
else bad "EXTRA_mut_png_filter_method: unsupported PNG filter method passed"; fi

mut; python3 - "$TMP/m/exports/icon-32.png" <<'PY'
import sys
p = sys.argv[1]; d = bytearray(open(p, "rb").read()); d[-1] ^= 1
open(p, "wb").write(d)
PY
if ! validate "$TMP/m" --only small && grep -q 'CRC' "$TMP/out"; then ok "EXTRA_mut_png_crc: malformed PNG chunk CRC fails"
else bad "EXTRA_mut_png_crc: malformed PNG chunk structure passed"; fi

mut; head -c 28 "$TMP/m/exports/icon-32.png" > "$TMP/m/exports/icon-32.png.tmp"
mv "$TMP/m/exports/icon-32.png.tmp" "$TMP/m/exports/icon-32.png"
if ! validate "$TMP/m" --only small && grep -q 'not a PNG' "$TMP/out"; then ok "EXTRA_mut_png_truncated: truncated PNG fails closed without a traceback"
else bad "EXTRA_mut_png_truncated: truncated PNG did not fail closed"; fi

mut; python3 - "$TMP/m/exports/icon-32.png" <<'PY'
import struct, sys
p = sys.argv[1]; d = open(p, "rb").read(); pos = 8
while pos < len(d):
    n = struct.unpack(">I", d[pos:pos + 4])[0]
    if d[pos + 4:pos + 8] == b"IEND":
        d = d[:pos]
        break
    pos += 12 + n
open(p, "wb").write(d)
PY
if ! validate "$TMP/m" --only small && grep -q 'terminal IEND' "$TMP/out"; then ok "EXTRA_mut_png_missing_iend: a PNG without IEND fails the structure check"
else bad "EXTRA_mut_png_missing_iend: a PNG without IEND passed"; fi

mut; python3 - "$TMP/m/exports/icon-32.png" <<'PY'
import struct, sys, zlib
p = sys.argv[1]; d = bytearray(open(p, "rb").read()); d[16:20] = struct.pack(">I", 4096)
d[29:33] = struct.pack(">I", zlib.crc32(d[12:29]) & 0xffffffff)
open(p, "wb").write(d)
PY
if ! validate "$TMP/m" --only small && grep -q 'dimensions' "$TMP/out"; then ok "EXTRA_mut_png_dimensions: header dimensions cannot inflate the standalone small check"
else bad "EXTRA_mut_png_dimensions: unbounded PNG dimensions passed"; fi

mut; python3 - "$TMP/m/exports/icon-32.png" <<'PY'
import struct, sys, zlib
p = sys.argv[1]; d = open(p, "rb").read(); pos = 8; out = bytearray(d[:8]); wrote_idat = False
payload = zlib.compress(b"\0" * (32 * (1 + 32 * 4) + 4096))
while pos < len(d):
    n = struct.unpack(">I", d[pos:pos + 4])[0]; kind = d[pos + 4:pos + 8]; end = pos + 12 + n
    if kind == b"IDAT":
        if not wrote_idat:
            out += struct.pack(">I", len(payload)) + kind + payload
            out += struct.pack(">I", zlib.crc32(kind + payload) & 0xffffffff); wrote_idat = True
    else:
        out += d[pos:end]
    pos = end
open(p, "wb").write(out)
PY
if ! validate "$TMP/m" --only small && grep -q 'decompressed data is not exactly' "$TMP/out"; then ok "EXTRA_mut_png_inflate_bound: excess decompressed PNG data fails at the expected scanline bound"
else bad "EXTRA_mut_png_inflate_bound: excess decompressed PNG data passed"; fi

mut; sed_edit 's/#F2EDE0/#FFFFFF/' "$TMP/m/icon.svg"
if ! validate "$TMP/m" --only exports && grep -q 'content' "$TMP/out"; then ok "EXTRA_mut_stale_export: exports no longer matching an edited source fail the content check"
else bad "EXTRA_mut_stale_export: validator passed exports that no longer match the source"; fi

mut; sed_edit 's/#F2EDE0/#0F5C56/' "$TMP/m/icon.svg"; python3 "$TMP/m/export.py" >/dev/null 2>&1
if ! validate "$TMP/m" --only small && grep -q 'pixel (16,9)' "$TMP/out"; then ok "EXTRA_mut_small_layer_lost: a 32 px export whose top block merged into the tile fails the probe"
else bad "EXTRA_mut_small_layer_lost: a 32 px export missing a layer passed the small-size check"; fi

mut; sed_edit 's/#F2EDE0/#38BAA0/' "$TMP/m/icon.svg"; sed_edit 's/"accent": "#F2EDE0"/"accent": "#38BAA0"/' "$TMP/m/palette.json"
python3 "$TMP/m/export.py" >/dev/null 2>&1
if ! validate "$TMP/m" --only small && grep -q 'accent next to tile at 32px measures' "$TMP/out"; then ok "EXTRA_mut_small_low_contrast: a layer present but too close to the tile at 32 px fails"
else bad "EXTRA_mut_small_low_contrast: a low-contrast 32 px layer passed the small-size check"; fi

mut; python3 - "$TMP/m/palette.json" <<'PY'
import json, sys; p = sys.argv[1]; d = json.load(open(p)); d["colors"]["tile"] = "#38BAA0"; json.dump(d, open(p, "w"))
PY
if ! validate "$TMP/m" --only contrast; then ok "EXTRA_mut_low_contrast: a lighter tile below the minimum fails the contrast check"
else bad "EXTRA_mut_low_contrast: a failing tile colour passed"; fi

mut; sed_edit 's/#1B1B18/#123456/' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only consistency; then ok "EXTRA_mut_offpalette: an off-palette colour fails the consistency check"
else bad "EXTRA_mut_offpalette: an off-palette colour passed"; fi

mut; sed_edit 's|>Appbase<|>Appbase™<|' "$TMP/m/lockup-dark.svg"
if ! validate "$TMP/m" --only claims; then ok "EXTRA_mut_tm: a ™ added to the wordmark fails the claims check"
else bad "EXTRA_mut_tm: a ™ in the wordmark passed"; fi

mut; touch "$TMP/m/logo-final.ai"
if ! validate "$TMP/m" --only surface; then ok "EXTRA_mut_surface: an unlisted file in docs/branding fails the surface check"
else bad "EXTRA_mut_surface: an extra file passed the surface check"; fi

mut; sed_edit 's|<text id="wordmark"|<path d="M0 0h1v1z"/><text id="wordmark-x"|' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only sources; then ok "EXTRA_mut_outlined_wordmark: a wordmark that is not live text fails (swappability)"
else bad "EXTRA_mut_outlined_wordmark: a lockup without a live wordmark passed"; fi

mut; sed_edit 's|</g>|</g><text x="10" y="20">A</text>|' "$TMP/m/icon.svg"
if ! validate "$TMP/m" --only sources && grep -q 'icon.svg contains text' "$TMP/out"; then ok "EXTRA_mut_icon_text: a letterform in the icon fails (the icon must survive a rename)"
else bad "EXTRA_mut_icon_text: an icon carrying text passed"; fi

mut; sed_edit 's|id="block-top" x="160"|id="block-top" x="150"|' "$TMP/m/lockup-dark.svg"
if ! validate "$TMP/m" --only consistency && grep -q "lockup-dark.svg's <g id=\"mark\"> differs" "$TMP/out"; then ok "EXTRA_mut_mark_drift: a lockup whose mark drifted from icon.svg fails"
else bad "EXTRA_mut_mark_drift: a drifted lockup mark passed"; fi

# R-2502-1: a processing instruction sits outside the element tree, so the allowlist cannot see it. An
# xml-stylesheet PI makes a browser fetch and apply an external stylesheet; it must be refused.
mut; sed_edit '1a\
<?xml-stylesheet type="text/css" href="https://example.invalid/b.css"?>' "$TMP/m/icon.svg"
if ! validate "$TMP/m" --only sources && grep -q 'icon.svg contains processing instruction(s) <?xml-stylesheet?>' "$TMP/out"; then ok "EXTRA_mut_xml_stylesheet: an xml-stylesheet processing instruction fails the source check"
else bad "EXTRA_mut_xml_stylesheet: an xml-stylesheet processing instruction passed"; fi

# Review findings (security + code-review panel, pre-PR): the source check is an allowlist, so each of
# these must fail. Each arm is the exact bypass the review reproduced against the earlier denylist.
mut; sed_edit 's|<rect id="block-top"\([^/]*\)/>|<rect id="block-top"\1><set attributeName="onmouseover" to="alert(1)"/></rect>|' "$TMP/m/icon.svg"
if ! validate "$TMP/m" --only sources && grep -q '<set>' "$TMP/out"; then ok "EXTRA_mut_smil_handler: SMIL <set attributeName=on…> fails the source allowlist"
else bad "EXTRA_mut_smil_handler: a SMIL event-handler injection passed"; fi

mut; for f in icon lockup-light lockup-dark; do sed_edit 's|fill="#F2EDE0"|style="fill:#123456"|' "$TMP/m/$f.svg"; done
if ! validate "$TMP/m" --only sources && grep -q 'attribute style' "$TMP/out"; then ok "EXTRA_mut_style_offpalette: an off-palette colour via style= fails (it cannot hide from the palette check)"
else bad "EXTRA_mut_style_offpalette: a style= colour passed the source allowlist"; fi

mut; sed_edit 's|<title>|<style>rect{fill:url(https://example.invalid/x.png)}</style><title>|' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only sources && grep -q '<style>' "$TMP/out"; then ok "EXTRA_mut_style_element: a <style> block (external url) fails"
else bad "EXTRA_mut_style_element: a <style> block passed"; fi

mut; sed_edit 's|>Building blocks for Java services<|> <|' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only sources && grep -q 'non-empty <text id="tagline">' "$TMP/out"; then ok "EXTRA_mut_empty_tagline: an empty tagline fails"
else bad "EXTRA_mut_empty_tagline: an empty tagline passed"; fi

# Cloud1 R1 (internal work item 2502): the claims guard must catch positive claims, in the assets and in the guide.
mut; sed_edit 's|<title>Placeholder icon:|<title>Official logo:|' "$TMP/m/icon.svg"
if ! validate "$TMP/m" --only claims && grep -q 'icon.svg matches' "$TMP/out"; then ok "EXTRA_mut_official_title: an 'Official logo' title fails the claims check"
else bad "EXTRA_mut_official_title: an 'Official logo' title passed"; fi

mut; echo "Appbase is a registered trademark of Viveworks. This is the official logo." >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_readme_claim: a trademark claim added to the guide fails the claims check"
else bad "EXTRA_mut_readme_claim: a trademark claim in README.md passed"; fi

# N1: parenthesized trademark marks are claims too, even when attached directly to the name.
mut; echo "Appbase(TM) is great." >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_parenthesized_tm: a (TM) claim added to the guide fails the claims check"
else bad "EXTRA_mut_parenthesized_tm: an Appbase(TM) claim in README.md passed"; fi

# P1: encoded marks render as trademark symbols and must be checked after entity decoding.
mut; sed_edit 's|>Appbase<|>Appbase \&#8482;<|' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only claims && grep -q 'lockup-light.svg matches' "$TMP/out"; then ok "EXTRA_mut_entity_tm_svg: an entity-encoded trademark symbol in an SVG fails the claims check"
else bad "EXTRA_mut_entity_tm_svg: an entity-encoded trademark symbol in an SVG passed"; fi

mut; echo 'Appbase &#174;' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_entity_registered_readme: an entity-encoded registered mark in the guide fails the claims check"
else bad "EXTRA_mut_entity_registered_readme: an entity-encoded registered mark in the guide passed"; fi

# P2: spelled-out marks and qualified official claims are still positive claims.
mut; echo 'Appbase TM' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_bare_tm: a bare TM claim in the guide fails the claims check"
else bad "EXTRA_mut_bare_tm: a bare TM claim in the guide passed"; fi

mut; echo 'Appbase SM' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_bare_sm: a bare SM claim in the guide fails the claims check"
else bad "EXTRA_mut_bare_sm: a bare SM claim in the guide passed"; fi

mut; echo 'Appbase is a trade mark of Viveworks.' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_trade_mark: a spelled-out trade mark claim in the guide fails the claims check"
else bad "EXTRA_mut_trade_mark: a spelled-out trade mark claim in the guide passed"; fi

mut; echo 'Appbase, a trade mark of Viveworks.' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_trade_mark_of: a trade-mark ownership claim in the guide fails the claims check"
else bad "EXTRA_mut_trade_mark_of: a trade-mark ownership claim in the guide passed"; fi

mut; sed_edit 's|>Building blocks for Java services<|>Appbase trade mark<|' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only claims && grep -q 'lockup-light.svg matches' "$TMP/out"; then ok "EXTRA_mut_asset_trade_mark: a spelled-out trade mark in an asset fails the claims check"
else bad "EXTRA_mut_asset_trade_mark: a spelled-out trade mark in an asset passed"; fi

mut; echo 'This is the officially approved logo.' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_officially_approved_logo: a qualified official-logo claim fails the claims check"
else bad "EXTRA_mut_officially_approved_logo: a qualified official-logo claim passed"; fi

# N2 / R-2502-3: prose wrapping must not let a claim evade a phrase matcher. The phrase is one whose
# pattern uses a LITERAL space ("all rights reserved"), so only the per-paragraph whitespace collapse in
# check_claims makes it match; a \s+ pattern would match the wrapped form with or without the collapse.
mut; printf '\nAll rights\nreserved.\n' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches /all rights reserved/' "$TMP/out"; then ok "EXTRA_mut_wrapped_official_logo: a literal-space claim phrase split across lines fails the claims check"
else bad "EXTRA_mut_wrapped_official_logo: a literal-space claim phrase split across lines passed"; fi

# P4: unspaced affirmative trademark forms.
mut; echo 'Appbase, a trademark of Viveworks.' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_trademark_of: an unspaced trademark-ownership claim in the guide fails the claims check"
else bad "EXTRA_mut_trademark_of: 'a trademark of' in the guide passed"; fi

mut; echo 'Appbase is trademarked.' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_trademarked: an 'is trademarked' claim in the guide fails the claims check"
else bad "EXTRA_mut_trademarked: 'is trademarked' in the guide passed"; fi

# R-2502-2: the service-mark sign (literal and entity-encoded) and a (c) copyright notice are claims too.
mut; sed_edit 's|>Appbase<|>Appbase℠<|' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only claims && grep -q 'lockup-light.svg matches' "$TMP/out"; then ok "EXTRA_mut_sm_symbol: a service-mark sign in the wordmark fails the claims check"
else bad "EXTRA_mut_sm_symbol: a service-mark sign in the wordmark passed"; fi

mut; echo 'Appbase &#8480;' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_entity_sm_readme: an entity-encoded service-mark sign in the guide fails the claims check"
else bad "EXTRA_mut_entity_sm_readme: an entity-encoded service-mark sign in the guide passed"; fi

mut; echo '(C) 2026 Viveworks' >> "$TMP/m/README.md"
if ! validate "$TMP/m" --only claims && grep -q 'README.md matches' "$TMP/out"; then ok "EXTRA_mut_copyright_c: a (C) copyright notice in the guide fails the claims check"
else bad "EXTRA_mut_copyright_c: a (C) copyright notice in the guide passed"; fi

mut; sed_edit 's|>Building blocks for Java services<|>The best building blocks for Java services<|' "$TMP/m/lockup-light.svg"
if ! validate "$TMP/m" --only claims && grep -q 'lockup-light.svg matches' "$TMP/out"; then ok "EXTRA_mut_superlative: a superlative marketing tagline fails the claims check"
else bad "EXTRA_mut_superlative: a superlative tagline passed"; fi

# R-2502-7: residual denylist forms. One arm per pattern family; each case is appended separately and
# every case must fail on its own, so one family member cannot mask another.
claims_family() {  # $1 = arm name, $2 = description, then "file|text" cases (file: README.md or an SVG)
  arm=$1; desc=$2; shift 2; missed=""
  for case in "$@"; do
    file=${case%%|*}; text=${case#*|}; mut
    if [ "$file" = README.md ]; then printf '\n%s\n' "$text" >> "$TMP/m/README.md"
    else python3 - "$TMP/m/$file" "$text" <<'PY2'
import sys; p, t = sys.argv[1], sys.argv[2]; s = open(p, encoding="utf-8").read()
open(p, "w", encoding="utf-8").write(s.replace(">Building blocks for Java services<", ">" + t + "<", 1))
PY2
    fi
    if validate "$TMP/m" --only claims || ! grep -q "$file matches" "$TMP/out"; then missed="$missed [$file: $text]"; fi
  done
  if [ -z "$missed" ]; then ok "$arm: $desc"; else bad "$arm: passed:$missed"; fi
}
claims_family EXTRA_mut_trademarks_of "plural, hyphenated and joint trade-mark ownership claims fail" \
  'README.md|Appbase and Foo are trademarks of Viveworks.' 'README.md|Both are trademarks of Viveworks.' \
  'README.md|Appbase, a trade-mark of Viveworks.' 'README.md|Appbase is trade-marked.' \
  'README.md|Appbase is a trade-mark.' \
  'lockup-light.svg|A trade-mark of Viveworks'
claims_family EXTRA_mut_registered_mark "registered mark / registered trade-mark claims fail" \
  'README.md|A registered mark of Viveworks.' 'README.md|Appbase is a registered trade-mark.' \
  'lockup-dark.svg|A registered mark of Viveworks'
claims_family EXTRA_mut_service_mark "spelled-out service mark claims fail" \
  'README.md|Appbase, a service mark of Viveworks.' 'lockup-light.svg|A service mark of Viveworks'
claims_family EXTRA_mut_copyright_variants "spaced, small-capital, fullwidth and Copr. copyright notices fail" \
  'README.md|( c ) 2026 Viveworks' 'lockup-light.svg|( c ) 2026 Viveworks' \
  'README.md|(ᴄ) 2026 Viveworks' 'lockup-dark.svg|(ᴄ) 2026 Viveworks' \
  'README.md|（ｃ） 2026 Viveworks' \
  'README.md|Copr. 2026 Viveworks' 'lockup-light.svg|Copr. 2026 Viveworks'

# R-2502-8: (c) is a copyright notice only when a year or a capitalised name follows it. Enumerations in
# prose must not trip the guard (positive control: these must PASS).
mut; printf '\nPick one of (a), (b) or (c). Then do step (c) again, and (c) repeat it.\n' >> "$TMP/m/README.md"
if validate "$TMP/m" --only claims; then ok "EXTRA_ctrl_enumeration_c: enumerations like '(a), (b) or (c)' and 'step (c)' pass the claims check"
else bad "EXTRA_ctrl_enumeration_c: an enumeration was reported as a claim — $(tail -2 "$TMP/out" | tr '\n' ' ')"; fi

# Cloud1 R4 (internal work item 2502): presence and size are stdlib checks and must be reported even without the renderer.
if [ -x "$TMP/venv/bin/python" ] || python3 -m venv "$TMP/venv" >/dev/null 2>&1; then
  mut
  "$TMP/venv/bin/python" "$BR/validate.py" --root "$TMP/m" --only exports >"$TMP/out" 2>&1; rc=$?
  if [ "$rc" -eq 3 ] && grep -q 'NOT_MEASURED' "$TMP/out"; then ok "EXTRA_no_renderer_intact: intact exports return 3/NOT_MEASURED without the renderer"
  else bad "EXTRA_no_renderer_intact: intact renderer-free validation returned rc=$rc instead of 3"; fi

  mut; rm -f "$TMP/m/exports/icon-64.png"
  "$TMP/venv/bin/python" "$BR/validate.py" --root "$TMP/m" --only exports >"$TMP/out" 2>&1; rc=$?
  if [ "$rc" -eq 1 ] && grep -q 'missing exports/icon-64.png' "$TMP/out"; then ok "EXTRA_no_renderer_still_reports_missing: exit 1 with the missing file named, even without the renderer"
  else bad "EXTRA_no_renderer_still_reports_missing: rc=$rc, missing file not reported without the renderer"; fi
else
  bad "EXTRA_no_renderer_still_reports_missing: could not create a renderer-free venv (NOT MEASURED)"
fi

echo; echo "  passed: $PASSED   failed: $FAILED"; [ "$FAILED" -eq 0 ] || exit 1
