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
 * Creates a new no-op notifier
 *
 *
 * Expects:
 *
 * authToken The auth token to use when creating the application
 * orgName The name of the org
 * appName The name of the app
 *
 * Produces:
 *
 * notifierName The name of the created notifier
 *
 */
object NotifierScenarios {
  
  val notifier = Settings.pushNotifier
  val provider = Settings.pushProvider

  /**
   * Create a notifier
   */
  val createNotifier = exec(
      session => {
        session.set("notifier", notifier)
        session.set("provider", provider)
      }
    )

    .exec(http("Create Notifier")
    .post("/notifiers")
    .body(StringBody("{\"name\":\"${notifier}\",\"provider\":\"${provider}\"}"))
    //remnants of trying to upload an apple certificate
//    .param("name", "${notifierName}")
//    .param("provider", "apple")
//    .param("environment", "mock")
//    .fileBody("p12Certificate", Map).fileBody(pkcs12Cert)
    .check(status.is(200)))


}
