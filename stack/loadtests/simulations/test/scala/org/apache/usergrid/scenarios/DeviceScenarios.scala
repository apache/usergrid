package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._

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
 *
 * Produces:
 *
 * deviceName the name of the device created
 *
 */
object DeviceScenarios {

  /**
   * Create a device
   */
  val postDeviceWithNotifier = exec(http("Create device with notifier")
    .post("/devices")
    .body(StringBody("{\"name\":\"${entityName}\"," +
      "\"deviceModel\":\"Fake Device\"," +
      " \"deviceOSVerion\":\"Negative Version\", " +
      "\"${notifier}.notifier.id\":\"${entityName}\"}"))
    .check(status.is(200)))

  val postDeviceWithNotifier400ok = exec(http("Create device with notifier")
    .post("/devices")
    .body(StringBody("{\"name\":\"${entityName}\"," +
    "\"deviceModel\":\"Fake Device\"," +
    " \"deviceOSVerion\":\"Negative Version\", " +
    "\"${notifier}.notifier.id\":\"${entityName}\"}"))
    .check(status.in(200 to 400)))

  /**
   * TODO: Add a device to a user, which would expect a user in the session
   */



}
