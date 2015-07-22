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

  def entityGetUrl(useCursor: Boolean): String = {
    val url = Settings.baseAppUrl + "/" + Settings.collectionType + "?dummy" +
      (if (useCursor) "&cursor=${" + SessionVarCursor + "}" else "") +
      (if (Settings.searchLimit > 0) "&limit=" + Settings.searchLimit.toString() else "") +
      (if (Settings.searchQuery != "") "&ql=" + Settings.searchQuery else "")

    url
  }

  def entityGetByNameUrl(entityName: String): String = {
    val url = Settings.baseCollectionUrl + "/" + entityName

    url
  }

  def randomEntityNameUrl(prefix: String = Settings.entityPrefix, numEntities: Int = Settings.numEntities, seed: Int = Settings.entitySeed): String = {
    Utils.randomEntityNameUrl(prefix, numEntities, seed, Settings.baseCollectionUrl)
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
    .doIfOrElse(session => Settings.endConditionType == EndConditionType.MinutesElapsed) {
      asLongAs(session => (System.currentTimeMillis() - Settings.testStartTime) < Settings.endMinutes*60*1000,"loopCounter",true) {
        exec(getEntitiesWithoutCursor)
          .asLongAs(stringParamExists(SessionVarCursor)) {
          exec(getEntitiesWithCursor)
        }
      }
    } {
      repeat(Settings.endRequestCount) {
        exec(getEntitiesWithoutCursor)
          .asLongAs(stringParamExists(SessionVarCursor)) {
          exec(getEntitiesWithCursor)
        }
      }
    }
    //.exec(sessionFunction => { sessionFunction })

  /*
   * Get random entities
   */
  val getRandomEntityAnonymous = exec(
    http("GET entity by name (anonymous)")
      .get(_ => randomEntityNameUrl())
      .headers(Headers.authAnonymous)
      .check(status.is(200))
  )

  val getRandomEntityWithToken = exec(
    http("GET entity by name (token)")
      .get(_ => randomEntityNameUrl())
      .headers(Headers.authToken)
      .check(status.is(200))
  )

  val getRandomEntitiesByName = scenario("Get entities by name randomly")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .doIfOrElse(session => Settings.endConditionType == EndConditionType.MinutesElapsed) {
      asLongAs(session => (System.currentTimeMillis() - Settings.testStartTime) < Settings.endMinutes*60*1000) {
        doIfOrElse(session => Settings.authType == AuthType.Anonymous) {
          exec(getRandomEntityAnonymous)
        } {
          exec(getRandomEntityWithToken)
        }
      }
    } {
      repeat(Settings.endRequestCount) {
        doIfOrElse(session => Settings.authType == AuthType.Anonymous) {
          exec(getRandomEntityAnonymous)
        } {
          exec(getRandomEntityWithToken)
        }
      }
    }

  /*
   * Create entities
   */
  val loadEntity = exec(
    doIf("${validEntity}", "yes") {
      exec(http("POST load entity")
        .post(Settings.baseCollectionUrl)
        .headers(Headers.authToken)
        .body(StringBody("""${entity}"""))
        // 200 for success, 400 if already exists
        .check(status.in(Seq(200))))
    }
  )

  val loadEntities = scenario("Load entities")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
      feed(FeederGenerator.generateCustomEntityFeeder2(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed))
        .doIf(session => session("validEntity").as[String] == "yes") {
          exec(loadEntity)
        }
    }
    //.rendezVous(Settings.totalUsers)

  /*
  val loadEntity = exec(
    http("POST load entity")
      .post(Settings.baseCollectionUrl)
      .headers(Headers.authToken)
      .body(StringBody("""${entity}"""))
      .check(status.in(Seq(200,400)))
  )

  val loadEntities = scenario("Load entities")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .forever(
      feed(FeederGenerator.generateCustomEntityFeeder(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed))
      .exec(loadEntity)
    )
    */


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
      feed(FeederGenerator.generateCustomEntityFeeder2(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed))
        .doIf(session => session("validEntity").as[String] == "yes") {
          exec(deleteEntity)
        }
    }
  /*
  val createEntityBatchScenario = scenario("Create custom entities")
      .exec(injectStaticTokenToSession())
      .feed(FeederGenerator.generateCustomEntityCreateFeeder(Settings.entityPrefix, Settings.numEntities))
      .exec(EntityScenarios.postEntity)
  */
}
