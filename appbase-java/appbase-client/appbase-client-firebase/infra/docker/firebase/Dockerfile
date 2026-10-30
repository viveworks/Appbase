# Pinned (not :lts-alpine): firebase-tools 12.6.2's bundled superstatic@9.2.0 declares
# engines.node "18 || 20 || 22"; the floating :lts-alpine tag has since moved to Node 24, which
# superstatic refuses at `yarn global add` time (found 2026-10-09, reproducing the
# Appbase#1138 close-gate's live-environment bring-up). Node 22 is the newest version still inside
# that declared range.
FROM node:22-alpine

ENV FIREBASE_TOOLS_VERSION=12.6.2
ENV HOME=/home/node

EXPOSE 4000
EXPOSE 9099

RUN apk --no-cache add openjdk11-jre bash && \
    yarn global add firebase-tools@${FIREBASE_TOOLS_VERSION} && \
    yarn cache clean && \
    firebase -V && \
    java -version && \
    chown -R node:node $HOME

USER node

RUN mkdir -p $HOME/app/firebase/export

COPY firebase-entrypoint.sh $HOME/app/firebase/firebase-entrypoint.sh
COPY firebase.json $HOME/app/firebase/firebase.json
COPY firebaserc $HOME/app/firebase/.firebaserc
COPY export/ $HOME/app/firebase/export

VOLUME $HOME/.cache
VOLUME $HOME/app/firebase/export

WORKDIR $HOME/app/firebase

ENTRYPOINT $HOME/app/firebase/firebase-entrypoint.sh
