package org.usergrid.tools;

import com.google.common.collect.BiMap;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.usergrid.persistence.cassandra.CassandraService.RETRY_COUNT;

public class RepairingMismatchedApplicationMetadata extends ToolBase {

    public static final ByteBufferSerializer be = new ByteBufferSerializer();

    @Override
    public Options createOptions() {
        Options options = super.createOptions();
        return options;
    }

    @Override
    public void runTool(CommandLine line) throws Exception {
        startSpring();

        BiMap<UUID, String> orgs = managementService.getOrganizations();
        for(Map.Entry org : orgs.entrySet()) {
            BiMap<UUID, String> apps = managementService.getApplicationsForOrganization((UUID)org.getKey());
            for(Map.Entry app : apps.entrySet()) {
                UUID applicationId = emf.lookupApplication((String)app.getValue());
                if( applicationId == null ) {
                    String appName = (String)app.getValue();
                    Keyspace ko = cass.getSystemKeyspace();
                    Mutator<ByteBuffer> m = createMutator(ko, be);
                    long timestamp = cass.createTimestamp();
                    addInsertToMutator(m, APPLICATIONS_CF, appName, PROPERTY_UUID, (UUID)app.getKey(), timestamp);
                    addInsertToMutator(m, APPLICATIONS_CF, appName, PROPERTY_NAME, appName, timestamp);
                    batchExecute(m, RETRY_COUNT);
                    logger.info("UUID {}, NAME {}", app.getKey(), app.getValue());
                }
            }
        }

        logger.info("Waiting 60 sec...");
        Thread.sleep(1000 * 60);
    }
}
