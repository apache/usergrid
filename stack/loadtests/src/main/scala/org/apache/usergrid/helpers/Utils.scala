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
package org.apache.usergrid.helpers

import scala.util.Random
import scala.util.parsing.json.JSONObject
import org.apache.usergrid.settings.Settings

 /**
 *
 * Utility for creating various data elements
 *
 */
object Utils {

  private val RNG = new Random(System.currentTimeMillis())

  /**
   * Generate a new uuid and replace the '-' with empty
   */
  def generateUUIDString(): String = {
    java.util.UUID.randomUUID.toString.replace("-", "")
  }

  /**
   * Generate a unique string with a prefix
   *
   * @param prefix
   * @return
   */
  def generateUniqueName(prefix : String): String = {
     prefix + generateUUIDString()
  }

  // random number in between [a...b]
  def generateRandomInt(lowerBound: Int, upperBound: Int) = RNG.nextInt(upperBound - lowerBound) + lowerBound

  def generateRandomGeolocation(radius: Double, centerLatitude: Double, centerLongitude: Double):Map[String, String] = {

    val rd = radius / 111300 // Convert Radius from meters to degrees.
    val u = RNG.nextFloat()
    val v = RNG.nextFloat()
    val q = math.sqrt(u) * rd
    val w = q * rd
    val t = 2 * math.Pi * v
    val x = math.cos(t) * w
    val y = math.sin(t) * w
    val xp = x/math.cos(centerLatitude)
    val latitude = (y + centerLatitude).toString
    val longitude = (xp + centerLongitude).toString
    val geolocation: Map[String, String] = Map("latitude"->latitude,"longitude"->longitude)

    geolocation
  }

  def generateRandomQueryString: String = {

    val queryParams = Array("age", "height", "weight")
    var queryString = ""

    for (numParams <- 1 to generateRandomInt(1, queryParams.length)) {
      queryString = s"age=${Utils.generateRandomInt(18,65).toString}"
      if (numParams >= 2) {
        queryString += s"%20AND%20height=${Utils.generateRandomInt(48,84).toString}"
      }
      if (numParams >= 3) {
        queryString += s"%20AND%20weight=${Utils.generateRandomInt(120,350).toString}"
      }
    }

    queryString
  }

   def randomEntityNameUrl(prefix: String, numEntities: Int, seed: Int, baseUrl: String): String = {
     val randomVal = generateRandomInt(seed, seed+numEntities-1)

     if (Settings.getViaQuery) s"$baseUrl?ql=name='$prefix$randomVal'" else s"$baseUrl/$prefix$randomVal"
   }

  def createRandomPushNotifierName:String = {
    Utils.generateUniqueName("notifier")
  }

  def toJSONStr(objectMap: Map[String, Any]):String = {
    JSONObject(objectMap).toString()
  }

}
