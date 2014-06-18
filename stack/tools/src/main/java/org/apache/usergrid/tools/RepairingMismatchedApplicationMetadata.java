/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.tools;

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
import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.batchExecute;
import static org.apache.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.RETRY_COUNT;

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
                    addInsertToMutator(m, APPLICATIONS_CF, appName, PROPERTY_UUID, app.getKey(), timestamp);
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
