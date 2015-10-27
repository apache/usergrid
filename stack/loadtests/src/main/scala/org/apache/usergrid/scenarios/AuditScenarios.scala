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
  val SessionVarStatus: String = "newStatus"
  val SessionVarEntityUuid: String = "entityUuid"
  val SessionVarEntityName: String = "entityName"
  val SessionVarDeletedUuid: String = "deletedUuid"
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
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200),status.saveAs(SessionVarStatus),extractAuditEntities(SessionVarCollectionEntities),maybeExtractCursor(SessionVarCursor)))
      .foreach("${" + SessionVarCollectionEntities + "}", "singleResult") {
        exec(session => {
          val resultObj = session("singleResult").as[Map[String,Any]]
          val status = session(SessionVarStatus).as[Int]
          val uuid = resultObj.getOrElse("uuid", "").asInstanceOf[String]
          val entityName = resultObj.getOrElse("name", "").asInstanceOf[String]
          val modified = resultObj.getOrElse("modified", "0").asInstanceOf[Long]
          val collectionName = session(SessionVarCollectionName).as[String]
          Settings.addUuid(uuid, collectionName, entityName, modified, status)
          session
        })
      }

  val getCollectionsWithCursor = exec(
    http("GET collections")
      .get(collectionGetUrl(true))
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200),status.saveAs(SessionVarStatus),extractAuditEntities(SessionVarCollectionEntities),maybeExtractCursor(SessionVarCursor)))
      .foreach("${" + SessionVarCollectionEntities + "}", "singleResult") {
        exec(session => {
          val resultObj = session("singleResult").as[Map[String,Any]]
          val status = session(SessionVarStatus).as[Int]
          val uuid = resultObj.getOrElse("uuid","").asInstanceOf[String]
          val entityName = resultObj.getOrElse("name","").asInstanceOf[String]
          val modified = resultObj.getOrElse("modified","-1").asInstanceOf[Long]
          val collectionName = session(SessionVarCollectionName).as[String]
          Settings.addUuid(uuid, collectionName, entityName, modified, status)
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
          tryMax(1+Settings.retryCount) {
            exec(getCollectionsWithoutCursor)
          }.asLongAs(stringParamExists(SessionVarCursor)) {
            tryMax(1+Settings.retryCount) {
              exec(getCollectionsWithCursor)
            }
          }
        }
    /*}.exec { session =>
      // displays the content of the session in the console (debugging only)
      println(session)

      // return the original session
      session */
    }

  val deleteAuditedEntity = exec(
    http("DELETE audited entity")
      .delete("/${collectionName}/${uuid}")
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
      .check(extractEntityUuid(SessionVarDeletedUuid)))
      .exec(session => {
        val uuid = session(SessionVarDeletedUuid).as[String]

        if (uuid != null && uuid != "") {
          // successful
          Settings.incAuditEntryDeleteSuccess()
        } else {
          val collectionName = session(SessionVarCollectionName).as[String]
          val reqName = session("name").as[String]
          val entityUuid = session(SessionVarEntityUuid).as[String]
          Settings.incAuditEntryDeleteFailure()
          println(s"DELETE FAILED: $collectionName:$reqName ($entityUuid)")
        }

        session
      })

  val getCollectionEntityDirect = exec(
    http("GET collection entity direct")
      .get("/${collectionName}/${accessField}")
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
      .check()
      .check(status.in(Seq(200,404)),status.saveAs(SessionVarStatus),extractAuditEntities(SessionVarCollectionEntities),
        extractEntityUuid(SessionVarEntityUuid),extractEntityName(SessionVarEntityName)))
      .exec(session => {
        val saveFailures = Settings.saveInvalidResponse
        val status = session(SessionVarStatus).as[Int]
        val collectionName = session(SessionVarCollectionName).as[String]
        val modified = session("modified").as[String].toLong
        val uuid = session("uuid").as[String]
        val reqName = session("name").as[String]
        val lastStatus = session("lastStatus").as[String]
        if (status == 200 || status == 404) {
          val collectionEntities = session(SessionVarCollectionEntities).as[Seq[Any]]
          val entityUuid = session(SessionVarEntityUuid).as[String]
          val entityName = session(SessionVarEntityName).as[String]

          val count = collectionEntities.length
          if (count < 1) {
            Settings.addAuditUuid(uuid, collectionName, reqName, modified, status, s"NotFoundAtAll", lastStatus)
            Settings.incAuditNotFoundAtAll()
            println(s"NOT FOUND AT ALL: $collectionName.$reqName ($uuid)")
          } else if (count > 1) {
            // invalid
            Settings.addAuditUuid(uuid, collectionName, reqName, modified, status, s"QueryInvalidCount$count", lastStatus)
            Settings.incAuditBadResponse()
            println(s"INVALID RESPONSE (count=$count): $collectionName.$reqName ($uuid)")
          } else {
            // count == 1 -> found via direct access but not query

            // will count as found directly even if there is a uuid or name mismatch
            val errorPrefix = "DirectAccessSuccessful"
            var errorString: String = errorPrefix
            if (entityUuid == null || entityUuid.isEmpty) {
              errorString += "|NoUuidReturned"
              Settings.incAuditPayloadUuidError()
              println(s"PAYLOAD UUID MISSING (DIRECT): requestedUuid=$uuid")
            } else if (!uuid.equalsIgnoreCase(entityUuid)) {
              errorString += "|ReturnedUuidMismatch"
              Settings.incAuditPayloadUuidError()
              println(s"PAYLOAD UUID MISMATCH (DIRECT): requestedUuid=$uuid returnedUuid=$entityUuid")
            }
            if (entityName == null || entityName.isEmpty) {
              errorString += "|NoNameReturned"
              Settings.incAuditPayloadNameError()
              println(s"PAYLOAD NAME MISSING (DIRECT): requestedName=$reqName")
            } else if (!reqName.equalsIgnoreCase(entityName)) {
              errorString += "|ReturnedNameMismatch"
              Settings.incAuditPayloadNameError()
              println(s"PAYLOAD NAME MISMATCH (DIRECT): requestedName=$reqName returnedName=$entityName")
            }

            Settings.addAuditUuid(uuid, collectionName, reqName, modified, status, errorString, lastStatus)
            Settings.incAuditNotFoundViaQuery()
            println(s"NOT FOUND VIA QUERY: $collectionName.$reqName ($uuid)")
          }
          session
        } else if (saveFailures) {
          Settings.addAuditUuid(uuid, collectionName, reqName, modified, status, "Failure", lastStatus)
          session
        } else {
          session.markAsFailed
        }

      }).exitHereIfFailed

  val getCollectionEntity = exec(
    http("GET collection entity")
      .get("/${collectionName}?ql=${queryField}")
      .headers(Headers.authToken)
      .headers(Headers.usergridRegionHeaders)
      .check(status.is(200), status.saveAs(SessionVarStatus), jsonPath("$.count").optional.saveAs("count"),
        extractAuditEntities(SessionVarCollectionEntities),
        extractEntityUuid(SessionVarEntityUuid),extractEntityName(SessionVarEntityName)))
      .exec(session => {
        val count = session("count").as[String].toInt
        val status = session(SessionVarStatus).as[Int]
        val uuid = session("uuid").as[String]
        val reqName = session("name").as[String]
        val modified = session("modified").as[String].toLong
        val lastStatus = session("lastStatus").as[String]
        val collectionName = session(SessionVarCollectionName).as[String]
        val entityUuid = session(SessionVarEntityUuid).as[String]
        val entityName = session(SessionVarEntityName).as[String]

        if (count < 1) {
          // will check to see whether accessible directly
        } else if (count > 1) {
          Settings.addAuditUuid(uuid, collectionName, reqName, modified, status, s"QueryInvalidCount$count", lastStatus)
          Settings.incAuditBadResponse()
          println(s"INVALID RESPONSE (count=$count): $collectionName.$reqName ($uuid)")
        } else {
          // count == 1 -> success
          // println(s"FOUND: $collectionName.$entityName ($uuid)")

          val errorPrefix = "QuerySuccessful"
          var errorString: String = errorPrefix
          if (entityUuid == null || entityUuid.isEmpty) {
            errorString += "|NoUuidReturned"
            Settings.incAuditPayloadUuidError()
            println(s"PAYLOAD UUID MISSING (QUERY): requestedUuid=$uuid")
          } else if (!uuid.equalsIgnoreCase(entityUuid)) {
            errorString += "|ReturnedUuidMismatch"
            Settings.incAuditPayloadUuidError()
            println(s"PAYLOAD UUID MISMATCH (QUERY): requestedUuid=$uuid returnedUuid=$entityUuid")
          }
          if (reqName != null && reqName != "") {
            if (entityName == null || entityName.isEmpty) {
              errorString += "|NoNameReturned"
              Settings.incAuditPayloadNameError()
              println(s"PAYLOAD NAME MISSING (QUERY): requestedName=$reqName")
            } else if (!reqName.equalsIgnoreCase(entityName)) {
              errorString += "|ReturnedNameMismatch"
              Settings.incAuditPayloadNameError()
              println(s"PAYLOAD NAME MISMATCH (QUERY): requestedName=$reqName returnedName=$entityName")
            }
          }

          // log even if technically successful -- we need to capture incorrect response
          if (errorString != errorPrefix) {
            Settings.addAuditUuid(uuid, collectionName, reqName, modified, status, errorString, lastStatus)
          }
          Settings.incAuditSuccess()
        }

        session
      })
      .doIf(session => session("count").as[String].toInt < 1) {
        exec(getCollectionEntityDirect)
      }
      .doIf(session => Settings.deleteAfterSuccessfulAudit && session("count").as[String].toInt == 1) {
        // tryMax(Settings.retryCount) {
          exec(deleteAuditedEntity)
        // }
      }

  val verifyCollections = scenario("Verify collections")
    .exec(injectTokenIntoSession())
    .exec(injectAuthType())
    .asLongAs(session => session("validEntity").asOption[String].map(validEntity => validEntity != "no").getOrElse[Boolean](true)) {
    feed(FeederGenerator.collectionCsvFeeder)
      .doIf(session => session("validEntity").as[String] == "yes") {
        tryMax(if (Settings.saveInvalidResponse) 1 else 1+Settings.retryCount) {
          exec(getCollectionEntity)
        }
      }
    }

}
