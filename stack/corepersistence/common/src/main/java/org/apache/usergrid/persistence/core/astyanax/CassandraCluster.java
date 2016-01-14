package org.apache.usergrid.persistence.core.astyanax;



import com.netflix.astyanax.Keyspace;

import java.util.Map;


public interface CassandraCluster {


    Map<String, Keyspace> getKeyspaces();

    Keyspace getApplicationKeyspace();

    Keyspace getLocksKeyspace();


}
