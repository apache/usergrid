#!/bin/bash

#Create an alias for running gatling.sh
cd gatling/bin
echo alias gatling=\"`pwd`/gatling.sh\" >> ~/.bashrc

gatling
