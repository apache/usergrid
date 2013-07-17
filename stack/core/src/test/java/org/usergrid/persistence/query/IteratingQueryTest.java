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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.AbstractPersistenceTest;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

/**
 * @author tnine
 * 
 */
public class IteratingQueryTest extends AbstractPersistenceTest {

  private static final Logger logger = LoggerFactory.getLogger(IteratingQueryTest.class);

  @Test
  public void singleOrderByMaxLimitCollection() throws Exception {
    singleOrderByMaxLimit(new CollectionIoHelper("singleOrderByMaxLimitCollection"));
  }

  @Test
  public void singleOrderByMaxLimitConnection() throws Exception {
    singleOrderByMaxLimit(new ConnectionHelper("singleOrderByMaxLimitConnection"));
  }

  public void singleOrderByMaxLimit(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = Query.MAX_LIMIT;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));

      io.writeEntity(entity);
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
      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(String.valueOf(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(size, count);

  }

  @Test
  public void singleOrderByIntersectionCollection() throws Exception {
    singleOrderByIntersection(new CollectionIoHelper("singleOrderByIntersectionCollection"));
  }

  @Test
  public void singleOrderByIntersectionConnection() throws Exception {
    singleOrderByIntersection(new ConnectionHelper("singleOrderByIntersectionConnection"));
  }

  private void singleOrderByIntersection(IoHelper io) throws Exception {

    io.doSetup();

    int size = 700;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;

    long start = System.currentTimeMillis();

    List<String> expected = new ArrayList<String>(size / intersectIncrement);

    logger.info("Writing {} entities.", size);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();

      String name = String.valueOf(i);

      boolean intersect = i % intersectIncrement == 0;

      entity.put("name", String.valueOf(i));
      // if we hit the increment, set this to true
      entity.put("intersect", intersect);

      io.writeEntity(entity);

      if (intersect) {
        expected.add(name);
      }

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("created");
    query.addEqualityFilter("intersect", true);
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();

    do {

      // now do simple ordering, should be returned in order
      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expected.get(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(expected.size(), count);
  }

  @Test
  public void singleOrderByComplexIntersectionCollection() throws Exception {
    singleOrderByComplexIntersection(new CollectionIoHelper("singleOrderByComplexIntersectionCollection"));
  }

  @Test
  public void singleOrderByComplexIntersectionConnection() throws Exception {
    singleOrderByComplexIntersection(new ConnectionHelper("singleOrderByComplexIntersectionConnection"));
  }

  private void singleOrderByComplexIntersection(IoHelper io) throws Exception {

    int size = 5000;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;
    int secondIncrement = 9;

    long start = System.currentTimeMillis();

    io.doSetup();

    logger.info("Writing {} entities.", size);

    List<String> expectedResults = new ArrayList<String>(size / secondIncrement);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();

      String name = String.valueOf(i);
      boolean intersect1 = i % intersectIncrement == 0;
      boolean intersect2 = i % secondIncrement == 0;
      entity.put("name", name);
      // if we hit the increment, set this to true

      entity.put("intersect", intersect1);
      entity.put("intersect2", intersect2);
      io.writeEntity(entity);

      if (intersect1 && intersect2) {
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
      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expectedResults.get(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(expectedResults.size(), count);
  }

  @Test
  public void singleOrderByNoIntersectionCollection() throws Exception {
    singleOrderByNoIntersection(new CollectionIoHelper("singleOrderByNoIntersectionCollection"));
  }

  @Test
  public void singleOrderByNoIntersectionConnection() throws Exception {
    singleOrderByNoIntersection(new CollectionIoHelper("singleOrderByNoIntersectionConnection"));
  }

  private void singleOrderByNoIntersection(IoHelper io) throws Exception {
    io.doSetup();

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
      io.writeEntity(entity);

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("created");
    // nothing will ever match this, the search should short circuit
    query.addEqualityFilter("intersect", true);
    query.addEqualityFilter("intersect2", true);
    query.setLimit(queryLimit);

    start = System.currentTimeMillis();

    Results results = io.getResults(query);

    // now do simple ordering, should be returned in order

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms to return {} entities", stop - start, 0);

    assertEquals(0, results.size());
  }

  @Test
  public void singleOrderByComplexUnionCollection() throws Exception {
    singleOrderByComplexUnion(new CollectionIoHelper("singleOrderByComplexUnionCollection"));
  }

  @Test
  public void singleOrderByComplexUnionConnection() throws Exception {
    singleOrderByComplexUnion(new ConnectionHelper("singleOrderByComplexUnionConnection"));
  }

  private void singleOrderByComplexUnion(IoHelper io) throws Exception {

    io.doSetup();

    int size = 2000;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;
    int secondIncrement = 9;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expectedResults = new ArrayList<String>(size / secondIncrement);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();

      String name = String.valueOf(i);
      boolean intersect1 = i % intersectIncrement == 0;
      boolean intersect2 = i % secondIncrement == 0;
      entity.put("name", name);
      // if we hit the increment, set this to true

      entity.put("intersect", intersect1);
      entity.put("intersect2", intersect2);
      io.writeEntity(entity);

      if (intersect1 || intersect2) {
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
      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expectedResults.get(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(expectedResults.size(), count);
  }

  @Test
  public void singleOrderByNotCollection() throws Exception {
    singleOrderByNot(new CollectionIoHelper("singleOrderByNotCollection"));
  }

  @Test
  public void singleOrderByNotConnection() throws Exception {
    singleOrderByNot(new ConnectionHelper("singleOrderByNotConnection"));
  }

  private void singleOrderByNot(IoHelper io) throws Exception {

    io.doSetup();

    int size = 2000;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;
    int secondIncrement = 9;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expectedResults = new ArrayList<String>(size / secondIncrement);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();

      String name = String.valueOf(i);
      boolean intersect1 = i % intersectIncrement == 0;
      boolean intersect2 = i % secondIncrement == 0;
      entity.put("name", name);
      // if we hit the increment, set this to true

      entity.put("intersect", intersect1);
      entity.put("intersect2", intersect2);
      io.writeEntity(entity);

      if (!(intersect1 && intersect2)) {
        expectedResults.add(name);
      }

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = Query.fromQL("select * where NOT (intersect = true AND intersect2 = true) order by created");
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();

    do {

      // now do simple ordering, should be returned in order
      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expectedResults.get(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(expectedResults.size(), count);
  }

  @Test
  public void singleOrderByLessThanLimitCollection() throws Exception {
    singleOrderByLessThanLimit(new CollectionIoHelper("singleOrderByLessThanLimitCollection"));
  }

  @Test
  public void singleOrderByLessThanLimitConnection() throws Exception {
    singleOrderByLessThanLimit(new ConnectionHelper("singleOrderByLessThanLimitConnection"));
  }

  public void singleOrderByLessThanLimit(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = Query.MAX_LIMIT;

    int matchMax = queryLimit - 1;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expected = new ArrayList<String>(matchMax);

    for (int i = 0; i < size; i++) {
      String name = String.valueOf(i);
      boolean searched = i < matchMax;

      Map<String, Object> entity = new HashMap<String, Object>();

      entity.put("name", name);
      entity.put("searched", searched);
      io.writeEntity(entity);

      if (searched) {
        expected.add(name);
      }

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("created");
    query.setLimit(queryLimit);
    query.addEqualityFilter("searched", true);

    int count = 0;

    start = System.currentTimeMillis();

    // now do simple ordering, should be returned in order
    Results results = io.getResults(query);

    for (int i = 0; i < results.size(); i++) {
      assertEquals(expected.get(count), results.getEntities().get(i).getName());
      count++;
    }

    assertTrue(results.getCursor() == null);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(expected.size(), count);

  }

  @Test
  public void singleOrderBySameRangeScanLessThanEqualCollection() throws Exception {
    singleOrderBySameRangeScanLessThanEqual(new CollectionIoHelper("singleOrderBySameRangeScanLessThanEqualCollection"));
  }

  @Test
  public void singleOrderBySameRangeScanLessThanEqualConnection() throws Exception {
    singleOrderBySameRangeScanLessThanEqual(new ConnectionHelper("singleOrderBySameRangeScanLessThanEqualConnection"));
  }

  public void singleOrderBySameRangeScanLessThanEqual(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = 100;
    int startValue = 400;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expected = new ArrayList<String>(size);

    for (int i = 0; i < size; i++) {
      String name = String.valueOf(i);

      Map<String, Object> entity = new HashMap<String, Object>();

      entity.put("name", name);
      entity.put("index", i);
      io.writeEntity(entity);
      expected.add(name);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("index desc");
    query.addLessThanEqualFilter("index", startValue);
    query.setLimit(queryLimit);

    int count = 0;
    int delta = size - startValue;
    
    start = System.currentTimeMillis();

    // now do simple ordering, should be returned in order
    Results results = null;

    do {

      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expected.get(size - delta - count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.hasCursor());
    
    assertEquals(expected.size()-delta+1, count);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

  }
  

  @Test
  public void singleOrderBySameRangeScanLessCollection() throws Exception {
    singleOrderBySameRangeScanLessEqual(new CollectionIoHelper("singleOrderBySameRangeScanLessCollection"));
  }

  @Test
  public void singleOrderBySameRangeScanLessConnection() throws Exception {
    singleOrderBySameRangeScanLessEqual(new ConnectionHelper("singleOrderBySameRangeScanLessConnection"));
  }

  public void singleOrderBySameRangeScanLessEqual(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = 100;
    int startValue = 400;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expected = new ArrayList<String>(size);

    for (int i = 0; i < size; i++) {
      String name = String.valueOf(i);

      Map<String, Object> entity = new HashMap<String, Object>();

      entity.put("name", name);
      entity.put("index", i);
      io.writeEntity(entity);
      expected.add(name);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("index desc");
    query.addLessThanFilter("index", startValue);
    query.setLimit(queryLimit);

    int count = 0;
    int delta = size - startValue;
    
    start = System.currentTimeMillis();

    // now do simple ordering, should be returned in order
    Results results = null;

    do {

      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expected.get(size - delta - count - 1), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.hasCursor());
    
    assertEquals(expected.size()-delta, count);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

  }
  
  

  @Test
  public void singleOrderBySameRangeScanGreaterThanEqualCollection() throws Exception {
    singleOrderBySameRangeScanGreaterThanEqual(new CollectionIoHelper("singleOrderBySameRangeScanGreaterThanEqualCollection"));
  }

  @Test
  public void singleOrderBySameRangeScanGreaterThanEqualConnection() throws Exception {
    singleOrderBySameRangeScanGreaterThanEqual(new ConnectionHelper("singleOrderBySameRangeScanGreaterThanEqualConnection"));
  }

  public void singleOrderBySameRangeScanGreaterThanEqual(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = 100;
    int startValue = 100;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expected = new ArrayList<String>(size);

    for (int i = 0; i < size; i++) {
      String name = String.valueOf(i);

      Map<String, Object> entity = new HashMap<String, Object>();

      entity.put("name", name);
      entity.put("index", i);
      io.writeEntity(entity);
      expected.add(name);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("index desc");
    query.addGreaterThanEqualFilter("index", startValue);
    query.setLimit(queryLimit);

    int count = 0;

    start = System.currentTimeMillis();

    // now do simple ordering, should be returned in order
    Results results = null;

    do {

      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expected.get(size - count - 1), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.hasCursor());
    
    assertEquals(expected.size()-startValue, count);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

  }
  

  @Test
  public void singleOrderBySameRangeScanGreaterCollection() throws Exception {
    singleOrderBySameRangeScanGreater(new CollectionIoHelper("singleOrderBySameRangeScanGreaterCollection"));
  }

  @Test
  public void singleOrderBySameRangeScanGreaterConnection() throws Exception {
    singleOrderBySameRangeScanGreater(new ConnectionHelper("singleOrderBySameRangeScanGreaterConnection"));
  }

  public void singleOrderBySameRangeScanGreater(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = 100;
    int startValue = 99;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expected = new ArrayList<String>(size);

    for (int i = 0; i < size; i++) {
      String name = String.valueOf(i);

      Map<String, Object> entity = new HashMap<String, Object>();

      entity.put("name", name);
      entity.put("index", i);
      io.writeEntity(entity);
      expected.add(name);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.addSort("index desc");
    query.addGreaterThanFilter("index", startValue);
    query.setLimit(queryLimit);

    int count = 0;

    start = System.currentTimeMillis();

    // now do simple ordering, should be returned in order
    Results results = null;

    do {

      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expected.get(size - count - 1), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.hasCursor());
    
    assertEquals(expected.size()-startValue-1, count);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

  }
  
  

  @Test
  public void singleOrderByBoundRangeScanDescCollection() throws Exception {
    singleOrderByBoundRangeScanDesc(new CollectionIoHelper("singleOrderByBoundRangeScanDescCollection"));
  }

  @Test
  public void singleOrderByBoundRangeScanDescConnection() throws Exception {
    singleOrderByBoundRangeScanDesc(new ConnectionHelper("singleOrderByBoundRangeScanDescConnection"));
  }

  public void singleOrderByBoundRangeScanDesc(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = 100;
    int startValue = 100;
    int endValue = 400;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expected = new ArrayList<String>(size);

    for (int i = 0; i < size; i++) {
      String name = String.valueOf(i);

      Map<String, Object> entity = new HashMap<String, Object>();

      entity.put("name", name);
      entity.put("index", i);
      io.writeEntity(entity);
      expected.add(name);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = Query.fromQL(String.format("select * where index >= %d AND index <= %d order by index desc", startValue, endValue));
    query.setLimit(queryLimit);

    int count = 0;
    int delta = size - endValue;

    start = System.currentTimeMillis();

    // now do simple ordering, should be returned in order
    Results results = null;

    do {

      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expected.get(size - count - delta), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.hasCursor());
    
    assertEquals(expected.size()-startValue - delta + 1, count);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

  }
  
  

  @Test
  public void singleOrderByBoundRangeScanAscCollection() throws Exception {
    singleOrderByBoundRangeScanAsc(new CollectionIoHelper("singleOrderByBoundRangeScanAscCollection"));
  }

  @Test
  public void singleOrderByBoundRangeScanAscConnection() throws Exception {
    singleOrderByBoundRangeScanAsc(new ConnectionHelper("singleOrderByBoundRangeScanAscConnection"));
  }

  public void singleOrderByBoundRangeScanAsc(IoHelper io) throws Exception {

    io.doSetup();

    int size = 500;
    int queryLimit = 100;
    int startValue = 100;
    int endValue = 400;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    List<String> expected = new ArrayList<String>(size);

    for (int i = 0; i < size; i++) {
      String name = String.valueOf(i);

      Map<String, Object> entity = new HashMap<String, Object>();

      entity.put("name", name);
      entity.put("index", i);
      io.writeEntity(entity);
      expected.add(name);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = Query.fromQL(String.format("select * where index >= %d AND index <= %d order by index asc", startValue, endValue));
    query.setLimit(queryLimit);

    int count = 0;
    int delta = size - endValue;

    start = System.currentTimeMillis();

    // now do simple ordering, should be returned in order
    Results results = null;

    do {

      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(expected.get(delta+count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.hasCursor());
    
    assertEquals(expected.size()-startValue - delta + 1, count);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

  }
  
  

  @Test
  public void allInCollection() throws Exception {
    allIn(new CollectionIoHelper("allInCollection"));
  }

  @Test
  public void allInConnection() throws Exception {
    allIn(new ConnectionHelper("allInConnection"));
  }

  @Test
  public void allInConnectionNoType() throws Exception {
    allIn(new ConnectionNoTypeHelper("allInConnectionNoType"));
  }

  /**
   * Tests that when an empty query is issued, we page through all entities
   * correctly
   * 
   * @param io
   * @throws Exception
   */
  public void allIn(IoHelper io) throws Exception {

    io.doSetup();

    int size = 300;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();
      entity.put("name", String.valueOf(i));

      io.writeEntity(entity);
    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = new Query();
    query.setLimit(100);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();

    do {

      // now do simple ordering, should be returned in order
      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        assertEquals(String.valueOf(count), results.getEntities().get(i).getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();
    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(size, count);

  }

  /**
   * Interface to abstract actually doing I/O targets. The same test logic can
   * be applied to both collections and connections
   * 
   * @author tnine
   * 
   */
  private interface IoHelper {

    /**
     * Sets the entity manager to user
     * 
     * @param em
     */
    public void setEntityManager(EntityManager em);

    /**
     * Perform any setup required
     * 
     * @throws Exception
     */
    public void doSetup() throws Exception;

    /**
     * Write the entity to the data store
     * 
     * @param entity
     * @throws Exception
     */
    public Entity writeEntity(Map<String, Object> entity) throws Exception;

    /**
     * Get the results for the query
     * 
     * @param query
     * @return
     * @throws Exception
     */
    public Results getResults(Query query) throws Exception;

  }

  private class CollectionIoHelper implements IoHelper {

    protected EntityManager em;
    private String appName;

    private CollectionIoHelper(String appName) {
      this.appName = appName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.IoHelper2#setEntityManager(org.usergrid
     * .persistence.EntityManager)
     */
    @Override
    public void setEntityManager(EntityManager em) {
      this.em = em;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.usergrid.persistence.query.IoHelper2#doSetup()
     */
    @Override
    public void doSetup() throws Exception {
      UUID applicationId = createApplication("IteratingQueryTest", appName);
      assertNotNull(applicationId);

      em = emf.getEntityManager(applicationId);
      assertNotNull(em);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.IoHelper2#writeEntity(java.lang.String,
     * java.util.Map)
     */
    @Override
    public Entity writeEntity(Map<String, Object> entity) throws Exception {
      return em.create("test", entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.IoHelper2#getResults(org.usergrid.persistence
     * .Query)
     */
    @Override
    public Results getResults(Query query) throws Exception {
      return em.searchCollection(em.getApplicationRef(), "tests", query);
    }
  }

  private class ConnectionHelper extends CollectionIoHelper {

    /**
     * 
     */
    protected static final String CONNECTION = "connection";
    protected Entity rootEntity;

    private ConnectionHelper(String name) {
      super(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.IteratingQueryTest.CollectionIoHelper#
     * writeEntity(java.lang.String, java.util.Map)
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.IteratingQueryTest.CollectionIoHelper#
     * doSetup()
     */
    @Override
    public void doSetup() throws Exception {
      super.doSetup();

      Map<String, Object> data = new HashMap<String, Object>();
      data.put("name", "rootentity");
      rootEntity = em.create("root", data);
    }

    @Override
    public Entity writeEntity(Map<String, Object> entity) throws Exception {
      // write to the collection
      Entity created = super.writeEntity(entity);

      em.createConnection(rootEntity, CONNECTION, created);

      return created;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.IteratingQueryTest.CollectionIoHelper#
     * getResults(org.usergrid.persistence.Query)
     */
    @Override
    public Results getResults(Query query) throws Exception {
      query.setConnectionType(CONNECTION);
      query.setEntityType("test");
      return em.searchConnectedEntities(rootEntity, query);
    }

  }

  private class ConnectionNoTypeHelper extends ConnectionHelper {

    private ConnectionNoTypeHelper(String name) {
      super(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.IteratingQueryTest.ConnectionHelper#getResults
     * (org.usergrid.persistence.Query)
     */
    @Override
    public Results getResults(Query query) throws Exception {
      query.setConnectionType(CONNECTION);
      // don't set it on purpose
      query.setEntityType(null);
      return em.searchConnectedEntities(rootEntity, query);

    }

  }
}
