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
package org.apache.usergrid.helpers

import org.apache.usergrid.enums.AuthType
import org.apache.usergrid.settings.Settings

/**
 *
 */
object Headers {

  /**
   * Headers for anonymous posts
   */
  val authAnonymous = Map(
    "Cache-Control" -> """no-cache""",
    "Content-Type" -> """application/json; charset=UTF-8"""
  )

  /**
   * Headers for authorized users with token and json content type
   */
  val authToken = Map(
    "Cache-Control" -> """no-cache""",
    "Content-Type" -> """application/json; charset=UTF-8""",
    "Authorization" -> "Bearer ${authToken}"
  )

  /**
  * Headers for basic auth
  */
  val authBasic = Map(
    "Cache-Control" -> """no-cache""",
    "Content-Type" -> """application/json; charset=UTF-8""",
    "Authorization" -> ("Basic " + Settings.appUserBase64)
  )

  /**
  * Header selector
  */
  def auth(authType:String): Map[String, String] = {
    if (authType == AuthType.Basic) authBasic
    else if (authType == AuthType.Token) authToken
    else authAnonymous
  }

  /**
  * Optional region header
  */
  def usergridRegionHeaders: Map[String, String] = {
    if (Settings.usergridRegion != "") Map( "UsergridRegion" -> Settings.usergridRegion )
    else Map()
  }

}
