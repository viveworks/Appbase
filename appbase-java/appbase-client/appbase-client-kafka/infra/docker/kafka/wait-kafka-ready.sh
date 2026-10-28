#!/bin/bash

# Readiness verifier for the single-node Kafka broker started by run-kafka.sh (epic P04·E01 internal work item 1135,
# task N02/internal work item 1292, finding 1292). Sourced by run-kafka.sh; wait_kafka_ready polls the published PLAINTEXT
# port until it accepts a TCP connection, bounded by KAFKA_READY_TIMEOUT seconds (default 60).
#   returns 0            the port accepted a connection
#   returns 1            timeout; stderr names the host:port and what to check
# The probe is `nc -z` when nc is on PATH, otherwise a bash /dev/tcp connect. Bash 3.2 compatible.

kafka_port_open() {
  local host="$1" port="$2"
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 1 "$host" "$port" >/dev/null 2>&1
  else
    (exec 3<>"/dev/tcp/$host/$port") >/dev/null 2>&1
  fi
}

wait_kafka_ready() {
  local host="${1:-localhost}" port="${2:-9092}" timeout="${KAFKA_READY_TIMEOUT:-60}" waited=0
  while ! kafka_port_open "$host" "$port"; do
    if [ "$waited" -ge "$timeout" ]; then
      echo "wait-kafka-ready: broker not accepting connections on $host:$port after ${timeout}s." >&2
      echo "  check: docker logs ${CONTAINER_NAME:-kafka.local} (KRaft format or listener errors);" >&2
      echo "  check: nothing else is bound to port $port (lsof -nP -iTCP:$port -sTCP:LISTEN);" >&2
      echo "  retry with a larger KAFKA_READY_TIMEOUT if the first-run log format is slow." >&2
      return 1
    fi
    sleep 1
    waited=$((waited + 1))
  done
  echo "wait-kafka-ready: broker accepting connections on $host:$port (after ${waited}s)."
  return 0
}
