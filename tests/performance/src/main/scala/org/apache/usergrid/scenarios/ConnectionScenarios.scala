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
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.helpers.Headers
import org.apache.usergrid.settings.Settings

object ConnectionScenarios {

  val postUserConnection = exec(
    http("POST connection")
    .post("/users/${user1}/likes/users/${user2}")
      .headers(Headers.authToken)

      .check(status.is(200))
  )

  val postUserToDeviceConnection = exec(
    http("Connect user with device")
    .post("/users/${username}/devices/${deviceId}")
      .headers(Headers.authToken)
      .check(status.is(200))
  )

   val postConnection = exec(
     http("Connect user with device")
       .post("/${connectionName}/${entityId}/${connectionType}/${entityId}")
       .headers(Headers.authToken)
       .check(status.is(200))
   )

   val entityNameFeeder = FeederGenerator.generateEntityNameFeeder("device", Settings.numEntities)
   val createScenario = scenario("Create Connections")
     .feed(Settings.getUserFeeder)
     .exec(TokenScenarios.getUserToken)
     .exec( UserScenarios.getUserByUsername)
     .repeat(2){
       feed(entityNameFeeder)
         .exec(DeviceScenarios.postDeviceWithNotifier)
         .exec(ConnectionScenarios.postUserToDeviceConnection)
     }
     .exec(session => {
     // print the Session for debugging, don't do that on real Simulations
     println(session)
     session
   })
     .exec( )

}
