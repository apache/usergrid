package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class GetEntitySimulation extends Simulation {

  // Target settings
  val httpConf = Settings.httpConf

  // Simulation settings
  val numUsers:Int = Settings.numUsers
  val rampTime:Int = Settings.rampTime

  val feeder = FeederGenerator.generateEntityNameFeeder("entity", 20000).circular

  val scnToRun = scenario("GET entity")
    .exec(UserScenarios.getRandomUser)

  setUp(scnToRun.inject(atOnceUsers(numUsers)).throttle(reachRps(5000) in (rampTime.seconds)).protocols(httpConf))

}