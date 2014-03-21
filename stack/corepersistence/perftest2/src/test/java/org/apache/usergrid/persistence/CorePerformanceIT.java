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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Results;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO: make configurable, add CHOP markup.
 */
public class CorePerformanceIT {
    private static final Logger log = LoggerFactory.getLogger(CorePerformanceIT.class);

    // max entities we will write and read
    static int maxEntities = Integer.MAX_VALUE;

    // each app will get all data
    static int orgCount = 2;
    static int appCount = 5  ;

    // number of threads = orgCount x appCount 

    // total number of records = orgCount x appCount x numRecords

    private final EntityCollectionManagerFactory ecmf;
    private final EntityCollectionIndexFactory ecif;

    public CorePerformanceIT() {
        Injector injector = Guice.createInjector( new TestIndexModule() );
        ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
        ecif = injector.getInstance( EntityCollectionIndexFactory.class );
    }

    @Ignore
    @Test
    public void loadAndReadData() throws IOException, InterruptedException {

        ConfigurationManager.loadCascadedPropertiesFromResources( "usergrid" );

        // only on first run
        //MigrationManager m = injector.getInstance( MigrationManager.class )
        //m.migrate()

        log.info("Start Data Load");
        List<CollectionScope> scopes = loadData();
        log.info("Finish Data Load");

        log.info("Start Data Read");
        readData( scopes );
        log.info("Finish Data Read");

        runSelectedQueries( scopes );

    }


    private List<CollectionScope> loadData() throws InterruptedException {

        long time = new Date().getTime();

        List<CollectionScope> scopes = new ArrayList<CollectionScope>();
        List<Thread> threads = new ArrayList<Thread>();

        for ( int i=0; i<orgCount; i++ ) {

            String orgName = "org-" + i + "-" + time;
            final Id orgId = new SimpleId(orgName);

            for ( int j=0; j<appCount; j++ ) {

                String appName = "app-" + j + "-" + time;
                final Id appId = new SimpleId(appName);

                CollectionScope scope = new CollectionScopeImpl( orgId, appId, "reviews" );
                scopes.add( scope );

                Thread t = new Thread( new DataLoader( scope ));
                t.start();
                threads.add(t);
            }
        }

        // wait for indexing to end
        for ( Thread t : threads ) {
            t.join();
        }

        return scopes;
    }


    private void readData( List<CollectionScope> scopes ) throws InterruptedException {

        List<Thread> threads = new ArrayList<Thread>();
        for ( CollectionScope scope : scopes ) {

            Thread t = new Thread( new DataReader( scope ));
            t.start();
            threads.add(t);
        }

        // wait for reading to end
        for ( Thread t : threads ) {
            t.join();
        }
    }

    /**
     * @return the ecmf
     */
    public EntityCollectionManagerFactory getEcmf() {
        return ecmf;
    }

    /**
     * @return the ecif
     */
    public EntityCollectionIndexFactory getEcif() {
        return ecif;
    }


    private class DataReader implements Runnable {
        CollectionScope scope;

        public DataReader( CollectionScope scope ) {
            this.scope = scope;
        }

        public void run() {

            Id orgId = scope.getOrganization();
            Id appId = scope.getOwner();

            EntityCollectionManager ecm = getEcmf().createCollectionManager( scope );
            EntityCollectionIndex eci = getEcif().createCollectionIndex( scope );

            Query query = Query.fromQL( "review_score > 0"); // get all reviews;
            query.withLimit( maxEntities < 1000 ? maxEntities : 1000 );

            Results results = eci.execute( query );
            results.getEntities(); // cause retrieval from Cassandra
            int count = results.size();

            while ( results.hasCursor() && count < maxEntities ) {
                query.setCursor( results.getCursor() )   ;
                results = eci.execute( query );
                results.getEntities(); // cause retrieval from Cassanda;
                count += results.size();

                log.info("Read {} reviews in {} / {} ", count, orgId, appId );
            }
        }
    }


    private class DataLoader implements Runnable {
        CollectionScope scope;

        public DataLoader( CollectionScope scope ) {
            this.scope = scope;
        }

        public void run() {

            EntityCollectionManager ecm = getEcmf().createCollectionManager( scope );
            EntityCollectionIndex eci = getEcif().createCollectionIndex( scope );

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
            Entity current = new Entity(
                new SimpleId(UUIDGenerator.newTimeUUID(), "review")); 

            Id orgId = scope.getOrganization();
            Id appId = scope.getOwner();

            int count = 0;
            try {
                while ( (s = br.readLine()) != null && count < maxEntities ) {
                    
                    try {
                        
                        if ( s.trim().equals("")) { // then we are at end of a record
                            
                            // write and index current entity
                            ecm.write( current ).toBlockingObservable().last();
                            eci.index( current );
                            
                            if ( maxEntities < 20 ) {
                                log.info("Index written for {}", current.getId());
                                log.info("---");
                            }
                            
                            // create the next entity
                            current = new Entity(
                                    new SimpleId(UUIDGenerator.newTimeUUID(), "review"));
                            
                            count++;
                            if (count % 100000 == 0) {
                                log.info("Indexed {} reviews in {} / {} ", count, orgId, appId );
                            }
                            continue;
                        }
                        
                        // process a field
                        String name = s.substring( 0, s.indexOf(":")).replace("/", "_").toLowerCase() ;
                        String value = s.substring( s.indexOf(":") + 1 ).trim();
                        
                        if ( maxEntities < 20 ) {
                            log.info("Indexing {} = {}", name, value);
                        }
                        
                        if ( NumberUtils.isNumber(value) && value.contains(".")) {
                            current.setField( new DoubleField( name, Double.parseDouble(value)));
                            
                        } else if ( NumberUtils.isNumber(value) ) {
                            current.setField( new LongField( name, Long.parseLong(value)));
                            
                        } else {
                            current.setField( new StringField( name, value.toString() ));
                        }

                    } catch ( Exception e ) {
                        log.info("Error on line " + count);
                    }
                }

            } catch (IOException ex) {
                throw new RuntimeException("Error reading file", ex);
            }

            eci.refresh();
        }
    }   


    public void runSelectedQueries( List<CollectionScope> scopes ) { 

        for ( CollectionScope scope : scopes ) {

            EntityCollectionManager ecm = getEcmf().createCollectionManager( scope );
            EntityCollectionIndex eci = getEcif().createCollectionIndex( scope );

            // TODO: come up with more and more complex queries for CorePerformanceIT

            query(eci, "product_productid = 'B006K2ZZ7K'") ;
            query(eci, "review_profilename = 'Twoapennything'") ;
            query(eci, "review_profilename contains 'Natalia'") ;
            query(eci, "review_profilename contains 'Patrick'") ;
            query(eci, "review_time = 1342051200") ;
            query(eci, "review_time > 1342051200") ;
            query(eci, "review_score > 0");
            query(eci, "review_score > 2");
            query(eci, "review_score > 3");
            query(eci, "review_score > 4");
            query(eci, "review_score > 5");
        }
    }

    public static void query( EntityCollectionIndex eci, String query ) {;
        Query q = Query.fromQL(query) ;
        Results results = eci.execute( q );
        log.info("size = {} returned from query {}",results.size(), q.getQl() );
    }

}
