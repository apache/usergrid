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
import org.apache.usergrid.helpers.Setup
import org.apache.usergrid.scenarios.UserScenarios
import org.apache.usergrid.settings.Settings

import scala.concurrent.duration._

class PostUsersSimulation extends Simulation {

  println("Begin setup")
  Setup.setupOrg()
  Setup.setupApplication()
  println("End Setup")


  setUp(
    UserScenarios.postUsersInfinitely
      .inject(constantUsersPerSec(Settings.maxPossibleUsers) during (Settings.duration)) // wait for 15 seconds so create org can finish, need to figure out coordination


  )  .throttle(reachRps(Settings.maxPossibleUsers) in (Settings.rampTime.seconds)).protocols(Settings.httpConf.acceptHeader("application/json"))

}
