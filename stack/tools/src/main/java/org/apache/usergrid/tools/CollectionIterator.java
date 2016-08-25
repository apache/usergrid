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


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.*;
import com.google.common.base.Optional;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.util.RangeBuilder;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.results.EntityQueryExecutor;
import org.apache.usergrid.corepersistence.results.IdQueryExecutor;
import org.apache.usergrid.corepersistence.service.CollectionSearch;
import org.apache.usergrid.corepersistence.service.CollectionService;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.*;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.*;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.InflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


import org.apache.usergrid.persistence.core.astyanax.MultiTenantColumnFamily;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKey;
import org.apache.usergrid.persistence.core.astyanax.ScopedRowKeySerializer;
import rx.*;

import static org.apache.usergrid.persistence.Schema.getDefaultSchema;


public class CollectionIterator extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( CollectionIterator.class );

    private static final String APPLICATION_ARG = "app";

    private static final String ENTITY_TYPE_ARG = "entityType";

    private EntityManager em;

    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = super.createOptions();


        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( true )
            .withDescription( "application id" ).create( APPLICATION_ARG );


        options.addOption( appOption );

        Option collectionOption =
            OptionBuilder.withArgName(ENTITY_TYPE_ARG).hasArg().isRequired( true ).withDescription( "collection name" )
                .create(ENTITY_TYPE_ARG);

        options.addOption( collectionOption );


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

        UUID appToFilter = null;
        if (!line.getOptionValue(APPLICATION_ARG).isEmpty()) {
            appToFilter = UUID.fromString(line.getOptionValue(APPLICATION_ARG));
        }

        String entityType = line.getOptionValue(ENTITY_TYPE_ARG);

        logger.info("Staring Tool: CollectionIterator");
        logger.info("Using Cassandra consistency level: {}", System.getProperty("usergrid.read.cl", "CL_LOCAL_QUORUM"));

        em = emf.getEntityManager( appToFilter );
        EntityRef headEntity = new SimpleEntityRef("application", appToFilter);

        CollectionService collectionService = injector.getInstance(CollectionService.class);
        String collectionName = InflectionUtils.pluralize(entityType);

        Query query = new Query();
        query.setCollection(collectionName);
        query.setLimit(1000);

        com.google.common.base.Optional<String> queryString = com.google.common.base.Optional.absent();

        CollectionInfo collection = getDefaultSchema().getCollection(headEntity.getType(), collectionName);

        final UUID app = appToFilter;


        IdQueryExecutor idQueryExecutor = new IdQueryExecutor(query.getCursor()) {
            @Override
            protected rx.Observable<ResultsPage<Id>> buildNewResultsPage(
                final Optional<String> cursor) {

                final CollectionSearch search =
                    new CollectionSearch(new ApplicationScopeImpl(new SimpleId(app, "application")), new SimpleId(app, "application"), collectionName, collection.getType(), query.getLimit(),
                        queryString, cursor);

                return collectionService.searchCollectionIds(search);
            }
        };

        while ( idQueryExecutor.hasNext() ){

            Results results = idQueryExecutor.next();

            List<UUID> ids = results.getIds();

            ids.forEach( uuid -> {

                try {
                    org.apache.usergrid.persistence.Entity retrieved = em.get(new SimpleEntityRef(entityType, uuid));

                    long timestamp = 0;
                    String dateString = "NOT TIME-BASED";
                    if (UUIDUtils.isTimeBased(uuid)){
                        timestamp = UUIDUtils.getTimestampInMillis(uuid);
                        Date uuidDate = new Date(timestamp);
                        DateFormat df = new SimpleDateFormat();
                        df.setTimeZone(TimeZone.getTimeZone("GMT"));
                        dateString = df.format(uuidDate) + " GMT";
                    }

                    if ( retrieved != null ){

                        logger.info("{} - {} - entity data found", uuid, dateString);
                    }else{
                        logger.info("{} - {} - entity data NOT found", uuid, dateString);
                    }
                } catch (Exception e) {
                    logger.error("{} - exception while trying to load entity data, {} ", uuid, e.getMessage());
                }


            });

        }


    }
}
