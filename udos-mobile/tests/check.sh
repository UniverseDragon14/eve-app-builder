#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_classes="$(mktemp -d)"
trap 'rm -rf -- "$test_classes"' EXIT
java com.sun.tools.javac.Main -encoding UTF-8 -d "$test_classes" \
  app/src/main/java/com/universaldragon/udosmobile/CommandRouter.java \
  tests/CommandRouterTest.java
java -cp "$test_classes" com.universaldragon.udosmobile.CommandRouterTest
node --test tests/ui.test.cjs
