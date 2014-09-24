package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object Settings {

  // Target settings
  val httpType = System.getProperty("http", "http")
  val subdomain = System.getProperty("subdomain", "api")
  val domain = System.getProperty("domain", "usergrid")
  val domainExtension = System.getProperty("domainExtension", "com")
  val org = System.getProperty("org", "superuser")
  val app = System.getProperty("app", "sandbox")
  val httpConf = http.baseURL(httpType + "://" + subdomain + "." + domain + "." + domainExtension + "/" + org + "/" + app)

  // Simulation settings
  val numUsers:Int = Integer.getInteger("numUsers", 10).toInt
  val numEntities:Int = Integer.getInteger("numEntities", 1000).toInt
  val rampTime:Int = Integer.getInteger("rampTime", 0).toInt // in seconds
  val duration:Int = Integer.getInteger("duration", 300).toInt // in seconds
  val numDevices:Int = Integer.getInteger("numDevices", 2000).toInt

  // Geolocation settings
  val centerLatitude:Double = 37.442348 // latitude of center point
  val centerLongitude:Double = -122.138268 // longitude of center point
  val userLocationRadius:Double = 32000 // location of requesting user in meters
  val geosearchRadius:Int = 8000 // search area in meters

  // Push Notification settings
  val numDevicesPerApp = Integer.getInteger("numDevices", 20).toInt
  val numNotificationsPerDevice = Integer.getInteger("numPushes", 20).toInt
  val pushNotifier = Utils.generateUniqueName("notifier")

  def createRandomPushNotifier:String = {
    return Utils.generateUniqueName("notifier")
  }

}
