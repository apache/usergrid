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

import java.io.{PrintWriter, FileOutputStream}
import javax.xml.bind.DatatypeConverter
import io.gatling.http.Predef._
import io.gatling.http.config.HttpProtocolBuilder
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.enums._
import org.apache.usergrid.helpers.Utils

object Settings {

  def initStrSetting(cfg: String): String = {
    val setting = System.getProperty(cfg)

    if (setting != null) setting else ConfigProperties.getDefault(cfg).toString
  }

  def initBoolSetting(cfg: String): Boolean = {
    val strSetting = System.getProperty(cfg)
    val default:Boolean = ConfigProperties.getDefault(cfg).asInstanceOf[Boolean]

    if (strSetting != null) {
      if (default) // default is true
        strSetting.toLowerCase != "false"
      else // default is false
        strSetting.toLowerCase == "true"
    } else {
      default
    }
  }

  def initIntSetting(cfg: String): Int = {
    val integerSetting:Integer = Integer.getInteger(cfg)

    if (integerSetting != null)
      integerSetting.toInt
    else
      ConfigProperties.getDefault(cfg).asInstanceOf[Int]
  }

  // load configuration settings via property or default
  val org = initStrSetting(ConfigProperties.Org)
  val app = initStrSetting(ConfigProperties.App)
  val adminUser = initStrSetting(ConfigProperties.AdminUser)
  val adminPassword = initStrSetting(ConfigProperties.AdminPassword)

  private val cfgBaseUrl = initStrSetting(ConfigProperties.BaseUrl)
  val baseUrl = if (cfgBaseUrl.takeRight(1) == "/") cfgBaseUrl.dropRight(1) else cfgBaseUrl
  val baseAppUrl:String = baseUrl + "/" + org + "/" + app

  val httpConf: HttpProtocolBuilder = http.baseURL(baseAppUrl)
  val authType = initStrSetting(ConfigProperties.AuthType)
  val tokenType = initStrSetting(ConfigProperties.TokenType)

  val skipSetup:Boolean = initBoolSetting(ConfigProperties.SkipSetup)
  val createOrg:Boolean = !skipSetup && initBoolSetting(ConfigProperties.CreateOrg)
  val createApp:Boolean = !skipSetup && initBoolSetting(ConfigProperties.CreateApp)
  val loadEntities:Boolean = !skipSetup && initBoolSetting(ConfigProperties.LoadEntities)
  val sandboxCollection:Boolean = initBoolSetting(ConfigProperties.SandboxCollection)
  val scenarioType = initStrSetting(ConfigProperties.ScenarioType)

  val rampUsers:Int = initIntSetting(ConfigProperties.RampUsers)
  val constantUsersPerSec:Int = initIntSetting(ConfigProperties.ConstantUsersPerSec) // users to add per second during constant injection
  val constantUsersDuration:Int = initIntSetting(ConfigProperties.ConstantUsersDuration) // number of seconds
  val totalUsers:Int = rampUsers + (constantUsersPerSec * constantUsersDuration)
  val userSeed:Int = initIntSetting(ConfigProperties.UserSeed)
  val appUser = initStrSetting(ConfigProperties.AppUser)
  val appUserPassword = initStrSetting(ConfigProperties.AppUserPassword)

  // val appUserBase64 = Base64.getEncoder.encodeToString((appUser + ":" + appUserPassword).getBytes(StandardCharsets.UTF_8))
  val appUserBase64: String = DatatypeConverter.printBase64Binary((appUser + ":" + appUserPassword).getBytes("UTF-8"))

  val totalNumEntities:Int = initIntSetting(ConfigProperties.NumEntities)
  val numDevices:Int = initIntSetting(ConfigProperties.NumDevices)

  val collection = initStrSetting(ConfigProperties.Collection)
  val baseCollectionUrl = baseAppUrl + "/" + collection

