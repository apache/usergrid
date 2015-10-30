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
import io.gatling.http.Predef.StringBody
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
 *
 * Produces:
 *
 * deviceName the name of the device created
 *
 */
object DeviceScenarios {

  val notifier = Settings.pushNotifier

  /**
   * Create a device
   */
  val postDeviceWithNotifier = exec(http("Create device with notifier")
    .post("/devices")
    .headers(Headers.authToken)
    .body(StringBody(session => """{ "deviceModel": "Fake Device", "deviceOSVersion": "Negative Version", """" + notifier + """.notifier.id": "${entityName}" }"""))
    .check(status.is(200),  jsonPath("$..entities[0]").exists , jsonPath("$..entities[0].uuid").exists , jsonPath("$..entities[0].uuid").saveAs("deviceId")))


  val postDeviceWithNotifier400ok = exec(http("Create device with notifier")
    .post("/devices")
    .headers(Headers.authToken)
    .body(StringBody(session => """{ "name":"${entityName}", "deviceModel":"Fake Device", "deviceOSVersion":"Negative Version", """" + notifier + """.notifier.id":"${entityName}" }"""))
    .check(status.in(Range(200, 400)), jsonPath("$.entities[0].uuid").saveAs("deviceId")))


  /**
   * Requires: entityName to feed to the device name.  If it exists, it will be created
   */
  val maybeCreateDevices = exec(
    //try to do a GET on device name, if it 404's create it
    http("Check and create device")
      .get("/users/${username}/devices")
      .headers(Headers.authToken)
      .check(jsonPath("$.entities").exists, jsonPath("$.entities").saveAs("devices"))
      )
      .exec(session =>{
        println("found "+session.attributes.get("devices").get+ " devices")
        session
      } )
    //create the device if we got a 404
    .doIf("${devices}","[]") {
      /*exec(session =>{
        println("adding devices")
        session
      } )*/
      exec(postDeviceWithNotifier)
      .exec(ConnectionScenarios.postUserToDeviceConnection)
    }
}
