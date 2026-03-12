#!/usr/bin/env bash
set -euo pipefail

MODULE=${1:?Usage: $0 <module-name> <aws-function-name> [handler-override]}
FUNCTION=${2:?Usage: $0 <module-name> <aws-function-name> [handler-override]}
HANDLER=${3:-}

echo "Packaging $MODULE"
./gradlew ":$MODULE:packageFunction" -q
ZIP=$(ls "$MODULE"/build/distributions/*-native.zip | head -n 1)

if [[ -z "$HANDLER" ]]; then
    FILE=$(find "$MODULE/src/main/kotlin" -name "*Handler.kt" | head -n 1)
    PKG=$(grep -m1 "^package " "$FILE" | cut -d' ' -f2 | tr -d ';')
    HANDLER="$PKG.$(basename "$FILE" .kt)::handleRequest"
fi

echo "Deploying to $FUNCTION ($HANDLER)"
aws lambda update-function-code --function-name "$FUNCTION" --zip-file "fileb://$ZIP" --no-cli-pager > /dev/null
aws lambda wait function-updated --function-name "$FUNCTION"

aws lambda update-function-configuration --function-name "$FUNCTION" --handler "$HANDLER" --no-cli-pager > /dev/null
aws lambda wait function-updated --function-name "$FUNCTION"

aws lambda publish-version --function-name "$FUNCTION" --no-cli-pager > /dev/null
echo "Done"
