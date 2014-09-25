package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 *
 * Simple test for setting up multiple orgs and creating push notifications
 *
 */
class PushNotificationSimulation extends Simulation {

  val numUsers:Int = Settings.numUsers
  val numEntities:Int = Settings.numEntities
  val rampTime:Int = Settings.rampTime
  val throttle:Int = Settings.throttle

  val httpConf = Settings.httpConf
    .acceptHeader("application/json")

  val createNotifier = NotifierScenarios.createNotifier
  val createDevice = DeviceScenarios.postDeviceWithNotifier
  val sendNotification = NotificationScenarios.sendNotification

  val deviceNameFeeder = FeederGenerator.generateEntityNameFeeder("device", numEntities).circular

  val scnToRun = scenario("Create Push Notification")    
    .during(duration) {
      feed(deviceNameFeeder)
      .exec(sendNotification)
    }


  setUp(scnToRun.inject(atOnceUsers(numUsers)).throttle(reachRps(throttle) in (rampTime.seconds)).protocols(httpConf))

}
