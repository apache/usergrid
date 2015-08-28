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

package org.apache.usergrid.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.settings.Settings
import org.apache.usergrid.helpers.Extractors._
import org.apache.usergrid.helpers.{Headers, Utils}


object UserScenarios {

   /**
    * Naming constants used in the scenarios for saving values into the sessions
    */

   //The value for the cursor
   val SessionVarCursor: String = "cursor"

   //the value for the json array of users
   val SessionVarUsers: String = "users"

  //the value for the users uuid
   val SessionVarUserId: String = "userId"

  //the value for HTTP response code after requests
   val SessionVarUserStatus: String = "userStatus"

   val getRandomUser = exec(
     http("GET user")
       .get("/users/user" + Utils.generateRandomInt(1, Settings.numEntities))
       .headers(Headers.authToken)
       .check(status.is(200))
   )


   val getUserByUsername = exec(
     http("GET user")
       .get("/users/${username}")
       .headers(Headers.authToken)
       .check(status.saveAs(SessionVarUserStatus), jsonPath("$..entities[0]").exists, jsonPath("$..entities[0].uuid").exists, jsonPath("$..entities[0].uuid").saveAs(SessionVarUserId))
   )


   /**
    * Post a user
    */
   val postUser =

     exec(
       http("POST geolocated Users")
         .post("/users")
         .body(StringBody( """{"location":{"latitude":"${latitude}","longitude":"${longitude}"},"username":"${username}",
           "displayName":"${displayName}","age":"${age}","seen":"${seen}","weight":"${weight}",
           "height":"${height}","aboutMe":"${aboutMe}","profileId":"${profileId}","headline":"${headline}",
           "showAge":"${showAge}","relationshipStatus":"${relationshipStatus}","ethnicity":"${ethnicity}","password":"password"}"""))
         .check(status.saveAs(SessionVarUserStatus))
         .check(status.is(200), jsonPath("$..entities[0].uuid").saveAs(SessionVarUserId))
     )


   /**
    * Try to get a user, if it returns a 404, create the user
    */
   val postUserIfNotExists =
     exec(getUserByUsername)
       .doIf("${userStatus}", "404") {
       exec(postUser)
     }


   val putUser = exec(
     http("PUT geolocated Users")
       .put("/users/${username}")
       .headers(Headers.authToken)
       .body(StringBody( """{"location":{"latitude":"${latitude}","longitude":"${longitude}"},"username":"${username}",
        "displayName":"${displayName}","age":"${age}","seen":"${seen}","weight":"${weight}",
        "height":"${height}","aboutMe":"${aboutMe}","profileId":"${profileId}","headline":"${headline}",
        "showAge":"${showAge}","relationshipStatus":"${relationshipStatus}","ethnicity":"${ethnicity}","password":"password"}"""))
       .check(status.is(200), jsonPath("$..entities[0].uuid").saveAs(SessionVarUserId))

       )


   val deleteUser = exec(
     http("DELETE geolocated Users")
       .delete("/users/${username}")
       .headers(Headers.authToken)
       .check(status.in(Seq(200,404)))
   )

   val deleteUserIfExists =
     exec(getUserByUsername)
       .doIf("${userStatus}", "200") {
       deleteUser
     }

   /**
    * Get a collection of users without a cursor.  Sets the cursor and entities array as "users"
    */
   val getUsersWithoutCursor = exec(
     http("GET user")
       .get("/users")
       .headers(Headers.authToken)
       .check(status.is(200), maybeExtractEntities(SessionVarUsers), maybeExtractCursor(SessionVarCursor))
   )

   /**
    * Get the next page of users with the cursor, expects the value "cursor" to be present in teh session
    */
   //maybe doif for detecting empty session?
   val getUsersWithCursor = exec(
     http("GET user")
       .get("/users?cursor=${" + SessionVarCursor + "}")
       .headers(Headers.authToken)
       .check(status.is(200), maybeExtractEntities(SessionVarUsers), maybeExtractCursor(SessionVarCursor))
   ) /**
     * Debugging block

          .exec(session => {

          val cursor = session.get(SessionVarCursor)
          val users = session.get(SessionVarUsers)

          session
        })    */


   val deleteUserByUsername = exec(
     http("DELETE user")
       .delete("/users/${username}")
       .headers(Headers.authToken)
       .check(status.is(200), jsonPath("$..entities[0].uuid").saveAs(SessionVarUserId))
   )

   /**
    * Logs in as the admin user.  Checks if a user exists, if not, creates the user
    * Logs in as the user, then creates 2 devices if they do not exist
    */
   val createUsersWithDevicesScenario = scenario("Create Users")
     .feed(Settings.getInfiniteUserFeeder)
     .exec(TokenScenarios.getManagementToken)
     .exec(UserScenarios.postUserIfNotExists)
     .exec(TokenScenarios.getUserToken)
     .exec(UserScenarios.getUserByUsername)
     .repeat(2) {
     feed(FeederGenerator.generateEntityNameFeeder("device", Settings.numDevices))
       .exec(DeviceScenarios.maybeCreateDevices)
   }

   /**
    * Posts a new user every time
    */
   val postUsersInfinitely =  scenario("Post Users")
        .feed(Settings.getInfiniteUserFeeder)
        .exec(postUser)


   /**
    * Puts a new user every time
    */
   val putUsersInfinitely =  scenario("Put Users").exec(injectManagementTokenIntoSession)
     .feed(Settings.getInfiniteUserFeeder)
     .exec(putUser)

   /**
    * Deletes user every time
    */
   val deleteUsersInfinitely =  scenario("Delete Users").exec(injectManagementTokenIntoSession)
     .feed(Settings.getInfiniteUserFeeder)
     .exec(deleteUser)

   /**
    * Get the users a page at a time until exhausted
    */
   val getUserPagesToEnd = scenario("Get User Pages").exec(injectManagementTokenIntoSession)
     //get users without a cursor
     .exec(getUsersWithoutCursor)
     //as long as we have a cursor, keep getting results
     .asLongAs(stringParamExists(SessionVarCursor)) {
        exec(getUsersWithCursor)
      }.exec(sessionFunction => {
     sessionFunction
   })

  val getUsersByUsername = scenario("Get User By Username").exec(injectManagementTokenIntoSession)
    .feed(Settings.getInfiniteUserFeeder)
         //get users without a cursor
         .exec(getUserByUsername)


 }
