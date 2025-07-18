#!/bin/bash
set -e

source "buildScript/init/env.sh"
ENV_NB4A=1
COMMIT_SING_BOX="01b72e129794acae89e1c7929d0ba5a63b0e67f8"
COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
pushd ..

####

if [ ! -d "sing-box" ]; then
  git clone --no-checkout https://github.com/MatsuriDayo/sing-box.git
fi
pushd sing-box
git checkout "$COMMIT_SING_BOX"
popd

####

if [ ! -d "libneko" ]; then
  git clone --no-checkout https://github.com/MatsuriDayo/libneko.git
fi
pushd libneko
git checkout "$COMMIT_LIBNEKO"
popd

####

popd
