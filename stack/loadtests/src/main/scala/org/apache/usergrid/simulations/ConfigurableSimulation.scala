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
 * Configurable simulations.
 *
 * Configuration items:
 * skipSetup, createOrg, org, createApp, app, adminUser, adminPassword, baseUrl,
 * numEntities, entityType, entityPrefix, entitySeed, rampUsers, rampTime,
 * constantUsersPerSec, constantUsersDuration, collection, scenarioType
 *
 * getAllByCursor scenario: searchQuery, searchLimit
 */
class ConfigurableSimulation extends Simulation {

  def getScenario(scenarioType: String): ScenarioBuilder = {
    scenarioType match {
      case ScenarioType.LoadEntities => EntityCollectionScenarios.loadEntities
      case ScenarioType.DeleteEntities => EntityCollectionScenarios.deleteEntities
      case ScenarioType.UpdateEntities => EntityCollectionScenarios.updateEntities
      case ScenarioType.GetAllByCursor => EntityCollectionScenarios.getEntityPagesToEnd
      case ScenarioType.NameRandomInfinite => EntityCollectionScenarios.getRandomEntitiesByName
      case ScenarioType.UuidRandomInfinite => EntityCollectionScenarios.getRandomEntitiesByUuid
      case ScenarioType.GetByNameSequential => EntityCollectionScenarios.getEntitiesByNameSequential
      case ScenarioType.DoNothing => EntityCollectionScenarios.doNothing
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
    var stepCount:Int = 0
    if (Settings.rampUsers > 0) stepCount += 1
    if (Settings.constantUsersPerSec > 0) stepCount += 1
    val injectStepList = new mutable.ArraySeq[InjectionStep](stepCount)
    var currentStep = 0
    if (Settings.rampUsers > 0) {
      injectStepList(currentStep) = rampUsers(Settings.rampUsers) over Settings.rampTime
      currentStep += 1
    }
    if (Settings.constantUsersPerSec > 0) {
      injectStepList(currentStep) = constantUsersPerSec(Settings.constantUsersPerSec) during Settings.constantUsersDuration
      currentStep += 1
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

