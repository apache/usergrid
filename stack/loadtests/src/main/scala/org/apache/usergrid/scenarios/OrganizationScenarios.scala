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
 import org.apache.usergrid.settings.{Settings, Headers}
 import scala.concurrent.duration._

/**
 * Performs organization registration
 *
 *
 * Produces:
 *
 * orgName The name of the created organization
 * userName  The user name of the admin to log in with
 * password The password of the admin to use
 */
object OrganizationScenarios {

  //register the org with the randomly generated org
  val createOrgAndAdmin = exec(http("Create Organization")
    .post(Settings.baseUrl+"/management/organizations")
    .headers(Headers.jsonAnonymous)
    .body(StringBody("{\"organization\":\"" + Settings.org + "\",\"username\":\"" + Settings.org + "\",\"name\":\"${entityName}\",\"email\":\"${entityName}@apigee.com\",\"password\":\"test\"}"))
    .check(status.in(200 to 400))
  )

  val getManagementToken = exec(http("POST Org Token")
    .post(Settings.baseUrl+"/management/token")
    .headers(Headers.jsonAnonymous)
    //pass in the the username and password, store the "access_token" json response element as the var "authToken" in the session
    .body(StringBody("{\"username\":\"" + Settings.org + "\",\"password\":\"test\",\"grant_type\":\"password\"}"))
    .check(jsonPath("$.access_token").find(0).saveAs("authToken"))
  )

}
