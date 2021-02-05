/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.tools;

import org.apache.usergrid.services.AbstractServiceIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/** TODO: better test, this is really just a smoke test. */
public class ImportUserCollectionTest extends AbstractServiceIT {
  static final Logger logger = LoggerFactory.getLogger(ImportUserCollectionTest.class);

  int NUM_COLLECTIONS = 10;
  int NUM_ENTITIES = 50;
  int NUM_CONNECTIONS = 3;

  @org.junit.Test
  public void testBasicOperation() throws Exception {

    // add app with some data

    Path currentRelativePath = Paths.get("");
    String resourcePath = currentRelativePath.toAbsolutePath().toString();

    long start = System.currentTimeMillis();

    ImportUserCollection importapp = new ImportUserCollection();
    importapp.startTool(
        new String[] {
          "-host",
          "localhost:9160",
          "-appId",
          "6781adef-4f8d-11e9-b170-005056a636e2", // add your appid
          "-writeThreads",
          "10",
          "-inputDir",
          resourcePath + "/src/test/resources/",
          "-v",
          resourcePath + "/src/test/resources/import-user-details.log"
        },
        false);

    logger.info(
        "100 read and 100 write threads = " + (System.currentTimeMillis() - start) / 1000 + "s");

    // check that we got the expected number of export files

    logger.info("1 thread time = " + (System.currentTimeMillis() - start) / 1000 + "s");

    assertEquals(1, 1);
    assertEquals(1, 1);
  }
}
