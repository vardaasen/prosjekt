#!/usr/bin/env bash
#
# Bygger .claude/skills (Claude Code) fra .github/skills (kilden, også brukt
# av Copilot). Rediger skills i .github/skills og kjør dette skriptet; ikke
# rediger .claude/skills direkte.
#
# Claude-spesifikke tilpasninger:
# - code-review heter two-axis-review, fordi Claude Code har en innebygd
#   /code-review-kommando med samme navn.
# - agents/openai.yaml (Codex-metadata) og postgres-skillens nestede
#   CLAUDE.md/AGENTS.md/README.md (bidragsyterguide) tas ikke med.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="$ROOT_DIR/.github/skills"
TARGET="$ROOT_DIR/.claude/skills"

rm -rf "$TARGET"
mkdir -p "$TARGET"
rsync -a \
    --exclude 'agents/openai.yaml' \
    --exclude 'postgres-best-practices/CLAUDE.md' \
    --exclude 'postgres-best-practices/AGENTS.md' \
    --exclude 'postgres-best-practices/README.md' \
    "$SOURCE/" "$TARGET/"
find "$TARGET" -type d -name agents -empty -delete

mv "$TARGET/code-review" "$TARGET/two-axis-review"
sed -i.bak 's/^name: code-review$/name: two-axis-review/' "$TARGET/two-axis-review/SKILL.md"
# Referanser til skillen i andre skills.
# shellcheck disable=SC2016 # backticks er literal markdown, ikke kommandosubstitusjon
grep -rl --include='*.md' -e 'code-review' "$TARGET" | while read -r file; do
    sed -i.bak \
        -e 's#/code-review#/two-axis-review#g' \
        -e 's#`code-review`#`two-axis-review`#g' \
        -e 's#used by code-review#used by two-axis-review#g' \
        -e 's#When code-review checks#When two-axis-review checks#g' \
        -e 's#when code-review checks#when two-axis-review checks#g' \
        "$file"
done
find "$TARGET" -name '*.bak' -delete

echo "Synkroniserte $(find "$TARGET" -name SKILL.md | wc -l | tr -d ' ') skills til .claude/skills."
