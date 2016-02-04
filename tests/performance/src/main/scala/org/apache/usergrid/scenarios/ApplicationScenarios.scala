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
 import org.apache.usergrid.helpers.Headers
 import org.apache.usergrid.settings.Settings

 /**
 * Performs organization registration
 *
 *
 * Expects:
 *
 * authToken The auth token to use when creating the application
 * orgName The organization name
 *
 * Produces:
 *
 * appName The name of the created application
 */
object ApplicationScenarios {

  val createApplication = exec(http("Create Application")
    .post(_ => Settings.baseUrl + "/management/organizations/" + Settings.org + "/applications")
    .headers(Headers.authToken)
    .body(StringBody(_ => """ { "name": """" + Settings.app + """" } """))
    .check(status.in(Range(200,204)))

    )

   val checkApplication = exec(http("Get Application")
     .get(Settings.baseAppUrl)
     .headers(Headers.authToken)
     .check(status.saveAs("applicationStatus"))
   )

}
