#!/bin/bash

export DOCKER_IMAGE="docker.elastic.co/elasticsearch/elasticsearch:8.5.3"
export HOSTNAME="elasticsearch.local"
export CONTAINER_NAME="${HOSTNAME}"
export VOLUME_MOUNT="/usr/share/elasticsearch/data"

## A NAMED VOLUME, not a host bind-mount (internal work item 522). The Elasticsearch image runs as uid 1000
## (elasticsearch), not root. Under the rootless daemon in the P-046 devcontainer, container-root maps
## to the dev user, so a bind-mounted directory that dev created shows up inside the nested container
## as root-owned 0755 -- and uid 1000 cannot create data/node.lock, so ES aborts a few seconds after
## `docker run --detach` has already returned 0. Docker initialises a NAMED volume from the image with
## the image's own ownership, so ES owns its data directory on both the host and in the container.
## Redis does not hit this despite also mounting a volume: the redis image runs as root, which maps to
## dev. The discriminator is the IMAGE'S USER, not whether a volume is mounted.
export DATA_VOLUME="appbase-elasticsearch-data"

appbase_elasticsearch_ready() {
    curl --fail --silent --show-error --connect-timeout 2 --max-time 8 \
        'http://127.0.0.1:9200/_cluster/health?wait_for_status=yellow&timeout=5s' >/dev/null
}
