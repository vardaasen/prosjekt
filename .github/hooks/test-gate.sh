#!/usr/bin/env bash
set -euo pipefail

HOOK_INPUT=$(cat)

STOP_HOOK_ACTIVE=$(echo "$HOOK_INPUT" | node -e "let input=''; process.stdin.on('data', chunk => input += chunk); process.stdin.on('end', () => console.log(JSON.parse(input).stop_hook_active === true ? 'True' : 'False'))" 2>/dev/null || echo "False")
if [ "$STOP_HOOK_ACTIVE" = "True" ]; then
  echo '{"hookSpecificOutput":{"hookEventName":"Stop"}}'
  exit 0
fi

if mvn test >/dev/null 2>&1; then
  echo '{"hookSpecificOutput":{"hookEventName":"Stop"}}'
else
  echo '{"hookSpecificOutput":{"hookEventName":"Stop","decision":"block","reason":"Maven tests are failing. Fix the implementation until mvn test passes before finishing."}}'
fi
