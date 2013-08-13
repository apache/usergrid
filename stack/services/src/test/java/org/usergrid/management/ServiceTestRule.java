package org.usergrid.management;


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.security.providers.SignInProviderFactory;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.services.ServiceManagerFactory;

import java.util.Properties;


/**
 * A {@link org.junit.rules.TestRule} that sets up services.
 */
public class ServiceTestRule extends ExternalResource
{
    private static final Logger LOG = LoggerFactory.getLogger( ServiceTestRule.class );
    private final CassandraResource cassandraResource;

    private ServiceManagerFactory smf;
    private ManagementService managementService;
    private EntityManagerFactory emf;
    private ApplicationCreator applicationCreator;
    private TokenService tokenService;
    private SignInProviderFactory providerFactory;
    private Properties properties;


    public ServiceTestRule( CassandraResource cassandraResource )
    {
        this.cassandraResource = cassandraResource;
    }


    @Override
    protected void before() throws Throwable
    {
        super.before();

        managementService = cassandraResource.getBean( ManagementService.class );
        applicationCreator = cassandraResource.getBean( ApplicationCreator.class );
        emf = cassandraResource.getBean( EntityManagerFactory.class );
        tokenService = cassandraResource.getBean( TokenService.class );
        providerFactory = cassandraResource.getBean( SignInProviderFactory.class );
        properties = cassandraResource.getBean(PropertiesFactoryBean.class).getObject();
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
