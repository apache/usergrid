package org.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.cassandra.ConcurrentSuite;
import org.usergrid.management.EmailFlowIT;
import org.usergrid.management.OrganizationIT;
import org.usergrid.management.RoleIT;
import org.usergrid.management.cassandra.ApplicationCreatorIT;
import org.usergrid.management.cassandra.ManagementServiceIT;
import org.usergrid.security.providers.FacebookProviderIT;
import org.usergrid.security.providers.PingIdentityProviderIT;
import org.usergrid.security.tokens.TokenServiceIT;
import org.usergrid.services.*;


@RunWith( ConcurrentSuite.class )
@Suite.SuiteClasses(
    {
            ActivitiesServiceIT.class,
            ApplicationCreatorIT.class,
            ApplicationsServiceIT.class,
            CollectionServiceIT.class,
            ConnectionsServiceIT.class,
            ManagementServiceIT.class,
            EmailFlowIT.class,
            FacebookProviderIT.class,
            GroupServiceIT.class,
            OrganizationIT.class,
            PingIdentityProviderIT.class,
            RolesServiceIT.class,
            RoleIT.class,
            ServiceRequestIT.class,
            ServiceFactoryIT.class,
            ServiceInvocationIT.class,
            TokenServiceIT.class,
            UsersServiceIT.class
    } )
@Concurrent()
public class ConcurrentServiceITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