  val rampTime:Int = initIntSetting(ConfigProperties.RampTime) // in seconds
  val throttle:Int = initIntSetting(ConfigProperties.Throttle) // in seconds
  val rpsTarget:Int = initIntSetting(ConfigProperties.RpsTarget) // requests per second target
  val rpsRampTime:Int = initIntSetting(ConfigProperties.RpsRampTime) // in seconds
  val holdDuration:Int = initIntSetting(ConfigProperties.HoldDuration) // in seconds

  // Geolocation settings
  val centerLatitude:Double = 37.442348 // latitude of center point
  val centerLongitude:Double = -122.138268 // longitude of center point
  val userLocationRadius:Double = 32000 // location of requesting user in meters
  val geoSearchRadius:Int = 8000 // search area in meters

  // Push Notification settings
  val pushNotifier = initStrSetting(ConfigProperties.PushNotifier)
  val pushProvider = initStrSetting(ConfigProperties.PushProvider)

  // Large Entity Collection settings
  val entityPrefix = initStrSetting(ConfigProperties.EntityPrefix)
  val entityType = initStrSetting(ConfigProperties.EntityType) // basic/trivial/?
  val overallEntitySeed = initIntSetting(ConfigProperties.EntitySeed)
  val searchLimit:Int = initIntSetting(ConfigProperties.SearchLimit)
  val searchQuery = initStrSetting(ConfigProperties.SearchQuery)
  val endConditionType = initStrSetting(ConfigProperties.EndConditionType)
  val endMinutes:Int = initIntSetting(ConfigProperties.EndMinutes)
  val endRequestCount:Int = initIntSetting(ConfigProperties.EndRequestCount)

  // Org creation fields
  private val cfgOrgCreationUsername = initStrSetting(ConfigProperties.OrgCreationUsername)
  private val cfgOrgCreationEmail = initStrSetting(ConfigProperties.OrgCreationEmail)
  private val cfgOrgCreationName = initStrSetting(ConfigProperties.OrgCreationName)
  val orgCreationUsername = if (cfgOrgCreationUsername == "") org.concat("_admin") else cfgOrgCreationUsername
  val orgCreationEmail = if (cfgOrgCreationEmail == "") orgCreationUsername.concat("@usergrid.com") else cfgOrgCreationEmail
  val orgCreationName = if (cfgOrgCreationName == "") orgCreationUsername else cfgOrgCreationName
  val orgCreationPassword = initStrSetting(ConfigProperties.OrgCreationPassword)

  // Entity update
  val updateProperty = initStrSetting(ConfigProperties.UpdateProperty)
  val updateValue = initStrSetting(ConfigProperties.UpdateValue)
  val updateBody = Utils.toJSONStr(Map(updateProperty -> updateValue))

  // Entity workers
  private val cfgEntityWorkerCount:Int = initIntSetting(ConfigProperties.EntityWorkerCount)
  private val cfgEntityWorkerNum:Int = initIntSetting(ConfigProperties.EntityWorkerNum)
  val useWorkers:Boolean = cfgEntityWorkerCount > 1 && cfgEntityWorkerNum >= 1 && cfgEntityWorkerNum <= cfgEntityWorkerCount
  val entityWorkerCount:Int = if (useWorkers) cfgEntityWorkerCount else 1
  val entityWorkerNum:Int = if (useWorkers) cfgEntityWorkerNum else 1

  // if only one worker system, these numbers will still be fine
  private val entitiesPerWorkerFloor:Int = totalNumEntities / entityWorkerCount
  private val leftOver:Int = totalNumEntities % entityWorkerCount  // will be 0 if only one worker
  private val extraEntity:Int = if (entityWorkerNum <= leftOver) 1 else 0
  private val zeroBasedWorkerNum:Int = entityWorkerNum - 1
  val entitySeed:Int = overallEntitySeed + zeroBasedWorkerNum * entitiesPerWorkerFloor + (if (extraEntity == 1) zeroBasedWorkerNum else leftOver)
  val numEntities:Int = entitiesPerWorkerFloor + extraEntity

