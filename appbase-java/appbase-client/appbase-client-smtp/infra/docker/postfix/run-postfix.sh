#!/bin/bash


BASEDIR=$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )
source $BASEDIR/env-postfix.sh


docker-compose up


echo "Setting up user appbase-test-user@postfix.local"
./setup.sh email add appbase-test-user@postfix.local password
echo "Status=$?"


echo "Post-installation notes were in a removed README.adoc and are not part of the public export."
