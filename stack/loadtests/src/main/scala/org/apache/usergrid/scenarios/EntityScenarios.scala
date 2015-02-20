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
import org.apache.usergrid.datagenerators.{EntityDataGenerator, FeederGenerator}
import org.apache.usergrid.settings.{Headers, Utils, Settings}

/**
 * Provides CRUD methods for custom entities
 *
 *
 *
 */
object EntityScenarios {

  val getEntity = exec(
    http("GET custom entityr")
      .get(Settings.baseAppUrl+"/${collectionType}/${entityName}")
      .headers(Headers.jsonAuthorized)
      .check(status.is(200))
  )

  val putEntity = exec(
    http("Put custom entity")
      .put(Settings.baseAppUrl+"/${collectionType}/${entityName}")
      .body(StringBody("{\"address\":\""+Utils.generateRandomInt(1, Settings.numEntities)+"\",\"phone\":\""+Utils.generateRandomInt(1, Settings.numEntities)+"\"}}"))
      .headers(Headers.jsonAuthorized)
      .check(status.is(200))
  )


  val deleteEntity = exec(
    http("DELETE custom entityr")
      .get(Settings.baseAppUrl+"/${collectionType}/${entityName}")
      .headers(Headers.jsonAuthorized)
      .check(status.is(200))
  )

  val postEntity = exec(
    http("Post custom entity")
      //.post(Settings.baseUrl+"/${collectionType}")
      .post(Settings.baseAppUrl+"/freds")
      //.body(StringBody(EntityDataGenerator.generateCustomEntity().toString()))
      .body(StringBody("{\"property\":\"fred\"}"))
      .headers(Headers.jsonAnonymous)
      .check(status.is(200))
  )

}
