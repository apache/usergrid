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
package org.apache.usergrid.datagenerators

import org.apache.usergrid.enums.EntityType
import org.apache.usergrid.helpers.Utils
import org.apache.usergrid.settings.Settings

import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.json.JSONObject

object EntityDataGenerator {

  def generateBlockUserLists(numUsers: Int): Map[String, String] = {

    var blocks: ArrayBuffer[String] = new ArrayBuffer[String]
    var blockedBy: ArrayBuffer[String] = new ArrayBuffer[String]

    for (numBlock <- 1 to Utils.generateRandomInt(1, 7)) {
      blocks += "user".concat(Utils.generateRandomInt(0, numUsers).toString)
    }

    for (numBlockedBy <- 1 to Utils.generateRandomInt(1, 7)) {
      blockedBy += "user".concat(Utils.generateRandomInt(0, numUsers).toString)
    }

    return Map("blocks" -> blocks.toArray.mkString(","), "blockedBy" -> blockedBy.toArray.mkString(","))

  }

  def generateUser(userId: String): Map[String,String] = {

    Map(

      "username" -> "user".concat(userId),
      "profileId" -> Utils.generateRandomInt(10000, 1000000).toString,
      "displayName" -> Utils.generateRandomInt(10000, 1000000).toString,
      "showAge" -> Utils.generateRandomInt(0, 1).toString,
      "ethnicity" -> Utils.generateRandomInt(1, 15).toString,
      "relationshipStatus" -> Utils.generateRandomInt(1, 4).toString,
      "headline" -> "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      "aboutMe" -> "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
      "age" -> Utils.generateRandomInt(18, 65).toString,
      "height" -> Utils.generateRandomInt(48, 84).toString,
      "weight" -> Utils.generateRandomInt(120, 350).toString,
      "seen" -> Utils.generateRandomInt(50, 100000).toString,
      "password" -> "password"
    )
  }

  def generateCustomEntity(): Map[String,String] = {

    val entity: Map[String, String] = Map(
      // "name" -> "fdsa",
      "address" -> Utils.generateRandomInt(10000, 1000000).toString,
      "city" -> Utils.generateRandomInt(10000, 1000000).toString,
      "state" -> Utils.generateRandomInt(10000, 1000000).toString,
      "zip" -> Utils.generateRandomInt(10000, 1000000).toString,
      "phone" -> Utils.generateRandomInt(10000, 1000000).toString,
      "businessname" -> Utils.generateRandomInt(0, 1).toString,
      "menu" -> Utils.generateRandomInt(1, 1000000).toString,
      "specials" -> Utils.generateRandomInt(1, 1000000).toString,
      "profile" -> "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      "description" -> "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
      "directions" -> Utils.generateRandomInt(18, 65).toString,
      "atmosphere" -> Utils.generateRandomInt(48, 84).toString,
      "bar" -> Utils.generateRandomInt(120, 350).toString,
      "tables" -> Utils.generateRandomInt(50, 100000).toString,
      "outdoor" -> Utils.generateRandomInt(50, 100000).toString
      )

    Map("entity" -> new JSONObject(entity).toString())
  }

   /* --------------------------- */

  def generateTrivialEntity(name: String = null): String = {

    val nameKey = if (name != null) "name" else "noname"
    val nameVal = if (name != null) name else Utils.generateRandomInt(1,10000000).toString

    val entity: Map[String, String] = Map(
      nameKey -> nameVal
    )

    new JSONObject(entity).toString()
  }

   def generateTrivialSortableEntity(name: String = null): String = {

     if (name != null)
       new JSONObject(Map("name" -> name, "sortField" -> Utils.generateRandomInt(1,10000000))).toString()
     else
       new JSONObject(Map("sortField" -> Utils.generateRandomInt(1,10000000))).toString()
   }

