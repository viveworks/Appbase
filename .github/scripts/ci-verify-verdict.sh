#!/usr/bin/env bash
# Fail-closed verdict for the ci-verify receipt. Inputs are supplied as environment variables.
#
# @author Navaneeth Mysore G
# @author Claude
set -euo pipefail
required=(HEAD_SHA MODULES RUN_HOSTED_TIERS ISOLATED_CAMPAIGN_SCOPE RUN_UNIT RUN_SHELL RUN_CONSOLE RUN_RELEASE_REHEARSAL CONSOLE_READY SELECTOR_MODIFIED_BY_PR CHANGED_RESULT COMPILE_RESULT UNIT_RESULT SHELL_RESULT CONSOLE_RESULT RELEASE_REHEARSAL_RESULT COMPILE_TESTED_SHA UNIT_TESTED_SHA SHELL_TESTED_SHA CONSOLE_TESTED_SHA RELEASE_REHEARSAL_TESTED_SHA)
# bash 3.2-portable indirect expansion: set-but-empty is valid.
for name in "${required[@]}"; do [ "${!name+x}" = x ] || { echo "missing input: $name" >&2; exit 2; }; done

verdict=FAIL
tested_sha=UNVERIFIED
valid=true
[[ "$RUN_UNIT" =~ ^(true|false)$ && "$RUN_SHELL" =~ ^(true|false)$ && "$RUN_CONSOLE" =~ ^(true|false)$ ]] || valid=false
[[ "$RUN_HOSTED_TIERS" =~ ^(true|false)$ ]] || valid=false
[[ "$ISOLATED_CAMPAIGN_SCOPE" =~ ^(true|false)$ ]] || valid=false
[[ "$RUN_RELEASE_REHEARSAL" =~ ^(true|false)$ ]] || valid=false
if [[ "$RUN_HOSTED_TIERS" == true ]]; then
  [[ "$MODULES" == ALL && "$RUN_UNIT" == true && "$RUN_SHELL" == true && "$RUN_CONSOLE" == true ]] || valid=false
fi
[[ "$SELECTOR_MODIFIED_BY_PR" =~ ^(true|false)$ ]] || valid=false
[[ "$CHANGED_RESULT" == success ]] || valid=false
[[ "$COMPILE_RESULT" == success || ( ( "$RUN_HOSTED_TIERS" == false || "$MODULES" == NONE ) && "$COMPILE_RESULT" == skipped ) ]] || valid=false
[[ "$UNIT_RESULT" == success || ( ( "$RUN_HOSTED_TIERS" == false || "$MODULES" == NONE || "$RUN_UNIT" == false ) && "$UNIT_RESULT" == skipped ) ]] || valid=false
[[ "$SHELL_RESULT" == success || ( ( "$RUN_HOSTED_TIERS" == false || "$RUN_SHELL" == false ) && "$SHELL_RESULT" == skipped ) ]] || valid=false
[[ "$CONSOLE_RESULT" == success || ( ( "$RUN_HOSTED_TIERS" == false || "$RUN_CONSOLE" == false ) && "$CONSOLE_RESULT" == skipped ) ]] || valid=false
[[ ( "$RUN_RELEASE_REHEARSAL" == true && "$RELEASE_REHEARSAL_RESULT" == success ) ||
   ( "$RUN_RELEASE_REHEARSAL" == false && "$RELEASE_REHEARSAL_RESULT" == skipped ) ]] || valid=false

executed=()
[[ "$COMPILE_RESULT" == success ]] && executed+=("$COMPILE_TESTED_SHA")
[[ "$UNIT_RESULT" == success ]] && executed+=("$UNIT_TESTED_SHA")
[[ "$SHELL_RESULT" == success ]] && executed+=("$SHELL_TESTED_SHA")
[[ "$CONSOLE_RESULT" == success ]] && executed+=("$CONSOLE_TESTED_SHA")
[[ "$RELEASE_REHEARSAL_RESULT" == success ]] && executed+=("$RELEASE_REHEARSAL_TESTED_SHA")
sha_state=unverified
sha_seen=false
sha_empty=false
sha_mismatch=false
# bash 3.2 + set -u: an empty array expansion is "unbound"; the ${a[@]+...} guard is portable.
for sha in ${executed[@]+"${executed[@]}"}; do
  sha_seen=true
  if [[ -z "$sha" ]]; then
    sha_empty=true
    valid=false
  elif [[ "$sha" != "$HEAD_SHA" ]]; then sha_mismatch=true; valid=false
  fi
done
if [[ "$sha_mismatch" == true ]]; then sha_state=mismatch
elif [[ "$sha_seen" == true && "$sha_empty" == false ]]; then sha_state=verified
fi
if [[ "$sha_state" == mismatch ]]; then tested_sha=MISMATCH
elif [[ "$sha_state" == verified ]]; then tested_sha="$HEAD_SHA"
fi
if [[ "$valid" == true ]]; then
  if [[ "$RUN_HOSTED_TIERS" == false || ( "$MODULES" == ALL && "$RUN_UNIT" == false ) ]]; then
    verdict=NOT_VERIFIED
    if [[ "$ISOLATED_CAMPAIGN_SCOPE" == true ]]; then verdict=NOT_APPLICABLE; fi
  elif [[ "$SELECTOR_MODIFIED_BY_PR" == true ]]; then verdict=PASS_WITH_SELECTOR_MODIFIED
  elif [[ "$RUN_CONSOLE" == true && "$CONSOLE_RESULT" == success && "$CONSOLE_READY" == false ]]; then verdict=PASS_WITH_ADVISORY_SKIP
  else verdict=PASS
  fi
fi
echo "verdict=$verdict"
echo "tested_sha=$tested_sha"
echo "tier_tested_shas=compile:${COMPILE_TESTED_SHA:-<empty>},unit:${UNIT_TESTED_SHA:-<empty>},shell:${SHELL_TESTED_SHA:-<empty>},console:${CONSOLE_TESTED_SHA:-<empty>},release-rehearsal:${RELEASE_REHEARSAL_TESTED_SHA:-<empty>}"
[[ "$verdict" != FAIL && "$verdict" != NOT_VERIFIED ]]
