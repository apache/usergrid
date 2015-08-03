package org.apache.usergrid.enums

object EndConditionType {
  val MinutesElapsed = "minutesElapsed"
  val RequestCount = "requestCount"

  val Values = Seq(MinutesElapsed,RequestCount)

  def isValid(str: String): Boolean = {
    Values.contains(str)
  }
}
