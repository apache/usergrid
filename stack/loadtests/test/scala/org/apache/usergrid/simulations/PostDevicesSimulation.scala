package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PostDevicesSimulation extends Simulation {

  // Target settings
  val httpConf = Settings.httpConf

  // Simulation settings
  val numDevices:Int = Settings.numUsers
  val rampTime:Int = Settings.rampTime

  val feeder = FeederGenerator.generateEntityNameFeeder("device", numDevices)

  val scnToRun = scenario("POST device")
    .feed(feeder)
    .exec(DeviceScenarios.postDeviceWithNotifier)

  setUp(scnToRun.inject(rampUsers(numDevices) over (rampTime.seconds)).protocols(httpConf))

}