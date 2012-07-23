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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.usergrid.management.ApplicationInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.EntityManagerImpl;
import org.usergrid.persistence.schema.CollectionInfo;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * This is a untiltiy to audit all available entity ids for existing target rows
 * If an entity Id exists in the collection index with no target entity, the id
 * is removed from the index. This is a cleanup tool as a result of the issue in
 * USERGRID-351 and paging issue
 * 
 * @author tnine
 * 
 */
public class AppAudit extends ToolBase {

    /**
     * 
     */
    private static final int PAGE_SIZE = 100;

    public static final ByteBufferSerializer be = new ByteBufferSerializer();

    private static final Logger logger = LoggerFactory
            .getLogger(AppAudit.class);

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

        logger.info("Starting app audit");

        // TODO TN use row iterator to fix this!

        Map<String, UUID> apps = emf.getApplications();

        OrgRepo repo = new OrgRepo();

        Set<String> foundOrgs = new HashSet<String>();
        // Set<String> foundApps = new HashSet<String>();

        for (Entry<String, UUID> entry : apps.entrySet()) {
            logger.info("Name: {}, Id: {}", entry.getKey(), entry.getValue());
            // repo.putData(entry.getKey(), entry.getValue());
            String[] parts = entry.getKey().split("/");
            foundOrgs.add(parts[0]);
            // foundApps.add(entry.getKey());
        }

        EntityManager em = emf
                .getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);

        // search for all orgs

        EntityRef ref = new SimpleEntityRef("application",
                CassandraService.MANAGEMENT_APPLICATION_ID);

        int limit = 100;
        Query query = new Query();
        query.setLimit(limit);

        Results r = null;

        do {

            r = em.searchCollection(ref, "groups", query);

            for (Entity entity : r.getEntities()) {
                foundOrgs.remove(entity.getProperty("path"));
            }

            query.setCursor(r.getCursor());

        } while (r != null && r.size() == limit);

        // search for all apps
        // query = new Query();
        //
        // do {
        //
        // r = em.searchCollection(ref, "application_info", query);
        //
        // for (Entity entity : r.getEntities()) {
        // foundApps.remove(entity.getProperty("name"));
        // }
        //
        // query.setCursor(r.getCursor());
        //
        // } while (r != null && r.size() == limit);

        // now dump what we couldn't find!

        for (String orgName : foundOrgs) {
            logger.info("Cound not find org name: {}", orgName);
        }
        //
        // for (String appName : foundApps) {
        // logger.info("Cound not find app with name: {}", appName);
        // }

    }

    /**
     * An organization repository to hold orgs in memory
     * 
     * @author tnine
     * 
     */
    private static class OrgRepo {
        private Map<String, OrgMeta> orgData = new HashMap<String, OrgMeta>();

        public void putData(String appName, UUID appId) {
            String[] names = appName.split("/");

            OrgMeta orgMeta = orgData.get(names[0]);

            if (orgMeta == null) {
                orgMeta = new OrgMeta(names[0]);
                orgData.put(names[0], orgMeta);
            }

            orgMeta.addApp(names[1], appId);
        }

        public Collection<OrgMeta> getOrgData() {
            return orgData.values();
        }
    }

    /**
     * Get the org meta data
     * 
     * @author tnine
     * 
     */
    private static class OrgMeta {

        private String orgName;

        private BiMap<String, UUID> apps = HashBiMap.create();

        /**
         * @param orgName
         */
        public OrgMeta(String orgName) {
            super();
            this.orgName = orgName;
        }

        /**
         * Add an app with it's id to this org
         * 
         * @param appName
         * @param appId
         */
        public void addApp(String appName, UUID appId) {
            apps.put(appName, appId);
        }

        public String getOrgName() {
            return orgName;
        }

        /**
         * Get info for applications
         * 
         * @return
         */
        public List<ApplicationInfo> getInfo() {
            List<ApplicationInfo> infos = new ArrayList<ApplicationInfo>();

            for (Entry<String, UUID> entryId : apps.entrySet()) {
                infos.add(new ApplicationInfo(entryId.getValue(), entryId
                        .getKey()));
            }

            return infos;
        }
    }
}
