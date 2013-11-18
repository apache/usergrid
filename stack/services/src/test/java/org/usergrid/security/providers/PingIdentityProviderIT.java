package org.usergrid.security.providers;


import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.usergrid.ServiceITSetup;
import org.usergrid.ServiceITSetupImpl;
import org.usergrid.ServiceITSuite;
import org.usergrid.cassandra.ClearShiroSubject;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.entities.User;
import org.usergrid.utils.MapUtils;

import static junit.framework.Assert.assertNotNull;


/** @author zznate */
@Ignore
@Concurrent()
public class PingIdentityProviderIT {
    private static UserInfo adminUser;
    private static OrganizationInfo organization;
    private static UUID applicationId;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );


    @BeforeClass
    public static void setup() throws Exception {
        adminUser = setup.getMgmtSvc()
                         .createAdminUser( "pinguser", "Ping User", "ping-user@usergrid.com", "test", false, false );
        organization = setup.getMgmtSvc().createOrganization( "ping-organization", adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "ping-application" ).getId();
    }


    @Test
    public void verifyLiveConnect() throws Exception {
        Application application = setup.getEmf().getEntityManager( applicationId ).getApplication();
        Map pingProps = MapUtils.hashMap( "api_url", "" ).map( "client_secret", "" )
                                .map( "client_id", "dev.app.appservicesvalidation" );

        PingIdentityProvider pingProvider =
                ( PingIdentityProvider ) setup.getProviderFactory().pingident( application );
        pingProvider.saveToConfiguration( pingProps );
        pingProvider.configure();
        User user = pingProvider.createOrAuthenticate( "u0qoW7TS9eT8Vmt7UzrEWrhHbhDK" );
        assertNotNull( user );
    }
}
