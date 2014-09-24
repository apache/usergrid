package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PostUsersSimulation extends Simulation {

  // Target settings
  val httpConf = Settings.httpConf

  // Simulation settings
  val numUsers:Int = Settings.numUsers
  val rampTime:Int = Settings.rampTime
  val duration:Int = Settings.duration

  // Geolocation settings
  val centerLatitude:Double = Settings.centerLatitude
  val centerLongitude:Double = Settings.centerLongitude
  val userLocationRadius:Double = Settings.userLocationRadius
  val geosearchRadius:Int = Settings.geosearchRadius

  val feeder = FeederGenerator.generateUserWithGeolocationFeeder(numUsers, userLocationRadius, centerLatitude, centerLongitude).queue

  val scnToRun = scenario("POST geolocated users")
    .feed(feeder)
    .exec(UserScenarios.postUser)

  setUp(scnToRun.inject(rampUsers(numUsers) over (rampTime.seconds)).protocols(httpConf))

}