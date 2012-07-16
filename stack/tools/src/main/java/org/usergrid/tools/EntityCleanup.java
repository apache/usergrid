/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.tools;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.usergrid.persistence.Schema.getDefaultSchema;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ID_SETS;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.usergrid.utils.UUIDUtils.newTimeUUID;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.EntityManagerImpl;
import org.usergrid.persistence.schema.CollectionInfo;

/**
 * This is a untiltiy to audit all available entity ids for existing target rows
 * If an entity Id exists in the collection index with no target entity, the id
 * is removed from the index. This is a cleanup tool as a result of the issue in
 * USERGRID-323
 * 
 * @author tnine
 * 
 */
public class EntityCleanup extends ToolBase {

    /**
     * 
     */
    private static final int PAGE_SIZE = 100;

    public static final ByteBufferSerializer be = new ByteBufferSerializer();

    private static final Logger logger = LoggerFactory
            .getLogger(EntityCleanup.class);

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption = OptionBuilder.withArgName("host").hasArg()
                .isRequired(true).withDescription("Cassandra host")
                .create("host");

        Options options = new Options();
        options.addOption(hostOption);

        return options;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void runTool(CommandLine line) throws Exception {
        startSpring();

        logger.info("Starting entity cleanup");

        Results results = null;
        List<UUID> ids = null;
        Query query = new Query();
        query.setLimit(PAGE_SIZE);
        String lastCursor = null;

        for (Entry<UUID, String> org : managementService.getOrganizations()
                .entrySet()) {

            for (Entry<UUID, String> app : managementService
                    .getApplicationsForOrganization(org.getKey()).entrySet()) {

                logger.info("Starting cleanup for org {} and app {}",
                        org.getValue(), app.getValue());

                UUID applicationId = app.getKey();
                EntityManagerImpl em = (EntityManagerImpl) emf
                        .getEntityManager(applicationId);

                CassandraService cass = em.getCass();
                IndexBucketLocator indexBucketLocator = em
                        .getIndexBucketLocator();

                UUID timestampUuid = newTimeUUID();
                long timestamp = getTimestampInMicros(timestampUuid);

                Set<String> collectionNames = em.getApplicationCollections();

                // go through each collection and audit the value
                for (String collectionName : collectionNames) {

                    lastCursor = null;

                    do {

                        query.setCursor(lastCursor);
                        // load all entity ids from the index itself.

                        ids = cass.getIdList(
                                cass.getApplicationKeyspace(applicationId),
                                key(applicationId, DICTIONARY_COLLECTIONS,
                                        collectionName),
                                query.getStartResult(), null,
                                query.getLimit() + 1, false,
                                indexBucketLocator, applicationId,
                                collectionName);

                        CollectionInfo collection = getDefaultSchema()
                                .getCollection("application", collectionName);

                        Results tempResults = Results.fromIdList(ids,
                                collection.getType());

                        if (tempResults != null) {
                            tempResults.setQuery(query);
                        }

                        results = em.loadEntities(tempResults,
                                query.getResultsLevel(), query.getLimit());

                        // advance the cursor for the next page of results

                        lastCursor = results.getCursor();

                        // nothing to do they're the same size so there's no
                        // orphaned uuid's in the entity index
                        if (ids.size() == results.size()) {
                            continue;
                        }

                        // they're not the same, we have some orphaned records,
                        // remove them

                        for (Entity returned : results.getEntities()) {
                            ids.remove(returned.getUuid());
                        }

                        // what's left needs deleted, do so

                        logger.info(
                                "Cleaning up {} orphaned entities for org {} and app {}",
                                new Object[] { ids.size(), org.getValue(),
                                        app.getValue() });

                        Keyspace ko = cass
                                .getApplicationKeyspace(applicationId);
                        Mutator<ByteBuffer> m = createMutator(ko, be);

                        for (UUID id : ids) {

                            Object collections_key = key(applicationId,
                                    Schema.DICTIONARY_COLLECTIONS,
                                    collectionName,
                                    indexBucketLocator.getBucket(applicationId,
                                            IndexType.COLLECTION, id,
                                            collectionName));

                            addDeleteToMutator(m, ENTITY_ID_SETS,
                                    collections_key, id, timestamp);

                            logger.info(
                                    "Deleting entity with id '{}' from collection '{}'",
                                    id, collectionName);
                        }

                        m.execute();

                    } while (ids.size() == PAGE_SIZE);
                }
            }

        }

    }
}
