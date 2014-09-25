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