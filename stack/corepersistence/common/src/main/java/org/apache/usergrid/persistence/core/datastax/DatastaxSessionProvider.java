package org.apache.usergrid.persistence.core.datastax;


import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class DatastaxSessionProvider implements Provider<Session> {

    private final DataStaxCluster dataStaxCluster;

    @Inject
    public DatastaxSessionProvider( final DataStaxCluster dataStaxCluster){

        this.dataStaxCluster = dataStaxCluster;
    }

    @Override
    public Session get(){

        return dataStaxCluster.getApplicationSession();
    }
}
