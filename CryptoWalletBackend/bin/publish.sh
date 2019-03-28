#!/bin/bash

BUILD_TYPE=$1
VERSION=$(cat cwb_version)

case $BUILD_TYPE in
    minor) 
        minor="$(echo $VERSION | grep -o -E '\w+' | head -n 2 | tail -n 1)"
        VERSION="$(echo $VERSION | grep -o -E '^\w+').$((minor+1)).$(echo $VERSION | grep -o -E '\w+$')"
    ;;
    major)
        major="$(echo $VERSION | grep -o -E '^\w+')"
        VERSION="$((major+1)).$(echo $VERSION | grep -o -E '\w+\.\w+$')"
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

function require {
    if [[ "$1" == "" ]]; then
        echo "Requires indicate '$2'"
        exit 1
    fi
}

CURRENT_PATH=$PWD
CRYPTOWALLET_PATH=$CURRENT_PATH/cryptowalletbackend

USERNAME=$2
PASSWORD=$3
HOSTNAME=$4
PORT=$5

require $USERNAME "username"
require $PASSWORD "password"
require $HOSTNAME "hostname"
require $PORT "port"

cd ..

echo "Build CryptoWalletBackend v$VERSION..."
mkdir $CRYPTOWALLET_PATH &> /dev/null

echo 'Copying data...'
cp -r dist/* $CRYPTOWALLET_PATH/

cd $CURRENT_PATH

echo 'Compressing build...'

tar -cf "cryptowallet_v${VERSION}.tar.gz" cryptowalletbackend

echo 'Delete temps...'

rm -rf $CRYPTOWALLET_PATH/

echo 'Upload files...'

sshpass -p $PASSWORD scp -P $PORT "cryptowallet_v${VERSION}.tar.gz" $USERNAME@$HOSTNAME:

echo 'Installing...'

sshpass -p $PASSWORD ssh -p $PORT $USERNAME@$HOSTNAME "tar -xf cryptowallet_v${VERSION}.tar.gz"

echo 'Executing...'

sshpass -p $PASSWORD ssh -p $PORT $USERNAME@$HOSTNAME "cd cryptowalletbackend; ./node_modules/pm2/bin/pm2 restart main.js"

rm -rf *.tar.gz

echo $VERSION > cwb_version

echo 'Completed'
