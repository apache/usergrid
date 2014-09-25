package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object GeoScenarios {

  val getGeolocation = exec(
      http("GET geolocated user")
        .get("/users?ql=location%20within%20" + Settings.geosearchRadius + "%20of%20${latitude},${longitude}")
        .check(status.is(200))
    )

  val getGeolocationWithQuery = exec(
      http("GET geolocated user with query")
        .get("/users?ql=${queryParams}%20AND%20location%20within%20" + Settings.geosearchRadius + "%20of%20${latitude},${longitude}")
        .check(status.is(200))
    )

  val updateGeolocation = exec(
    http("PUT user location")
      .put("/users/user" + Utils.generateRandomInt(1, Settings.numUsers))
      .body(StringBody("{\"location\":{\"latitude\":\"${latitude}\",\"longitude\":\"${longitude}\"}}"))
      .check(status.is(200))
  )

}