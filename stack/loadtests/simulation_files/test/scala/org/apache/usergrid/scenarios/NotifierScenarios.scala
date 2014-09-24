package org.apache.usergrid

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 *
 * Creates a new no-op notifier
 *
 *
 * Expects:
 *
 * authToken The auth token to use when creating the application
 * orgName The name of the org
 * appName The name of the app
 *
 * Produces:
 *
 * notifierName The name of the created notifier
 *
 */
object NotifierScenarios {
  /**
   * Read the pkcs12 cert from our classpath, then get the absolute file path for uploading
   */
  /*val pkcs12Cert = Files.readAllBytes(Paths.get(getClass.getResource("/notifier.p12").getPath() ))*/

  /**
   * Create a notifier
   */
  val createNotifier = exec(
      session => {
        session.set("notifier", Utils.generateUniqueName("notifier"))
      }
    )

    .exec(http("Create Notifier")
    .post("/notifiers")
    .body(StringBody("{\"name\":\"${notifier}\",\"provider\":\"noop\"}"))
    //remnants of trying to upload an apple certificate
//    .param("name", "${notifierName}")
//    .param("provider", "apple")
//    .param("environment", "mock")
//    .fileBody("p12Certificate", Map).fileBody(pkcs12Cert)
    .check(status.is(200)))


}
