package org.usergrid;


import org.usergrid.management.ApplicationCreator;
import org.usergrid.management.ManagementService;
import org.usergrid.security.providers.SignInProviderFactory;
import org.usergrid.security.tokens.TokenService;
import org.usergrid.services.ServiceManagerFactory;

import java.util.Properties;


public interface ServiceITSetup extends CoreITSetup
{
    ManagementService getMgmtSvc();

    ApplicationCreator getAppCreator();

    ServiceManagerFactory getSmf();

    TokenService getTokenSvc();

    Properties getProps();

    /**
     * Convenience method to set a property in the Properties object returned
     * by getProps();
     *
     * @param key the property key
     * @param value the value of the property to set
     * @return the previous value of the property
     */
    Object set( String key, String value );

    /**
     * Convenience method to get a property in the Properties object returned
     * by getProps().
     *
     * @param key the property key
     * @return value the value of the property
     */
    String get( String key );

    SignInProviderFactory getProviderFactory();
}
