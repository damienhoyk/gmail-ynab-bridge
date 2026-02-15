#!/usr/bin/env bash
set -euo pipefail

BUILDER_IMAGE="paketobuildpacks/builder-jammy-base"

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <REPOSITORY_URI> <GRADLE_BUILT_ARTIFACT>"
  exit 1
fi

REPOSITORY_URI="$1"
GRADLE_BUILT_ARTIFACT="$2"

pack build "${REPOSITORY_URI}" \
  --builder "${BUILDER_IMAGE}" \
  --publish \
  --env "BP_GRADLE_BUILT_ARTIFACT=${GRADLE_BUILT_ARTIFACT}" \
  --volume $HOME/.gradle-packeto:/home/cnb/.gradle:rw
