#!/bin/bash

function requireCommand() {
    if [ "$(command -v $1)" == "" ]
    then 
        echo "Require be install: $1 "
        exit 1
    fi
}

# requireCommand sshpass
requireCommand ssh

PROJECT_NAME="CriptoActivoServer"

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
PROJECT_TEMP_PATH=$CURRENT_PATH/$PROJECT_NAME

USERNAME=$4
KEYFILE=$5
HOSTNAME=$2
PORT=$3

require "$HOSTNAME" "hostname"
require "$PORT" "port"
require "$USERNAME" "username"
require "$KEYFILE" "sshkey"

if [[ ! -f "$KEYFILE" ]]; then
    hasError 1 "Ssh key no found: $KEYFILE"
fi

cd ..

echo "Publishing $PROJECT_NAME v$VERSION"
echo "Creating temp directory..."

if [[ -d  $PROJECT_TEMP_PATH ]]; then
    rm -rf $PROJECT_TEMP_PATH
fi

mkdir $PROJECT_TEMP_PATH &> /dev/null

hasError $? "Fail to create temp directory"

echo 'Copying data...'
cp -r build/{config,src} $PROJECT_TEMP_PATH/
cp package.json $PROJECT_TEMP_PATH/

hasError $? "Fail to copy data"

cd $CURRENT_PATH

echo 'Compressing build...'

tar -cf "${PROJECT_NAME}_v${VERSION}.tar.gz" $PROJECT_NAME

hasError $? "Fail to compress data"

echo 'Delete temps...'

rm -rf $PROJECT_TEMP_PATH/

hasError $? "Fail to delete temps"

echo 'Upload files...'

scp -i $CURRENT_PATH/$KEYFILE -P $PORT "${PROJECT_NAME}_v${VERSION}.tar.gz" $USERNAME@$HOSTNAME:

hasError $? "Fail to upload files"

echo 'Installing...'

ssh -i $CURRENT_PATH/$KEYFILE -p $PORT $USERNAME@$HOSTNAME "tar -xf ${PROJECT_NAME}_v${VERSION}.tar.gz"

hasError $? "Fail to install on remote"

echo 'Executing...'

ssh -i $CURRENT_PATH/$KEYFILE -p $PORT $USERNAME@$HOSTNAME "cd $PROJECT_NAME; ./killServer ; node src/server.js --log-level=trace &> server.log &"

hasError $? "Fail to execute on remote"

rm -rf *.tar.gz

echo $VERSION > cwb_version

echo "Completed at $(date +"%y-%m-%d %H:%M:%S")"
