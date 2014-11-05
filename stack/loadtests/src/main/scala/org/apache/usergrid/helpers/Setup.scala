/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.ning.http.client.AsyncHttpClient
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import io.gatling.jsonpath.JsonPath
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.settings.{Settings, Headers}

/**
 * Classy class class.
 */
object Setup {
  var token:String = null
  def setupOrg(): Integer = {
    val client = new AsyncHttpClient()

    val createOrgPost = client
      .preparePost(Settings.baseUrl + "/management/organizations")
      .setBody("{\"organization\":\"" + Settings.org + "\",\"username\":\"" + Settings.admin + "\",\"name\":\"" + Settings.admin + "\",\"email\":\"" + Settings.admin + "@apigee.com\",\"password\":\"" + Settings.password + "\"}")
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")

    val orgResponse = createOrgPost.execute().get();

    return orgResponse.getStatusCode
  }
  def setupApplication():Integer = {
    val client = new AsyncHttpClient()

    val authToken = getManagementToken()
    val createAppPost = client
      .preparePost(Settings.baseUrl + "/management/organizations/"+Settings.org+"/applications")
      .setBody("{\"name\":\"" + Settings.app + "\"}")
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization","Bearer "+authToken)


    val appResponse = createAppPost.execute().get();


    return appResponse.getStatusCode
  }

  def setupNotifier():Integer = {
    val client = new AsyncHttpClient()

    val authToken = getManagementToken()
    val createNotifier = client
      .preparePost(Settings.baseAppUrl + "/notifiers")
      .setBody("{\"name\":\"" + Settings.pushNotifier + "\",\"provider\":\"" + Settings.pushProvider + "\"}")
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization","Bearer "+authToken)

    val notifierResponse = createNotifier.execute().get();

    return notifierResponse.getStatusCode
  }

  def getManagementToken():String = {
//    if(token==null) {

      val client = new AsyncHttpClient()
      val getToken = client
        .preparePost(Settings.baseUrl + "/management/token")
        .setBody("{\"username\":\"" + Settings.admin + "\",\"password\":\"" + Settings.password + "\",\"grant_type\":\"password\"}")
        .setHeader("Cache-Control", "no-cache")
        .setHeader("Content-Type", "application/json; charset=UTF-8")
      val body = getToken.execute().get().getResponseBody()
      val omapper = new ObjectMapper();
      val tree = omapper.readTree(body)
      val node = tree.get("access_token");
      token = node.toString
//    }
    return token
  }

  def setupUsers() = {
    val userFeeder = FeederGenerator.generateUserWithGeolocationFeeder(Settings.numUsers *  Settings.duration, Settings.userLocationRadius, Settings.centerLatitude, Settings.centerLongitude)
    userFeeder.foreach(user => {
      setupUser(user);
    });

  }

  def setupUser(user:Map[String,String]):Integer = {

    val client = new AsyncHttpClient()

    val authToken = getManagementToken()
    val createUser = client
      .preparePost(Settings.baseAppUrl + "/users")
      .setBody("{\"location\":{\"latitude\":\"" + user.get("latitude") + "\",\"longitude\":\"" + user.get("longitude") + "\"},\"username\":\"" + user.get("username") + "\"," +
      "\"displayName\":\""+user.get("displayName")+"\",\"age\":\""+user.get("age")+"\",\"seen\":\""+user.get("seen")+"\",\"weight\":\""+user.get("weight")+"\"," +
      "\"height\":\""+user.get("height")+"\",\"aboutMe\":\""+user.get("aboutMe")+"\",\"profileId\":\""+user.get("profileId")+"\",\"headline\":\""+user.get("headline")+"\"," +
      "\"showAge\":\""+user.get("showAge")+"\",\"relationshipStatus\":\""+user.get("relationshipStatus")+"\",\"ethnicity\":\""+user.get("ethnicity")+"\",\"password\":\""+user.get("password")+"\"}"
      )
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization","Bearer "+authToken)

    val createUserResponse = createUser.execute().get();

    return createUserResponse.getStatusCode

  }

}
