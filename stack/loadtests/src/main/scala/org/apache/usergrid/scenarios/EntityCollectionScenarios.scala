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
import io.gatling.core.feeder.RecordSeqFeederBuilder
import io.gatling.http.Predef._
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.enums.{EndConditionType, AuthType}
import org.apache.usergrid.helpers.Extractors._
import org.apache.usergrid.helpers.Utils
import org.apache.usergrid.settings.{Headers, Settings}

/**
 * Provides CRUD methods for custom entities
 *
 *
 *
 */
object EntityCollectionScenarios {

  //The value for the cursor
  val SessionVarCursor: String = "cursor"
  val SessionVarUuid: String = "createUuid"

  def entityGetUrl(useCursor: Boolean): String = {
    val url = s"/${Settings.collection}?" +
      (if (useCursor) "cursor=${" + SessionVarCursor + "}&" else "") +
      (if (Settings.searchQuery != "") s"ql=${Settings.searchQuery}&" else "") +
      (if (Settings.searchLimit > 0) s"limit=${Settings.searchLimit}&" else "")

    // remove trailing & or ?
    url.dropRight(1)
  }

  def entityGetByNameUrl(entityName: String): String = {
    val url = s"/${Settings.collection}/$entityName"

    url
  }

  def randomEntityNameUrl(prefix: String = Settings.entityPrefix, numEntities: Int = Settings.numEntities, seed: Int = Settings.entitySeed): String = {
    Utils.randomEntityNameUrl(prefix, numEntities, seed, Settings.baseCollectionUrl)
  }

  def uuidFeeder(): RecordSeqFeederBuilder[String] = {
    csv(Settings.feedUuidFilename).random
  }

  /*
   * Loop through entities using cursor
   */
  val getEntitiesWithoutCursor = exec(
    http("GET entities")
      .get(entityGetUrl(false))
      .headers(Headers.authToken)
      .check(status.is(200), maybeExtractCursor(SessionVarCursor))
  )

  val getEntitiesWithCursor = exec(
    http("GET entities")
      .get(entityGetUrl(true))
      .headers(Headers.authToken)
      .check(status.is(200), maybeExtractCursor(SessionVarCursor))
  )

  val getEntityPagesToEnd = scenario("Get all entities")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .doIfOrElse(_ => Settings.endConditionType == EndConditionType.MinutesElapsed) {
      asLongAs(_ => Settings.continueMinutesTest, exitASAP = true) {
        exec(getEntitiesWithoutCursor)
          .asLongAs(stringParamExists(SessionVarCursor)) {
          exec(getEntitiesWithCursor)
        }
      }
    } {
      repeat(_ => Settings.endRequestCount.toInt) {
        exec(getEntitiesWithoutCursor)
          .asLongAs(stringParamExists(SessionVarCursor)) {
          exec(getEntitiesWithCursor)
        }
      }
    }
    //.exec(sessionFunction => { sessionFunction })

  /*
   * Get random entities by name
   */
  val getRandomEntityAnonymous = exec(
    http("GET entity by name (anonymous)")
      .get(randomEntityNameUrl())
      .headers(Headers.authAnonymous)
      .check(status.is(200))
  )

  val getRandomEntityWithToken = exec(
    http("GET entity by name (token)")
      .get(randomEntityNameUrl())
      .headers(Headers.authToken)
      .check(status.is(200))
  )

  val getRandomEntitiesByName = scenario("Get entities by name randomly")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .doIfOrElse(_ => Settings.endConditionType == EndConditionType.MinutesElapsed) {
    asLongAs(_ => Settings.continueMinutesTest) {
      doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
        exec(getRandomEntityAnonymous)
      } {
        exec(getRandomEntityWithToken)
      }
    }
  } {
    repeat(_ => Settings.endRequestCount.toInt) {
      doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
        exec(getRandomEntityAnonymous)
      } {
        exec(getRandomEntityWithToken)
      }
    }
  }

  /*
   * Get random entities by UUID
   */
  val getRandomEntityByUuidAnonymous = exec(
    http("GET entity by UUID (anonymous)")
      .get("/" + Settings.collection + "/${uuid}")
      .headers(Headers.authAnonymous)
      .check(status.is(200))
  )

  val getRandomEntityByUuidWithToken = exec(
    http("GET entity by UUID (token)")
      .get("/" + Settings.collection + "/${uuid}")
      .headers(Headers.authToken)
      .check(status.is(200))
  )

  val getRandomEntitiesByUuid = scenario("Get entities by uuid randomly")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .doIfOrElse(_ => Settings.endConditionType == EndConditionType.MinutesElapsed) {
      asLongAs(_ => Settings.continueMinutesTest) {
        feed(uuidFeeder())
          /*.exec{
            session => println(s"UUID: ${session("uuid").as[String]}")
            session
          }*/
          .doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
            exec(getRandomEntityByUuidAnonymous)
          } {
            exec(getRandomEntityByUuidWithToken)
          }
      }
    } {
      repeat(_ => Settings.endRequestCount.toInt) {
        feed(uuidFeeder())
          /*.exec {
            session => println(s"UUID: ${session("uuid").as[String]}")
            session
          }*/
          .doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
            exec(getRandomEntityByUuidAnonymous)
          } {
            exec(getRandomEntityByUuidWithToken)
          }
      }
    }

  /*
   * Create entities
   */
  val loadEntity = exec(
    doIf("${validEntity}", "yes") {
      exec(http("POST load entity")
        .post(_ => "/" + Settings.collection)
        .headers(Headers.authToken)
        .body(StringBody("""${entity}"""))
        // 200 for success, 400 if already exists
        .check(status.in(Seq(200)), extractCreateUuid(SessionVarUuid)))
        .exec(session => {
          Settings.addUuid(session("entityNum").as[String].toInt, session(SessionVarUuid).as[String])
          session
        })
    }
  )

  val loadEntities = scenario("Load entities")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
      feed(FeederGenerator.generateCustomEntityFeeder(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed))
        /*.exec{
          session => if (session("validEntity").as[String] == "yes") { println("Loading entity #" + session("entityNum").as[String]) }
          session
        }*/
        .doIf(session => session("validEntity").as[String] == "yes") {
          tryMax(5) {
            exec(loadEntity)
          }
        }
    }

  /*
   * Delete entities
   */
  val deleteEntity = exec(
    http("DELETE entity")
      .delete("""${entityUrl}""")
      .headers(Headers.authToken)
      // 200 for success, 404 if doesn't exist
      .check(status.in(Seq(200)))
  )

  val deleteEntities = scenario("Delete entities")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
    feed(FeederGenerator.generateCustomEntityFeeder(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed))
      /*.exec {
      session => if (session("validEntity").as[String] == "yes") { println("Deleting entity #" + session("entityNum").as[String]) }
        session
      }*/
      .doIf(session => session("validEntity").as[String] == "yes") {
        exec(deleteEntity)
      }
  }

  /*
   * Update entities
   */
  val updateEntity = exec(
    http("UPDATE entity")
      .put("""${entityUrl}""")
      .headers(Headers.authToken)
      .body(StringBody(Settings.updateBody))
      // 200 for success, 404 if doesn't exist
      .check(status.in(Seq(200)))
  )

  val updateEntities = scenario("Update entities")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
      feed(FeederGenerator.generateCustomEntityFeeder(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed))
        /*.exec {
          session => if (session("validEntity").as[String] == "yes") { println("Updating entity #" + session("entityNum").as[String]) }
          session
        }*/
        .doIf(session => session("validEntity").as[String] == "yes") {
          exec(updateEntity)
        }
    }
}
