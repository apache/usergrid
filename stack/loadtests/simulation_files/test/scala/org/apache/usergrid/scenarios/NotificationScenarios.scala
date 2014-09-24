package org.apache.usergrid

import java.io.File
import java.nio.file.{Paths, Files}

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

import scala.io.Source

/**
 *
 * Creates a new device
 *
 * Expects:
 *
 * authToken The auth token to use when creating the application
 * orgName The name of the org
 * appName The name of the app
 * notifierName The name of the created notifier
 * deviceName the name of the device created to send the notification to
 *
 * Produces:
 *
 * N/A
 *
 *
 */
object NotificationScenarios {


  /**
   * send the notification now
   */
  val sendNotification = exec(http("Send Single Notification")
      .post("/devices/${entityName}/notifications")
      .body(StringBody("{\"payloads\":{\"${notifier}\":\"testmessage\"}}"))
      .check(status.is(200))
    )


  /**
   * TODO: Add posting to users, which would expect a user in the session
   */




}
