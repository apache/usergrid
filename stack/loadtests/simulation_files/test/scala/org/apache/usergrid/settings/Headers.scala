package org.apache.usergrid

/**
 *
 */
object Headers {

  /**
   * Headers for anonymous posts
   */
  val jsonAnonymous = Map(
    "Cache-Control" -> """no-cache""",
    "Content-Type" -> """application/json; charset=UTF-8"""
  )

  /**
   * Headers for authorized users with token and json content type
   */
  val jsonAuthorized = Map(
    "Cache-Control" -> """no-cache""",
    "Content-Type" -> """application/json; charset=UTF-8""",
    "Authorization" -> "Bearer ${authToken}"
  )



}
