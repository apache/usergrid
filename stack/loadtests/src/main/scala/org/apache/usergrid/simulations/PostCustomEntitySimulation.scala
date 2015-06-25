/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.simulations


import io.gatling.core.scenario.Simulation
import org.apache.usergrid.helpers.Setup
import org.apache.usergrid.scenarios.{EntityScenarios, TokenScenarios}
import java.nio.file.{Paths, Files}
import io.gatling.core.Predef._
import org.apache.usergrid.datagenerators.FeederGenerator
import scala.concurrent.duration._
import org.apache.usergrid.settings.{Utils, Headers, Settings}

/**
 * PostCustomEntitySimulation - creates lots of custom entities
 * 
 * Run this way:
 * mvn gatling:execute -DrampTime=10 -DmaxPossibleUsers=10 -Dduration=120 -Dorg=yourorgname -Dapp=sandbox -Dbaseurl=https://api.usergrid.com -DadminUser=yourusername -DadminPassword='yourpassword' -Dgatling.simulationClass=org.apache.usergrid.simulations.PostCustomEntitySimulation -DcollectionType=yourcollection
 * 
 *
 */
class PostCustomEntitySimulation extends Simulation {

  if(!Settings.skipSetup) {
    println("Begin setup")
    println("These aren't the droids you are looking for...")
    //exec(TokenScenarios.getManagementToken)
    println("End Setup")
  }else{
    println("Skipping Setup")
  }

  val numEntities:Int = Settings.numEntities
  val collectionType = Settings.collectionType
  println("collection type = " + collectionType)
  val rampTime:Int = Settings.rampTime
  val throttle:Int = Settings.throttle
  val feeder = FeederGenerator.generateCustomEntityInfinite(0)
  val httpConf = Settings.httpConf

  val scnToRun = scenario("POST custom entities")
    .feed(feeder)
    .exec(EntityScenarios.postEntity)
  
  /*
  val scnToRun = scenario("POST custom entities")
    .feed(feeder)
    .doIfOrElse(session => session("token").as[String].nonEmpty(session)) {
      exec(EntityScenarios.postEntityWithToken)
    } {
      exec(EntityScenarios.postEntity)
    }
*/


  setUp(scnToRun.inject(
    rampUsers(Settings.maxPossibleUsers) over Settings.rampTime,
    constantUsersPerSec(Settings.maxPossibleUsers) during Settings.duration
  ).protocols(httpConf)).maxDuration(Settings.duration)

}
