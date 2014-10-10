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


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.utils.UUIDUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;


/**
 * Index rebuild utility for Usergrid. Can be used to rebuild the index for a specific 
 * application, a specific application's collection or for an entire Usergrid system.
 */
public class RepersistAll extends ToolBase {

    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger(RepersistAll.class );


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOpt = OptionBuilder.withArgName( "host" ).hasArg().isRequired( true )
                .withDescription( "Cassandra host" ).create( "host" );

        Option esHostsOpt = OptionBuilder.withArgName( "host" ).hasArg().isRequired( true )
                .withDescription( "ElasticSearch host" ).create( "eshost" );

        Option esClusterOpt = OptionBuilder.withArgName( "host" ).hasArg().isRequired( true )
                .withDescription( "ElasticSearch cluster name" ).create( "escluster" );

        Options options = new Options();
        options.addOption( hostOpt );
        options.addOption( esHostsOpt );
        options.addOption( esClusterOpt );

        return options;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        logger.info( "Starting index rebuild" );

        EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {
            @Override
            public void onProgress(EntityRef s, EntityRef t, String etype) {
                logger.info("Repersisting from {}:{} to {}:{}", new Object[] {
                    s.getType(), s.getUuid(), t.getType(), t.getUuid(), etype });
            }
        };

        emf.rebuildAllIndexes( po );

        logger.info( "Finished index rebuild" );
    }
}
