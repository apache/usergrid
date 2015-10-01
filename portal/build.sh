#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

buildMain(){
  echo #####
  echo grunt $1
  echo #####
  ./node_modules/grunt-cli/bin/grunt $1
  ./node_modules/grunt-cli/bin/grunt compress
}


npm install

case "$1" in
e2e) buildMain

    webdriver-manager start --standalone > seleniumLog.txt &
    sleep 10
    # run the build
    ./node_modules/grunt-cli/bin/grunt e2e-chrome
      # stop selenium
    curl -s -L http://localhost:4444/selenium-server/driver?cmd=shutDownSeleniumServer > seleniumLog.txt &
    ;;
dev) buildMain
    ./node_modules/grunt-cli/bin/grunt  $1
    ;;
*)   echo "pass e2e to run e2e tests"
   buildMain
   ;;
esac

echo ####
echo The Admin Portal has been built and delivered to /dist
echo deploy the contents directory to your webserver
echo or run it now using the following command:
echo grunt dev
echo ###
echo Happy Usergriding!
