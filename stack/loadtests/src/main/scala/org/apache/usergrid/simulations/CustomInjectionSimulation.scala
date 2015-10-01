/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.simulations

import io.gatling.core.Predef._
import io.gatling.core.controller.inject.InjectionStep
import io.gatling.core.structure.ScenarioBuilder
import org.apache.usergrid.enums.ScenarioType
import org.apache.usergrid.helpers.Setup
import org.apache.usergrid.scenarios.EntityCollectionScenarios
import org.apache.usergrid.settings.Settings

import scala.collection.mutable

/**
 * Simulations with custom injection.
 */
class CustomInjectionSimulation extends Simulation {

  def getScenario(scenarioType: String): ScenarioBuilder = {
    scenarioType match {
      case ScenarioType.LoadEntities => EntityCollectionScenarios.loadEntities
      case ScenarioType.DeleteEntities => EntityCollectionScenarios.deleteEntities
      case ScenarioType.UpdateEntities => EntityCollectionScenarios.updateEntities
      case ScenarioType.GetAllByCursor => EntityCollectionScenarios.getEntityPagesToEnd
      case ScenarioType.NameRandomInfinite => EntityCollectionScenarios.getRandomEntitiesByName
      case ScenarioType.UuidRandomInfinite => EntityCollectionScenarios.getRandomEntitiesByUuid
      case ScenarioType.GetByNameSequential => EntityCollectionScenarios.getEntitiesByNameSequential
      case _ => null
    }
  }

  before{
    if (!Settings.skipSetup) {
      println("Begin setup")
      if (Settings.createOrg) Setup.setupOrg()
      if (Settings.createApp) Setup.setupApplication()
      if (Settings.loadEntities) Setup.setupEntitiesCollection(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed)
    } else {
      println("Skipping setup")
    }
    if (Settings.sandboxCollection) Setup.sandboxCollection()
    Settings.setTestStartTime()
  }

  if (ScenarioType.isValid(Settings.scenarioType)) {
    val scenario: ScenarioBuilder = getScenario(Settings.scenarioType)

    val injectionList:String = Settings.injectionList
    val injectStepsArray:Array[String] = injectionList.split("\\s*;\\s*")
    val injectStepList:mutable.ArraySeq[InjectionStep] = new mutable.ArraySeq[InjectionStep](injectStepsArray.length)
    for (i <- injectStepsArray.indices) {
      val injectionStep = injectStepsArray(i).trim
      println(injectionStep)
      val stepRegex = """(.+)\((.*)\)""".r
      val stepRegex(stepType,stepArgsStr) = injectionStep
      println(s"stepType:$stepType stepArgs:$stepArgsStr")
      val stepArgs = stepArgsStr.split("\\s*,\\s*")
      injectStepList(i) = stepType match {
        case "rampUsers" => rampUsers(stepArgs(0).toInt) over stepArgs(1).toInt
        case "constantUsersPerSec" => constantUsersPerSec(stepArgs(0).toDouble) during stepArgs(1).toInt
        case "constantUsersPerSecRandomized" => constantUsersPerSec(stepArgs(0).toDouble) during stepArgs(1).toInt randomized
        case "atOnceUsers" => atOnceUsers(stepArgs(0).toInt)
        case "rampUsersPerSec" => rampUsersPerSec(stepArgs(0).toDouble) to stepArgs(1).toInt during stepArgs(2).toInt
        case "rampUsersPerSecRandomized" => rampUsersPerSec(stepArgs(0).toDouble) to stepArgs(1).toInt during stepArgs(2).toInt randomized
        case "heavisideUsers" => heavisideUsers(stepArgs(0).toInt) over stepArgs(1).toInt
        case "nothingFor" => nothingFor(stepArgs(0).toInt)
      }
    }

    setUp(
      scenario
        .inject(injectStepList)
          .protocols(Settings.httpAppConf.connection("keep-alive").acceptHeader("application/json"))
    )
  } else {
    println(s"scenarioType ${Settings.scenarioType} not found.")
  }

  after {
    endHandler
  }

  def endHandler: Unit = {
    Settings.setTestEndTime()
    if (Settings.captureUuids) Settings.writeUuidsToFile()
    Settings.printSettingsSummary(true)
  }

  sys addShutdownHook(endHandler)

}

