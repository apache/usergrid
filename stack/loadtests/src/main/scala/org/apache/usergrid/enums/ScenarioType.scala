package org.apache.usergrid.enums

object ScenarioType {
  val GetAllByCursor = "getAllByCursor"
  val NameRandomInfinite = "nameRandomInfinite"
  val LoadEntities = "loadEntities"
  val DeleteEntities = "deleteEntities"
  val UpdateEntities = "updateEntities"

  val Values = Seq(GetAllByCursor,NameRandomInfinite,LoadEntities,DeleteEntities,UpdateEntities)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
