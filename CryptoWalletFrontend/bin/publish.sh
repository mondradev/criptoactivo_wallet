#!/bin/bash

function require() {
    if [[ "$1" == "" ]]; then
        echo "Requires indicate '$2'"
        exit 1
    fi
}

CURRENT_PATH=$PWD
CRYPTOWALLET_PATH=$CURRENT_PATH/cryptowallet

USERNAME=$2
PASSWORD=$3
HOSTNAME=$4
PORT=$5

require "$USERNAME" "username"
require "$PASSWORD" "password"
require "$HOSTNAME" "hostname"
require "$PORT" "port"

cd ..

echo "Create temporal directory..."
mkdir $CRYPTOWALLET_PATH &> /dev/null

echo 'Copying data...'
cp -r web/* $CRYPTOWALLET_PATH/
cp package.json $CRYPTOWALLET_PATH/

cd $CURRENT_PATH

echo 'Compressing build...'

tar -cf "cryptowallet_frontend.tar.gz" cryptowallet/*

echo 'Delete temps...'

rm -rf $CRYPTOWALLET_PATH/

echo 'Upload files...'

sshpass -p $PASSWORD scp -P $PORT "cryptowallet_frontend.tar.gz" $USERNAME@$HOSTNAME:public_html/

echo 'Installing...'

sshpass -p $PASSWORD ssh -p $PORT $USERNAME@$HOSTNAME 'cd public_html ; tar -xf cryptowallet_frontend.tar.gz; rm -rf cryptowallet_frontend.tar.gz; if [[ $? -ne 0 ]]; then echo "Fail to install"; fi'

rm -rf *.tar.gz

echo "Completed at $(date +"%y-%m-%d %H:%M:%S")"
