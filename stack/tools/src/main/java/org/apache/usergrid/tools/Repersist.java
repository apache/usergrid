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


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;


/**
 * Iterate through all data in a list of apps and re-persist every entity in every collection.
 */
public class Repersist extends ToolBase {
    static final Logger logger = LoggerFactory.getLogger( Repersist.class );

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {
        Options options = super.createOptions();
        options.addOption("apps", true, "Comma-separated list of apps to re-persisted in {appName}/{orgName} format");
        options.addOption("wait (ms)", true, "Time to  wait between entity re-persist");
        return options;
    }

    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();
        setVerbose( line );

        String appsLine = line.getOptionValue("apps");
        String[] orgApps = appsLine.split(",");

        long wait = 100;
        try {
            Long.parseLong(line.getOptionValue("wait"));
        } catch( Exception e ) {
            logger.error("Incorrect or missing wait time, using default: " + wait);
        }

        for ( String orgApp : orgApps ) {

            String[] orgAppParts = orgApp.split("/");
            String org = orgAppParts[0];
            String app = orgAppParts[1];

            repersist( org, org + "/" + app, wait );
        }

    }

    private void repersist( String organizationName, String applicationName, long wait ) throws Exception {

        logger.info( "\n\nRepersisting {}/{}\n", organizationName, applicationName );

        UUID applicationId = emf.lookupApplication( applicationName );
        if (applicationId == null) {
            throw new RuntimeException( "Cannot find application " + applicationName );
        }
        final EntityManager em = emf.getEntityManager( applicationId );
        organizationName = em.getApplication().getOrganizationName();


        Map<String, Object> collectionMetadata = em.getApplicationCollectionMetadata();

        for ( String collectionName : collectionMetadata.keySet() ) {

            int count = 0;

            Query query = new Query();
            query.setLimit( MAX_ENTITY_FETCH );

            Results results = em.searchCollection( em.getApplicationRef(), collectionName, query );

            while (results.size() > 0) {
                for (Entity entity : results.getEntities()) {

                    // repersist
                    em.update(entity);
                    count++;
                    Thread.sleep( wait );

                }
                if (results.getCursor() == null) {
                    break;
                }
                query.setCursor( results.getCursor() );
                results = em.searchCollection( em.getApplicationRef(), collectionName, query );
            }

            logger.info("Completed app {}/{} collection {}. Persisted {} entities",
                organizationName, applicationName, collectionName, count);
        }


    }

}
