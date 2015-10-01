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
package org.apache.usergrid.enums

object ScenarioType {
  val GetAllByCursor = "getAllByCursor"
  val NameRandomInfinite = "nameRandomInfinite"
  val LoadEntities = "loadEntities"
  val DeleteEntities = "deleteEntities"
  val UpdateEntities = "updateEntities"
  val UuidRandomInfinite = "uuidRandomInfinite"
  val GetByNameSequential = "getByNameSequential"
  val AuditGetCollectionEntities = "auditGetCollectionEntities"
  val AuditVerifyCollectionEntities = "auditVerifyCollectionEntities"
  val DoNothing = "doNothing"

  val Values = Seq(GetAllByCursor,NameRandomInfinite,LoadEntities,DeleteEntities,UpdateEntities,UuidRandomInfinite,
    GetByNameSequential,AuditGetCollectionEntities,AuditVerifyCollectionEntities,DoNothing)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
