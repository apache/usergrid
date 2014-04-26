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
package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.scope.OrganizationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO: make CorePerformanceIT configurable, add CHOP markup.
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

    static EntityCollectionManagerFactory ecmf;
    static EntityIndexFactory ecif ;


    @Ignore
    @Test
    public void loadAndReadData() throws IOException, InterruptedException {

        ConfigurationManager.loadCascadedPropertiesFromResources( "usergrid" );
        Injector injector = Guice.createInjector( new TestIndexModule() );

        // only on first run
        //MigrationManager m = injector.getInstance( MigrationManager.class )
        //m.migrate()

        ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
        ecif = injector.getInstance( EntityIndexFactory.class );

        log.info("Start Data Load");
        List<OrgAppCollectionScope> scopes = loadData();
        log.info("Finish Data Load");

        log.info("Start Data Read");
        readData( scopes );
        log.info("Finish Data Read");

        runSelectedQueries( scopes );

    }

    private static class OrgAppCollectionScope {
        public OrganizationScope orgScope;
        public CollectionScope appScope;
        public CollectionScope scope;
        public OrgAppCollectionScope( 
                OrganizationScope orgScope, CollectionScope appScope, CollectionScope scope ) {
            this.orgScope = orgScope;
            this.appScope = appScope;
            this.scope = scope;
        }
    }

    private List<OrgAppCollectionScope> loadData() throws InterruptedException {

        long time = new Date().getTime();

        List<OrgAppCollectionScope> scopes = new ArrayList<OrgAppCollectionScope>();
        List<Thread> threads = new ArrayList<Thread>();

        for ( int i=0; i<orgCount; i++ ) {

            String orgName = "org-" + i + "-" + time;
            Id orgId = new SimpleId(orgName);
            OrganizationScope orgScope = new OrganizationScopeImpl(orgId);

            for ( int j=0; j<appCount; j++ ) {

                String appName = "app-" + j + "-" + time;
                Id appId = new SimpleId(appName);
                CollectionScope appScope = new CollectionScopeImpl( orgId, appId, appName );

                CollectionScope scope = new CollectionScopeImpl( orgId, appId, "reviews" );
                OrgAppCollectionScope orgAppScope = new OrgAppCollectionScope(orgScope, appScope, scope); 
                scopes.add( orgAppScope );

                Thread t = new Thread( new DataLoader( orgAppScope ));
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


    private void readData( List<OrgAppCollectionScope> scopes ) throws InterruptedException {

        List<Thread> threads = new ArrayList<Thread>();
        for ( OrgAppCollectionScope scope : scopes ) {

            Thread t = new Thread( new DataReader( scope ));
            t.start();
            threads.add(t);
        }

        // wait for reading to end
        for ( Thread t : threads ) {
            t.join();
        }
    }


    static class DataReader implements Runnable {
        OrgAppCollectionScope orgAppScope;

        public DataReader( OrgAppCollectionScope orgAppScope ) {
            this.orgAppScope = orgAppScope;
        }

        public void run() {

            Id orgId = orgAppScope.scope.getOrganization();
            Id appId = orgAppScope.scope.getOwner();

            EntityCollectionManager ecm = ecmf.createCollectionManager( orgAppScope.scope );
            EntityIndex eci = ecif.createEntityIndex(orgAppScope.orgScope, orgAppScope.appScope );

            Query query = Query.fromQL( "review_score > 0"); // get all reviews;
            query.withLimit( maxEntities < 1000 ? maxEntities : 1000 );

            Results results = eci.search( orgAppScope.scope, query );
            results.getEntities(); // cause retrieval from Cassandra
            int count = results.size();

            while ( results.hasCursor() && count < maxEntities ) {
                query.setCursor( results.getCursor() )   ;
                results = eci.search( orgAppScope.scope, query );
                results.getEntities(); // cause retrieval from Cassanda;
                count += results.size();

                log.info("Read {} reviews in {} / {} ", new String[] { ""+count, ""+orgId, ""+appId });
            }
        }
    }


    static class DataLoader implements Runnable {
        OrgAppCollectionScope orgAppScope;

        public DataLoader( OrgAppCollectionScope orgAppScope ) {
            this.orgAppScope = orgAppScope;
        }

        public void run() {

            EntityCollectionManager ecm = ecmf.createCollectionManager( orgAppScope.scope );
            EntityIndex eci = ecif.createEntityIndex(orgAppScope.orgScope, orgAppScope.appScope );

            FileReader fr;
            try {
                fr = new FileReader("../../resources/finefoods.txt");
            } catch (FileNotFoundException ex) {
                throw new RuntimeException("Error opening file", ex);
            }
            BufferedReader br = new BufferedReader(fr);
            String s = null;

            // create the first entry
            Entity current = new Entity(
                new SimpleId(UUIDGenerator.newTimeUUID(), "review")); 

            Id orgId = orgAppScope.scope.getOrganization();
            Id appId = orgAppScope.scope.getOwner();

            int count = 0;
            try {
                while ( (s = br.readLine()) != null && count < maxEntities ) {
                    
                    try {
                        
                        if ( s.trim().equals("")) { // then we are at end of a record
                            
                            // write and index current entity
                            ecm.write( current ).toBlockingObservable().last();
                            eci.index( orgAppScope.scope, current );
                            
                            if ( maxEntities < 20 ) {
                                log.info("Index written for {}", current.getId());
                                log.info("---");
                            }
                            
                            // create the next entity
                            current = new Entity(
                                    new SimpleId(UUIDGenerator.newTimeUUID(), "review"));
                            
                            count++;
                            if (count % 100000 == 0) {
                                log.info("Indexed {} reviews in {} / {} ", new String[] { count+"", orgId+"", appId+"" } );
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


    public void runSelectedQueries( List<OrgAppCollectionScope> orgAppScopes ) { 

        for ( OrgAppCollectionScope orgAppScope : orgAppScopes ) {

            EntityCollectionManager ecm = ecmf.createCollectionManager( orgAppScope.scope );
            EntityIndex eci = ecif.createEntityIndex(orgAppScope.orgScope, orgAppScope.appScope );

            // TODO: come up with more and more complex queries for CorePerformanceIT

            query(eci, orgAppScope.scope, "product_productid = 'B006K2ZZ7K'") ;
            query(eci, orgAppScope.scope, "review_profilename = 'Twoapennything'") ;
            query(eci, orgAppScope.scope, "review_profilename contains 'Natalia'") ;
            query(eci, orgAppScope.scope, "review_profilename contains 'Patrick'") ;
            query(eci, orgAppScope.scope, "review_time = 1342051200") ;
            query(eci, orgAppScope.scope, "review_time > 1342051200") ;
            query(eci, orgAppScope.scope, "review_score > 0");
            query(eci, orgAppScope.scope, "review_score > 2");
            query(eci, orgAppScope.scope, "review_score > 3");
            query(eci, orgAppScope.scope, "review_score > 4");
            query(eci, orgAppScope.scope, "review_score > 5");
        }
    }

    public static void query( EntityIndex eci, CollectionScope scope, String query ) {;
        Query q = Query.fromQL(query) ;
        Results results = eci.search( scope, q );
        log.info("size = {} returned from query {}",results.size(), q.getQl() );
    }

}
