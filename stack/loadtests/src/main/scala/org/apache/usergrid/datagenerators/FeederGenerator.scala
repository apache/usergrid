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
package org.apache.usergrid.datagenerators

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.Predef._
import org.apache.usergrid.helpers.{Setup, Utils}
import org.apache.usergrid.settings.Settings
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object FeederGenerator {

  def generateUserWithGeolocationFeeder(numUsers: Int, radius: Double, centerLatitude: Double, centerLongitude: Double): Array[Map[String, String]] = {
    var userArray: ArrayBuffer[Map[String, String]] = new ArrayBuffer[Map[String, String]]
    for (userCount <- 1 to numUsers) {
      var user: Map[String, String] = EntityDataGenerator.generateUser(userCount.toString)
      val geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
      val blockLists: Map[String, String] = EntityDataGenerator.generateBlockUserLists(numUsers)

      user = user ++ geolocation ++ blockLists

      userArray += user
    }

    userArray.toArray
  }

  /**
   * Generate users forever
   * @param seed
   * @param radius
   * @param centerLatitude
   * @param centerLongitude
   * @return
   */
  def generateUserWithGeolocationFeederInfinite(seed:Int,radius: Double, centerLatitude: Double, centerLongitude: Double): Iterator[Map[String, String]] = {
    Iterator.from(seed).map(i=>generateUserData(i.toString, radius, centerLatitude, centerLongitude))
  }

  /**
   * Generate user data based on atomically increasing integers
   * @param radius
   * @param centerLatitude
   * @param centerLongitude
   * @return
   */
  def generateUserData(id: String, radius: Double, centerLatitude: Double, centerLongitude: Double): Map[String, String] = {

    val user: Map[String, String] = EntityDataGenerator.generateUser(id)
    val geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
    val blockLists: Map[String, String] = EntityDataGenerator.generateBlockUserLists(1)

    // merge maps
    user ++ geolocation ++ blockLists
  }


  def generateGeolocationFeeder(radius: Double, centerLatitude: Double, centerLongitude: Double): Feeder[String] = {

    val geolocationFeeder = new Feeder[String] {

      // always return true as this feeder can be polled infinitively
      override def hasNext = true

      override def next(): Map[String, String] = {
        val geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
        Map("latitude" -> geolocation("latitude"), "longitude" -> geolocation("longitude"))
      }
    }

    geolocationFeeder
  }

  def generateGeolocationWithQueryFeeder(radius: Double, centerLatitude: Double, centerLongitude: Double): Feeder[String] = {

    val geolocationFeeder = new Feeder[String] {

      // always return true as this feeder can be polled infinitively
      override def hasNext = true

      override def next(): Map[String, String] = {
        val geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
        val queryParams = Utils.generateRandomQueryString
        Map("latitude" -> geolocation("latitude"), "longitude" -> geolocation("longitude"), "queryParams" -> queryParams)
      }
    }

    geolocationFeeder
  }

  def generateUserConnectionFeeder(numUsers: Int): Feeder[String] = {

    val userIdFeeder = new Feeder[String] {

      // always return true as this feeder can be polled infinitely
      override def hasNext = true

      override def next(): Map[String, String] = {
        Map("user1" -> "user".concat(Utils.generateRandomInt(1, numUsers).toString), "user2" -> "user".concat(Utils.generateRandomInt(1, numUsers).toString))
      }
    }

    userIdFeeder
  }

  def generateEntityNameFeeder(prefix: String, numEntities: Int): Iterator[Map[String, String]]  = {
    Iterator.from(1).map(i=> Map("entityName" -> prefix.concat(i.toString).concat(UUID.randomUUID().toString)))
  }

  def generateRandomEntityNameFeeder(prefix: String, numEntities: Int): Array[Map[String, String]] = {

    var nameArray: ArrayBuffer[Map[String, String]] = new ArrayBuffer[Map[String, String]]

    for (_ <- 1 to numEntities) {
      nameArray += Map("entityName" -> prefix.concat(Utils.generateRandomInt(0, 100000000).toString))
    }

    nameArray.toArray
  }




   /**
    * Generate users forever
    * @param seed The seed
    * @return
    */
   def generateCustomEntityInfinite(seed:Int): Iterator[Map[String, String]] = {
     //val rod = "rod"
     val userFeeder = Iterator.from(seed).map(i=>EntityDataGenerator.generateCustomEntity())
     return userFeeder
   }



   /**
    * Generate users forever
    * @param seed The seed
    * @return
    */
   def generateCustomEntityPutInfinite(seed:Int): Iterator[Map[String, Any]] = {
     //val rod = "rod"
     val userFeeder = Iterator.from(seed).map(i=>Map("entityName" -> i.toString.concat(UUID.randomUUID().toString), "entity" -> EntityDataGenerator.generateCustomEntity()));
     return userFeeder
   }


   /*
      def testFeeder(seed:Int): Iterator[Map[String, String]] = {
        var entity: Map[String, String] = EntityDataGenerator.generateCustomEntity();
        Map("entity" -> entity)
        val userFeeder = Iterator.from(seed).map(i=>EntityDataGenerator.generateCustomEntity())
        return userFeeder
      }


      def testFeeder(): Array[Map[String, String]] = {
        var userArray: ArrayBuffer[Map[String, String]] = new ArrayBuffer[Map[String, String]]
        for (userCount <- 1 to numUsers) {
          var user: Map[String, String] = EntityDataGenerator.generateUser(userCount.toString)
          var geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
          var blockLists: Map[String, String] = EntityDataGenerator.generateBlockUserLists(numUsers)

          user = user ++ geolocation ++ blockLists

          userArray += user
        }
        return userArray.toArray
      }
     */

 /* --------------------------- */

 /**
  * Generate specified number of custom entities
  * @param numEntities Number of entities to create
  * @param entityType Type of entity to create
  * @param prefix Prefix for entities
  * @return
  */
 def generateCustomEntityArray(numEntities: Int, entityType: String, prefix: String, seed: Int = 1): Array[String] = {
   //val entityFeeder = Iterator.from(1).take(numEntities).map(i=>Map("entity" -> EntityDataGenerator.generateNamedCustomEntityJSONString(prefix.concat(i.toString()))))
   var entityArray: ArrayBuffer[String] = new ArrayBuffer[String]
   for (i <- seed to numEntities+seed-1) {
     var entity = EntityDataGenerator.generateEntity(entityType, prefix.concat(i.toString), i)
     entityArray += entity
   }

   entityArray.toArray
 }

 /*
  * Gatling doesn't handle feeders running out of data well -- ending test with failure and not building a report.
  * This feeder will serve data forever, but validEntity will be set to "no" when data has run out. Each user can
  * then exit in a controlled fashion.
  */
 def generateCustomEntityFeeder(numEntities: Int, entityType: String, prefix: String = "", seed: Int = 1): Feeder[String] =
 new Feeder[String] {
   var counter = new AtomicInteger(0)

   // runs forever -- users detect when data is done using validEntity field
   override def hasNext: Boolean = true

   override def next(): Map[String, String] = {
     val i = counter.getAndIncrement()
     val seededVal = if (Settings.interleavedWorkerFeed) seed + (i * Settings.entityWorkerCount) + (Settings.entityWorkerNum - 1) else i + seed
     val noPrefix = prefix == null || prefix == ""
     val entityName = if (noPrefix) seededVal.toString else prefix.concat(seededVal.toString)
     val entity = EntityDataGenerator.generateEntity(entityType, if (noPrefix) null else entityName, seededVal)
     //println(entity)
     val entityUrl = Settings.baseCollectionUrl + "/" + entityName
     val validEntity = if (!Settings.unlimitedFeed && i >= numEntities) "no" else "yes"
     val collectionName = Settings.app + "/" + Settings.collection

     // println(entityName)

     Map("entityName" -> entityName, "entity" -> entity, "entityUrl" -> entityUrl, "validEntity" -> validEntity, "entityNum" -> (i+1).toString, "seededEntityNum" -> seededVal.toString,
         "collectionName" -> collectionName)
   }
 }

  def collectionNameFeeder: Feeder[String] = new Feeder[String] {
    val list: List[String] = if (Settings.allApps) Setup.getApplicationCollectionsList else Setup.getCollectionsList()
    var counter = new AtomicInteger(0)

    override def hasNext: Boolean = true

    override def next(): Map[String, String] = {
      val i = counter.getAndIncrement()
      val collectionName = if (i < list.length) list(i) else ""
      val validEntity = if (i >= list.length) "no" else "yes"

      Map("collectionName" -> collectionName, "validEntity" -> validEntity)
    }
  }

  def collectionCsvFeeder: Feeder[String] = new Feeder[String] {
    val csvLines = if (Settings.feedAuditUuids) Source.fromFile(Settings.feedAuditUuidFilename).getLines().toArray else Array[String]()
    val csvLinesLen = csvLines.length
    var counter = new AtomicInteger(0)

    override def hasNext: Boolean = true

    def getNextLine: String = {
      do {
        val i = counter.getAndIncrement()
        if (i >= csvLinesLen) return null

        val line = csvLines(i)
        if (line != Settings.auditUuidsHeader) return line

      } while (true)

      null
    }

    override def next: Map[String, String] = {
      val line = getNextLine
      val validEntity = if (line == null) "no" else "yes"
      val array = if (line != null) line.split(",") else null
      val collectionName = if (line != null) array(0) else ""
      val name = if (line != null) array(1) else ""
      val uuid = if (line != null) array(2) else ""
      val modified = if (line != null) array(3) else ""
      //println(s"$collectionName|$name|$uuid|$modified")

      Map("collectionName" -> collectionName, "name" -> name,  "uuid" -> uuid, "modified" -> modified, "validEntity" -> validEntity)
    }
  }

 def generateCustomEntityInfiniteFeeder(seed: Int = Settings.entitySeed, entityType: String = Settings.entityType, prefix: String = Settings.entityPrefix): Iterator[String] = {
   Iterator.from(seed).map(i=>EntityDataGenerator.generateEntity(entityType, if (prefix == null || prefix == "") null else prefix.concat(i.toString), i))
 }

}
