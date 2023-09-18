## ZENDAO_BALANCER stage

# Ubuntu 20.04
ARG FROM_IMAGE_BUILD=zencash/sc-ci-base:focal_jdk-11_latest

FROM $FROM_IMAGE_BUILD as zendao-balancer

LABEL authors=infrastructure@zensystem.io

# Scoped ARGS

ARG ARG_BALANCER_GIT_REPO=https://github.com/HorizenLabs/balancer.git

ARG ARG_BALANCER_COMMITTISH=as/balancer_dockerization

ARG ARG_BALANCER_PY_NAME=zen_balancer

ARG ARG_GOSU_VERSION=1.14

ARG ARG_TINI_VERSION=v0.19.0

ARG ARG_VIRTUAL_ENV=/opt/venv

ARG ARG_WORKDIR=/zendao_balancer

ARG ARG_REPO_DEST=/balancer

ENV BALANCER_PY_NAME=${ARG_BALANCER_PY_NAME} \
    GOSU_VERSION=${ARG_GOSU_VERSION} \
    VIRTUAL_ENV=$ARG_VIRTUAL_ENV \
    PATH="$ARG_VIRTUAL_ENV/bin:$PATH" \
    PYTHONUNBUFFERED=1 \
    TINI_VERSION=${ARG_TINI_VERSION} \
    BALANCER_COMMITTISH=${ARG_BALANCER_COMMITTISH} \
    BALANCER_GIT_REPO=${ARG_BALANCER_GIT_REPO} \
    REPO_DEST=${ARG_REPO_DEST} \
    WORK_DIR=${ARG_WORKDIR}

WORKDIR ${ARG_WORKDIR}

SHELL ["/bin/bash", "-c"]

COPY ci/docker/entrypoint.sh /usr/local/bin/entrypoint.sh

RUN set -euo pipefail && chmod +x /usr/local/bin/entrypoint.sh \
    && mkdir -p "$VIRTUAL_ENV" \
    && apt-get update && DEBIAN_FRONTEND=noninteractive apt-get -y --no-install-recommends install apt-utils \
    && DEBIAN_FRONTEND=noninteractive apt-get -y --no-install-recommends dist-upgrade \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ca-certificates \
      curl \
      dnsutils \
      gettext-base \
      jq \
      python3-pip \
      python3-dev \
      python3-venv \
      build-essential \
      vim \
    && savedAptMark="$(apt-mark showmanual)" \
    && if ! command -v gosu &> /dev/null; then \
      if ! command -v gpg2 &> /dev/null; then \
        DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends gnupg2 dirmngr; \
      fi \
      && dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')" \
      && curl -sSfL "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch" > /usr/local/bin/gosu \
      && curl -sSfL "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch.asc" > /usr/local/bin/gosu.asc \
      && export GNUPGHOME="$(mktemp -d)" \
      && ( gpg2 --batch --keyserver hkps://keys.openpgp.org --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 || \
       gpg2 --batch --keyserver hkps://keyserver.ubuntu.com:443 --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 || \
       gpg2 --batch --keyserver pgp.mit.edu --recv-key B42F6819007F00F88E364FD4036A9C25BF357DD4 ) \
      && gpg2 --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
      && ( gpgconf --kill dirmngr || true ) \
      && ( gpgconf --kill gpg-agent || true ) \
      && rm -rf "$GNUPGHOME" /usr/local/bin/gosu.asc \
      && unset GNUPGHOME \
      && chmod +x /usr/local/bin/gosu \
      && gosu --version; \
    fi \
    && if ! command -v tini &> /dev/null; then \
      if ! command -v gpg2 &> /dev/null; then \
        DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends gnupg2 dirmngr; \
      fi \
      && curl -sSfL "https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini" > /usr/local/bin/tini \
      && curl -sSfL "https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini.asc" > /usr/local/bin/tini.asc \
      && export GNUPGHOME="$(mktemp -d)" \
      && ( gpg2 --batch --keyserver hkps://keys.openpgp.org --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 || \
       gpg2 --batch --keyserver hkps://keyserver.ubuntu.com:443 --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 || \
       gpg2 --batch --keyserver pgp.mit.edu --recv-key 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 ) \
      && gpg2 --batch --verify /usr/local/bin/tini.asc /usr/local/bin/tini \
      && ( gpgconf --kill dirmngr || true ) \
      && ( gpgconf --kill gpg-agent || true ) \
      && rm -rf "$GNUPGHOME" /usr/local/bin/tini.asc \
      && unset GNUPGHOME \
      && chmod +x /usr/local/bin/tini \
      && tini --version; \
    fi \
    && apt-mark auto '.*' &> /dev/null && [ -z "$savedAptMark" ] || apt-mark manual $savedAptMark &> /dev/null \
    && DEBIAN_FRONTEND=noninteractive apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
    && rm -rf /var/{lib/apt/lists/*,cache/apt/archives/*.deb} /tmp/* \
    && python3 -m venv "$VIRTUAL_ENV" \
    && . "$VIRTUAL_ENV/bin/activate" \
    && python3 -m pip install --no-cache-dir --upgrade pip \
    && python3 -m pip install --no-cache-dir flask \
    && python3 -m pip install --no-cache-dir requests \
    && python3 -m pip install --no-cache-dir base58 \
    && python3 -m pip install --no-cache-dir eth_utils \
    && python3 -m pip install --no-cache-dir eth_abi \ 
    && python3 -m pip install --no-cache-dir python-dateutil \
    && python3 -m pip install --no-cache-dir pycryptodome \
    && python3 -m pip install --no-cache-dir pysha3 \
    && git clone ${BALANCER_GIT_REPO} ${REPO_DEST} && cd ${REPO_DEST} \
    && git checkout ${BALANCER_COMMITTISH}

VOLUME ["/zendao_balancer/datadir/"]

VOLUME ["/zendao_balancer/logs/"]

ENTRYPOINT ["/usr/local/bin/tini", "--", "/usr/local/bin/entrypoint.sh"]

# This is a placeholder CMD, the actual CMD is constructed in entrypoint.sh based on ENV vars like this:
# python ${REPO_DEST}/py/server/${BALANCER_PY_NAME}.py  2>&1 | tee  ${WORK_DIR}/logs/balancer_$(date +%Y%m%d_%H%M%S).log 
CMD ["/usr/bin/true"]
