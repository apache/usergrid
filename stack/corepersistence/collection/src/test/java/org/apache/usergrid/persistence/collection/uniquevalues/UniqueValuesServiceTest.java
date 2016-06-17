package org.apache.usergrid.persistence.collection.uniquevalues;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.exception.WriteUniqueVerifyException;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Test the unique values service.
 */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class UniqueValuesServiceTest {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValuesServiceTest.class );

    @Inject
    private EntityCollectionManagerFactory factory;

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    AkkaFig akkaFig;

    @Inject
    UniqueValuesService uniqueValuesService;

    private static AtomicBoolean startedAkka = new AtomicBoolean( false );

    int numThreads = 6;
    int poolSize = 5;
    int numUsers = 100;

    @Before
    public void initAkka() {
        if ( !startedAkka.getAndSet( true ) ) {
            ApplicationScope context = new ApplicationScopeImpl( new SimpleId( "organization" ) );
            EntityCollectionManager manager = factory.createCollectionManager( context );
            manager.startAkkaForTesting( "127.0.0.1", 2554, "us-east" );
        }
    }


    /**
     * Use multiple threads to attempt to create entities with duplicate usernames.
     */
    @Test
    public void testDuplicatePrevention() throws Exception {

        initAkka();

        final AtomicInteger successCounter = new AtomicInteger( 0 );
        final AtomicInteger errorCounter = new AtomicInteger( 0 );

        Multimap<String, Entity> usersCreated =
            generateDuplicateUsers( numUsers, successCounter, errorCounter );

        int userCount = 0;
        int usernamesWithDuplicates = 0;
        for ( String username : usersCreated.keySet() ) {
            Collection<Entity> users = usersCreated.get( username );
            if ( users.size() > 1 ) {
                usernamesWithDuplicates++;
            }
            userCount++;
        }

        Assert.assertEquals( 0, usernamesWithDuplicates );

        Assert.assertEquals( numUsers, successCounter.get() );
        Assert.assertEquals( 0, errorCounter.get() );
        Assert.assertEquals( numUsers, usersCreated.size() );
        Assert.assertEquals( numUsers, userCount );
    }


    private Multimap<String, Entity> generateDuplicateUsers(
        int numUsers, AtomicInteger successCounter, AtomicInteger errorCounter ) {

        ApplicationScope context = new ApplicationScopeImpl( new SimpleId( "organization" ) );

        EntityCollectionManager manager = factory.createCollectionManager( context );

        Multimap<String, Entity> usersCreated =
                Multimaps.synchronizedListMultimap( ArrayListMultimap.create() );

        ExecutorService execService = Executors.newFixedThreadPool( poolSize );

        for (int i = 0; i < numUsers; i++) {

            // multiple threads simultaneously trying to create a user with the same propertyName
            for (int j = 0; j < numThreads; j++) {
                String username = "user_" + i;

                execService.submit( () -> {

                    try {

                        // give entity two unqiue fields username and email
                        Entity newEntity = new Entity( new SimpleId( "user" ) );
                        newEntity.setField( new StringField( "username", username, true ) );
                        newEntity.setField( new StringField( "email", username + "@example.org", true ) );

                        Observable<Entity> observable = manager.write( newEntity, null );
                        Entity returned = observable.toBlocking().lastOrDefault( null );

                        usersCreated.put( username, newEntity );
                        successCounter.incrementAndGet();

                        logger.debug("Created user {}", username);

                    } catch ( Throwable t ) {
                        if ( t instanceof WriteUniqueVerifyException) {
                            // we expect lots of these
                        } else {
                            errorCounter.incrementAndGet();
                            logger.error( "Error creating user " + username, t );
                        }
                    }

                } );
            }
        }
        execService.shutdown();

        try {
            while (!execService.awaitTermination( 60, TimeUnit.SECONDS )) {
                System.out.println( "Waiting..." );
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return usersCreated;
    }
}
