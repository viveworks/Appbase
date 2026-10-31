#!/bin/bash

echo "Running firebase auth emulator"

ls -lR /home/node/app

cd /home/node/app/firebase
echo "PWD=${PWD}"
cat firebase.json
cat .firebaserc

firebase --version
firebase emulators:start --import=/home/node/app/firebase/export --only auth
