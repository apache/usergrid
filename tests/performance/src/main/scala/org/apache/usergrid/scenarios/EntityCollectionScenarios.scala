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

import java.nio.charset.StandardCharsets

import io.gatling.core.Predef._
import io.gatling.core.feeder.RecordSeqFeederBuilder
import io.gatling.http.Predef._
import io.gatling.http.response.ResponseWrapper
import io.gatling.http.response.StringResponseBody
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.enums.{CsvFeedPatternType, EndConditionType, AuthType}
import org.apache.usergrid.helpers.Extractors._
import org.apache.usergrid.helpers.{Headers, Utils}
import org.apache.usergrid.settings.Settings

/**
 * Provides CRUD methods for custom entities
 *
 *
 *
 */
object EntityCollectionScenarios {

  //The value for the cursor
  val SessionVarCursor: String = "cursor"
  val SessionVarStatus: String = "status"
  val SessionVarUuid: String = "createUuid"
  val SessionVarModified: String = "createModified"
  val SessionVarBodyString: String = "bodyString"

  def entityGetUrl(useCursor: Boolean): String = {
    val url = s"/${Settings.collection}?" +
      (if (useCursor) "cursor=${" + SessionVarCursor + "}&" else "") +
      (if (Settings.searchQuery != "") s"ql=${Settings.searchQuery}&" else "") +
      (if (Settings.searchLimit > 0) s"limit=${Settings.searchLimit}&" else "")

    // remove trailing & or ?
    url.dropRight(1)
  }

  def entityGetByNameUrl(entityName: String): String = {

    s"/${Settings.collection}/$entityName"
  }

  def randomEntityNameUrl(prefix: String = Settings.entityPrefix, numEntities: Int = Settings.numEntities, seed: Int = Settings.entitySeed): String = {

    Utils.randomEntityNameUrl(prefix, numEntities, seed, Settings.baseCollectionUrl)
  }

  def uuidFeeder(): RecordSeqFeederBuilder[String] = {
    if (Settings.csvFeedPattern == CsvFeedPatternType.Circular) {
      csv(Settings.feedUuidFilename).circular
    } else {
      csv(Settings.feedUuidFilename).random
    }
  }