  // UUID log file, have to go through this because creating a csv feeder with an invalid csv file fails at maven compile time
  private val dummyCsv = ConfigProperties.getDefault(ConfigProperties.UuidFilename).toString
  private val uuidFilename = initStrSetting(ConfigProperties.UuidFilename)
  val captureUuids = uuidFilename != dummyCsv && (scenarioType == ScenarioType.LoadEntities || scenarioType == ScenarioType.GetByNameSequential)
  val feedUuids = uuidFilename != dummyCsv && scenarioType == ScenarioType.UuidRandomInfinite
  val captureUuidFilename = if (captureUuids) uuidFilename else dummyCsv
  val feedUuidFilename = if (feedUuids) uuidFilename else dummyCsv
  val purgeUsers:Int = initIntSetting(ConfigProperties.PurgeUsers)

  private var uuidMap: Map[Int, String] = Map()
  def addUuid(num: Int, uuid: String): Unit = {
    if (captureUuids) uuidMap += (num -> uuid)
    // println(s"UUID: ${name},${uuid}")
  }

  def writeUuidsToFile(): Unit = {
    if (captureUuids) {
      val writer = {
        val fos = new FileOutputStream(captureUuidFilename)
        new PrintWriter(fos, false)
      }
      writer.println("name,uuid")
      val uuidList: List[(Int, String)] = uuidMap.toList.sortBy(l => l._1)
      uuidList.foreach { l =>
        writer.println(s"${Settings.entityPrefix}${l._1},${l._2}")
      }
      writer.flush()
      writer.close()
    }
  }

  def getUserFeeder:Array[Map[String, String]]= {
    FeederGenerator.generateUserWithGeolocationFeeder(totalUsers, userLocationRadius, centerLatitude, centerLongitude)
  }

  def getInfiniteUserFeeder:Iterator[Map[String, String]]= {
    FeederGenerator.generateUserWithGeolocationFeederInfinite(userSeed, userLocationRadius, centerLatitude, centerLongitude)
  }

  private var testStartTime: Long = System.currentTimeMillis()

  def getTestStartTime: Long = {
    testStartTime
  }

  def setTestStartTime(): Unit = {
    testStartTime = System.currentTimeMillis()
  }

  def continueMinutesTest: Boolean = {
    (System.currentTimeMillis() - testStartTime) < (endMinutes.toLong*60L*1000L)
  }

  def printSettingsSummary(): Unit = {
    val authTypeStr = authType + (if (authType == AuthType.Token) s"($tokenType)" else "")
    val endConditionStr = if (endConditionType == EndConditionType.MinutesElapsed) s"$endMinutes minutes elapsed" else s"$endRequestCount requests"
    println("-----------------------------------------------------------------------------")
    println("SIMULATION SETTINGS")
    println("-----------------------------------------------------------------------------")
    println()
    println(s"Org:$org  App:$app  Collection:$collection")
    println(s"CreateOrg:$createOrg  CreateApp:$createApp  LoadEntities:$loadEntities")
    println(s"ScenarioType:$scenarioType  AuthType:$authTypeStr")
    println()
    println(s"Entity Type:$entityType  Prefix:$entityPrefix")
    println()
    println(s"Overall: NumEntities:$totalNumEntities  Seed:$overallEntitySeed  Workers:$entityWorkerCount")
    println(s"Worker:  NumEntities:$numEntities  Seed:$entitySeed  WorkerNum:$entityWorkerNum")
    println()
    println(s"Ramp: Users:$rampUsers  Time:$rampTime")
    println(s"Constant: UsersPerSec:$constantUsersPerSec  Time:$constantUsersDuration")
    println(s"End Condition:$endConditionStr")
    println()
    println("-----------------------------------------------------------------------------")
  }

  printSettingsSummary()

}
