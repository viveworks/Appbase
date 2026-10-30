#!/bin/bash

echo "Running Druid"
exec "/opt/druid/bin/supervise" -c "/opt/druid-docker/druid-supervise.conf" -d "/opt/druid-volume/var"