  /*
   * Loop through entities using cursor
   */
  val getEntitiesWithoutCursor = exec(
    http("GET entities")
      .get(entityGetUrl(false))
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200), maybeExtractCursor(SessionVarCursor))
  )

  val getEntitiesWithCursor = exec(
    http("GET entities")
      .get(entityGetUrl(true))
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
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
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200))
  )

  val getRandomEntityWithToken = exec(
    http("GET entity by name (token)")
      .get(randomEntityNameUrl())
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200))
  )

  val getRandomEntitiesByName = scenario("Get entities by name randomly")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .doIfOrElse(_ => Settings.endConditionType == EndConditionType.MinutesElapsed) {
    asLongAs(_ => Settings.continueMinutesTest) {
      tryMax(1+Settings.retryCount) {
        doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
          exec(getRandomEntityAnonymous)
        } {
          exec(getRandomEntityWithToken)
        }
      }
    }
  } {
    repeat(_ => Settings.endRequestCount.toInt) {
      tryMax(1+Settings.retryCount) {
        doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
          exec(getRandomEntityAnonymous)
        } {
          exec(getRandomEntityWithToken)
        }
      }
    }
  }

  /*
   * Get random entities by UUID
   */
  val getRandomEntityByUuidAnonymous = exec(
    http("GET entity by UUID (anonymous)")
      .get("/" + Settings.collection + "/${uuid}")
      .queryParamMap(Settings.queryParamMap)
      .headers(Headers.authAnonymous)
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200))
  )

  val getRandomEntityByUuidWithToken = exec(
    http("GET entity by UUID (token)")
      .get("/" + Settings.collection + "/${uuid}")
      .queryParamMap(Settings.queryParamMap)
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200))
  )

  val getRandomEntitiesByUuid = scenario("Get entities by uuid")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .doIfOrElse(_ => Settings.endConditionType == EndConditionType.MinutesElapsed) {
      asLongAs(_ => Settings.continueMinutesTest) {
          feed(uuidFeeder())
            /*.exec{
            session => println(s"UUID: ${session("uuid").as[String]}")
            session
            }*/
            .tryMax(1+Settings.retryCount) {
            doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
              exec(getRandomEntityByUuidAnonymous)
            } {
              exec(getRandomEntityByUuidWithToken)
            }
          }
      }
    } {
      repeat(_ => Settings.endRequestCount.toInt) {
          feed(uuidFeeder())
            /*.exec {
            session => println(s"UUID: ${session("uuid").as[String]}")
            session
            }*/
            .tryMax(1+Settings.retryCount) {
              doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
                exec(getRandomEntityByUuidAnonymous)
              } {
                exec(getRandomEntityByUuidWithToken)
              }
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
        .headers(Headers.usergridRegionHeaders)
        .body(StringBody("""${entity}"""))
        .transformResponse {
          case response if response.isReceived =>
            new ResponseWrapper(response) {
              val contentType = response.header("content-type").getOrElse("").toLowerCase
              val bodyStr = if (contentType.contains("json")) response.body.string else "[]"
              if (bodyStr == "[]") { println(">>>>>>>>>>> USING EMPTY BODY") }
              override val body = StringResponseBody(bodyStr, response.charset)
            }
        }
        // 200 for success, 400 if already exists
        .check(status.saveAs(SessionVarStatus), bodyString.saveAs(SessionVarBodyString), extractEntityUuid(SessionVarUuid), extractEntityModified(SessionVarModified)))
        .exec(session => {
          val saveFailures = Settings.saveInvalidResponse
          val status = session(SessionVarStatus).as[Int]
          Settings.addStatus(status)
          if (saveFailures || status == 200) {
            val uuid = if (status == 200) session(SessionVarUuid).as[String] else ""
            val entityName = session("entityName").as[String]
            val modified = if (status == 200) session(SessionVarModified).as[Long] else 0
            val collectionName = session("collectionName").as[String]
            Settings.addUuid(uuid, collectionName, entityName, modified, status)
            if (status != 200) {
              val bodyString = session(SessionVarBodyString).as[String]
              println(s">>>>>>>> LOAD ERROR - Status: $status\nBody:\n$bodyString")
              session.markAsFailed
            } else {
              session
            }
          } else {
            session.markAsFailed
          }
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
          tryMax(if (Settings.saveInvalidResponse) 1 else 1+Settings.retryCount) {
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
      .headers(Headers.usergridRegionHeaders)
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
        tryMax(1+Settings.retryCount) {
          exec(deleteEntity)
        }
      }
  }

  /*
   * Update entities
   */
  val updateEntity = exec(
    http("UPDATE entity")
      .put("""${entityUrl}""")
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
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

  /*
   * Get by name sequential
   */
  val getEntityByNameSequentialAnonymous = exec(
    doIf("${validEntity}", "yes") {
      exec(http("GET entity by name sequential (anonymous)")
        .get("/" + Settings.collection + "/${entityName}")
        .queryParamMap(Settings.queryParamMap)
        .headers(Headers.authAnonymous)
        .headers(Headers.usergridRegionHeaders)
        .check(status.saveAs(SessionVarStatus), extractEntityUuid(SessionVarUuid), extractEntityModified(SessionVarModified)))
        .exec(session => {
          val saveFailures = Settings.saveInvalidResponse
          val status = session(SessionVarStatus).as[Int]
          Settings.addStatus(status)
          if (saveFailures || status == 200) {
            val uuid = if (status == 200) session(SessionVarUuid).as[String] else ""
            val entityName = session("entityName").as[String]
            val modified = if (status == 200) session(SessionVarModified).as[Long] else 0
            val collectionName = session("collectionName").as[String]
            Settings.addUuid(uuid, collectionName, entityName, modified, status)
            session
          } else {
            session.markAsFailed
          }
        }).exitHereIfFailed
    }
  )

  val getEntityByNameSequentialWithToken = exec(
    doIf("${validEntity}", "yes") {
      exec(http("GET entity by name sequential (anonymous)")
        .get("/" + Settings.collection + "/${entityName}")
        .queryParamMap(Settings.queryParamMap)
        .headers(Headers.authToken)
        .headers(Headers.usergridRegionHeaders)
        .check(status.saveAs(SessionVarStatus), extractEntityUuid(SessionVarUuid), extractEntityModified(SessionVarModified)))
        .exec(session => {
          val saveFailures = Settings.saveInvalidResponse
          val status = session(SessionVarStatus).as[Int]
          Settings.addStatus(status)
          if (saveFailures || status == 200) {
            val uuid = if (status == 200) session(SessionVarUuid).as[String] else ""
            val entityName = session("entityName").as[String]
            val modified = if (status == 200) session(SessionVarModified).as[Long] else 0
            val collectionName = session("collectionName").as[String]
            Settings.addUuid(uuid, collectionName, entityName, modified, status)
            session
          } else {
            session.markAsFailed
          }
      }).exitHereIfFailed
    }
  )

  val getEntitiesByNameSequential = scenario("Get entities by name sequentially")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
      feed(FeederGenerator.generateCustomEntityFeeder(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed))
        /*.exec{
          session => if (session("validEntity").as[String] == "yes") { println("Loading entity #" + session("entityNum").as[String]) }
          session
        }*/
        .doIf(session => session("validEntity").as[String] == "yes") {
          tryMax(if (Settings.saveInvalidResponse) 1 else 1+Settings.retryCount) {
            doIfOrElse(_ => Settings.authType == AuthType.Anonymous) {
              exec(getEntityByNameSequentialAnonymous)
            } {
              exec(getEntityByNameSequentialWithToken)
            }
          }
        }
    }

  val doNothing = scenario("Do Nothing").exec(http("Get Page").get("http://google.com"))

}
