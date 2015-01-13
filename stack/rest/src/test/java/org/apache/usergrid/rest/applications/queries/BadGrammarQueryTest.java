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
package org.apache.usergrid.rest.applications.queries;


import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Test for an exception with incorrect query grammar
 * 1. Insert an entity
 * 2. Issue an invalid query
 * 3. Check for an exception
 */
public class BadGrammarQueryTest extends AbstractRestIT {

  @Test
  public void catchBadQueryGrammar() throws IOException {

    //1. Insert an entity
    Entity actor = new Entity();
    actor.put("displayName", "Erin");
    Entity props = new Entity();
    props.put("actor", actor);
    props.put("content", "bragh");

    this.app().collection("things").post(props);
    refreshIndex();

    //2. Issue an invalid query
    String query = "select * where name != 'go'";
    try {

      QueryParameters params = new QueryParameters().setQuery(query);
      this.app().collection("things").get(params);
      fail("This should throw an exception");
    } catch (UniformInterfaceException uie) {
      //3. Check for an exception
      assertEquals(400, uie.getResponse().getStatus());
    }
  }
}
