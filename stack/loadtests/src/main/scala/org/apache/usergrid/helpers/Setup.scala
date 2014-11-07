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

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.ning.http.client.{ListenableFuture, AsyncHttpClient,Response}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import io.gatling.jsonpath.JsonPath
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.settings.{Settings, Headers}

import scala.collection.mutable.ArrayBuffer

/**
 * Classy class class.
 */
object Setup {
  var token:String = null
  val client = new AsyncHttpClient()

  def setupOrg(): Integer = {

    val createOrgPost = client
      .preparePost(Settings.baseUrl + "/management/organizations")
       .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setBody("{\"organization\":\"" + Settings.org + "\",\"username\":\"" + Settings.admin + "\",\"name\":\"" + Settings.admin + "\",\"email\":\"" + Settings.admin + "@apigee.com\",\"password\":\"" + Settings.password + "\"}")
    .build()
    val orgResponse = client.executeRequest(createOrgPost).get()
    printResponse("POST ORG",orgResponse.getStatusCode,orgResponse.getResponseBody())
    return orgResponse.getStatusCode
  }
  def setupApplication():Integer = {

    val authToken = getManagementToken()
    val createAppPost = client
      .preparePost(Settings.baseUrl + "/management/organizations/"+Settings.org+"/applications")
      .setBody("{\"name\":\"" + Settings.app + "\"}")
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization","Bearer "+authToken)
      .build()

    val appResponse = client.executeRequest(createAppPost).get();
    printResponse("POST Application",appResponse.getStatusCode, appResponse.getResponseBody())

    return appResponse.getStatusCode
  }

  def setupNotifier():Integer = {

    val authToken = getManagementToken()
    val createNotifier = client
      .preparePost(Settings.baseAppUrl + "/notifiers")
      .setBody("{\"name\":\"" + Settings.pushNotifier + "\",\"provider\":\"" + Settings.pushProvider + "\"}")
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization","Bearer "+authToken)
      .build()

    val notifierResponse = client.executeRequest(createNotifier).get();
    printResponse("POST Notifier", notifierResponse.getStatusCode ,notifierResponse.getResponseBody())

    return notifierResponse.getStatusCode
  }

  def getManagementToken():String = {
    if(token == null) {
      val getToken = client
        .preparePost(Settings.baseUrl + "/management/token")
        .setBody("{\"username\":\"" + Settings.admin + "\",\"password\":\"" + Settings.password + "\",\"grant_type\":\"password\"}")
        .setHeader("Cache-Control", "no-cache")
        .setHeader("Content-Type", "application/json; charset=UTF-8")
        .build()
      val response = client.executeRequest(getToken).get()
      val omapper = new ObjectMapper();
      val tree = omapper.readTree(response.getResponseBody())
      val node = tree.get("access_token");
      token = node.asText()
      println("Token is "+token)
    }
    return token
  }

  def setupUsers() = {
    val userFeeder = Settings.userFeeder
    val numUsers = userFeeder.length
    println(s"setupUsers: Sending requests for $numUsers users")

    val list:ArrayBuffer[ListenableFuture[Response]] = new ArrayBuffer[ListenableFuture[Response]]
    userFeeder.foreach(user => {
      list += setupUser(user);
    });
    var successCount:Int = 0;
    list.foreach(f => {
      val response = f.get()
      if(response.getStatusCode != 200) {
        printResponse("Post User", response.getStatusCode, response.getResponseBody())
      }else{
        successCount+=1
      }
    })
    println(s"setupUsers: Received $successCount successful responses out of $numUsers requests.")

  }

  def setupUser(user:Map[String,String]):ListenableFuture[Response] = {

    val latitude = user.get("latitude").get
    val longitude = user.get("longitude").get
    val username = user.get("username").get
    val displayName = user.get("displayName").get
    val age = user.get("age").get
    val seen = user.get("seen").get
    val weight = user.get("weight").get
    val height = user.get("height").get
    val aboutMe = user.get("aboutMe").get
    val profileId = user.get("profileId").get
    val headline = user.get("headline").get
    val showAge = user.get("showAge").get
    val relationshipStatus = user.get("relationshipStatus").get
    val ethnicity = user.get("ethnicity").get
    val password= user.get("password").get
    val body = s"""{"location":{"latitude":"$latitude","longitude":"$longitude"},"username":"$username",
      "displayName":"$displayName","age":"$age","seen":"$seen","weight":"$weight",
      "height":"$height","aboutMe":"$aboutMe","profileId":"$profileId","headline":"$headline",
      "showAge":"$showAge","relationshipStatus":"$relationshipStatus","ethnicity":"$ethnicity","password":"$password"}"""
    val authToken = getManagementToken()
    val createUser = client
      .preparePost(Settings.baseAppUrl + "/users")
      .setBody(body)
      .setBodyEncoding("UTF-8")
      .setHeader("Authorization","Bearer "+authToken)
      .build()

    return client.executeRequest(createUser)


  }

  def printResponse(caller:String, status:Int, body:String) = {
    println(caller + ": status: "+status.toString+" body:"+body)

  }

}
