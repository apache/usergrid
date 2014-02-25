package org.apache.usergrid;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.cassandra.ConcurrentSuite;
import org.apache.usergrid.management.EmailFlowIT;
import org.apache.usergrid.management.OrganizationIT;
import org.apache.usergrid.management.RoleIT;
import org.apache.usergrid.management.cassandra.ApplicationCreatorIT;
import org.apache.usergrid.management.cassandra.ManagementServiceIT;
import org.apache.usergrid.security.providers.FacebookProviderIT;
import org.apache.usergrid.security.providers.PingIdentityProviderIT;
import org.apache.usergrid.security.tokens.TokenServiceIT;
import org.apache.usergrid.services.ActivitiesServiceIT;
import org.apache.usergrid.services.ApplicationsServiceIT;
import org.apache.usergrid.services.CollectionServiceIT;
import org.apache.usergrid.services.ConnectionsServiceIT;
import org.apache.usergrid.services.GroupServiceIT;
import org.apache.usergrid.services.RolesServiceIT;
import org.apache.usergrid.services.ServiceFactoryIT;
import org.apache.usergrid.services.ServiceInvocationIT;
import org.apache.usergrid.services.ServiceRequestIT;
import org.apache.usergrid.services.UsersServiceIT;


@RunWith(ConcurrentSuite.class)
@Suite.SuiteClasses(
        {
                ActivitiesServiceIT.class, ApplicationCreatorIT.class, ApplicationsServiceIT.class,
                CollectionServiceIT.class, ConnectionsServiceIT.class, ManagementServiceIT.class, EmailFlowIT.class,
                FacebookProviderIT.class, GroupServiceIT.class, OrganizationIT.class, PingIdentityProviderIT.class,
                RolesServiceIT.class, RoleIT.class, ServiceRequestIT.class, ServiceFactoryIT.class,
                ServiceInvocationIT.class, TokenServiceIT.class, UsersServiceIT.class
        })
@Concurrent()
public class ConcurrentServiceITSuite {
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
