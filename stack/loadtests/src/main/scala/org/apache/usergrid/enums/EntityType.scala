package org.apache.usergrid.enums

object EntityType {
  val Trivial = "trivial"
  val Basic = "basic"

  val Values = Seq(Trivial,Basic)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
