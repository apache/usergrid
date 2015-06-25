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


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.math.NumberUtils;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.SearchTypes;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.index.query.EntityResults;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Guice;
import com.google.inject.Injector;



/**
 * TODO: make CorePerformanceIT configurable, add CHOP markup.
 */
public class CorePerformanceIT extends BaseIT {
    private static final Logger log = LoggerFactory.getLogger(CorePerformanceIT.class);

    @ClassRule
    public static ElasticSearchResource es = new ElasticSearchResource();

    // max entities we will write and read
    static int maxEntities = 10; // TODO: make this configurable when you add Chop

    // each app will get all data
    static int appCount = 10;

    // number of threads = orgCount x appCount

    // total number of records = orgCount x appCount x numRecords

    static EntityCollectionManagerFactory ecmf;
    static EntityIndexFactory ecif ;


    @Ignore("Relies on finefoods.txt which must be downloaded separately")
    @Test
    public void loadAndReadData() throws IOException, InterruptedException {

        Injector injector = Guice.createInjector( new TestIndexModule() );

        // only on first run
        //MigrationManager m = injector.getInstance( MigrationManager.class )
        //m.migrate()

        ecmf = injector.getInstance( EntityCollectionManagerFactory.class );
        ecif = injector.getInstance( EntityIndexFactory.class );

        final ApplicationScope scope = new ApplicationScopeImpl( new SimpleId( "application" ) );

        log.info("Start Data Load");

        List<IndexScope> scopes = loadData(scope);

        log.info("Finish Data Load");

        log.info("Start Data Read");


        readData( scope, scopes );
        log.info("Finish Data Read");

        runSelectedQueries( scope, scopes );

    }


    private List<IndexScope> loadData(final ApplicationScope applicationScope) throws InterruptedException {

        long time = new Date().getTime();

        List<IndexScope> scopes = new ArrayList<IndexScope>();
        List<Thread> threads = new ArrayList<Thread>();


        for ( int j = 0; j < appCount; j++ ) {

            String appName = "app-" + j + "-" + time;
            Id appId = new SimpleId( appName );
            IndexScope indexScope = new IndexScopeImpl( appId, "reviews");
            scopes.add( indexScope );

            Thread t = new Thread( new DataLoader( applicationScope, indexScope ) );
            t.start();
            threads.add( t );
        }

        // wait for indexing to end
        for ( Thread t : threads ) {
            t.join();
        }

        return scopes;
    }


    private void readData(final ApplicationScope applicationScope,  List<IndexScope> scopes ) throws InterruptedException {

        List<Thread> threads = new ArrayList<Thread>();
        for ( IndexScope scope : scopes ) {

            Thread t = new Thread( new DataReader( applicationScope, scope ));
            t.start();
            threads.add(t);
        }

        // wait for reading to end
        for ( Thread t : threads ) {
            t.join();
        }
    }


    static class DataReader implements Runnable {
        final ApplicationScope scope;
       final  IndexScope indexScope;

        public DataReader( final ApplicationScope scope, IndexScope indexScope ) {
            this.scope = scope;
            this.indexScope = indexScope;
        }

        public void run() {

            EntityIndex eci =   ecif.createEntityIndex( scope);
            EntityCollectionManager ecm = ecmf.createCollectionManager( new CollectionScopeImpl( scope.getApplication(), indexScope.getOwner(), indexScope.getName() ) );

            Query query = Query.fromQL( "review_score > 0"); // get all reviews;
            query.withLimit( maxEntities < 1000 ? maxEntities : 1000 );

            final SearchTypes searchType = SearchTypes.fromTypes( "review" );

            CandidateResults candidateResults = eci.search(indexScope, searchType, query );
            int count = candidateResults.size();

            while ( candidateResults.hasCursor() && count < maxEntities ) {
                query.setCursor( candidateResults.getCursor() )   ;
                candidateResults = eci.search(indexScope, searchType,  query );
                count += candidateResults.size();

                //cause retrieval from cassandra
                EntityResults entityResults = new EntityResults(
                    candidateResults, ecm, UUIDGenerator.newTimeUUID() );

                while(entityResults.hasNext()){
                    entityResults.next();
                }

                log.info("Read {} reviews in {} / {} ", new Object[] {
                    count, indexScope.getOwner(), indexScope.getName() } );
            }
        }
    }


    static class DataLoader implements Runnable {
        final ApplicationScope applicationScope;
        final IndexScope indexScope;

        public DataLoader( final ApplicationScope applicationScope, IndexScope indexScope ) {
            this.applicationScope = applicationScope;
            this.indexScope = indexScope;
        }

        public void run() {

            CollectionScope collectionScope = new CollectionScopeImpl(
                    applicationScope.getApplication(), indexScope.getOwner(), indexScope.getName() );
            EntityCollectionManager ecm = ecmf.createCollectionManager(collectionScope );
            EntityIndex eci = ecif.createEntityIndex(applicationScope );

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

//            Id orgId = orgAppScope.scope.getApplication();
//            Id appId = orgAppScope.scope.getOwner();

            int count = 0;

            EntityIndexBatch entityIndexBatch = eci.createBatch();

            try {
                while ( (s = br.readLine()) != null && count < maxEntities ) {

                    try {

                        if ( s.trim().equals("")) { // then we are at end of a record

                            // write and index current entity
                            ecm.write( current ).toBlocking().last();

                            entityIndexBatch.index(indexScope, current  );

                            if ( maxEntities < 20 ) {
                                log.info("Index written for {}", current.getId());
                                log.info("---");
                            }

                            // create the next entity
                            current = new Entity(
                                    new SimpleId(UUIDGenerator.newTimeUUID(), "review"));

                            count++;
                            if(count % 1000 == 0){
                                entityIndexBatch.execute().get();
                            }

                            if (count % 100000 == 0) {
                                log.info("Indexed {} reviews in {} / {} ",
                                    new Object[] {
                                        count,
                                            applicationScope,
                                        indexScope.getOwner() } );
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


    public void runSelectedQueries(final ApplicationScope scope,  List<IndexScope> indexScopes ) {

        for ( IndexScope indexScope : indexScopes ) {
            EntityIndex eci = ecif.createEntityIndex(scope );

            // TODO: come up with more and more complex queries for CorePerformanceIT

            query(indexScope, eci, "product_productid = 'B006K2ZZ7K'") ;
            query(indexScope, eci, "review_profilename = 'Twoapennything'") ;
            query(indexScope, eci, "review_profilename contains 'Natalia'") ;
            query(indexScope, eci, "review_profilename contains 'Patrick'") ;
            query(indexScope, eci, "review_time = 1342051200") ;
            query(indexScope, eci, "review_time > 1342051200") ;
            query(indexScope, eci, "review_score > 0");
            query(indexScope, eci, "review_score > 2");
            query(indexScope, eci, "review_score > 3");
            query(indexScope, eci, "review_score > 4");
            query(indexScope, eci, "review_score > 5");
        }
    }

    public static void query(final IndexScope indexScope, final EntityIndex eci, final String query ) {;
        Query q = Query.fromQL(query) ;
//        CandidateResults candidateResults = eci.search(indexScope,  q );  TODO FIXME
//        log.info("size = {} returned from query {}", candidateResults.size(), q.getQl() );
    }

}
