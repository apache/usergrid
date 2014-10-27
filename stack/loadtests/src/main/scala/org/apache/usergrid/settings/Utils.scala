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
 package org.apache.usergrid.settings

import scala.util.Random
import scala.math
import Array._

/**
 *
 * Utility for creating various data elements
 *
 */
object Utils {

  private val RNG = new Random

  /**
   * Generate a new uuid and replace the '-' with empty
   */
  def generateUUIDString(): String = {
    return java.util.UUID.randomUUID.toString.replace("-", "")
  }

  /**
   * Generate a unique string with a prefix
   *
   * @param prefix
   * @return
   */
  def generateUniqueName(prefix : String): String = {
     return prefix + generateUUIDString()
  }

  // random number in between [a...b]
  def generateRandomInt(lowerBound: Int, upperBound: Int) = RNG.nextInt(upperBound - lowerBound) + lowerBound

  def generateRandomGeolocation(radius: Double, centerLatitude: Double, centerLongitude: Double):Map[String, String] = {

    var rd = radius / 111300 // Convert Radius from meters to degrees.
    var u = RNG.nextFloat()
    var v = RNG.nextFloat()
    var q = math.sqrt(u) * rd
    var w = q * rd
    var t = 2 * math.Pi * v
    var x = math.cos(t) * w
    var y = math.sin(t) * w
    var xp = x/math.cos(centerLatitude)
    var latitude = (y + centerLatitude).toString
    var longitude = (xp + centerLongitude).toString
    var geolocation: Map[String, String] = Map("latitude"->latitude,"longitude"->longitude)

    return geolocation
  }

  def generateRandomQueryString: String = {

    val queryParams = Array("age", "height", "weight")
    var queryString = ""

    for (numParams <- 1 to generateRandomInt(1, queryParams.length)) {
      queryString = "age=" + Utils.generateRandomInt(18, 65).toString
      if (numParams == 2) {
        queryString += "%20AND%20height=" + Utils.generateRandomInt(48, 84).toString
      } else if (numParams == 3) {
        queryString += "%20AND%20weight=" + Utils.generateRandomInt(120, 350).toString
      }
    }

    return queryString
  }

  def createRandomPushNotifier:String = {
    return Utils.generateUniqueName("notifier")
  }

}
