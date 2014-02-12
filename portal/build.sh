#!/bin/bash

buildMain(){
  echo #####
  echo grunt --gruntfile Gruntfile.js $1
  echo #####
  ./node_modules/grunt-cli/bin/grunt --gruntfile Gruntfile.js $1
}


npm install

case "$1" in
e2e) buildMain
    ./node_modules/grunt-cli/bin/grunt --gruntfile Gruntfile.js $1
    ;;
dev) buildMain
    ./node_modules/grunt-cli/bin/grunt --gruntfile Gruntfile.js $1
    ;;
*)   echo "pass e2e to run e2e tests"
   buildMain
   ;;
esac

echo ####
echo zip has been delivered to ./dist/


