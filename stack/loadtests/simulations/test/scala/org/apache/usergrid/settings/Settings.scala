package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object Settings {

  // Target settings
  val org = System.getProperty("org")
  val app = System.getProperty("app")
  val baseUrl = System.getProperty("baseurl")
  val httpConf = http.baseURL(baseUrl + "/" + org + "/" + app)

  // Simulation settings
  val numUsers:Int = Integer.getInteger("numUsers", 10).toInt
  val numEntities:Int = Integer.getInteger("numEntities", 5000).toInt
  val numDevices:Int = Integer.getInteger("numDevices", 2000).toInt

  val rampTime:Int = Integer.getInteger("rampTime", 0).toInt // in seconds
  val duration:Int = Integer.getInteger("duration", 300).toInt // in seconds
  val rps:Int = Integer.getInteger("throttle", 50).toInt // in seconds

  // Geolocation settings
  val centerLatitude:Double = 37.442348 // latitude of center point
  val centerLongitude:Double = -122.138268 // longitude of center point
  val userLocationRadius:Double = 32000 // location of requesting user in meters
  val geosearchRadius:Int = 8000 // search area in meters

  // Push Notification settings
  val pushNotifier = System.getProperty("notifier")
  val pushProvider = System.getProperty("provider")

  def createRandomPushNotifier:String = {
    return Utils.generateUniqueName("notifier")
  }

}
