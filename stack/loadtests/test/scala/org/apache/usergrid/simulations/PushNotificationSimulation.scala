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

  val numDevices = 2000
  val numNotificationsPerDevice = 1
  val numUsers = 150
  val rampTime = 0

  val httpConf = Settings.httpConf
    .acceptHeader("application/json")

  val createNotifier = NotifierScenarios.createNotifier
  val createDevice = DeviceScenarios.postDeviceWithNotifier
  val sendNotification = NotificationScenarios.sendNotification

  val deviceNameFeeder = FeederGenerator.generateEntityNameFeeder("device", numDevices).queue.circular

  val scnToRun = scenario("Create Push Notification")
    .exec(createNotifier)
    .repeat(numDevices/numUsers) {
      feed(deviceNameFeeder)
      .exec(createDevice)
    }
    .during(200 seconds) {
      feed(deviceNameFeeder)
      .exec(sendNotification)
    }


  setUp(scnToRun.inject(rampUsers(numUsers) over (rampTime.seconds)).protocols(httpConf));

}
