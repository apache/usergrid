package org.apache.usergrid.enums

object TokenType {
  val None = "none"
  val User = "user"
  val Management = "management"

  val Values = Seq(None, User, Management)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
