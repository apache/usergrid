package org.apache.usergrid.enums

/**
 * Created by mdunker on 7/20/15.
 */
object ConfigProperties {
  val Org = "org"
  val App = "app"
  val AdminUser = "adminUser"
  val AdminPassword = "adminPassword"
  val BaseUrl = "baseurl"
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
  val CollectionType = "collectionType"
  val RampTime = "rampTime"
  val Throttle = "throttle"
  val RpsTarget = "rpsTarget"
  val RpsRampTime = "rpsRampTime"
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

  val Values = Seq(Org,App,AdminUser,AdminPassword,BaseUrl,AuthType,TokenType,SkipSetup,CreateOrg,CreateApp,LoadEntities,
    ScenarioType,RampUsers,ConstantUsersPerSec,ConstantUsersDuration,UserSeed,AppUser,AppUserPassword,NumEntities,
    NumDevices,CollectionType,RampTime,Throttle,RpsTarget,RpsRampTime,HoldDuration,PushNotifier,EntityPrefix,EntityType,
    EntitySeed,SearchLimit,SearchQuery,EndConditionType,EndMinutes,EndRequestCount,OrgCreationUsername,OrgCreationName,
    OrgCreationEmail,OrgCreationPassword,UpdateProperty,UpdateValue)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
