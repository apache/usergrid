package org.usergrid.rest;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.usergrid.cassandra.CassandraResource;
import org.usergrid.cassandra.Concurrent;
import org.usergrid.rest.applications.users.GroupResourceTest;


@RunWith( Suite.class )
@Suite.SuiteClasses(
{
    GroupResourceTest.class,
} )
@Concurrent()
public class RestITSuite
{
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
