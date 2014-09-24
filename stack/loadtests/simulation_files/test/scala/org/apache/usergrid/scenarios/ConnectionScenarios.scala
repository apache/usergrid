package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object ConnectionScenarios {

  val postConnection = exec(
    http("POST connection")
      .post("/users/${user1}/likes/users/${user2}")
      .check(status.is(200))
  )

}