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
package org.apache.usergrid.enums

import java.util.Date

/**
 * Created by mdunker on 7/20/15.
 */
object ConfigProperties {
  val Org = "org"
  val App = "app"
  val AdminUser = "adminUser"
  val AdminPassword = "adminPassword"
  val BaseUrl = "baseUrl"
  val AuthType = "authType"
  val TokenType = "tokenType"
  val SkipSetup = "skipSetup"
  val CreateOrg = "createOrg"
  val CreateApp = "createApp"
  val LoadEntities = "loadEntities"
  val ScenarioType = "scenarioType"
  val RampUsers = "rampUsers"
  val ConstantUsersPerSec = "constantUsersPerSec"
  val ConstantUsersDuration = "constantUsersDuration"
  val UserSeed = "userSeed"
  val AppUser = "appUser"
  val AppUserPassword = "appUserPassword"
  val NumEntities = "numEntities"
  val NumDevices = "numDevices"
  val Collection = "collection"
  val RampTime = "rampTime"
  val Throttle = "throttle"
  val HoldDuration = "holdDuration"
  val PushNotifier = "pushNotifier"
  val PushProvider = "pushProvider"
  val EntityPrefix = "entityPrefix"
  val EntityType = "entityType"
  val EntitySeed = "entitySeed"
  val SearchLimit = "searchLimit"
  val SearchQuery = "searchQuery"
  val EndConditionType = "endConditionType"
  val EndMinutes = "endMinutes"
  val EndRequestCount = "endRequestCount"
  val OrgCreationUsername = "orgCreationUsername"
  val OrgCreationName = "orgCreationName"
  val OrgCreationEmail = "orgCreationEmail"
  val OrgCreationPassword = "orgCreationPassword"
  val UpdateProperty = "updateProperty"
  val UpdateValue = "updateValue"
  val EntityWorkerCount = "entityWorkerCount"
  val EntityWorkerNum = "entityWorkerNum"
  val UuidFilename = "uuidFilename"
  val AuditUuidFilename = "auditUuidFilename"
  val FailedUuidFilename = "failedUuidFilename"
  val SandboxCollection = "sandboxCollection"
  val PurgeUsers = "purgeUsers"
  val RetryCount = "retryCount"
  val LaterThanTimestamp = "laterThanTimestamp"
  val EntityProgressCount = "entityProgressCount"
  val InjectionList = "injectionList"
  val PrintFailedRequests = "printFailedRequests"
  val GetViaQuery = "getViaQuery"
  val MultiPropertyPrefix = "multiPropertyPrefix"
  val MultiPropertyCount = "multiPropertyCount"
  val MultiPropertySizeInK = "multiPropertySizeInK"
  val EntityNumberProperty = "entityNumberProperty"
  val QueryParams = "queryParams"
  val CsvFeedPattern = "csvFeedPattern"
  val UnlimitedFeed = "unlimitedFeed"
  val FlushCsv = "flushCsv"
  val InterleavedWorkerFeed = "interleavedWorkerFeed"
  val NewCsvOnFlush = "newCsvOnFlush"
  val DeleteAfterSuccessfulAudit = "deleteAfterSuccessfulAudit"
  val UsergridRegion = "usergridRegion";

  val Values = Seq(Org,App,AdminUser,AdminPassword,BaseUrl,AuthType,TokenType,SkipSetup,CreateOrg,CreateApp,LoadEntities,
    ScenarioType,RampUsers,ConstantUsersPerSec,ConstantUsersDuration,UserSeed,AppUser,AppUserPassword,NumEntities,
    NumDevices,Collection,RampTime,Throttle,HoldDuration,PushNotifier,PushProvider,EntityPrefix,
    EntityType,EntitySeed,SearchLimit,SearchQuery,EndConditionType,EndMinutes,EndRequestCount,OrgCreationUsername,
    OrgCreationName,OrgCreationEmail,OrgCreationPassword,UpdateProperty,UpdateValue,EntityWorkerCount,EntityWorkerNum,
    UuidFilename,AuditUuidFilename,FailedUuidFilename,SandboxCollection,PurgeUsers,RetryCount,LaterThanTimestamp,
    EntityProgressCount,InjectionList,PrintFailedRequests,GetViaQuery,MultiPropertyPrefix,MultiPropertyCount,
    MultiPropertySizeInK,EntityNumberProperty,QueryParams,CsvFeedPattern,UnlimitedFeed,FlushCsv,InterleavedWorkerFeed,
    NewCsvOnFlush,DeleteAfterSuccessfulAudit,UsergridRegion)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }

  // defaults for all the configuration items
  def getDefault(cfg: String): Any = {
    if (isValid(cfg)) {
      cfg match {
        case Org => ""
        case App => ""
        case AdminUser => ""
        case AdminPassword => ""
        case BaseUrl => ""
        case AuthType => org.apache.usergrid.enums.AuthType.Anonymous
        case TokenType => org.apache.usergrid.enums.TokenType.User
        case SkipSetup => false
        case CreateOrg => false
        case CreateApp => false
        case LoadEntities => false
        case ScenarioType => org.apache.usergrid.enums.ScenarioType.NameRandomInfinite
        case RampUsers => 0
        case ConstantUsersPerSec => 0
        case ConstantUsersDuration => 10
        case UserSeed => 1
        case AppUser => ""
        case AppUserPassword => ""
        case NumEntities => 5000
        case NumDevices => 4000
        case Collection => "customentities"
        case RampTime => 0
        case Throttle => 50
        case HoldDuration => 300
        case PushNotifier => "loadNotifier"
        case PushProvider => "noop"
        case EntityPrefix => "entity"
        case EntityType => org.apache.usergrid.enums.EntityType.Basic
        case EntitySeed => 1
        case SearchLimit => 0
        case SearchQuery => ""
        case EndConditionType => org.apache.usergrid.enums.EndConditionType.MinutesElapsed
        case EndMinutes => 10
        case EndRequestCount => 1000
        case OrgCreationUsername => ""     // actual default is {org}_admin
        case OrgCreationName => ""         // actual default is {org}_admin
        case OrgCreationEmail => ""        // actual default is {org}_admin@usergrid.com
        case OrgCreationPassword => "test"
        case UpdateProperty => "updateProp"
        case UpdateValue => new Date().toString
        case EntityWorkerCount => 0
        case EntityWorkerNum => 0
        case UuidFilename => "dummyUuid.csv"
        case AuditUuidFilename => "dummyAuditUuid.csv"
        case FailedUuidFilename => "/tmp/dummyFailedUuid.csv"
        case SandboxCollection => false
        case PurgeUsers => 100
        case RetryCount => 5
        case LaterThanTimestamp => 0L
        case EntityProgressCount => 10000L
        case InjectionList => "rampUsers(10,60)"
        case PrintFailedRequests => true
        case GetViaQuery => false
        case MultiPropertyPrefix => "prop"
        case MultiPropertyCount => 1
        case MultiPropertySizeInK => 1
        case EntityNumberProperty => ""
        case QueryParams => ""
        case CsvFeedPattern => org.apache.usergrid.enums.CsvFeedPatternType.Random
        case UnlimitedFeed => false
        case FlushCsv => 0L
        case InterleavedWorkerFeed => false
        case NewCsvOnFlush => false
        case DeleteAfterSuccessfulAudit => false
        case UsergridRegion => ""
      }
    } else {
      null
    }
  }
}
