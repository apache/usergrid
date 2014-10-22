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

/**
 *
 * Simple test for setting up multiple orgs and creating push notifications
 *
 */
class PushNotificationTargetDeviceSimulation extends Simulation {

  val numUsers:Int = Settings.numUsers
  val numEntities:Int = Settings.numEntities
  val rampTime:Int = Settings.rampTime
  val throttle:Int = Settings.throttle
  val duration:Int = Settings.duration  
  val httpConf = Settings.httpConf
    .acceptHeader("application/json")

  val createNotifier = NotifierScenarios.createNotifier
  val createDevice = DeviceScenarios.postDeviceWithNotifier
  val sendNotification = NotificationScenarios.sendNotification

  val deviceNameFeeder = FeederGenerator.generateEntityNameFeeder("device", numEntities).circular

  val scnToRun = scenario("Create Push Notification")    
    .during(duration.seconds) {
      feed(deviceNameFeeder)
      .exec(sendNotification)
    }


  setUp(scnToRun.inject(atOnceUsers(numUsers)).throttle(reachRps(throttle) in (rampTime.seconds)).protocols(httpConf))

}
