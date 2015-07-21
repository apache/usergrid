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

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.enums._
import scala.concurrent.duration._

object Settings {

  // load configuration settings via property or default
  val org = System.getProperty(ConfigProperties.Org)
  val app = System.getProperty(ConfigProperties.App)
  val adminUser = System.getProperty(ConfigProperties.AdminUser)
  val adminPassword = System.getProperty(ConfigProperties.AdminPassword)
  val baseUrl = System.getProperty(ConfigProperties.BaseUrl)
  val baseAppUrl = baseUrl + "/" + org + "/" + app
  val httpConf = http.baseURL(baseAppUrl)
  val authType = System.getProperty(ConfigProperties.AuthType, AuthType.Anonymous)
  val tokenType = System.getProperty(ConfigProperties.TokenType, TokenType.User)

  val skipSetup:Boolean = System.getProperty(ConfigProperties.SkipSetup) == "true"
  val createOrg:Boolean = System.getProperty(ConfigProperties.CreateOrg) == "true"
  val createApp:Boolean = System.getProperty(ConfigProperties.CreateApp) != "false"
  val loadEntities:Boolean = System.getProperty(ConfigProperties.LoadEntities) != "false"
  val scenarioType = System.getProperty(ConfigProperties.ScenarioType, ScenarioType.NameRandomInfinite)

  val rampUsers:Int = Integer.getInteger(ConfigProperties.RampUsers, 0).toInt
  val constantUsersPerSec:Int = Integer.getInteger(ConfigProperties.ConstantUsersPerSec, 0).toInt // users to add per second during constant injection
  val constantUsersDuration:Int = Integer.getInteger(ConfigProperties.ConstantUsersDuration, 10).toInt // number of seconds
  val totalUsers:Int = rampUsers + (constantUsersPerSec * constantUsersDuration)
  val userSeed:Int = Integer.getInteger(ConfigProperties.UserSeed,1).toInt
  val appUser = System.getProperty(ConfigProperties.AppUser)
  val appUserPassword = System.getProperty(ConfigProperties.AppUserPassword)
  val appUserBase64 = Base64.getEncoder.encodeToString((appUser + ":" + appUserPassword).getBytes(StandardCharsets.UTF_8))

  val numEntities:Int = Integer.getInteger(ConfigProperties.NumEntities, 5000).toInt
  val numDevices:Int = Integer.getInteger(ConfigProperties.NumDevices, 4000).toInt

  val collectionType = System.getProperty(ConfigProperties.CollectionType, "customentities")
  val baseCollectionUrl = baseAppUrl + "/" + collectionType

  val rampTime:Int = Integer.getInteger(ConfigProperties.RampTime, 0).toInt // in seconds
  val throttle:Int = Integer.getInteger(ConfigProperties.Throttle, 50).toInt // in seconds
  val rpsTarget:Int = Integer.getInteger(ConfigProperties.RpsTarget, 50).toInt // requests per second target
  val rpsRampTime:Int = Integer.getInteger(ConfigProperties.RpsRampTime, 10).toInt // in seconds
  val holdDuration:Int = Integer.getInteger(ConfigProperties.HoldDuration, 300).toInt // in seconds

  // Geolocation settings
  val centerLatitude:Double = 37.442348 // latitude of center point
  val centerLongitude:Double = -122.138268 // longitude of center point
  val userLocationRadius:Double = 32000 // location of requesting user in meters
  val geoSearchRadius:Int = 8000 // search area in meters

  // Push Notification settings
  val pushNotifier = System.getProperty(ConfigProperties.PushNotifier, "loadNotifier")
  val pushProvider = System.getProperty(ConfigProperties.PushProvider, "noop")

  // Large Entity Collection settings
  val entityPrefix = System.getProperty(ConfigProperties.EntityPrefix, "entity")
  val entityType = System.getProperty(ConfigProperties.EntityType, EntityType.Basic) // basic/trivial/?
  val entitySeed = Integer.getInteger(ConfigProperties.EntitySeed, 1).toInt
  val searchLimit = Integer.getInteger(ConfigProperties.SearchLimit, 0).toInt
  val searchQuery = System.getProperty(ConfigProperties.SearchQuery, "")
  val endConditionType = System.getProperty(ConfigProperties.EndConditionType, EndConditionType.MinutesElapsed)
  val endMinutes = Integer.getInteger(ConfigProperties.EndMinutes, 10).toInt
  val endRequestCount = Integer.getInteger(ConfigProperties.EndRequestCount, 10).toInt

  def getUserFeeder():Array[Map[String, String]]= {
    FeederGenerator.generateUserWithGeolocationFeeder(totalUsers, userLocationRadius, centerLatitude, centerLongitude)
  }

  def getInfiniteUserFeeder():Iterator[Map[String, String]]= {
    FeederGenerator.generateUserWithGeolocationFeederInfinite(userSeed, userLocationRadius, centerLatitude, centerLongitude)
  }

  var testStartTime = System.currentTimeMillis()

  def setTestStartTime(): Unit = {
    testStartTime = System.currentTimeMillis()
  }

}
