#!/bin/bash
#
# appbase-vitess-purge.sh — stop the local Vitess processes and delete its data root.
#
# `vitess_purge` deletes VTDATAROOT recursively. VTDATAROOT is caller-controlled (an env var), so the
# delete is guarded by `_vitess_resolve_purge_target`, which fails CLOSED on any value that is empty,
# non-absolute, whitespace-bearing (multiword), glob-bearing, `..`-traversing, resolves to `/`, or lands
# outside APPBASE_VITESS_ALLOWED_DATA_ROOT. Every purge entry point sources this file and calls
# vitess_purge, so the invariant lives here once. (A10-08 / internal work item 1489.)
#
# @author Navaneeth Mysore G
export APPBASE_VITESS_SCRIPTS_BASEDIR=${APPBASE_VITESS_SCRIPTS_BASEDIR:-$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )}
export VTDATAROOT=${VTDATAROOT-"/Users/nav/Temporary/Data/Vitess"}
# The deletion boundary: the purge target must be, or sit beneath, this root. Defaults to the default
# data root, so an unconfigured purge is safe; override both together to relocate the data area.
export APPBASE_VITESS_ALLOWED_DATA_ROOT=${APPBASE_VITESS_ALLOWED_DATA_ROOT-"/Users/nav/Temporary/Data/Vitess"}
export CELL=${CELL:-'zone1'}
source "${APPBASE_VITESS_SCRIPTS_BASEDIR}/appbase-vitess-stop.sh"

# Collapse repeated slashes and drop a trailing slash (a lone "/" is preserved). Portable to bash 3.2
# (macOS) — no external tools.
function _vitess_norm_path() {
  local p="$1"
  while [ "${p}" != "${p//\/\//\/}" ]; do p="${p//\/\//\/}"; done
  [ "$p" != "/" ] && p="${p%/}"
  printf '%s\n' "$p"
}

# Reject an existing symbolic-link component before relying on textual containment. This is portable
# to the Bash 3.2 shipped by macOS and also covers an intermediate link (for example
# /allowed/link/data -> /outside/data), which a final-component-only check would miss.
function _vitess_reject_symlink_components() {
  local path="$1" component prefix="" remaining="${1#/}"
  while [ -n "$remaining" ]; do
    component="${remaining%%/*}"
    if [ "$remaining" = "$component" ]; then
      remaining=""
    else
      remaining="${remaining#*/}"
    fi
    [ -z "$component" ] && continue
    prefix="${prefix}/${component}"
    if [ -L "$prefix" ]; then
      echo "vitess purge refused: path contains symbolic-link component '$prefix'" >&2
      return 1
    fi
  done
}

# Validate VTDATAROOT and print the single safe path to delete on stdout. Prints nothing and
# returns non-zero on any unsafe value — the caller MUST abort the purge when this fails.
function _vitess_resolve_purge_target() {
  local raw="${VTDATAROOT}"
  local allowed="${APPBASE_VITESS_ALLOWED_DATA_ROOT}"

  if [ -z "$raw" ]; then
    echo "vitess purge refused: VTDATAROOT is empty" >&2; return 1
  fi
  case "$raw" in
    *[[:space:]]*) echo "vitess purge refused: VTDATAROOT contains whitespace: '$raw'" >&2; return 1 ;;
    *'*'*|*'?'*|*'['*|*']'*) echo "vitess purge refused: VTDATAROOT contains glob metacharacters: '$raw'" >&2; return 1 ;;
    */../*|*/..|../*|..) echo "vitess purge refused: VTDATAROOT contains a '..' component: '$raw'" >&2; return 1 ;;
    /*) : ;;
    *) echo "vitess purge refused: VTDATAROOT is not an absolute path: '$raw'" >&2; return 1 ;;
  esac

  # The allowed root is the whole boundary, so validate it too — an empty or non-absolute root would make
  # the containment test below match every path (fail OPEN). Fail closed here, independent of the
  # source-time ${APPBASE_VITESS_ALLOWED_DATA_ROOT:-default} that only masks this at the entry points.
  if [ -z "$allowed" ]; then
    echo "vitess purge refused: allowed data root is empty" >&2; return 1
  fi
  case "$allowed" in
    *[[:space:]]*) echo "vitess purge refused: allowed data root contains whitespace: '$allowed'" >&2; return 1 ;;
    *'*'*|*'?'*|*'['*|*']'*) echo "vitess purge refused: allowed data root contains glob metacharacters: '$allowed'" >&2; return 1 ;;
    */../*|*/..|../*|..) echo "vitess purge refused: allowed data root contains a '..' component: '$allowed'" >&2; return 1 ;;
    /*) : ;;
    *) echo "vitess purge refused: allowed data root is not an absolute path: '$allowed'" >&2; return 1 ;;
  esac

  # Normalize textually (collapse repeated slashes, drop a trailing slash), then reject every existing
  # symlink component before evaluating containment. Avoiding `realpath -m` keeps this macOS-portable.
  local target canon_allowed
  target="$(_vitess_norm_path "$raw")"
  canon_allowed="$(_vitess_norm_path "$allowed")"

  if [ "$target" = "/" ]; then
    echo "vitess purge refused: VTDATAROOT resolves to the filesystem root" >&2; return 1
  fi
  if [ "$canon_allowed" = "/" ]; then
    echo "vitess purge refused: allowed data root resolves to the filesystem root" >&2; return 1
  fi
  _vitess_reject_symlink_components "$target" || return 1
  _vitess_reject_symlink_components "$canon_allowed" || return 1
  if [ "$target" != "$canon_allowed" ] && [[ "$target" != "$canon_allowed"/* ]]; then
    echo "vitess purge refused: '$target' is outside the allowed data root '$canon_allowed'" >&2; return 1
  fi

  printf '%s\n' "$target"
}

# Delete a validated target relative to an opened parent directory. The earlier validation is not a
# capability: an attacker able to rename path components could replace an ancestor after the check and
# before an absolute `rm`. `cd -P` resolves the parent once, the physical-path equality rejects a swap
# to a symlink, and `./$leaf` remains anchored to that directory even if its pathname is renamed later.
# `rm -rf` does not follow a final-component symlink, so a leaf swap removes only the link.
function _vitess_delete_purge_target() {
  local target="$1" parent leaf expected_parent physical_parent
  leaf="${target##*/}"
  parent="${target%/*}"
  [ -n "$parent" ] || parent="/"
  expected_parent="$(_vitess_norm_path "$parent")"

  (
    cd -P -- "$parent" 2>/dev/null || {
      echo "vitess purge refused: target parent is unavailable: '$parent'" >&2
      return 1
    }
    physical_parent="$(pwd -P)" || return 1
    if [ "$physical_parent" != "$expected_parent" ]; then
      echo "vitess purge refused: target parent changed from '$expected_parent' to '$physical_parent'" >&2
      return 1
    fi
    rm -rf -- "./$leaf"
  )
}

function vitess_purge() {
  echo "vitess purge starting..."

  local target
  target="$(_vitess_resolve_purge_target)" || return 1

  stop_vitess_processes
  _vitess_delete_purge_target "$target" || return 1
  zookeeper_clear

  echo "vitess purge done."

  disown -a
}
