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
import org.apache.usergrid.enums.ScenarioType
import org.apache.usergrid.helpers.Setup
import org.apache.usergrid.scenarios.EntityCollectionScenarios
import org.apache.usergrid.settings.Settings

/**
 * Configurable simulations.
 *
 * Configuration items:
 * skipSetup, createOrg, org, createApp, app, adminUser, adminPassword, baseUrl,
 * numEntities, entityType, entityPrefix, entitySeed, rampUsers, rampTime,
 * constantUsersPerSec, constantUsersDuration, collectionType, scenarioType
 *
 * getAllByCursor scenario: searchQuery, searchLimit
 */
class ConfigurableSimulation extends Simulation {
  before(
    if (!Settings.skipSetup) {
      println("Begin setup")
      if (Settings.createOrg) Setup.setupOrg()
      if (Settings.createApp) Setup.setupApplication()
      if (Settings.loadEntities) Setup.setupEntitiesCollection(Settings.numEntities, Settings.entityType, Settings.entityPrefix, Settings.entitySeed)
    } else {
      println("Skipping setup")
    }
  )



  Settings.setTestStartTime()
  if (Settings.scenarioType == ScenarioType.LoadEntities) {
    setUp(
      EntityCollectionScenarios.loadEntities
        .inject(
          rampUsers(Settings.rampUsers) over Settings.rampTime,
          constantUsersPerSec(Settings.constantUsersPerSec) during Settings.constantUsersDuration

        ).protocols(Settings.httpConf.acceptHeader("application/json"))
    )
  } else if (Settings.scenarioType == ScenarioType.DeleteEntities) {
    setUp(
      EntityCollectionScenarios.deleteEntities
        .inject(
          rampUsers(Settings.rampUsers) over Settings.rampTime,
          constantUsersPerSec(Settings.constantUsersPerSec) during Settings.constantUsersDuration

        ).protocols(Settings.httpConf.acceptHeader("application/json"))
    )
  } else if (Settings.scenarioType == ScenarioType.GetAllByCursor) {
    setUp(
      EntityCollectionScenarios.getEntityPagesToEnd
        .inject(
          rampUsers(Settings.rampUsers) over Settings.rampTime,
          constantUsersPerSec(Settings.constantUsersPerSec) during Settings.constantUsersDuration

        ).protocols(Settings.httpConf.acceptHeader("application/json"))
    )
  } else if (Settings.scenarioType == ScenarioType.NameRandomInfinite) {
    setUp(
      EntityCollectionScenarios.getRandomEntitiesByName
        .inject(
          rampUsers(Settings.rampUsers) over Settings.rampTime,
          constantUsersPerSec(Settings.constantUsersPerSec) during Settings.constantUsersDuration

        ).protocols(Settings.httpConf.acceptHeader("application/json"))
    )
  } else {
    println(s"scenarioType ${Settings.scenarioType} not found.")
  }

}

