package org.apache.usergrid.management.cassandra;


import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/** @author zznate */
@Concurrent()
public class ApplicationCreatorIT {
    private static final Logger LOG = LoggerFactory.getLogger( ApplicationCreatorIT.class );

    private CassandraResource cassandraResource = ServiceITSuite.cassandraResource;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @Rule
    public ServiceITSetup setup = new ServiceITSetupImpl( cassandraResource );


    @Test
    public void testCreateSampleApplication() throws Exception {
        OrganizationOwnerInfo orgOwner = setup.getMgmtSvc()
                                              .createOwnerAndOrganization( "appcreatortest", "nate-appcreatortest",
                                                      "Nate", "nate+appcreatortest@apigee.com", "password", true,
                                                      false );

        ApplicationInfo appInfo = setup.getAppCreator().createSampleFor( orgOwner.getOrganization() );
        assertNotNull( appInfo );
        assertEquals( "appcreatortest/sandbox", appInfo.getName() );

        Set<String> rolePerms = setup.getEmf().getEntityManager( appInfo.getId() ).getRolePermissions( "guest" );
        assertNotNull( rolePerms );
        assertTrue( rolePerms.contains( "get,post,put,delete:/**" ) );
    }


    @Test
    public void testCreateSampleApplicationAltName() throws Exception {
        OrganizationOwnerInfo orgOwner = setup.getMgmtSvc().createOwnerAndOrganization( "appcreatortestcustom",
                "nate-appcreatortestcustom", "Nate", "nate+appcreatortestcustom@apigee.com", "password", true, false );

        ApplicationCreatorImpl customCreator = new ApplicationCreatorImpl( setup.getEmf(), setup.getMgmtSvc() );
        customCreator.setSampleAppName( "messagee" );
        ApplicationInfo appInfo = customCreator.createSampleFor( orgOwner.getOrganization() );
        assertNotNull( appInfo );
        assertEquals( "appcreatortestcustom/messagee", appInfo.getName() );
    }
}
