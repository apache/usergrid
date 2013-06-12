/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.persistence.AbstractPersistenceTest;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

/**
 * @author tnine
 * 
 */
public class IteratingQueryTest extends AbstractPersistenceTest {


  @Test
  public void singleOrderByMaxLimit() throws Exception {
    UUID applicationId = createApplication("IteratingQueryTest", "singleOrderByMaxLimit");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    int size = 500;
    int queryLimit = Query.MAX_LIMIT;

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));
      em.create("test", entity);
    }

    Query query = new Query();
    query.addSort("created");
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    do {

      // now do simple ordering, should be returned in order
      results = em.searchCollection(em.getApplicationRef(), "tests", query);

      for (int i = 0; i < results.size(); i ++) {
        assertEquals(String.valueOf(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    assertEquals(size, count);

  }
  
  
  @Test
  public void singleOrderByIntersection() throws Exception {
    UUID applicationId = createApplication("IteratingQueryTest", "singleOrderByIntersection");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    int size = 700;
    int queryLimit = Query.MAX_LIMIT;
    
    //the number of entities that should be written including an intersection
    int intersectIncrement = 5;

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));
      //if we hit the increment, set this to true
      entity.put("intersect", i%intersectIncrement == 0);
      em.create("test", entity);
      
    }

    Query query = new Query();
    query.addSort("created");
    query.addEqualityFilter("intersect", true);
    query.setLimit(queryLimit);

    int count = 0;

    int name = 0;
    int maxPossibleResults = size/intersectIncrement;
    
    Results results;

    do {

      // now do simple ordering, should be returned in order
      results = em.searchCollection(em.getApplicationRef(), "tests", query);

      for (int i = 0; i < results.size(); i ++) {
        assertEquals(String.valueOf(name), results.getEntities().get(i).getName());
        count++;
        name += intersectIncrement;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    assertEquals(maxPossibleResults, count);
  }
  
  
  @Test
  public void singleOrderByComplexIntersection() throws Exception {
    UUID applicationId = createApplication("IteratingQueryTest", "singleOrderByComplexIntersection");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    int size = 2000;
    int queryLimit = Query.MAX_LIMIT;

    
    
    //the number of entities that should be written including an intersection
    int intersectIncrement = 5;
    int secondIncrement = 10;

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));
      //if we hit the increment, set this to true
      entity.put("intersect", i%intersectIncrement == 0);
      entity.put("intersect2", i%secondIncrement == 0);
      em.create("test", entity);
      
    }

    Query query = new Query();
    query.addSort("created");
    query.addEqualityFilter("intersect", true);
    query.addEqualityFilter("intersect2", true);
    query.setLimit(queryLimit);

    int count = 0;

    int name = 0;
    int maxPossibleResults = size/secondIncrement;
    
    Results results;

    do {

      // now do simple ordering, should be returned in order
      results = em.searchCollection(em.getApplicationRef(), "tests", query);

      for (int i = 0; i < results.size(); i ++) {
        assertEquals(String.valueOf(name), results.getEntities().get(i).getName());
        count++;
        name += secondIncrement;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    assertEquals(maxPossibleResults, count);
  }
}
