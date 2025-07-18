#!/bin/bash

chmod -R 777 .build 2>/dev/null
rm -rf .build 2>/dev/null

if [ -z "$GOPATH" ]; then
    GOPATH=$(go env GOPATH)
fi

# Install gomobile
if [ ! -f "$GOPATH/bin/gomobile-matsuri" ]; then
    git clone https://github.com/MatsuriDayo/gomobile.git
    cd gomobile
    go install -v ./cmd/gomobile/
    cd ../ && rm -rf gomobile
    mv -v "$GOPATH/bin/gomobile" "$GOPATH/bin/gomobile-matsuri"
fi

PATH="$PATH:$GOPATH/bin" gomobile-matsuri init
