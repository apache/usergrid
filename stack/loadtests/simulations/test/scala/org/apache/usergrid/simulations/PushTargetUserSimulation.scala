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
package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PushNotificationTargetUserSimulation extends Simulation {

  val numUsers:Int = Settings.numUsers
  val numEntities:Int = Settings.numEntities
  val rampTime:Int = Settings.rampTime
  val throttle:Int = Settings.throttle
  val duration:Int = Settings.duration
  val httpConf = Settings.httpConf
    .acceptHeader("application/json")

  val notifier = Settings.pushNotifier
  val createDevice = DeviceScenarios.postDeviceWithNotifier400ok
  val sendNotification = NotificationScenarios.sendNotification
  val createUser = UserScenarios.postUser400ok
  val deviceNameFeeder = FeederGenerator.generateEntityNameFeeder("device", numEntities).circular
  val userFeeder = FeederGenerator.generateUserWithGeolocationFeeder(numEntities, Settings.userLocationRadius, Settings.centerLatitude, Settings.centerLongitude)

  val scnToRun = scenario("Create Push Notification")
    .feed(userFeeder)
    .exec(createUser)
    .pause(1000)
    .exec(http("Check user and user devices")
      .get("/users/${username}/devices")
      .check(status.is(200))
    )
    .feed(deviceNameFeeder)
    .exec(createDevice)
    .pause(1000)
    .exec(http("Check device connections")
      .get("/devices/${entityName}/users")
      .check(status.is(200))
    )
    .exec(http("Connect user with device")
      .post("/users/${username}/devices/${entityName}")
      .check(status.is(200))
    )
    .exec(http("Send Notification to All Devices")
      .post("/users/${username}/notifications")
      .body(StringBody("{\"payloads\":{\"" + notifier + "\":\"testmessage\"}}"))
      .check(status.is(200))
    )


  setUp(scnToRun.inject(constantUsersPerSec(numUsers) during (duration)).throttle(reachRps(throttle) in (rampTime.seconds)).protocols(httpConf))

}
