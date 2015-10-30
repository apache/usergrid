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
import org.apache.usergrid.helpers.{Headers, Utils}
import org.apache.usergrid.settings.Settings

object GeoScenarios {

  val getGeolocation = exec(
      http("GET geolocated user")
        .get("/users?ql=location%20within%20" + Settings.geoSearchRadius + "%20of%20${latitude},${longitude}")
        .headers(Headers.authToken)

        .check(status.is(200))
    )

  val getGeolocationWithQuery = exec(
      http("GET geolocated user with query")
        .get("/users?ql=${queryParams}%20AND%20location%20within%20" + Settings.geoSearchRadius + "%20of%20${latitude},${longitude}")
        .headers(Headers.authToken)
        .check(status.is(200))
    )

  val updateGeolocation = exec(
    http("PUT user location")
      .put(_ => "/users/user" + Utils.generateRandomInt(1, Settings.totalUsers))
      .body(StringBody("""{ "location": { "latitude": "${latitude}", "longitude": "${longitude}"} }"""))
      .headers(Headers.authToken)
      .check(status.is(200))
  )

}
