package org.apache.usergrid.enums

object EntityType {
  val Trivial = "trivial"
  val TrivialSortable = "trivialSortable"
  val Basic = "basic"

  val Values = Seq(Trivial,TrivialSortable,Basic)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
