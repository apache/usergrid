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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.AbstractPersistenceTest;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

/**
 * @author tnine
 * 
 */
public class IteratingQueryTest extends AbstractPersistenceTest {

  private static final Logger logger = LoggerFactory.getLogger(IteratingQueryTest.class);

  @Test
  public void singleOrderByMaxLimit() throws Exception {

    UUID applicationId = createApplication("IteratingQueryTest", "singleOrderByMaxLimit");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    int size = 500;
    int queryLimit = Query.MAX_LIMIT;

    long start = System.currentTimeMillis();
    
    logger.info("Writing {} entities.", size);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));
      em.create("test", entity);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("created");
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();

    do {

      // now do simple ordering, should be returned in order
      results = em.searchCollection(em.getApplicationRef(), "tests", query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(String.valueOf(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms", stop - start);

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

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;

    long start = System.currentTimeMillis();
    
    logger.info("Writing {} entities.", size);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));
      // if we hit the increment, set this to true
      entity.put("intersect", i % intersectIncrement == 0);
      em.create("test", entity);

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("created");
    query.addEqualityFilter("intersect", true);
    query.setLimit(queryLimit);

    int count = 0;

    int name = 0;
    int maxPossibleResults = size / intersectIncrement;

    Results results;

    start = System.currentTimeMillis();

    do {

      // now do simple ordering, should be returned in order
      results = em.searchCollection(em.getApplicationRef(), "tests", query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(String.valueOf(name), results.getEntities().get(i).getName());
        count++;
        name += intersectIncrement;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms", stop - start);

    assertEquals(maxPossibleResults, count);
  }

  @Test
  public void singleOrderByComplexIntersection() throws Exception {
    UUID applicationId = createApplication("IteratingQueryTest", "singleOrderByComplexIntersection");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    int size = 20000;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;
    int secondIncrement = 9;

    long start = System.currentTimeMillis();
    
    logger.info("Writing {} entities.", size);
    
    List<String> expectedResults = new ArrayList<String>(size/secondIncrement);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      
      String name =  String.valueOf(i);
      boolean intersect1 =  i % intersectIncrement == 0;
      boolean intersect2 = i % secondIncrement == 0;
      entity.put("name",name);
      // if we hit the increment, set this to true
      
      entity.put("intersect", intersect1);
      entity.put("intersect2", intersect2);
      em.create("test", entity);
      
      if(intersect1 && intersect2){
        expectedResults.add(name);
      }

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("created");
    query.addEqualityFilter("intersect", true);
    query.addEqualityFilter("intersect2", true);
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();

    do {

      // now do simple ordering, should be returned in order
      results = em.searchCollection(em.getApplicationRef(), "tests", query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expectedResults.get(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms", stop - start);

    assertEquals(expectedResults.size(), count);
  }
  
  @Test
  public void singleOrderByNoIntersection() throws Exception {
    UUID applicationId = createApplication("IteratingQueryTest", "singleOrderByNoIntersection");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    int size = 2000;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int secondIncrement = 9;

    long start = System.currentTimeMillis();
    
    logger.info("Writing {} entities.", size);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));
      // if we hit the increment, set this to true
      entity.put("intersect", false);
      entity.put("intersect2", i % secondIncrement == 0);
      em.create("test", entity);

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("created");
    //nothing will ever match this, the search should short circuit
    query.addEqualityFilter("intersect", true);
    query.addEqualityFilter("intersect2", true);
    query.setLimit(queryLimit);

    start = System.currentTimeMillis();
    
    Results  results = em.searchCollection(em.getApplicationRef(), "tests", query);


      // now do simple ordering, should be returned in order
   
    stop = System.currentTimeMillis();

    logger.info("Query took {} ms", stop - start);

    assertEquals(0, results.size());
  }
  
  
  @Test
  public void singleOrderByComplexUnion() throws Exception {
    UUID applicationId = createApplication("IteratingQueryTest", "singleOrderByComplexUnion");
    assertNotNull(applicationId);

    EntityManager em = emf.getEntityManager(applicationId);
    assertNotNull(em);

    int size = 200;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;
    int secondIncrement = 9;

    long start = System.currentTimeMillis();
    
    logger.info("Writing {} entities.", size);
    
    List<String> expectedResults = new ArrayList<String>(size/secondIncrement);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      
      String name =  String.valueOf(i);
      boolean intersect1 =  i % intersectIncrement == 0;
      boolean intersect2 = i % secondIncrement == 0;
      entity.put("name",name);
      // if we hit the increment, set this to true
      
      entity.put("intersect", intersect1);
      entity.put("intersect2", intersect2);
      em.create("test", entity);
      
      if(intersect1 || intersect2){
        expectedResults.add(name);
      }

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = Query.fromQL("select * where intersect = true OR intersect2 = true order by created");
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();

    do {

      // now do simple ordering, should be returned in order
      results = em.searchCollection(em.getApplicationRef(), "tests", query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expectedResults.get(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms", stop - start);

    assertEquals(expectedResults.size(), count);
  }
  
  

}
