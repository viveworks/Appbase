#!/usr/bin/env bash
#
# aerospike-bc-digest-equivalence.sh — does `aerospike-client-bc-jdk21` compute the SAME key digest
# as the `aerospike-client` it replaces, for the same inputs?
#
# WHY THIS EXISTS. P05/E02's Option E (internal work item 1381, internal work item 1139, owner ruling 2026-10-09) swaps
# `com.aerospike:aerospike-client:7.1.0` for `com.aerospike:aerospike-client-bc-jdk21:8.1.5` to drop
# the GPL `org.gnu:gnu-crypto` dependency. Both jars expose the identical public method
# `com.aerospike.client.util.Crypto.computeDigest(String, Value)` (confirmed by `javap`), backed by
# RIPEMD-160 — a fixed standard algorithm where any correct implementation must produce the same
# digest for the same input. That is a reason to expect equivalence, not a substitute for measuring
# it: a client swap changes more than the crypto backend (major version 7 -> 8), and this is the
# load-bearing operation — every key operation computes this digest for partition routing. A
# digest mismatch would not fail the build; it would silently miss or misroute existing records.
#
# This probe computes the SAME digests through BOTH jars, one at a time (the two clients are
# alternate flavours of the same `com.aerospike.client` package and cannot share a classpath), and
# compares the byte arrays directly. See `aerospike-digest-reachability.sh` for the sibling probe
# this one is modelled on (same classpath-discipline reasoning, same measured-not-assumed posture).
#
# Usage:  aerospike-bc-digest-equivalence.sh [--repo <local-maven-repo>]   (default ~/.m2/repository)
#
# Exit codes:
#   0  measured — prints EQUIVALENT (all digests matched) or MISMATCH (see which key)
#   2  usage
#   3  COULD NOT MEASURE — jars or JDK absent. Never a verdict; cannot-verify is not verified.
#   4  unexpected — one run failed for a reason other than what is being measured
#
# @author Navaneeth Mysore G
# @author Claude
#
set -uo pipefail

say() { echo "aerospike-bc-digest-equivalence: $*"; }
die() { echo "aerospike-bc-digest-equivalence: $2" >&2; exit "$1"; }

REPO="${HOME}/.m2/repository"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      [[ $# -ge 2 && -n "$2" ]] \
        || die 2 "usage: aerospike-bc-digest-equivalence.sh [--repo <local-maven-repo>]"
      REPO="$2"
      shift 2
      ;;
    *) echo "aerospike-bc-digest-equivalence: unknown argument '$1'" >&2; exit 2 ;;
  esac
done

command -v java  >/dev/null 2>&1 || die 3 "COULD NOT MEASURE — no JDK on PATH (java missing)"
command -v javac >/dev/null 2>&1 || die 3 "COULD NOT MEASURE — no JDK on PATH (javac missing)"

# Exact runtime coordinates: the OLD client this repo shipped, and the NEW one Option E swaps to.
OLD_AERO="$REPO/com/aerospike/aerospike-client/7.1.0/aerospike-client-7.1.0.jar"
OLD_GNU="$REPO/org/gnu/gnu-crypto/2.0.1/gnu-crypto-2.0.1.jar"
OLD_LUAJ="$REPO/org/luaj/luaj-jse/3.0/luaj-jse-3.0.jar"
OLD_BCRYPT="$REPO/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"
NEW_AERO="$REPO/com/aerospike/aerospike-client-bc-jdk21/8.1.5/aerospike-client-bc-jdk21-8.1.5.jar"
NEW_BC="$REPO/org/bouncycastle/bcprov-jdk15on/1.69/bcprov-jdk15on-1.69.jar"
NEW_LUAJ="$REPO/org/luaj/luaj-jse/3.0.1/luaj-jse-3.0.1.jar"
NEW_BCRYPT="$REPO/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"

for pair in "aerospike-client:$OLD_AERO" "gnu-crypto:$OLD_GNU" "luaj-jse:$OLD_LUAJ" \
            "jbcrypt:$OLD_BCRYPT" "aerospike-client-bc-jdk21:$NEW_AERO" "bcprov-jdk15on:$NEW_BC" \
            "luaj-jse(new):$NEW_LUAJ" "jbcrypt(new):$NEW_BCRYPT"; do
  [[ -f "${pair#*:}" ]] || die 3 "COULD NOT MEASURE — exact runtime artifact ${pair%%:*} not found at '${pair#*:}'. Run 'mvn dependency:tree' on appbase-client-aerospike first to populate it."
done

say "measuring:"
for j in "$OLD_AERO" "$OLD_GNU" "$OLD_LUAJ" "$OLD_BCRYPT" "$NEW_AERO" "$NEW_BC" "$NEW_LUAJ" "$NEW_BCRYPT"; do
  say "  $(basename "$j")"
done

WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
cat > "$WORK/DigestProbe.java" <<'JAVA'
import com.aerospike.client.Value;
import com.aerospike.client.util.Crypto;
public class DigestProbe {
  public static void main(String[] a) {
    String[] setNames = {"", "testset", "appbase-user-session"};
    String[] keys = {"abc", "user:12345", "", "a".repeat(200)};
    for (String set : setNames) {
      for (String key : keys) {
        byte[] d = Crypto.computeDigest(set, Value.get(key));
        StringBuilder hex = new StringBuilder();
        for (byte b : d) hex.append(String.format("%02x", b));
        System.out.println(set + "|" + key + "=" + hex);
      }
    }
  }
}
JAVA

OLD_CP="$OLD_AERO:$OLD_GNU:$OLD_LUAJ:$OLD_BCRYPT"
NEW_CP="$NEW_AERO:$NEW_BC:$NEW_LUAJ:$NEW_BCRYPT"

old_compile="$(javac -cp "$OLD_CP" -d "$WORK/old" "$WORK/DigestProbe.java" 2>&1)" \
  || die 3 "COULD NOT MEASURE — the probe did not compile against the OLD client ($OLD_AERO):
$old_compile"
mkdir -p "$WORK/new"
new_compile="$(javac -cp "$NEW_CP" -d "$WORK/new" "$WORK/DigestProbe.java" 2>&1)" \
  || die 3 "COULD NOT MEASURE — the probe did not compile against the NEW client ($NEW_AERO):
$new_compile"

old_out="$(java -cp "$WORK/old:$OLD_CP" DigestProbe 2>&1)"; old_rc=$?
[[ $old_rc -eq 0 ]] || die 4 "UNEXPECTED — the OLD-client run did not succeed, so nothing can be concluded:
$old_out"

new_out="$(java -cp "$WORK/new:$NEW_CP" DigestProbe 2>&1)"; new_rc=$?
[[ $new_rc -eq 0 ]] || die 4 "UNEXPECTED — the NEW-client run did not succeed, so nothing can be concluded:
$new_out"

if [[ "$old_out" == "$new_out" ]]; then
  say "EQUIVALENT — all $(echo "$old_out" | wc -l | tr -d ' ') (set, key) digests are byte-identical between aerospike-client:7.1.0 (gnu-crypto) and aerospike-client-bc-jdk21:8.1.5 (Bouncy Castle)."
  exit 0
fi
say "MISMATCH — the two clients computed DIFFERENT digests for at least one (set, key) pair. This is a release blocker, not a style difference: existing Aerospike data would be misrouted."
diff <(echo "$old_out") <(echo "$new_out") | while IFS= read -r line; do say "  $line"; done
exit 1
