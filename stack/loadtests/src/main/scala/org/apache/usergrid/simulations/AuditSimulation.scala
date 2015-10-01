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
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.config.HttpProtocolBuilder
import org.apache.usergrid.enums.ScenarioType
import org.apache.usergrid.helpers.Setup
import org.apache.usergrid.scenarios.{AuditScenarios, EntityCollectionScenarios}
import org.apache.usergrid.settings.Settings

/**
 * Audit simulations.
 *
 */
class AuditSimulation extends Simulation {

  def getScenario(scenarioType: String): ScenarioBuilder = {
    scenarioType match {
      case ScenarioType.AuditGetCollectionEntities => AuditScenarios.getAllCollections
      case ScenarioType.AuditVerifyCollectionEntities => AuditScenarios.verifyCollections
    }
  }

  before{
    Settings.setTestStartTime()
  }

  if (ScenarioType.isValid(Settings.scenarioType)) {
    val scenario: ScenarioBuilder = getScenario(Settings.scenarioType)
    val httpConf: HttpProtocolBuilder = Settings.httpOrgConf
      .acceptHeader("application/json")

    setUp(
      scenario
        .inject(
          rampUsers(Settings.rampUsers) over Settings.rampTime
        ).protocols(httpConf)
    )
  } else {
    println(s"Audit scenario type ${Settings.scenarioType} not found.")
  }

  after {
    Settings.setTestEndTime()
    if (Settings.captureAuditUuids) {
      val uuidDesc = Settings.scenarioType match {
        case ScenarioType.AuditGetCollectionEntities => "found"
        case ScenarioType.AuditVerifyCollectionEntities => "failed"
      }
      Settings.writeAuditUuidsToFile(uuidDesc)
    }
    Settings.printSettingsSummary(true)
    Settings.printAuditResults()
  }

}

