/**
 * *****************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. ****************************************************************************
 */
package org.usergrid.rest.applications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.persistence.CounterResolution;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.rest.AbstractRestIT;
import org.usergrid.services.ServiceManager;
import org.usergrid.utils.UUIDUtils;

/**
 * Invoke application request counters
 *
 * @author realbeast
 */
@Concurrent()
public class ApplicationRequestCounterIT extends AbstractRestIT {
  private static final Logger log = LoggerFactory.getLogger(ApplicationRequestCounterIT.class);
  long ts = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

  @Test
  public void applicationrequestInternalCounters() throws Exception {
    // Get application id
    JsonNode node = resource().path("/test-organization/test-app")
            .queryParam("access_token", access_token)
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

    assertNotNull(node.get("entities"));

    String uuid = node.get("application").asText();
    assertEquals(true, UUIDUtils.isUUID(uuid));

    UUID applicationId = UUID.fromString(uuid);
    EntityManagerFactory emf = setup.getEmf();
    EntityManager em = emf.getEntityManager(applicationId);

    int beforeTotalCall = getCounter(em, ServiceManager.APPLICATION_REQUESTS);
    int beforeCall = getCounter(em, ServiceManager.APPLICATION_REQUESTS_PER.concat("get"));

    // call
    node = resource().path("/test-organization/test-app/counters")
            .queryParam("resolution", "all")
            .queryParam("counter", "application.requests")
            .queryParam("access_token", adminToken()).accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE).get(JsonNode.class);

    assertNotNull(node.get("counters"));

    int afterTotalCall = getCounter(em, ServiceManager.APPLICATION_REQUESTS);
    int afterCall = getCounter(em, ServiceManager.APPLICATION_REQUESTS_PER.concat("get"));

    assertEquals(1, afterCall - beforeCall);
    assertEquals(1, afterTotalCall - beforeTotalCall);
  }

  private int getCounter(EntityManager em, String key) throws Exception {
    Query query = new Query();
    query.addCounterFilter(key + ":*:*:*");
    query.setStartTime(ts);
    query.setFinishTime(System.currentTimeMillis());
    query.setResolution(CounterResolution.ALL);
    Results r = em.getAggregateCounters(query);
    return (int) r.getCounters().get(0).getValues().get(0).getValue();
  }
}