#!/bin/bash

echo ":::STAGE_STATUS:::"
git status --porcelain=v1

echo -e "\n:::LAYER_ANALYSIS:::"
STAGED_FILES=$(git diff --cached --name-only)
echo "domain_files:$(echo "$STAGED_FILES" | grep -c "domain/" || echo 0)"
echo "infrastructure_files:$(echo "$STAGED_FILES" | grep -c "infrastructure/" || echo 0)"

echo -e "\n:::KOTLIN_TODO_CHECK:::"
git diff --cached --name-only | grep "\.kt$" | xargs grep -nE "TODO|FIXME" || echo "No technical debt found."

echo -e "\n:::DIFF_NUMSTAT:::"
git diff --cached --numstat

echo -e "\n:::RECENT_HISTORY:::"
git log -n 5 --format="%h|%s"