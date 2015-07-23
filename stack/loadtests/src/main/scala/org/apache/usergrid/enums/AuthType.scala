package org.apache.usergrid.enums

/**
 * Created by mdunker on 7/20/15.
 */
object AuthType {
  val Anonymous = "anonymous"
  val Token = "token"
  val Basic = "basic"

  val Values = Seq(Anonymous,Token,Basic)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
