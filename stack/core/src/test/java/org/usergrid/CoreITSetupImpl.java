package org.usergrid;


import java.util.UUID;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.mq.QueueManagerFactory;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.utils.JsonUtils;


public class CoreITSetupImpl implements CoreITSetup {
    private static final Logger LOG = LoggerFactory.getLogger( CoreITSetupImpl.class );

    protected EntityManagerFactory emf;
    protected QueueManagerFactory qmf;
    protected IndexBucketLocator indexBucketLocator;
    protected CassandraService cassandraService;
    protected CassandraResource cassandraResource;
    protected boolean enabled = false;


    public CoreITSetupImpl( CassandraResource cassandraResource ) {
        this.cassandraResource = cassandraResource;
    }


    @Override
    public Statement apply( Statement base, Description description ) {
        return statement( base, description );
    }


    private Statement statement( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );

                try {
                    base.evaluate();
                }
                finally {
                    after( description );
                }
            }
        };
    }


    /**
     * Sets up the resources for the test here.
     *
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    protected void before( Description description ) throws Throwable {
        LOG.info( "Setting up for {}", description.getDisplayName() );
        initialize();
    }


    private void initialize() {
        if ( !enabled ) {
            emf = cassandraResource.getBean( EntityManagerFactory.class );
            qmf = cassandraResource.getBean( QueueManagerFactory.class );
            indexBucketLocator = cassandraResource.getBean( IndexBucketLocator.class );
            cassandraService = cassandraResource.getBean( CassandraService.class );
            enabled = true;
        }
    }


    /** Override to tear down your specific external resource. */
    protected void after( Description description ) {
        LOG.info( "Tearing down for {}", description.getDisplayName() );
    }


    @Override
    public EntityManagerFactory getEmf() {
        if ( emf == null ) {
            initialize();
        }

        return emf;
    }


    @Override
    public QueueManagerFactory getQmf() {
        if ( qmf == null ) {
            initialize();
        }

        return qmf;
    }


    @Override
    public IndexBucketLocator getIbl() {
        if ( indexBucketLocator == null ) {
            initialize();
        }

        return indexBucketLocator;
    }


    @Override
    public CassandraService getCassSvc() {
        if ( cassandraService == null ) {
            initialize();
        }

        return cassandraService;
    }


    @Override
    public UUID createApplication( String organizationName, String applicationName ) throws Exception {
        if ( USE_DEFAULT_APPLICATION ) {
            return CassandraService.DEFAULT_APPLICATION_ID;
        }

        if ( emf == null ) {
            emf = cassandraResource.getBean( EntityManagerFactory.class );
        }

        return emf.createApplication( organizationName, applicationName );
    }


    @Override
    public void dump( String name, Object obj ) {
        if ( obj != null && LOG.isInfoEnabled() ) {
            LOG.info( name + ":\n" + JsonUtils.mapToFormattedJsonString( obj ) );
        }
    }
}
