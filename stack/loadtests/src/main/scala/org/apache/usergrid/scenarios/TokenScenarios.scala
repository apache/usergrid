/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import org.apache.usergrid.helpers.Headers
import org.apache.usergrid.settings.Settings


/**
 * Class that will get the token and insert it into the test session.
 * Assumes that  the following values are present in the session.
 *
 * Expects:
 *
 * userName  The user name to log in with
 * password The password to use
 *
 * Produces:
 *
 * authToken A valid access token if the login attempt is successful
 */

object TokenScenarios {
  val getManagementToken = exec(http("POST Org Token")
    .post(_ => Settings.baseUrl + "/management/token")
    .headers(Headers.authAnonymous)
    //pass in the the username and password, store the "access_token" json response element as the var "authToken" in the session
    .body(StringBody(_ => """{ "username": """" + Settings.adminUser + """", "password": """" + Settings.adminPassword + """", "grant_type": "password" }"""))
    .check(jsonPath("$.access_token").find(0).saveAs("authToken"))
  )

  val getUserToken =
    exec(
      http("POST user token")
        .post("/token")
        .body(StringBody("""{ "grant_type": "password", "username": "${username}", "password": "password" }"""))
        .check(status.is(200),jsonPath("$..access_token").exists,jsonPath("$..access_token").saveAs("authToken"))
    )
}
