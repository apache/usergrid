package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class GetEntitySimulation extends Simulation {

  // Target settings
  val httpConf = Settings.httpConf

  // Simulation settings
  val numUsers:Int = Settings.numUsers
  val numEntities:Int = Settings.numEntities
  val rampTime:Int = Settings.rampTime
  val throttle:Int = Settings.throttle

  val feeder = FeederGenerator.generateEntityNameFeeder("user", numEntities).circular

  val scnToRun = scenario("GET entity")
    .exec(UserScenarios.getRandomUser)

  setUp(scnToRun.inject(atOnceUsers(numUsers)).throttle(reachRps(throttle) in (rampTime.seconds)).protocols(httpConf)).maxDuration(Settings.duration)

}