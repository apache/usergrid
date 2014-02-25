package org.apache.usergrid.test;


import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.utils.JsonUtils;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CoreITSetupImpl implements CoreITSetup {
    private static final Logger LOG = LoggerFactory.getLogger( CoreITSetupImpl.class );

    protected EntityCollectionManagerFactory emf;
    protected CassandraService cassandraService;
    protected boolean enabled = false;


//    public CoreITSetupImpl( CassandraResource cassandraResource ) {
//        this.cassandraResource = cassandraResource;
//    }


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
    }


    /** Override to tear down your specific external resource. */
    protected void after( Description description ) {
        LOG.info( "Tearing down for {}", description.getDisplayName() );
    }


    @Override
    public void dump( String name, Object obj ) {
        if ( obj != null && LOG.isInfoEnabled() ) {
            LOG.info( name + ":\n" + JsonUtils.mapToFormattedJsonString( obj ) );
        }
    }

    public EntityCollectionManagerFactory getEmf() {
        return emf;
    }
}