  val pirate1KString = "Yarrrrrrrrrrrrrrrr! Blimey! Avast ye scurvy dog! Dead men tell no tales. Hang him from the yardarm! Heave ho! Run a shot across the bow. Yo-ho-ho and a bottle of rum. Heave ho ye scallywag, or ye shall walk the plank! Weigh anchor and hoist the mizzen! Thar she blows! Shiver me timbers! Splice the mainbrace! Keelhaul the hornswaggler! Raise the Jolly Roger! Feed him to the fish! You fight like a dairy farmer. How appropriate, you fight like a cow. Batten down the hatches! Blow the man down! Swab the deck! Ahoy, matey! I'll crush ye barnacles! Fetch me grog! Gangway! Arrr, he's gone to Davy Jones's Locker. He be three sheets to the wind. Yo-ho me hearties! Prepare to be boarded! All your pieces of eight, else ye meet me cat o'nine tails! Bring 'er alongside! The rougher the seas, the smoother we sail! It's more fun to be a pirate than to join the navy. The beatings will continue until morale improves! Fifteen men on the dead man's chest! I be the captain of this ship -- swab the decks! "

  def generateLargeMultiFieldEntity(name: String = null, entityNum: Int = 0): String = {

    val objectMap:scala.collection.mutable.HashMap[String,Any] = scala.collection.mutable.HashMap.empty[String,Any]
    if (name != null) objectMap += ("name" -> name)
    if (Settings.entityNumberProperty != null && Settings.entityNumberProperty != "") objectMap += (Settings.entityNumberProperty -> entityNum)
    val fieldValue = pirate1KString * Settings.multiPropertySizeInK
    for (i <- 1 to Settings.multiPropertyCount) {
      objectMap += (s"${Settings.multiPropertyPrefix}$i" -> fieldValue)
    }

    new JSONObject(objectMap.toMap).toString()
  }

   def generateBasicEntity(name: String = null): String = {

     val nameKey = if (name != null) "name" else "noname"
     val nameVal = if (name != null) name else Utils.generateRandomInt(1,10000000).toString

     val entity: Map[String, String] = Map(
        nameKey -> nameVal,
        "address" -> Utils.generateRandomInt(10000, 1000000).toString,
        "city" -> Utils.generateRandomInt(10000, 1000000).toString,
        "state" -> Utils.generateRandomInt(10000, 1000000).toString,
        "zip" -> Utils.generateRandomInt(10000, 1000000).toString,
        "phone" -> Utils.generateRandomInt(10000, 1000000).toString,
        "businessname" -> Utils.generateRandomInt(0, 1).toString,
        "menu" -> Utils.generateRandomInt(1, 1000000).toString,
        "specials" -> Utils.generateRandomInt(1, 1000000).toString,
        "profile" -> "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        "description" -> "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
        "directions" -> Utils.generateRandomInt(18, 65).toString,
        "atmosphere" -> Utils.generateRandomInt(48, 84).toString,
        "bar" -> Utils.generateRandomInt(120, 350).toString,
        "tables" -> Utils.generateRandomInt(50, 100000).toString,
        "outdoor" -> Utils.generateRandomInt(50, 100000).toString
     )

     new JSONObject(entity).toString()
   }

   def generateEntity(entityType: String = EntityType.Basic, entityName: String = null, entityNum: Int = 0): String = {

     entityType match {
       case EntityType.Trivial => generateTrivialEntity(entityName)
       case EntityType.TrivialSortable => generateTrivialSortableEntity(entityName)
       case EntityType.Basic => generateBasicEntity(entityName)
       case EntityType.LargeMultiField => generateLargeMultiFieldEntity(entityName, entityNum)
     }
   }

   def generateUserEntity(userId: String = "user" + System.currentTimeMillis().toString): String = {

     val entity: Map[String, Any] = Map(
       "username" -> userId,
       "profileId" -> Utils.generateRandomInt(10000, 1000000),
       "displayName" -> Utils.generateRandomInt(10000, 1000000).toString,
       "showAge" -> Utils.generateRandomInt(0, 1),
       "ethnicity" -> Utils.generateRandomInt(1, 15),
       "relationshipStatus" -> Utils.generateRandomInt(1, 4),
       "headline" -> "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
       "aboutMe" -> "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
       "age" -> Utils.generateRandomInt(18, 65),
       "height" -> Utils.generateRandomInt(48, 84),
       "weight" -> Utils.generateRandomInt(120, 350),
       "seen" -> Utils.generateRandomInt(50, 100000).toString,
       "password" -> "password"
     )

     new JSONObject(entity).toString()
   }
}
