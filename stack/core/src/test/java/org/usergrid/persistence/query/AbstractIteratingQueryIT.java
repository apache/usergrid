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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.*;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.utils.JsonUtils;


import static org.junit.Assert.*;


/**
 * @author tnine
 * 
 */
public abstract class AbstractIteratingQueryIT
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractIteratingQueryIT.class);
    public static final boolean USE_DEFAULT_APPLICATION = false;

    @ClassRule
    public final static CassandraResource cassandraResource
            = CassandraResource.newWithAvailablePorts("coreManager");

    protected EntityManagerFactory emf;
    protected QueueManagerFactory qmf;


    public AbstractIteratingQueryIT()
    {
        logger.info( "Initializing test ..." );
        emf = cassandraResource.getBean( EntityManagerFactory.class );
        qmf = cassandraResource.getBean( QueueManagerFactory.class );
    }


    @BeforeClass
    public static void setup() throws Exception
    {
        logger.info( "setup" );
    }


    @AfterClass
    public static void teardown() throws Exception
    {
        logger.info( "teardown" );
    }



    public UUID createApplication( String organizationName, String applicationName ) throws Exception
    {
        if ( USE_DEFAULT_APPLICATION )
        {
            return CassandraService.DEFAULT_APPLICATION_ID;
        }

        return emf.createApplication( organizationName, applicationName );
    }


    public void dump( String name, Object obj )
    {
        if ( obj != null )
        {
            logger.info( name + ":\n" + JsonUtils.mapToFormattedJsonString(obj) );
        }
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


  protected void singleOrderByIntersection(IoHelper io) throws Exception {

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


  protected void singleOrderByComplexIntersection(IoHelper io) throws Exception {

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


  protected void singleOrderByNoIntersection(IoHelper io) throws Exception {
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

  protected void singleOrderByComplexUnion(IoHelper io) throws Exception {

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


  protected void singleOrderByNot(IoHelper io) throws Exception {

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
  

  protected void multiOrderBy(IoHelper io) throws Exception {

    io.doSetup();

    int size = 2000;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection

    Set<Entity> sortedResults = new TreeSet<Entity>(new Comparator<Entity>() {

      @Override
      public int compare(Entity o1, Entity o2) {
       boolean o1Boolean = (Boolean) o1.getProperty("boolean");
       boolean o2Boolean = (Boolean) o2.getProperty("boolean");
       
       if(o1Boolean != o2Boolean){
         if(o1Boolean){
           return -1;
         }
         
         return 1;
       }
       
       int o1Index = (Integer) o1.getProperty("index");
       int o2Index = (Integer) o2.getProperty("index");
       
       if(o1Index > o2Index){
         return 1;
       }else if (o2Index > o1Index){
         return -1;
       }
       
       return 0;
       
      }
    });


    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();

      String name = String.valueOf(i);
      boolean bool = i %2 == 0;
      entity.put("name", name);
      entity.put("boolean", bool);
      
      /**
       * we want them to be ordered from the "newest" time uuid to the oldec since we 
       * have a low cardinality value as the first second clause.  This way the test
       *won't accidentally pass b/c the UUID ordering matches the index ordering.  If we were
       *to reverse the value of index (size-i) the test would pass incorrectly
       */
      
      entity.put("index", i);
      
      Entity saved = io.writeEntity(entity);
      
      sortedResults.add(saved);

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = Query.fromQL("select * order by boolean desc, index asc");
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();
    
    Iterator<Entity> itr = sortedResults.iterator();

    do {

      // now do simple ordering, should be returned in order
      results = io.getResults(query);

      for (int i = 0; i < results.size(); i++) {
        Entity expected = itr.next();
        Entity returned = results.getEntities().get(i);
        
        assertEquals("Order incorrect", expected.getName(), returned.getName());
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(sortedResults.size(), count);
  }

  protected void multiOrderByComplexUnion(IoHelper io) throws Exception {

    io.doSetup();

    int size = 2000;
    int queryLimit = Query.MAX_LIMIT;

    // the number of entities that should be written including an intersection
    int intersectIncrement = 5;
    int secondIncrement = 9;

    long start = System.currentTimeMillis();

    logger.info("Writing {} entities.", size);

    Set<Entity> sortedResults = new TreeSet<Entity>(new Comparator<Entity>() {

      @Override
      public int compare(Entity o1, Entity o2) {
        long o1Index = (Long) o1.getProperty("created");
        long o2Index = (Long) o2.getProperty("created");

        if(o1Index > o2Index){
          return 1;
        }else if (o2Index > o1Index){
          return -1;
        }


        boolean o1Boolean = (Boolean) o1.getProperty("intersect");
        boolean o2Boolean = (Boolean) o2.getProperty("intersect");

        if(o1Boolean != o2Boolean){
          if(o1Boolean){
            return -1;
          }

          return 1;
        }



        return 0;

      }
    });

    for (int i = 0; i < size; i++) {
      Map<String, Object> entity = new HashMap<String, Object>();

      String name = String.valueOf(i);
      boolean intersect1 = i % intersectIncrement == 0;
      boolean intersect2 = i % secondIncrement == 0;
      entity.put("name", name);
      // if we hit the increment, set this to true

      entity.put("intersect", intersect1);
      entity.put("intersect2", intersect2);
      Entity e = io.writeEntity(entity);

      if (intersect1 || intersect2) {
        sortedResults.add(e);
      }

    }

    long stop = System.currentTimeMillis();

    logger.info("Writes took {} ms", stop - start);

    Query query = Query.fromQL("select * where intersect = true OR intersect2 = true order by created, intersect desc");
    query.setLimit(queryLimit);

    int count = 0;

    Results results;

    start = System.currentTimeMillis();

    Iterator<Entity> expected = sortedResults.iterator();

    do {

      // now do simple ordering, should be returned in order
      results = io.getResults(query);

      for (Entity result: results.getEntities()) {
        assertEquals(expected.next(), result);
        count++;
      }

      query.setCursor(results.getCursor());

    } while (results.getCursor() != null);

    stop = System.currentTimeMillis();

    logger.info("Query took {} ms to return {} entities", stop - start, count);

    assertEquals(sortedResults.size(), count);
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

  class CollectionIoHelper implements IoHelper {

    protected EntityManager em;
    private String appName;

    CollectionIoHelper(String appName) {
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
      UUID applicationId = createApplication("SingleOrderByMaxLimitCollection", appName);
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

  class ConnectionHelper extends CollectionIoHelper {

    /**
     * 
     */
    protected static final String CONNECTION = "connection";
    protected Entity rootEntity;

    ConnectionHelper(String name) {
      super(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.SingleOrderByMaxLimitCollection.CollectionIoHelper#
     * writeEntity(java.lang.String, java.util.Map)
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.persistence.query.SingleOrderByMaxLimitCollection.CollectionIoHelper#
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
     * org.usergrid.persistence.query.SingleOrderByMaxLimitCollection.CollectionIoHelper#
     * getResults(org.usergrid.persistence.Query)
     */
    @Override
    public Results getResults(Query query) throws Exception {
      query.setConnectionType(CONNECTION);
      query.setEntityType("test");
      return em.searchConnectedEntities(rootEntity, query);
    }

  }



}
