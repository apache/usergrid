/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */

package org.apache.usergrid.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.ning.http.client.{ListenableFuture, AsyncHttpClient,Response}
import io.gatling.jsonpath.JsonPath
import org.apache.usergrid.datagenerators.FeederGenerator
import org.apache.usergrid.enums.TokenType
import org.apache.usergrid.settings.Settings

import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.json.JSON

object Setup {
  var managementToken:String = null
  val client = new AsyncHttpClient()

  def getToken(tokenUrl:String, username:String, password:String):String = {
    val getToken = client
      .preparePost(tokenUrl)
      .setBody(Utils.toJSONStr(Map(
                          "username" -> username,
                          "password" -> password,
                          "grant_type" -> "password"
      )))
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .build()
    val response = client.executeRequest(getToken).get()
    val mapper = new ObjectMapper()
    val tree = mapper.readTree(response.getResponseBody)
    tree.get("access_token").asText()
  }

  def getManagementToken:String = {
    if(managementToken == null) {
      managementToken = getToken(s"${Settings.baseUrl}/management/token", Settings.adminUser, Settings.adminPassword)
      println("Management token is "+managementToken)
    }
    managementToken
  }

  def getUserToken:String = {
    getToken(s"${Settings.baseAppUrl}/token", Settings.appUser, Settings.appUserPassword)
  }

  def getToken:String = {
    var token = ""
    if (Settings.tokenType == TokenType.Management) token = getManagementToken
    if (Settings.tokenType == TokenType.User) token = getUserToken

    token
  }

  def setupOrg(): Integer = {

    val createOrgPost = client
      .preparePost(s"${Settings.baseUrl}/management/organizations")
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setBody(Utils.toJSONStr(Map(
                          "organization" -> Settings.org,
                          "username" -> Settings.orgCreationUsername,
                          "name" -> Settings.orgCreationName,
                          "email" -> Settings.orgCreationEmail,
                          "password" -> Settings.orgCreationPassword
      )))
      .build()

    val orgResponse = client.executeRequest(createOrgPost).get()

    printResponse("POST ORG", orgResponse.getStatusCode, orgResponse.getResponseBody)

    orgResponse.getStatusCode
  }

  def setupApplication(): Integer = {

    val createAppPost = client
      .preparePost(s"${Settings.baseUrl}/management/organizations/${Settings.org}/applications")
      .setBody(Utils.toJSONStr(Map("name" -> Settings.app)))
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization", s"Bearer $getManagementToken")
      .build()

    val appResponse = client.executeRequest(createAppPost).get()

    val statusCode = appResponse.getStatusCode
    printResponse("POST APP", statusCode, appResponse.getResponseBody)

    statusCode
  }

  def getCollectionsList(org:String = Settings.org, app:String = Settings.app): List[String] = {
    val url = s"${Settings.baseUrl}/$org/$app"
    println(s"url: $url")
    val appInfoGet = client
      .prepareGet(url)
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Accept", "application/json; charset=UTF-8")
      .setHeader("Authorization", s"Bearer $getManagementToken")
      .build()

    val getResponse = client.executeRequest(appInfoGet).get()
    println(s"responseBody\n: ${getResponse.getResponseBody}\n")
    val topLevel: Map[String, Any] = JSON.parseFull(getResponse.getResponseBody).get.asInstanceOf[Map[String,Any]]
    val entities: List[Map[String, Any]] = topLevel("entities").asInstanceOf[List[Map[String,Any]]]
    //println(s"entities: $entities")
    val firstEntity: Map[String, Any] = entities.head
    //println(s"firstEntity: $firstEntity")
    val metadata: Map[String, Any] = firstEntity("metadata").asInstanceOf[Map[String, Any]]
    //println(s"metadata: $metadata")
    val collections: Map[String, Any] = metadata("collections").asInstanceOf[Map[String, Any]]
    //println(s"collections: $collections")

    val collectionsList: List[String] = (collections map { case (key, value) => s"$app/$key" }).toList
    println(s"collectionsList: $collectionsList")

    collectionsList
  }

