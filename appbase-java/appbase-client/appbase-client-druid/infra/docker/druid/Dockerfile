FROM debian:buster-slim

ENV DEBIAN_FRONTEND=noninteractive \
    DRUID_VERSION=34.0.0

# Debian 10 (buster) is EOL. Pin both repositories to one immutable snapshot: archive.debian.org
# repeatedly returned transient 404s for different indexed packages during hosted builds, while
# APT acquisition retries intentionally do not retry a valid HTTP 404 response.
RUN rm -f /etc/apt/sources.list.d/*.list \
    && printf 'deb [check-valid-until=no] http://snapshot.debian.org/archive/debian/20240311T000000Z buster main\ndeb [check-valid-until=no] http://snapshot.debian.org/archive/debian-security/20240311T000000Z buster/updates main\n' > /etc/apt/sources.list

RUN apt-get -o Acquire::Check-Valid-Until=false -o Acquire::Retries=5 update -y \
    && apt-get -o Acquire::Retries=5 install -y \
      bash curl wget perl gnupg apt-transport-https vim coreutils lsb-release

RUN mkdir -p /opt/druid-docker/conf/_common/
RUN mkdir -p /opt/druid-volume/shared
RUN mkdir -p /opt/druid-volume/var
RUN mkdir -p /opt/druid-volume/data


## Java

RUN echo "Installing Java"
RUN apt-get -o Acquire::Check-Valid-Until=false -o Acquire::Retries=5 -qq update -y \
    && apt-get -o Acquire::Retries=5 -qq install -y openjdk-11-jdk
RUN java -version


## Druid
RUN echo "Downloading Druid"
COPY .druid-dist/apache-druid-${DRUID_VERSION}-bin.tar.gz /opt/
COPY .druid-dist/apache-druid-${DRUID_VERSION}-bin.tar.gz.sha512 /opt/
WORKDIR /opt
RUN echo "$(cat apache-druid-${DRUID_VERSION}-bin.tar.gz.sha512) apache-druid-${DRUID_VERSION}-bin.tar.gz" | \
  sha512sum --check --status

RUN echo "Installing Druid"
RUN tar -zxf apache-druid-${DRUID_VERSION}-bin.tar.gz
RUN mv apache-druid-${DRUID_VERSION} /opt/druid

COPY druid-entrypoint.sh /opt/druid-docker/druid-entrypoint.sh
COPY conf/druid-supervise.conf /opt/druid-docker/druid-supervise.conf
COPY conf/druid-common.properties /opt/druid/conf/druid/single-server/micro-quickstart/_common/common.runtime.properties
COPY conf/druid-log4j2.xml /opt/druid/conf/druid/single-server/micro-quickstart/_common/log4j2.xml

COPY conf/druid-jvm.config /opt/druid/conf/druid/single-server/micro-quickstart/coordinator-overlord/jvm.config
COPY conf/druid-jvm.config /opt/druid/conf/druid/single-server/micro-quickstart/broker/jvm.config
COPY conf/druid-jvm.config /opt/druid/conf/druid/single-server/micro-quickstart/router/jvm.config
COPY conf/druid-jvm.config /opt/druid/conf/druid/single-server/micro-quickstart/historical/jvm.config
COPY conf/druid-jvm.config /opt/druid/conf/druid/single-server/micro-quickstart/middleManager/jvm.config

COPY conf/druid-historical.properties /opt/druid/conf/druid/single-server/micro-quickstart/historical/runtime.properties
COPY conf/druid-middle-manager.properties /opt/druid/conf/druid/single-server/micro-quickstart/middleManager/runtime.properties
COPY conf/druid-broker.properties /opt/druid/conf/druid/single-server/micro-quickstart/broker/runtime.properties


## Volume
VOLUME /opt/druid-volume


## Ports
# 8081=Coordinator
EXPOSE 8081
# 8082=Broker
EXPOSE 8082
# 8083=Historical
EXPOSE 8083
# 8090=Overlord
EXPOSE 8090
# 8100-8105=Middle Manager
EXPOSE 8091
EXPOSE 8100
EXPOSE 8101
EXPOSE 8102
EXPOSE 8103
EXPOSE 8104
EXPOSE 8105
# 8888=Router
EXPOSE 8888


WORKDIR /opt
ENTRYPOINT /opt/druid-docker/druid-entrypoint.sh
