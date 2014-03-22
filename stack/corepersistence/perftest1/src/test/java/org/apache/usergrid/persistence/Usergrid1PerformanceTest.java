/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO: make configurable, add CHOP markup.
 */
public class Usergrid1PerformanceTest {
    private static final Logger log = LoggerFactory.getLogger(Usergrid1PerformanceTest.class);

    // max entities we will write and read
    static int maxEntities = Integer.MAX_VALUE;

    // each app will get all data
    static int orgCount = 2;
    static int appCount = 5  ;

    // number of threads = orgCount x appCount 

    // total number of records = orgCount x appCount x numRecords

    private final EntityManagerFactory emf;


    public Usergrid1PerformanceTest() throws Throwable {
        emf = UsergridBootstrap.newInstance().getBean( EntityManagerFactory.class );
    }
   

    public EntityManagerFactory getEmf() {
        return emf;
    }


    @Test
    public void loadAndReadData() throws Exception {

        log("Start Data Load");
        List<UUID> apps = loadData();
        log("Finish Data Load");

        log("Start Data Read");
        readData( apps );
        log("Finish Data Read");

        runSelectedQueries( apps );

    }


    private List<UUID> loadData() throws Exception {

        long time = new Date().getTime();

        List<UUID> apps = new ArrayList<UUID>();
        List<Thread> threads = new ArrayList<Thread>();

        for ( int i=0; i<orgCount; i++ ) {

            for ( int j=0; j<appCount; j++ ) {

                UUID appId = getEmf().createApplication(
                    "testorg-" + RandomStringUtils.randomAlphanumeric(6), 
                    "testapp-" + RandomStringUtils.randomAlphanumeric(6));

                apps.add( appId );

                Thread t = new Thread( new DataLoader( appId ));
                t.start();
                threads.add(t);
            }
        }

        // wait for indexing to end
        for ( Thread t : threads ) {
            t.join();
        }

        return apps;
    }


    private void readData( List<UUID> apps ) throws InterruptedException {

        List<Thread> threads = new ArrayList<Thread>();
        for ( UUID app : apps ) {

            Thread t = new Thread( new DataReader( app ));
            t.start();
            threads.add(t);
        }

        // wait for reading to end
        for ( Thread t : threads ) {
            t.join();
        }
    }


    private class DataReader implements Runnable {
        UUID app;

        public DataReader( UUID app ) {
            this.app = app;
        }

        public void run() {

            final EntityManager em;
            try {
                em = getEmf().getEntityManager( app );
            } catch (Throwable ex) {
                log.error("Error getting Entity Manager, aborting", ex);
                return;
            }

            UUID appId = app;

            Query query = Query.fromQL( "review_score > 0"); // get all reviews;
            query.withLimit( maxEntities < 1000 ? maxEntities : 1000 );

            Results results;
            try {
                results = em.searchCollection( em.getApplicationRef(), "reviews", query );
            } catch (Exception ex) {
                log.error("Error on search, aborting", ex);
                return;
            }

            results.getEntities(); // cause retrieval from Cassandra
            int count = results.size();

            while ( results.hasCursor() && count < maxEntities ) {
                query.setCursor( results.getCursor() )   ;
                try {
                    results = em.searchCollection( em.getApplicationRef(), "reviews", query );
                } catch (Exception ex) {
                    log.error("Error on search, aborting", ex);
                    log( String.format("Read %d reviews in %s", count, appId) );
                    return;
                }
                results.getEntities(); // cause retrieval from Cassanda;
                count += results.size();

                log( String.format("Read %d reviews in %s", count, appId.toString()) );
            }
        }
    }


    private class DataLoader implements Runnable {
        UUID app;

        public DataLoader( UUID scope ) {
            this.app = scope;
        }

        public void run() {

            final EntityManager em;
            try {
                em = getEmf().getEntityManager( app );
            } catch (Throwable ex) {
                log.error("Error getting Entity Manager, aborting", ex);
                return;
            }

            BufferedReader br;
            try {
                InputStreamReader isr = new InputStreamReader( 
                    getClass().getResourceAsStream("/finefoods.txt")); // TODO: make configurable
                br = new BufferedReader(isr);
            } catch (Exception ex) {
                throw new RuntimeException("Error opening file", ex);
            }
            String s = null;

            // create the first entry
            Map<String, Object> currentEntityMap = new HashMap<String, Object>();

            UUID appId = app;

            int count = 0;
            try {
                while ( (s = br.readLine()) != null && count < maxEntities ) {
                    
                    try {
                        
                        if ( s.trim().equals("")) { // then we are at end of a record
                            
                            // write and index current entity
                            Entity entity = em.create("review", currentEntityMap );
                            
                            if ( maxEntities < 20 ) {
                                log( String.format("Index written for %s", entity.getUuid().toString()));
                                log("---");
                            }
                            
                            // create the next entity
                            currentEntityMap = new HashMap<String, Object>();
                            
                            count++;
                            if (count % 100000 == 0) {
                                log( String.format("Indexed %d reviews in %s", count, appId.toString()) );
                            }
                            continue;
                        }
                        
                        // process a field
                        String name = s.substring( 0, s.indexOf(":")).replace("/", "_").toLowerCase() ;
                        String value = s.substring( s.indexOf(":") + 1 ).trim();
                        
                        if ( maxEntities < 20 ) {
                            log( String.format("Indexing %s = %s", name, value));
                        }
                        
                        if ( NumberUtils.isNumber(value) && value.contains(".")) {
                            currentEntityMap.put( name, Double.parseDouble(value));
                            
                        } else if ( NumberUtils.isNumber(value) ) {
                            currentEntityMap.put( name, Long.parseLong(value));
                            
                        } else {
                            currentEntityMap.put( name, value.toString() );
                        } 

                    } catch ( Exception e ) {
                        log("Error on line " + count);
                    }
                }

            } catch (IOException ex) {
                throw new RuntimeException("Error reading file", ex);
            }
        }
    }   


    public void runSelectedQueries( List<UUID> apps ) throws Exception { 

        for ( UUID app : apps ) {

            final EntityManager em;
            try {
                em = getEmf().getEntityManager( app );
            } catch (Throwable ex) {
                log.error("Error getting Entity Manager, aborting", ex);
                return;
            }

            query(em, "product_productid = 'B006K2ZZ7K'") ;
            query(em, "review_profilename = 'Twoapennything'") ;
            query(em, "review_profilename contains 'Natalia'") ;
            query(em, "review_profilename contains 'Patrick'") ;
            query(em, "review_time = 1342051200") ;
            query(em, "review_time > 1342051200") ;
            query(em, "review_score > 0");
            query(em, "review_score > 2");
            query(em, "review_score > 3");
            query(em, "review_score > 4");
            query(em, "review_score > 5");
        }
    }

    public void query( EntityManager em, String query ) throws Exception {
        Query q = Query.fromQL(query) ;
        Results results = em.searchCollection( em.getApplicationRef(), "reviews", q );
        log( String.format("size = %d returned from query %s", results.size(), q.getQl()) );
    }

    private void log( String s ) {
        //log.info(s);
        System.out.println( System.currentTimeMillis() + ": " + s );
    }
}
