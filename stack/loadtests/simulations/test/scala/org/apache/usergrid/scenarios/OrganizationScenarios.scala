package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Performs organization registration
 *
 *
 * Produces:
 *
 * orgName The name of the created organization
 * userName  The user name of the admin to log in with
 * password The password of the admin to use
 */
object OrganizationScenarios {

  //register the org with the randomly generated org
  val createOrgAndAdmin = exec(http("Create Organization")
  .post("/management/organizations")
  .headers(Headers.jsonAnonymous)
  .body(StringBody("{\"organization\":\"" + Settings.org + "\",\"username\":\"${username}\",\"name\":\"${username}\",\"email\":\"${username}@apigee.com\",\"password\":\"${password}\"}"))
  .check(status.is(200)))

}