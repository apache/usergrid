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

 import java.util
 import java.util.UUID
 import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
 import io.gatling.core.Predef._
 import org.apache.usergrid.settings.Utils
 import scala.collection.mutable.ArrayBuffer
 import scala.util.Random

 object FeederGenerator {

  def generateUserWithGeolocationFeeder(numUsers: Int, radius: Double, centerLatitude: Double, centerLongitude: Double): Array[Map[String, String]] = {
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



  /**
   * Generate users forever
   * @param radius
   * @param centerLatitude
   * @param centerLongitude
   * @return
   */
  def generateUserWithGeolocationFeederInfinite(seed:Int,radius: Double, centerLatitude: Double, centerLongitude: Double, maxPossible: Int): Iterator[Map[String, String]] = {
    val userFeeder = Iterator.from(seed).map(i=>generateUserData(i.toString, radius, centerLatitude, centerLongitude))
    return userFeeder

  }

  /**
   * Generate user data based on atomically increasing integers
   * @param radius
   * @param centerLatitude
   * @param centerLongitude
   * @return
   */
  def generateUserData(id: String, radius: Double, centerLatitude: Double, centerLongitude: Double): Map[String, String] = {


    var user: Map[String, String] = EntityDataGenerator.generateUser(id)
    var geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
    var blockLists: Map[String, String] = EntityDataGenerator.generateBlockUserLists(1)

    user = user ++ geolocation ++ blockLists

    return user
  }


  def generateGeolocationFeeder(radius: Double, centerLatitude: Double, centerLongitude: Double): Feeder[String] = {

    val geolocationFeeder = new Feeder[String] {

      // always return true as this feeder can be polled infinitively
      override def hasNext = true

      override def next: Map[String, String] = {
        var geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
        Map("latitude" -> geolocation("latitude"), "longitude" -> geolocation("longitude"))
      }
    }

    return geolocationFeeder

  }

  def generateGeolocationWithQueryFeeder(radius: Double, centerLatitude: Double, centerLongitude: Double): Feeder[String] = {

    val geolocationFeeder = new Feeder[String] {

      // always return true as this feeder can be polled infinitively
      override def hasNext = true

      override def next: Map[String, String] = {
        var geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
        var queryParams = Utils.generateRandomQueryString
        Map("latitude" -> geolocation("latitude"), "longitude" -> geolocation("longitude"), "queryParams" -> queryParams)
      }
    }

    return geolocationFeeder

  }

  def generateUserConnectionFeeder(numUsers: Int): Feeder[String] = {

    val userIdFeeder = new Feeder[String] {

      // always return true as this feeder can be polled infinitively
      override def hasNext = true

      override def next: Map[String, String] = {
        Map("user1" -> "user".concat(Utils.generateRandomInt(1, numUsers).toString), "user2" -> "user".concat(Utils.generateRandomInt(1, numUsers).toString))
      }
    }

    return userIdFeeder

  }

  def generateEntityNameFeeder(prefix: String, numEntities: Int): Iterator[Map[String, String]]  = {
    val itr = Iterator.from(1).map(i=> Map("entityName" -> prefix.concat(i.toString).concat(UUID.randomUUID().toString)))
    return itr
  }

  def generateRandomEntityNameFeeder(prefix: String, numEntities: Int): Array[Map[String, String]] = {

    var nameArray: ArrayBuffer[Map[String, String]] = new ArrayBuffer[Map[String, String]]

    for (entityCount <- 1 to numEntities) {
      nameArray += Map("entityName" -> prefix.concat(Utils.generateRandomInt(0, 100000000).toString))
    }

    return nameArray.toArray

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
     val userFeeder = Iterator.from(seed).map(i=>Map("entityName" -> i.toString.concat(UUID.randomUUID().toString), "entity" -> EntityDataGenerator.generateCustomEntityJSONString()));
     return userFeeder
   }


   def testFeeder(seed:Int): Iterator[Map[String, String]] = {
     var entity: Map[String, String] = EntityDataGenerator.generateCustomEntity();
     Map("entity" -> entity)
     val userFeeder = Iterator.from(seed).map(i=>EntityDataGenerator.generateCustomEntity())
     return userFeeder
   }

/*

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
}
