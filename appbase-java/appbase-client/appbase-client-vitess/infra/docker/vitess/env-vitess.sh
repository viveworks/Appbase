#!/bin/bash

# --- Image -------------------------------------------------------------------
# Upstream-official Vitess test image. See the Vitess section of
# docs/spec/1.0.0/P04/decisions/E01-image-versions.md for the full rationale.
#
# `vitess/vttestserver` is a single-process, throwaway Vitess cluster purpose-built for integration
# tests: it brings up vtcombo (vtctld + vttablet + an in-process MySQL + vtgate) and exposes a vtgate
# MySQL endpoint — with NO local source build. This replaces the stale, deleted `vitess/bootstrap:3-common`
# base whose Dockerfile cloned vitess and ran `make install` on every build.
#
# The tag is held to the repository's pinned `vitess.version` (17.0.0 — root pom.xml). Upstream v17's
# HTTP and gRPC servers listen on `:<port>` (all interfaces); MYSQL_BIND_HOST below opens the one
# listener that otherwise defaults to loopback, the vtgate MySQL wire endpoint.
export DOCKER_IMAGE="vitess/vttestserver:v17.0.0-mysql80"
# Immutable manifest digest of DOCKER_IMAGE. The Dockerfile pins `FROM …@${VITESS_IMAGE_DIGEST}` so the
# base bytes cannot silently change under a registry retag; V2c makes this pin load-bearing (Dockerfile
# digest == this value == the digest recorded in E01). Resolved daemonless 2026-09-14.
export VITESS_IMAGE_DIGEST="sha256:f6d7a91aad3e8526a1b63d1713c766fe56f0956d7d1ede754837844ed4634005"
export TAG="viveworks.com/infra/local/vitess"     # thin local wrapper; V2b enforces its Dockerfile ARG matches DOCKER_IMAGE
export HOSTNAME="vitess.local"
export CONTAINER_NAME="${HOSTNAME}"
export DATA_VOLUME="${CONTAINER_NAME}-data"
export VOLUME_MOUNT="/vt/vtdataroot"
export VITESS_SCHEMA_MOUNT="/vt/appbase-schema"
export VITESS_KEYSPACES="appbase-vitess-test-keyspace,appbase-stacktest-unsharded-keyspace,appbase-stacktest-sharded-keyspace"
export VITESS_NUM_SHARDS="1,1,1"

# --- Ports -------------------------------------------------------------------
# vttestserver derives every port from one base (PORT): vtcombo HTTP/status is PORT, its gRPC port is
# PORT+1, and the vtgate MySQL-protocol port is PORT+3. We pin the base so the vtgate MySQL port lands
# on 15306 — the port the Vitess JDBC service-test resources bind to. The R2DBC MariaDB resources stay
# on MariaDB's own 3306; pointing one at 15306 would be an explicit alternate Vitess target, not today.
#
# ⚠️ 15306 is vtgate speaking the MySQL WIRE PROTOCOL. It is deliberately NOT MariaDB's own 3306:
# MariaDB (appbase-client-jdbc/infra/docker/mariadb) is a SEPARATE backend on 3306. Vitess/vtgate and
# MariaDB are distinct services; the two ports stay distinct so that distinction stays honest.
export VITESS_BASE_PORT=15303          # vtcombo HTTP=15303, gRPC=15304, vtgate MySQL=15306 (base+3)
export VTGATE_MYSQL_PORT=15306         # vtgate MySQL wire (= VITESS_BASE_PORT + 3) — the modules' expectation
export VTCTLD_GRPC_PORT=15999          # vtctld control-plane gRPC (legacy scripts/local lifecycle)
export VTGATE_GRPC_PORT=15991          # vtgate data-plane gRPC used by the client YAML
export VITESS_WEB_PORT=15000           # host port for the vtcombo status dashboard
