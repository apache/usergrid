package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object UserScenarios {

  val getRandomUser = exec(
    http("GET user")
      .get("/users/user" + Utils.generateRandomInt(1, Settings.numEntities))
      .check(status.is(200))
  )

  val postUser = exec(
    http("POST geolocated Users")
      .post("/users")
      .body(StringBody("{\"location\":{\"latitude\":\"${latitude}\",\"longitude\":\"${longitude}\"},\"username\":\"${username}\"," +
      "\"displayName\":\"${displayName}\",\"age\":\"${age}\",\"seen\":\"${seen}\",\"weight\":\"${weight}\"," +
      "\"height\":\"${height}\",\"aboutMe\":\"${aboutMe}\",\"profileId\":\"${profileId}\",\"headline\":\"${headline}\"," +
      "\"showAge\":\"${showAge}\",\"relationshipStatus\":\"${relationshipStatus}\",\"ethnicity\":\"${ethnicity}\",\"password\":\"password\"}"))
      .check(status.is(200))
  )

  val postUser400ok = exec(
    http("POST geolocated Users")
      .post("/users")
      .body(StringBody("{\"location\":{\"latitude\":\"${latitude}\",\"longitude\":\"${longitude}\"},\"username\":\"${username}\"," +
      "\"displayName\":\"${displayName}\",\"age\":\"${age}\",\"seen\":\"${seen}\",\"weight\":\"${weight}\"," +
      "\"height\":\"${height}\",\"aboutMe\":\"${aboutMe}\",\"profileId\":\"${profileId}\",\"headline\":\"${headline}\"," +
      "\"showAge\":\"${showAge}\",\"relationshipStatus\":\"${relationshipStatus}\",\"ethnicity\":\"${ethnicity}\",\"password\":\"password\"}"))
      .check(status.in(200 to 400))
  )

}