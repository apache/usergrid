# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import sys
import logging
from logging.handlers import RotatingFileHandler
import argparse
import time
import requests
import json

# Creates two organizations each with two apps each with three collections each with 100 entities
# Caller must provide a "slug" string which will be used as a prefix for all names
#
# For example, if the slug is mytest then:
#
#   Orgs will be named        mytest_org0 and mytest_org1
#   Org admins will be named  mytest_org0_admin and mytest_org1_admin (both with password test)
#
#   Apps will be named        mytest_org0_app0, mytest_org0_app1 and so on
#   Collections will be named mytest_org0_app0_col0 and so on
#   Entities will be named    mytest_org0_app0_col0_entity and so on
#
# All entities in collection 0 will be connected to entities in collection 1.
# All entities in collection 1 will be connected to entities in collection 2.

def parse_args():
    parser = argparse.ArgumentParser(description="Usergrid Test Data Creation Tool")

    parser.add_argument("--endpoint",
                        help="The endpoint to use for making API requests.",
                        type=str,
                        default="http://localhost:8080")

    parser.add_argument("--user",
                        help="Superuser credentials used to authenticate with Usergrid  <user:pass>",
                        type=str,
                        required=True)

    parser.add_argument("--slug",
                        help="Unique string to be used to name organization, applications and other things create",
                        type=str,
                        required=True)

    my_args = parser.parse_args(sys.argv[1:])

    arg_vars = vars(my_args)
    creds = arg_vars["user"].split(":")
    if len(creds) != 2:
        print("Credentials not properly specified.  Must be '--user <user:pass>'. Exiting...")
        exit_on_error()
    else:
        arg_vars["user"] = creds[0]
        arg_vars["pass"] = creds[1]

    return arg_vars


class Creator:
    def __init__(self):
        self.args = parse_args()
        self.endpoint = self.args["endpoint"]
        self.logger = init_logging(self.__class__.__name__)
        self.admin_user = self.args["user"]
        self.admin_pass = self.args["pass"]
        self.slug = self.args["slug"]

    def run(self):
        self.logger.info("Initializing...")

        if not self.is_endpoint_available():
            exit_on_error("Endpoint is not available, aborting")

        for orgIndex in range(2):
            orgName = self.slug + "_org" + str(orgIndex)
            orgUser = orgName + "_admin"
            orgEmail = orgUser + "@example.com"

            url = self.endpoint + "/management/orgs"
            body = json.dumps({"username":orgUser, "email":orgEmail, "password":"test", "organization":orgName })
            r = requests.post(url=url, data=body, auth=(self.admin_user, self.admin_pass))
            if ( r.status_code >= 400 ):
                print "Error creating organization " + orgName + ": " + r.text
                return

            print "Created org " + orgName

            url = self.endpoint + "/management/token"
            body = json.dumps({"grant_type":"password","username":orgUser,"password":"test"})
            r = requests.post(url=url, data=body)
            if ( r.status_code != 200 ):
                print "Error logging into organization " + orgName + ": " + r.text
                return

            accessToken = r.json()["access_token"]

            for appIndex in range(2):
                appName = orgName + "_app" + str(appIndex)

                url = self.endpoint + "/management/orgs/" + orgName + "/apps?access_token=" + accessToken
                body = json.dumps({"name":appName})
                r = requests.post(url=url, data=body, auth=(self.admin_user, self.admin_pass))
                if ( r.status_code >= 400 ):
                    print "Error creating application" + appName + ": " + r.text
                    return

                print "   Created app: " + orgName + "/" + appName
                appUrl = self.endpoint + "/" + orgName + "/" + appName
                time.sleep(2)

                for userIndex in range(2):
                    userName = appName + "_user" + str(userIndex)
                    email = userName + "@example.com"

                    url = appUrl + "/users?access_token=" + accessToken
                    body = json.dumps({"name":userName, "username":userName, "email":email, "password":"test"})
                    r = requests.post(url=url, data=body)
                    if ( r.status_code >= 400 ):
                        print "Error creating user " + userName + ": " + r.text
                        return

                for colIndex in range(3):
                    colName = appName + "_col" + str(colIndex)
                    print "      Creating collection: " + colName

                    for entityIndex in range(100):
                        entityName = colName + "_entity" + str(entityIndex)

                        url = appUrl + "/" + colName + "s?access_token=" + accessToken
                        body = json.dumps({"name":entityName})
                        r = requests.post(url=url, data=body)
                        if ( r.status_code >= 400 ):
                            print "Error creating entity" + userName + ": " + r.text
                            retur

                # connect entities in collection 0 to collection 1
                for entityIndex in range(100):
                    sourceCollection = appName + "_col0s"
                    sourceName = appName + "_col0_entity" + str(entityIndex)
                    targetName = appName + "_col1_entity" + str(entityIndex)
                    targetType = appName + "_col1"
                    url = appUrl + "/" + sourceCollection + "/" + sourceName + "/has/" + targetType + "/" + targetName
                    r = requests.post(url=url + "?access_token=" + accessToken)
                    r = requests.post(url=url + "?access_token=" + accessToken)
                    r = requests.post(url=url + "?access_token=" + accessToken)
                    if ( r.status_code >= 400 ):
                        print "Error connecting entity " + sourceName + " to " + targetName + ": " + r.text
                        print "url is: " + url
                        return

                # connect entities in collection 1 to collection 2
                for entityIndex in range(100):
                    sourceCollection = appName + "_col1s"
                    sourceName = appName + "_col1_entity" + str(entityIndex)
                    targetName = appName + "_col2_entity" + str(entityIndex)
                    targetType = appName + "_col2"
                    url = appUrl + "/" + sourceCollection + "/" + sourceName + "/has/" + targetType + "/" + targetName
                    r = requests.post(url=url + "?access_token=" + accessToken)
                    r = requests.post(url=url + "?access_token=" + accessToken)
                    r = requests.post(url=url + "?access_token=" + accessToken)
                    if ( r.status_code >= 400 ):
                        print "Error connecting entity " + sourceName + " to " + targetName + ": " + r.text
                        print "url is: " + url
                        return

    def is_endpoint_available(self):

        try:
            r = requests.get(url=self.endpoint+"/status")
            if r.status_code == 200:
                return True
        except requests.exceptions.RequestException as e:
            self.logger.error("Endpoint is unavailable, %s", str(e))
            return False


def exit_on_error(e=""):
    print ("Exiting script due to error: " + str(e))
    sys.exit(1)


def init_logging(name):

    logger = logging.getLogger(name)
    log_file_name = "./create-test-data.log"
    log_formatter = logging.Formatter(fmt="%(asctime)s [%(name)s] %(levelname)s %(message)s",
                                      datefmt="%Y-%m-%d %H:%M:%S")

    rotating_file = logging.handlers.RotatingFileHandler(filename=log_file_name,
                                                         mode="a",
                                                         maxBytes=104857600,
                                                         backupCount=10)
    rotating_file.setFormatter(log_formatter)
    rotating_file.setLevel(logging.INFO)
    logger.addHandler(rotating_file)
    logger.setLevel(logging.INFO)

    stdout_logger = logging.StreamHandler(sys.stdout)
    stdout_logger.setFormatter(log_formatter)
    stdout_logger.setLevel(logging.INFO)
    logger.addHandler(stdout_logger)

    return logger

if __name__ == "__main__":

    creator = Creator()
    creator.run()
