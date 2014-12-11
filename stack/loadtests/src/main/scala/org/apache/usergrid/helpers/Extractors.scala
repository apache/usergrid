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

import io.gatling.core.Predef._
import io.gatling.core.session._
import io.gatling.http.Predef._

/**
 * Helper object that will perform extractions
 */
object Extractors {

  /**
   * Will extract the cursor from the response.  If the cursor is not present, an empty string will be set
   */
  def maybeExtractCursor(saveAsName: String) = {
    jsonPath("$..cursor").transformOption(extract => {
      //it may or may not be present.  If it is, save it, otherwise save it as an empty string
      extract.orElse(Some(""))
    }).saveAs(saveAsName)
  }


  /**
   * tries to extract the cursor from the session, if it exists, it returns true. if it's the default, returns false
   * @param nameInSession The name of the variable in the session
   * @return
   */
  def stringParamExists(nameInSession: String): Expression[Boolean] = {
    session => session(nameInSession) != null && session(nameInSession).as[String] != ""
  }

  /**
   * Will extract entities as a json array into the session. If they do not exist, it will set to an empty list
   * @param saveAsName The name to use when saving to the session
   */
  def maybeExtractEntities(saveAsName: String) = {
    jsonPath("$..entities").ofType[Seq[Any]].transformOption(extract => {
      extract.orElse(Some(Seq()));
    }).saveAs(saveAsName)
  }

  /**
   * Returns true if sequence is not null and has elements.  Expects a seq object
   * @param nameInSession  The name ot use when saving to the session
   * @return
   */
  def sequenceHasElements(nameInSession: String): Expression[Boolean] = {
    session => session(nameInSession) != null && session(nameInSession).as[Seq[Any]].length > 0
  }


  val ManagementToken: String = Setup.getManagementToken()


  /**
   * Get the management token for the admin username and password in settings, then inject it into the session
   * under the variable "authToken"
   * @return
   */
  def injectStaticTokenToSession(): Expression[Session] = {
    session => session.set("authToken", ManagementToken)
  }


}
