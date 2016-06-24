package org.apache.usergrid.security.sso;

import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.tokens.cassandra.TokenServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Properties;

/**
 * Created by russo on 6/24/16.
 */
public class SSOProviderFactory {

    enum Provider {
        APIGEE, USERGRID
    }

    private EntityManagerFactory emf;
    protected Properties properties;


    public ExternalSSOProvider getProvider(){

        final Provider configuredProvider;
        try{
            configuredProvider =
                Provider.valueOf(properties.getProperty(TokenServiceImpl.USERGRID_EXTERNAL_PROVIDER).toUpperCase());
        }catch (IllegalArgumentException e){
            throw new RuntimeException("Property usergrid.external.sso.provider must " +
                "be configured when external SSO is enabled");
        }

        switch (configuredProvider){

            case APIGEE:
                return ((CpEntityManagerFactory)emf).getApplicationContext().getBean( ApigeeSSO2Provider.class );
            case USERGRID:
                return ((CpEntityManagerFactory)emf).getApplicationContext().getBean( UsergridExternalProvider.class );
            default:
                throw new RuntimeException("Unknown SSO provider");
        }

    }

    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
