package org.apache.usergrid.persistence.collection.guice;


import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import static junit.framework.TestCase.assertNotNull;


/**
 */
@RunWith( JukitoRunner.class )
@UseModules( { CassandraModule.class } )
public class CassandraModuleTest {
    @Inject
    CassandraRule cassandraRule;


    @Test
    public void testCassandra() {
        assertNotNull( cassandraRule );
    }
}
