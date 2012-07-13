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

import static org.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.usergrid.persistence.Schema.getDefaultSchema;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;

import java.util.Map.Entry;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ApplicationInfo;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.EntityManagerImpl;
import org.usergrid.persistence.entities.Application;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.utils.UUIDUtils;

/**
 * @author tnine
 * 
 */
public class EntityCleanup extends ToolBase {

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

        Query query = null;
        Results results = null;

        for (Entry<UUID, String> org : managementService.getOrganizations()
                .entrySet()) {

            for (Entry<UUID, String> app : managementService
                    .getApplicationsForOrganization(org.getKey()).entrySet()) {

                UUID applicationId = app.getKey();
                EntityManagerImpl em = (EntityManagerImpl) emf
                        .getEntityManager(applicationId);

                CassandraService cass = em.getCass();
                IndexBucketLocator indexBucketLocator = em
                        .getIndexBucketLocator();

                // go through each collection and audit the value
                for (String collectionName : em
                        .getCollections(new SimpleEntityRef(applicationId))) {

                    do {

                        query = new Query();
                        query.setLimit(100);

                        // advance the cursor for the next page of results
                        if (results != null) {
                            query.setCursor(results.getCursor());
                        }

                        // load all entity ids from the index itself.

                        List<UUID> ids = cass.getIdList(
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
                        for (UUID id : ids) {
                            em.deleteEntity(id);
                        }

                    } while (results != null && results.size() > 0);
                }
            }

        }

    }

}
