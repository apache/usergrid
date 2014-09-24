package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Class that will get the token and insert it into the test session.
 * Assumes that  the following values are present in the session.
 *
 * Expects:
 *
 * userName  The user name to log in with
 * password The password to use
 *
 * Produces:
 *
 * authToken A valid access token if the login attempt is successful
 */

object TokenScenarios {


  val getManagementToken =
    exec(
      http("POST Org Token")
        .post("/management/token")
        .headers(Headers.jsonAnonymous)
        //pass in the the username and password, store the "access_token" json response element as the var "authToken" in the session
        .body(StringBody("{\"username\":\"${username}\",\"password\":\"${password}\",\"grant_type\":\"password\"}"))
        .check(jsonPath("access_token")
        .saveAs("authToken"))
    )

  val getUserToken =
    exec(
      http("POST user token")
        .post("/token")
        .body(StringBody("{\"grant_type\":\"password\",\"username\":\"${user1}\",\"password\":\"password\"}"))
        .check(status.is(200))
    )

}