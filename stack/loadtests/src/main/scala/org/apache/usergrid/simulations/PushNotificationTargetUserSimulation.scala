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
 package org.apache.usergrid.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.apache.usergrid.settings.Utils
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.scenarios._
import org.apache.usergrid.settings.Settings
import scala.concurrent.duration._

class PushNotificationTargetUserSimulation extends Simulation {

  val duration:Int = Settings.duration
  val numUsersPerSecond:Int = Settings.numUsers
  val numEntities:Int = numUsersPerSecond * 3 * duration
  val rampTime:Int = Settings.rampTime
  val throttle:Int = Settings.throttle

  val httpConf = Settings.httpConf.acceptHeader("application/json")
  val notifier = Settings.pushNotifier

  val createNotifier = NotifierScenarios.createNotifier
  val createDevice = DeviceScenarios.postDeviceWithNotifier
  val sendNotification = NotificationScenarios.sendNotificationToUser
  val createUser = UserScenarios.postUser
  val createOrg = OrganizationScenarios.createOrgAndAdmin
  val connectUserToDevice = ConnectionScenarios.postUserToDeviceConnection

  val deviceNameFeeder = FeederGenerator.generateEntityNameFeeder("device", numEntities)
  val userFeeder = FeederGenerator.generateUserWithGeolocationFeeder(numUsersPerSecond * duration, Settings.userLocationRadius, Settings.centerLatitude, Settings.centerLongitude)
  val orgFeeder = FeederGenerator.generateRandomEntityNameFeeder("org", 1)

  val scnCreateOrg = scenario("Create org")
    .feed(orgFeeder)
    .exec(createOrg)

  val scnCreateNotifier = scenario("Create notifier")
    .exec(createNotifier)

  val scnToRun = scenario("Create Push Notification")
    .feed(userFeeder)
    .exec(createUser)
    .repeat(2){
      feed(deviceNameFeeder)
      .exec(createDevice)
      .exec(connectUserToDevice)
    }
    .exec(sendNotification)



  setUp(scnCreateOrg.inject(atOnceUsers(1)).protocols(http.baseURL(Settings.baseUrl)),
    scnCreateNotifier.inject(nothingFor(5), atOnceUsers(1)).protocols(httpConf),
    scnToRun.inject(nothingFor(7), constantUsersPerSec(numUsersPerSecond) during (duration)).throttle(reachRps(throttle) in (rampTime.seconds)).protocols(httpConf))

}
