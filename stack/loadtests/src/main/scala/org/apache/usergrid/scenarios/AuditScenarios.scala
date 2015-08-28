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
import org.apache.usergrid.enums.{AuthType, EndConditionType}
import org.apache.usergrid.helpers.Extractors._
import org.apache.usergrid.helpers.{Headers, Utils}
import org.apache.usergrid.settings.Settings

/**
 * Provides CRUD methods for audits
 */
object AuditScenarios {

  //The value for the cursor
  val SessionVarCursor: String = "cursor"
  val SessionVarUuid: String = "entityUuid"
  val SessionVarCollectionName: String = "collectionName"
  val SessionVarCollectionEntities: String = "collectionEntities"

  def collectionGetUrl(useCursor: Boolean): String = {
    val searchQuery =
      // later than timestamp replaces query
      if (Settings.laterThanTimestamp > 0) s"modified%20gte%20${Settings.laterThanTimestamp}"
      else if (Settings.searchQuery != "") s"${Settings.searchQuery}"
      else ""
    val url = "/${" + SessionVarCollectionName + "}?" +
      (if (useCursor) "cursor=${" + SessionVarCursor + "}&" else "") +
      (if (searchQuery != "") s"ql=$searchQuery&" else "") +
      (if (Settings.searchLimit > 0) s"limit=${Settings.searchLimit}&" else "")

    // remove trailing & or ?
    println (url.dropRight(1))
    url.dropRight(1)
  }

  val getCollectionsWithoutCursor = exec(
    http("GET collections")
      .get(collectionGetUrl(false))
      .headers(Headers.authToken)
      .check(status.is(200),extractAuditEntities(SessionVarCollectionEntities),maybeExtractCursor(SessionVarCursor)))
      .foreach("${" + SessionVarCollectionEntities + "}", "singleResult") {
        exec(session => {
          val resultObj = session("singleResult").as[Map[String,Any]]
          val uuid = resultObj.getOrElse("uuid", "").asInstanceOf[String]
          val entityName = resultObj.getOrElse("name", "").asInstanceOf[String]
          val modified = resultObj.getOrElse("modified", "-1").asInstanceOf[Long]
          val collectionName = session(SessionVarCollectionName).as[String]
          Settings.addAuditUuid(uuid, collectionName, entityName, modified)
          session
        })
      }

  val getCollectionsWithCursor = exec(
    http("GET collections")
      .get(collectionGetUrl(true))
      .headers(Headers.authToken)
      .check(status.is(200),extractAuditEntities(SessionVarCollectionEntities),maybeExtractCursor(SessionVarCursor)))
      .foreach("${" + SessionVarCollectionEntities + "}", "singleResult") {
        exec(session => {
          val resultObj = session("singleResult").as[Map[String,Any]]
          val uuid = resultObj.getOrElse("uuid","").asInstanceOf[String]
          val entityName = resultObj.getOrElse("name","").asInstanceOf[String]
          val modified = resultObj.getOrElse("modified","-1").asInstanceOf[Long]
          val collectionName = session(SessionVarCollectionName).as[String]
          Settings.addAuditUuid(uuid, collectionName, entityName, modified)
          session
        })
      }

  val getAllCollections = scenario("Get all collections")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
      feed(FeederGenerator.collectionNameFeeder)
        .exec{
          session => if (session("validEntity").as[String] == "yes") { println("Getting collection " + session("collectionName").as[String]) }
          session
        }
        .doIf(session => session("validEntity").as[String] == "yes") {
          tryMax(Settings.retryCount) {
            exec(getCollectionsWithoutCursor)
          }.asLongAs(stringParamExists(SessionVarCursor)) {
            tryMax(Settings.retryCount) {
              exec(getCollectionsWithCursor)
            }
          }
        }
    }.exec { session =>
      // displays the content of the session in the console (debugging only)
      println(session)

      // return the original session
      session
    }


  val getCollectionEntity = exec(
    http("GET collection entity")
      .get("/${collectionName}?ql=uuid=${uuid}")
      .headers(Headers.authToken)
      .check(status.is(200),jsonPath("$.count").optional.saveAs("count"),extractAuditEntities(SessionVarCollectionEntities)))
      .exec(session => {
        val count = session("count").as[String].toInt
        val uuid = session("uuid").as[String]
        val entityName = session("name").as[String]
        val modified = session("modified").as[String].toLong
        val collectionName = session(SessionVarCollectionName).as[String]

        if (count < 1) {
          Settings.addAuditUuid(uuid, collectionName, entityName, modified)
          Settings.incAuditNotFound()
          println(s"NOT FOUND: $collectionName.$entityName ($uuid)")
        } else if (count > 1) {
          Settings.addAuditUuid(uuid, collectionName, entityName, modified)
          Settings.incAuditBadResponse()
          println(s"INVALID RESPONSE (count=$count): $collectionName.$entityName ($uuid)")
        } else {
          // println(s"FOUND: $collectionName.$entityName ($uuid)")
          Settings.incAuditSuccess()
        }

        session
      })

  val verifyCollections = scenario("Verify collections")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
    feed(FeederGenerator.collectionCsvFeeder)
      .doIf(session => session("validEntity").as[String] == "yes") {
        tryMax(Settings.retryCount) {
          exec(getCollectionEntity)
        }
      }
    }

}
