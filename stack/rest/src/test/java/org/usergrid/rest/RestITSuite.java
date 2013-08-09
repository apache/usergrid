package org.usergrid.rest;


import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.rest.applications.users.GroupResourceTest;
import org.usergrid.rest.management.organizations.AdminEmailEncodingTest;


@RunWith( Suite.class )
@Suite.SuiteClasses(
    {
            GroupResourceTest.class,
            AdminEmailEncodingTest.class
    } )
@Concurrent()
@Ignore( "TODO: Todd fix. Does not reliably pass on our build server." )
// TODO - this suite actually runs correctly now so we can
// remove this ignore if Todd is OK with it.
public class RestITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
