# Aerospike Community Server 8.2.0.0, maintained by Aerospike.
#
# Provenance: the multi-arch OCI index digest was resolved from registry-1.docker.io on 2026-10-09
# and matches both the exact 8.2.0.0 tag and Aerospike's 8.2/community Docker source. Re-pin from the
# upstream registry only; do not compose old Debian packages onto an unrelated base distribution.
FROM aerospike/aerospike-server:8.2.0.0@sha256:583e882d540f4ba813b67f2167452b4893f025760b7541d2d9a8b5107a2f0324

LABEL "docker-image-name"="aerospike.local"

# Fail the image build if the server binary is absent, has an unresolved dynamic dependency, or
# cannot execute. The previous `grep && exit 1 || true` shape accidentally swallowed its own failure.
RUN set -eux; \
    asd_path="$(command -v asd)"; \
    if ldd "$asd_path" 2>&1 | grep -q 'not found'; then \
      ldd "$asd_path" >&2; \
      exit 1; \
    fi; \
    asd --version