  def getApplicationCollectionsList: List[String] = {
    val orgInfoGet = client
      .prepareGet(s"${Settings.baseUrl}/management/organizations/${Settings.org}/applications")
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Accept", "application/json; charset=UTF-8")
      .setHeader("Authorization", s"Bearer $getManagementToken")
      .build()

    val getResponse = client.executeRequest(orgInfoGet).get()
    val topLevel: Map[String,Any] = JSON.parseFull(getResponse.getResponseBody).get.asInstanceOf[Map[String,Any]]
    val data: Map[String,Any] = topLevel("data").asInstanceOf[Map[String,Any]]
    val applicationsList: List[String] = (data map { case (key,value) => key }).toList
    //println(applicationsList)

    val collectionApplicationsListBuffer: scala.collection.mutable.ListBuffer[String] = scala.collection.mutable.ListBuffer[String]()
    // for each app, get the list of collections
    for (orgPlusApp <- applicationsList) {
      println(s"getting $orgPlusApp")
      val orgAppArray = orgPlusApp.split("/")
      val org = orgAppArray(0)
      val app = orgAppArray(1)
      collectionApplicationsListBuffer.appendAll(getCollectionsList(org,app))
    }

    println(s"appCollections: ${collectionApplicationsListBuffer.toList}")
    collectionApplicationsListBuffer.toList
  }

  def sandboxCollection(): Integer = {

    val sandboxCollectionPost = client
      .preparePost(s"${Settings.baseAppUrl}/roles/guest/permissions")
      .setBody(Utils.toJSONStr(Map("permission" -> s"GET,PUT,POST,DELETE:/${Settings.collection}/**")))
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization", s"Bearer $getManagementToken")
      .build()

    val response = client.executeRequest(sandboxCollectionPost).get()

    val statusCode = response.getStatusCode
    printResponse("SANDBOX COLLECTION", statusCode, response.getResponseBody)

    statusCode
  }

  def setupNotifier():Integer = {

    val createNotifier = client
      .preparePost(s"${Settings.baseAppUrl}/notifiers")
      .setBody(Utils.toJSONStr(Map("name" -> Settings.pushNotifier, "provider" -> Settings.pushProvider)))
      .setHeader("Cache-Control", "no-cache")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .setHeader("Authorization", s"Bearer $getManagementToken")
      .build()

    val notifierResponse = client.executeRequest(createNotifier).get()
    printResponse("POST Notifier", notifierResponse.getStatusCode ,notifierResponse.getResponseBody)

    notifierResponse.getStatusCode
  }

  def setupUsers() = {
    val userFeeder = Settings.getUserFeeder
    val numUsers = userFeeder.length
    println(s"setupUsers: Sending requests for $numUsers users")

    val list: ArrayBuffer[ListenableFuture[Response]] = new ArrayBuffer[ListenableFuture[Response]]
    var successCount: Int = 0
    def purgeList() = {
      list.foreach(f => {
        val response = f.get()
        if (response.getStatusCode != 200) {
          printResponse("Post User", response.getStatusCode, response.getResponseBody)
        } else {
          successCount += 1
        }
      })
      list.clear()
    }
    userFeeder.foreach(user => {
      list += setupUser(user)
      if (list.length == Settings.purgeUsers) {
        purgeList()
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
    val authToken = getManagementToken
    val createUser = client
      .preparePost(s"${Settings.baseAppUrl}/users")
      .setBody(body)
      .setBodyEncoding("UTF-8")
      .setHeader("Authorization",s"Bearer $authToken")
      .build()

    client.executeRequest(createUser)
  }

  def setupEntitiesCollection(numEntities: Int, entityType: String, prefix: String, seed: Int = 1) = {
    val entitiesFeeder = FeederGenerator.generateCustomEntityArray(numEntities, entityType, prefix, seed)

    val list:ArrayBuffer[ListenableFuture[Response]] = new ArrayBuffer[ListenableFuture[Response]]
    var successCount:Int = 0
    def purgeList():Unit = {
      list.foreach(f => {
        val response = f.get()
        if (response.getStatusCode != 200) {
          printResponse("Post Entity", response.getStatusCode, response.getResponseBody)
        } else {
          successCount += 1
        }
      })
      list.clear()
    }

    entitiesFeeder.foreach(payload => {
      //println("setupEntitiesCollection: payload=" + payload)
      list += setupEntity(payload)
      if(list.length == Settings.purgeUsers) {
        purgeList()
      }
    })

    // purgeList()

    println(s"setupEntitiesCollection: Received $successCount successful responses out of $numEntities requests.")
  }

  def setupEntity(payload: String):ListenableFuture[Response] = {
    val authToken = getManagementToken
    val createEntity = client
      .preparePost(Settings.baseCollectionUrl)
      .setBody(payload)
      .setBodyEncoding("UTF-8")
      .setHeader("Authorization", s"Bearer $authToken")
      .setHeader("Content-Type", "application/json; charset=UTF-8")
      .build()
    //println("setupEntity: baseAppUrl=" + Settings.baseAppUrl + ", collection=" + Settings.collection + ", payload=" + payload)

    client.executeRequest(createEntity)
  }

  def printResponse(caller:String, status:Int, body:String) = {
    println(caller + s"$caller:\nstatus: $status\nbody:\n$body")
  }

}
