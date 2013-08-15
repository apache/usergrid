package org.usergrid.rest;


import org.apache.commons.lang.math.RandomUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.cassandra.AvailablePortFinder;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.management.ApplicationCreator;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.security.providers.SignInProviderFactory;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.services.ServiceManagerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Properties;


/**
 * A {@link org.junit.rules.TestRule} that sets up services.
 */
public class ITSetup extends ExternalResource
{
    private static final int DEFAULT_JETTY_PORT = 9998;
    private static final Logger LOG = LoggerFactory.getLogger( ITSetup.class );
    private final CassandraResource cassandraResource;

    private ServiceManagerFactory smf;
    private ManagementService managementService;
    private EntityManagerFactory emf;
    private ApplicationCreator applicationCreator;
    private TokenService tokenService;
    private SignInProviderFactory providerFactory;
    private Properties properties;
    private Server jetty;
    private int jettyPort = DEFAULT_JETTY_PORT;


    public ITSetup(CassandraResource cassandraResource)
    {
        this.cassandraResource = cassandraResource;
    }

    private boolean setupCalled = false;
    private boolean ready = false;
    private URI uri;


    @Override
    protected void before() throws Throwable
    {
        synchronized ( cassandraResource )
        {
            super.before();

            managementService = cassandraResource.getBean( ManagementService.class );

            if ( ! setupCalled )
            {
                managementService.setup();
                setupCalled = true;
            }

            applicationCreator = cassandraResource.getBean( ApplicationCreator.class );
            emf = cassandraResource.getBean( EntityManagerFactory.class );
            tokenService = cassandraResource.getBean( TokenService.class );
            providerFactory = cassandraResource.getBean( SignInProviderFactory.class );
            properties = cassandraResource.getBean( "properties", Properties.class );
            smf = cassandraResource.getBean(ServiceManagerFactory.class);

            while ( jetty == null )
            {
                startJetty();
            }

            // Initialize Jersey Client
            uri = UriBuilder.fromUri("http://localhost/").port( jettyPort ).build();

            ready = true;
            LOG.info( "Test setup complete..." );
        }
    }


    private void startJetty()
    {
        LOG.info( "Starting the Jetty Server on port {}", jettyPort );
        jettyPort = AvailablePortFinder.getNextAvailable( DEFAULT_JETTY_PORT + RandomUtils.nextInt( 1000 ) );

        jetty = new Server( jettyPort );
        jetty.setHandler( new WebAppContext( "src/main/webapp", "/" ) );

        try
        {
            jetty.start();
        }
        catch ( Exception e )
        {
            jetty = null;
        }
    }


    public void protect()
    {
        if ( ready ) return;

        try
        {
            LOG.warn( "Calls made to access members without being ready ... initializing..." );
            before();
        }
        catch ( Throwable t )
        {
            throw new RuntimeException( "Failed on before()", t );
        }
    }


    public int getJettyPort()
    {
        protect();
        return jettyPort;
    }


    public ManagementService getMgmtSvc()
    {
        protect();
        return managementService;
    }


    public EntityManagerFactory getEmf()
    {
        protect();
        return emf;
    }


    public ServiceManagerFactory getSmf()
    {
        protect();
        return smf;
    }


    public ApplicationCreator getAppCreator()
    {
        protect();
        return applicationCreator;
    }


    public TokenService getTokenSvc()
    {
        protect();
        return tokenService;
    }


    public Properties getProps()
    {
        protect();
        return properties;
    }


    public SignInProviderFactory getProviderFactory()
    {
        protect();
        return providerFactory;
    }


    public URI getBaseURI()
    {
        protect();
        return uri;
    }
}
