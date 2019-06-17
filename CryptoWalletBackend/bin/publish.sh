#!/bin/bash

BUILD_TYPE=$1
VERSION=$(cat cwb_version)

case $BUILD_TYPE in
    minor) 
        minor="$(echo $VERSION | grep -o -E '\w+' | head -n 2 | tail -n 1)"
        VERSION="$(echo $VERSION | grep -o -E '^\w+').$((minor+1)).0"
    ;;
    major)
        major="$(echo $VERSION | grep -o -E '^\w+')"
        VERSION="$((major+1)).0.0"
    ;;
    build)
        build="$(echo $VERSION | grep -o -E '\w+$')"
        VERSION="$(echo $VERSION | grep -o -E '^\w+\.\w+').$((build+1))"
    ;;
    *) 
        echo "Requires indicate (major|minor|build) to compile"
        exit 1
    ;;
esac 

function require() {
    if [[ "$1" == "" ]]; then
        echo "Requires indicate '$2'"
        exit 1
    fi
}

function hasError() {
    if [[ $1 -ne  0 ]]; then
        echo "$2"
        exit 1
    fi
}

CURRENT_PATH=$PWD
CRYPTOWALLET_PATH=$CURRENT_PATH/cryptowalletbackend

USERNAME=$2
PASSWORD=$3
HOSTNAME=$4
PORT=$5

require "$USERNAME" "username"
require "$PASSWORD" "password"
require "$HOSTNAME" "hostname"
require "$PORT" "port"

cd ..

echo "Build CryptoWalletBackend v$VERSION..."
mkdir $CRYPTOWALLET_PATH &> /dev/null

hasError $? "Fail to create temp directory"

echo 'Copying data...'
cp -r build/* $CRYPTOWALLET_PATH/
cp package.json $CRYPTOWALLET_PATH/

hasError $? "Fail to copy data"

cd $CURRENT_PATH

echo 'Compressing build...'

tar -cf "cryptowallet_v${VERSION}.tar.gz" cryptowalletbackend

hasError $? "Fail to compress data"

echo 'Delete temps...'

rm -rf $CRYPTOWALLET_PATH/

hasError $? "Fail to delete temps"

echo 'Upload files...'

sshpass -p $PASSWORD scp -P $PORT "cryptowallet_v${VERSION}.tar.gz" $USERNAME@$HOSTNAME:

hasError $? "Fail to upload files"

echo 'Installing...'

sshpass -p $PASSWORD ssh -p $PORT $USERNAME@$HOSTNAME "tar -xf cryptowallet_v${VERSION}.tar.gz"

hasError $? "Fail to install on remote"

echo 'Executing...'

sshpass -p $PASSWORD ssh -p $PORT $USERNAME@$HOSTNAME "cd cryptowalletbackend; ./node_modules/pm2/bin/pm2 restart cryptowallet-server"

hasError $? "Fail to execute on remote"

rm -rf *.tar.gz

echo $VERSION > cwb_version

echo "Completed at $(date +"%y-%m-%d %H:%M:%S")"
