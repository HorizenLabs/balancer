#!/bin/bash
set -eo pipefail

export IS_A_RELEASE="false"

if [ -z "${TRAVIS_TAG}" ]; then
  echo "TRAVIS_TAG:                           No TAG"
else
  echo "TRAVIS_TAG:                           ${TRAVIS_TAG}"
fi

# Functions
function import_gpg_keys() {
  # shellcheck disable=SC2207
  declare -r my_arr=( $(echo "${@}" | tr " " "\n") )

  if [ "${#my_arr[@]}" -eq 0 ]; then
    echo "Warning: there are ZERO gpg keys to import. Please check if MAINTAINERS_KEYS variable is set correctly. The build is not going to be released ..."
    export IS_A_RELEASE="false"
  else
    # shellcheck disable=SC2145
    printf "%s\n" "Tagged build, fetching keys:" "${@}" ""
    for key in "${my_arr[@]}"; do
      gpg -v --batch --keyserver hkps://keys.openpgp.org --recv-keys "${key}" ||
      gpg -v --batch --keyserver hkp://keyserver.ubuntu.com --recv-keys "${key}" ||
      gpg -v --batch --keyserver hkp://pgp.mit.edu:80 --recv-keys "${key}" ||
      gpg -v --batch --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys "${key}" ||
      { echo -e "Warning: ${key} can not be found on GPG key servers. Please upload it to at least one of the following GPG key servers:\nhttps://keys.openpgp.org/\nhttps://keyserver.ubuntu.com/\nhttps://pgp.mit.edu/"; export IS_A_RELEASE="false"; }
    done
  fi
}

function check_signed_tag() {
  local tag="${1}"

  # Checking if git tag signed by the maintainers
  if git verify-tag -v "${tag}"; then
    echo "${tag} is a valid signed tag"
    export IS_A_RELEASE="true"
  else
    echo "" && echo "=== Warning: GIT's tag = ${tag} signature is NOT valid. The build is not going to be released ... ===" && echo ""
  fi
}

if [ -n "${TRAVIS_TAG}" ]; then
  echo "TRAVIS_TAG: ${TRAVIS_TAG}"
  echo "The current production branch is: ${RELEASE_BRANCH}"

  if [[ -z "${MAINTAINERS_KEYS}" ]]; then
    echo "Warning: MAINTAINERS_KEYS variables is not set. Make sure to set it up for PROD release build !!!"
  fi
  # shellcheck disable=SC2155
  export GNUPGHOME="$(mktemp -d 2>/dev/null || mktemp -d -t 'GNUPGHOME')"

  # Prod vs development release
  if (git branch -r --contains "${TRAVIS_TAG}" | grep -xqE ". origin\/${RELEASE_BRANCH}$"); then
    echo "" && echo "=== Production release ===" && echo ""
    # shellcheck disable=SC2086
    import_gpg_keys "${MAINTAINERS_KEYS}"
    check_signed_tag "${TRAVIS_TAG}" false
  fi
fi

# Final check for release vs non-release build
if [ "${IS_A_RELEASE}" = "false" ]; then
  echo "" && echo "=== NOT a release build ===" && echo ""
fi

set +eo pipefail