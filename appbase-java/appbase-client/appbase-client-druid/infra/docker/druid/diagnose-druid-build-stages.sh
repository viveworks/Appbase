#!/bin/bash

set -euo pipefail


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
SMOKE_DIR=$(mktemp -d)
trap 'rm -rf "$SMOKE_DIR"' EXIT

cat > "$SMOKE_DIR/Dockerfile" <<'DOCKERFILE'
FROM debian:buster-slim AS packages

ENV DEBIAN_FRONTEND=noninteractive \
    DRUID_VERSION=34.0.0

RUN rm -f /etc/apt/sources.list.d/*.list \
    && printf 'deb [check-valid-until=no] http://archive.debian.org/debian buster main\ndeb [check-valid-until=no] http://archive.debian.org/debian-security buster/updates main\n' > /etc/apt/sources.list \
    && apt-get -o Acquire::Check-Valid-Until=false update -y \
    && apt-get install -y bash curl wget perl gnupg apt-transport-https vim coreutils lsb-release

FROM packages AS java

RUN apt-get -o Acquire::Check-Valid-Until=false -qq update -y \
    && apt-get -qq install -y openjdk-11-jre-headless \
    && apt-get -qq install -y openjdk-11-jdk \
    && java -version

FROM java AS downloads

RUN wget -q --timeout=30 --tries=3 https://downloads.apache.org/druid/${DRUID_VERSION}/apache-druid-${DRUID_VERSION}-bin.tar.gz \
    && wget -q --timeout=30 --tries=3 https://downloads.apache.org/druid/${DRUID_VERSION}/apache-druid-${DRUID_VERSION}-bin.tar.gz.sha512

FROM downloads AS archive

RUN echo "$(cat apache-druid-${DRUID_VERSION}-bin.tar.gz.sha512) apache-druid-${DRUID_VERSION}-bin.tar.gz" \
      | sha512sum --check --status \
    && tar -zxf apache-druid-${DRUID_VERSION}-bin.tar.gz \
    && mv apache-druid-${DRUID_VERSION} /opt/druid
DOCKERFILE

run_stage() {
  local stage=$1
  local description=$2
  local stage_exit

  echo "Starting Druid Dockerfile stage smoke: ${stage} (${description}, timeout=90s)"
  if timeout 90 env DOCKER_BUILDKIT=1 docker build --progress=plain \
      --target "$stage" \
      -t "viveworks.com/infra/local/druid-stage-smoke:${stage}" \
      "$SMOKE_DIR"
  then
    echo "Druid Dockerfile stage smoke completed: ${stage}"
  else
    stage_exit=$?
    echo "Druid Dockerfile stage smoke failed or timed out: ${stage} (exit=${stage_exit})" >&2
    exit "$stage_exit"
  fi
}

run_stage packages "archive apt sources, apt update, and base package install"
run_stage java "OpenJDK 11 install and java -version"
run_stage downloads "Druid archive and checksum downloads"
run_stage archive "checksum verification, archive extraction, and install move"

echo "All Druid Dockerfile stage smokes completed"
