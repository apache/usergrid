package org.usergrid.rest;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.rest.applications.ApplicationRequestCounterIT;
import org.usergrid.rest.applications.ApplicationResourceIT;
import org.usergrid.rest.applications.DevicesResourceIT;
import org.usergrid.rest.applications.assets.AssetResourceIT;
import org.usergrid.rest.applications.collection.PagingResourceIT;
import org.usergrid.rest.applications.events.EventsResourceIT;
import org.usergrid.rest.applications.users.*;
import org.usergrid.rest.filters.ContentTypeResourceIT;
import org.usergrid.rest.management.ManagementResourceIT;
import org.usergrid.rest.management.RegistrationIT;
import org.usergrid.rest.management.organizations.AdminEmailEncodingIT;
import org.usergrid.rest.management.organizations.OrganizationResourceIT;
import org.usergrid.rest.management.organizations.OrganizationsResourceIT;
import org.usergrid.rest.management.users.MUUserResourceIT;
import org.usergrid.rest.management.users.organizations.UsersOrganizationsResourceIT;


@RunWith( Suite.class )
@Suite.SuiteClasses(
    {
            ActivityResourceIT.class,
            AdminEmailEncodingIT.class,
            ApplicationRequestCounterIT.class,
            AssetResourceIT.class,
            BasicIT.class,
            CollectionsResourceIT.class,
            ContentTypeResourceIT.class,
            DevicesResourceIT.class,
            EventsResourceIT.class,
            GroupResourceIT.class,
            MUUserResourceIT.class,
            OrganizationResourceIT.class,
            OrganizationsResourceIT.class,
            OwnershipResourceIT.class,
            PagingResourceIT.class,
            PermissionsResourceIT.class,
            RegistrationIT.class,
            UserResourceIT.class,
            UsersOrganizationsResourceIT.class
    } )
@Concurrent()
public class RestITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
