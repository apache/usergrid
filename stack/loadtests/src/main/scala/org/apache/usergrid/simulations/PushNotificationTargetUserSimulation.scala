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
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.scenarios._
import org.apache.usergrid.settings.Settings
import scala.concurrent.duration._

class PushNotificationTargetUserSimulation extends Simulation {

  val duration:Int = Settings.duration
  val numUsersPerSecond:Int = Settings.numUsers
  val numEntities:Int = numUsersPerSecond * 3 * duration
  val httpConf = Settings.httpConf.acceptHeader("application/json")
  val userFeeder = FeederGenerator.generateUserWithGeolocationFeeder(numUsersPerSecond * duration, Settings.userLocationRadius, Settings.centerLatitude, Settings.centerLongitude)

  val scnToRun = scenario("Create Push Notification")
    .feed(userFeeder)
    .exec( UserScenarios.postUser)
    .exec(TokenScenarios.getUserToken)
    .repeat(2){
      feed(FeederGenerator.generateEntityNameFeeder("device", numEntities))
        .exec( DeviceScenarios.postDeviceWithNotifier)
        .exec(ConnectionScenarios.postUserToDeviceConnection)
      }
    .exec(session => {
      // print the Session for debugging, don't do that on real Simulations
      println(session)
      session
    })
    .exec( NotificationScenarios.sendNotificationToUser)
    .inject(nothingFor(15),constantUsersPerSec(numUsersPerSecond) during (duration))
    .throttle(reachRps(Settings.throttle) in ( Settings.rampTime.seconds))
    .protocols(httpConf)

  setUp(OrganizationScenarios.createOrgScenario,scnToRun)

}
