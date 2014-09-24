package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
 * Performs organization registration
 *
 *
 * Expects:
 *
 * authToken The auth token to use when creating the application
 * orgName The organization name
 *
 * Produces:
 *
 * appName The name of the created application
 */
object ApplicationScenarios {

  val createApplication = exec(http("Create Application")
    .post("/management/organizations/${org}/applications")
    .headers(Headers.jsonAuthorized)
    .body(StringBody("{\"name\":\"" + Settings.app + "\"}"))
    .check(status.is(200))

    )

}
