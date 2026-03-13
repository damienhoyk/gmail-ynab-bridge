#!/bin/bash

# Ensure we don't use a pager and avoid locks for routine audits
# This prevents hangs in environments where the agent is running in a non-TTY shell
GIT="git --no-pager --no-optional-locks"

echo "### Stage Status ###"
$GIT status --short --branch

echo -e "\n### Layer Analysis ###"
STAGED_FILES=$($GIT diff --cached --name-only)

if [ -z "$STAGED_FILES" ]; then
    echo "No files staged."
else
    DOMAIN_COUNT=$(echo "$STAGED_FILES" | grep -E "/domain/" | wc -l | xargs)
    INFRA_COUNT=$(echo "$STAGED_FILES" | grep -E "(-client|-dynamodb|-handler|-event-handler)/" | wc -l | xargs)
    TEST_COUNT=$(echo "$STAGED_FILES" | grep -E "/test/" | wc -l | xargs)
    AFFECTED_MODULES=$(echo "$STAGED_FILES" | cut -d/ -f1 | sort -u | xargs echo)

    echo "domain-files:${DOMAIN_COUNT}"
    echo "infrastructure-files:${INFRA_COUNT}"
    echo "test-files:${TEST_COUNT}"
    echo "affected-modules:${AFFECTED_MODULES}"

    echo -e "\n### Kotlin TODO Check ###"
    TODO_LIST=$(echo "$STAGED_FILES" | grep "\.kt$" | xargs -r grep -nE "TODO|FIXME")
    echo "${TODO_LIST:-none}"

    echo -e "\n### Debt Check ###"
    MANUAL_LOGGING=$(echo "$STAGED_FILES" | grep "\.kt$" | xargs -r grep -E "println\(|System\.out\." | wc -l | xargs)
    echo "manual-logging-detected:${MANUAL_LOGGING}"

    echo -e "\n### Diff Stats ###"
    $GIT diff --cached --stat

    TOTAL_CHANGES=$($GIT diff --cached --shortstat | awk '{print $4 + $6}')
    if [ "${TOTAL_CHANGES:-0}" -gt 300 ]; then
        echo "large-change-warning:true"
    else
        echo "large-change-warning:false"
        if [ "${TOTAL_CHANGES:-0}" -lt 100 ]; then
            echo -e "\n### Detailed Diff (First 50 lines) ###"
            $GIT diff --cached | head -n 50
        fi
    fi
fi

echo -e "\n### Recent History (Limited) ###"
$GIT log -n 5 --pretty=format:"%h %as %an: %s" --compact-summary | head -n 30
