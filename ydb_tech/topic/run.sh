#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
topic_sdk_version=$(mvn -q -Dstyle.color=never help:evaluate -Dexpression=project.version -DforceStdout)
mvn -B -DskipTests install
mvn -B -f ydb_tech/topic/pom.xml -Dydb.sdk.version="$topic_sdk_version" compile exec:java
