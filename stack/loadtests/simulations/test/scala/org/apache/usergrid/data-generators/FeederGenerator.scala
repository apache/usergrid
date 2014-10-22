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
import scala.collection.mutable.ArrayBuffer

object FeederGenerator {

  def generateUserWithGeolocationFeeder(numUsers: Int, radius: Double, centerLatitude: Double, centerLongitude: Double): Array[Map[String, String]] = {
    var userArray: ArrayBuffer[Map[String, String]] = new ArrayBuffer[Map[String, String]]
    for (userCount <- 1 to numUsers) {
      var user: Map[String, String] = EntityDataGenerator.generateUser(userCount)
      var geolocation: Map[String, String] = Utils.generateRandomGeolocation(radius, centerLatitude, centerLongitude)
      var blockLists: Map[String, String] = EntityDataGenerator.generateBlockUserLists(numUsers)

      user = user ++ geolocation ++ blockLists

      userArray += user
    }
    return userArray.toArray
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

  def generateEntityNameFeeder(prefix: String, numEntities: Int): Array[Map[String, String]] = {

    var nameArray: ArrayBuffer[Map[String, String]] = new ArrayBuffer[Map[String, String]]

    for (entityCount <- 1 to numEntities) {
      nameArray += Map("entityName" -> prefix.concat(entityCount.toString))
    }

    return nameArray.toArray

  }

}