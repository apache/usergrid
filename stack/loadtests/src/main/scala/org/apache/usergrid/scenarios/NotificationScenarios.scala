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
 *
 * Creates a new device
 *
 * Expects:
 *
 * authToken The auth token to use when creating the application
 * orgName The name of the org
 * appName The name of the app
 * notifierName The name of the created notifier
 * deviceName the name of the device created to send the notification to
 *
 * Produces:
 *
 * N/A
 *
 *
 */
object NotificationScenarios {

  val notifier = Settings.pushNotifier

  /**
   * send the notification now
   */
  val sendNotification = exec(http("Send Single Notification")
    .post("/devices/${entityName}/notifications")
    .body(StringBody(_ => """{ "payloads": { """" + notifier + """": "testmessage"} }"""))
    .headers(Headers.authToken)
    .check(status.is(200))
  )

  val sendNotificationToUser= exec(http("Send Notification to All Devices")
    .post("/users/${userId}/notifications")
    .body(StringBody(_ => """{ "payloads": {"""" + notifier + """": "testmessage"} }"""))
    .headers(Headers.authToken)
    .check(status.is(200))
  )


  val userFeeder = Settings.getInfiniteUserFeeder
  val createScenario = scenario("Create Push Notification")
    .feed(userFeeder)
    .exec(TokenScenarios.getUserToken)
    .exec(UserScenarios.getUserByUsername)
    .exec( sendNotificationToUser)

  /**
   * TODO: Add posting to users, which would expect a user in the session
   */




}
