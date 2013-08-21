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

    SignInProviderFactory getProviderFactory();
}
