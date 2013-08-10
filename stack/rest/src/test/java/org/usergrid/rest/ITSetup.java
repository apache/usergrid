package org.usergrid.rest;


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.management.ApplicationCreator;
import org.usergrid.management.ManagementService;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.security.providers.SignInProviderFactory;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.services.ServiceManagerFactory;

import java.util.Properties;


/**
 * A {@link org.junit.rules.TestRule} that sets up services.
 */
public class ITSetup extends ExternalResource
{
    private static final Logger LOG = LoggerFactory.getLogger( ITSetup.class );
    private final CassandraResource cassandraResource;

    private ServiceManagerFactory smf;
    private ManagementService managementService;
    private EntityManagerFactory emf;
    private ApplicationCreator applicationCreator;
    private TokenService tokenService;
    private SignInProviderFactory providerFactory;
    private Properties properties;


    public ITSetup(CassandraResource cassandraResource)
    {
        this.cassandraResource = cassandraResource;
    }

    private boolean setupCalled = false;


    @Override
    protected void before() throws Throwable
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

        LOG.info( "Test setup complete..." );
    }


    public ManagementService getMgmtSvc()
    {
        return managementService;
    }


    public EntityManagerFactory getEmf()
    {
        return emf;
    }


    public ServiceManagerFactory getSmf()
    {
        return smf;
    }


    public ApplicationCreator getAppCreator()
    {
        return applicationCreator;
    }


    public TokenService getTokenSvc()
    {
        return tokenService;
    }


    public Properties getProps()
    {
        return properties;
    }


    public SignInProviderFactory getProviderFactory()
    {
        return providerFactory;
    }
}
