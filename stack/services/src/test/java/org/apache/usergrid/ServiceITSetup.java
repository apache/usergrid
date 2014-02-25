package org.apache.usergrid;


import java.util.Properties;

import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;


public interface ServiceITSetup extends CoreITSetup {
    ManagementService getMgmtSvc();

    ApplicationCreator getAppCreator();

    ServiceManagerFactory getSmf();

    TokenService getTokenSvc();

    Properties getProps();

    ExportService getExportService();

    /**
     * Convenience method to set a property in the Properties object returned by getProps();
     *
     * @param key the property key
     * @param value the value of the property to set
     *
     * @return the previous value of the property
     */
    Object set( String key, String value );

    /**
     * Convenience method to get a property in the Properties object returned by getProps().
     *
     * @param key the property key
     *
     * @return value the value of the property
     */
    String get( String key );

    SignInProviderFactory getProviderFactory();
}
