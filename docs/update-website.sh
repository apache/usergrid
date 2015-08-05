#!/usr/bin/env bash

# make dcoumenatation and copy to website directry
make clean html 
cp -r target/html/* ../content/docs

