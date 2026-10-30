#
# Vitess (integration-test backend)
#
# A THIN wrapper over the upstream-official `vitess/vttestserver` image — a single-process, throwaway
# Vitess cluster (vtcombo: vtctld + vttablet + in-process MySQL + vtgate) built for integration tests.
# It replaces the previous image, which was based on the now-deleted `vitess/bootstrap:3-common` and
# cloned vitess + ran `make install` from source on every build (a heavy, network-bound local build).
#
# The tag matches the repository's pinned Vitess v17 clients. In v17, vtcombo HTTP/gRPC listen on
# `:<port>` (all interfaces); only the MySQL listener needs MYSQL_BIND_HOST for Docker publication.
# The digest below binds the registry-manifest choice; live bring-up remains a separate gate.
#
# Keeping a wrapper (rather than running the upstream image directly) preserves the build/run/start/
# stop/remove/exec lifecycle the shared docker-*-all.sh orchestrators drive for this backend, and gives
# a single place to layer any test-local customization later.
#
ARG vttestserver_version="v17.0.0-mysql80"
# Immutable manifest-digest pin. The tag above is mutable — a registry compromise or a legitimate retag
# could make a later build execute different upstream bytes while every offline tag comparison stays
# green. Pinning `FROM` to the manifest digest makes the base bytes load-bearing: the build resolves
# exactly this manifest or fails. Digest resolved daemonlessly from the Docker Hub registry v2 API on
# 2026-09-14; the manifest it names carries config digest
# sha256:73528de1c8c70bcd4f25e78d3c52870c16ce6e1c87c2da22999468ed318b104c — the same config recorded in
# E01 on 2026-09-11, proving this digest is the exact image already verified.
ARG vttestserver_digest="sha256:f6d7a91aad3e8526a1b63d1713c766fe56f0956d7d1ede754837844ed4634005"

FROM "vitess/vttestserver:${vttestserver_version}@${vttestserver_digest}"

# vttestserver persists its data (mysql datadir, logs) under VTDATAROOT; keep it a volume so the
# sibling run-vitess.sh bind-mount lands on it.
VOLUME /vt/vtdataroot

# The upstream /vt/run.sh hardcodes --schema_dir=/vt/schema/. Feeding that directory from a host bind
# mount raced on hosted runners: the files were visible after the crash, but LocalCluster.loadSchema
# transiently observed one keyspace subdirectory as absent and exited 255. Copy the read-only staging
# mount into the image-owned filesystem before starting vttestserver so schema discovery is atomic from
# the server's perspective and independent of bind-mount propagation.
COPY --chown=vitess:vitess appbase-run-vttestserver.sh /vt/appbase-run-vttestserver.sh
RUN chmod 0755 /vt/appbase-run-vttestserver.sh
CMD ["/vt/appbase-run-vttestserver.sh"]
