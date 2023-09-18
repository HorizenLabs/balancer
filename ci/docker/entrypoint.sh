#!/bin/bash

set -euo pipefail

USER_ID="${LOCAL_USER_ID:-9001}"
GRP_ID="${LOCAL_GRP_ID:-9001}"
LD_PRELOAD="${LD_PRELOAD:-}"

if [ "$USER_ID" != "0"  ]; then
    getent group "$GRP_ID" &> /dev/null || groupadd -g "$GRP_ID" user
    id -u user &> /dev/null || useradd --shell /bin/bash -u "$USER_ID" -g "$GRP_ID" -o -c "" -m user
    CURRENT_UID="$(id -u user)"
    CURRENT_GID="$(id -g user)"
    if [ "$USER_ID" != "$CURRENT_UID" ] || [ "$GRP_ID" != "$CURRENT_GID" ]; then
        echo -e "WARNING: User with differing UID $CURRENT_UID/GID $CURRENT_GID already exists, most likely this container was started before with a different UID/GID. Re-create it to change UID/GID.\n"
    fi
else
    CURRENT_UID="$USER_ID"
    CURRENT_GID="$GRP_ID"
    echo -e "WARNING: Starting container processes as root. This has some security implications and goes against docker best practice.\n"
fi

# set $HOME
if [ "$CURRENT_UID" != "0" ]; then
    export USERNAME=user
    export HOME=/home/"$USERNAME"
else
    export USERNAME=root
    export HOME=/root
fi

export MOCK_NSC=false
export MOCK_SNAPSHOT=false
export MOCK_ROSETTA=true
export LISTENING_ON_HTTP=true
export RUNNING_ON_LOCALHOST=false
export BALANCER_PORT=5000
export USING_WSGI_PROXY=false
export NSC_URL=http://139.144.68.109/
export ETH_CALL_FROM_ADDRESS=0xeDEb4BF692A4a1bfeCad78E09bE5C946EcF6C6da
export ROSETTA_URL=http://localhost:8080/
export ROSETTA_NETWORK_TYPE=test
export SNAPSHOT_URL=https://hub.snapshot.org/graphql
export PROPOSAL_JSON_DATA_PATH=${WORK_DIR}/datadir/
export PROPOSAL_JSON_DATA_FILE_NAME=active_proposal.json


to_check=(
    "MOCK_NSC"
    "MOCK_SNAPSHOT"
    "MOCK_ROSETTA"
    "LISTENING_ON_HTTP"
    "RUNNING_ON_LOCALHOST"
    "BALANCER_PORT"
    "USING_WSGI_PROXY"
    "NSC_URL"
    "ETH_CALL_FROM_ADDRESS"
    "ROSETTA_URL"
    "ROSETTA_NETWORK_TYPE"
    "SNAPSHOT_URL"
    "PROPOSAL_JSON_DATA_PATH"
    "PROPOSAL_JSON_DATA_FILE_NAME"
)
for var in "${to_check[@]}"; do
  if [ -z "${!var:-}" ]; then
    echo "Error: Environment variable ${var} required."
    sleep 5
    exit 1
  fi
done


# set file ownership
find ${WORK_DIR} -writable -print0 | xargs -0 -I{} -P64 -n1 chown -f "${CURRENT_UID}":"${CURRENT_GID}" "{}"

# use a bash script for starting py server and redirecting io to a file
REDIRECT_SCRIPT="./start_with_io_redirection.sh"

# this arg is passed by Dockerfile for hooking this script and build the actual command
if [ "${1}" = "/usr/bin/true" ]; then

cat << EOF > ${REDIRECT_SCRIPT}
env && python ${REPO_DEST}/py/server/${BALANCER_PY_NAME}.py  2>&1 | tee  ${WORK_DIR}/logs/balancer_$(date +%Y%m%d_%H%M%S).log 
EOF
chmod 744 ${REDIRECT_SCRIPT}
set -- bash ${REDIRECT_SCRIPT} 

fi

echo "Username: ${USERNAME}, UID: ${CURRENT_UID}, GID: ${CURRENT_GID}"
echo "Balancer py name: ${BALANCER_PY_NAME}"
echo "Repo dir (source files): ${REPO_DEST}"
echo "Work dir (logs and data): ${WORK_DIR}"
echo "Starting zendao balancer via script [${REDIRECT_SCRIPT}]:"
echo
cat ${REDIRECT_SCRIPT}
echo
echo "Command: '$@'"

if [ "${USERNAME}" = "user" ]; then
    exec /usr/local/bin/gosu user "$@"
else
    exec "$@"
fi
