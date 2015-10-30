/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.index.migration;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.migration.data.MigrationInfoSerialization;
import org.apache.usergrid.persistence.core.migration.data.MigrationPlugin;
import org.apache.usergrid.persistence.core.migration.data.PluginPhase;
import org.apache.usergrid.persistence.core.migration.data.ProgressObserver;
import org.apache.usergrid.persistence.index.impl.EsProvider;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Classy class class.
 */
public class EsIndexMappingMigrationPlugin implements MigrationPlugin {

    private static final Logger logger = LoggerFactory.getLogger(EsIndexMappingMigrationPlugin.class);

    private final MigrationInfoSerialization migrationInfoSerialization;
    private final EsProvider provider;

    @Inject
    public EsIndexMappingMigrationPlugin(
        final MigrationInfoSerialization migrationInfoSerialization,
        final EsProvider provider
    ){

        this.migrationInfoSerialization = migrationInfoSerialization;
        this.provider = provider;
    }

    @Override
    public String getName() {
        return "index_mapping_migration";
    }

    @Override
    public void run(ProgressObserver observer) {

        final int version = migrationInfoSerialization.getVersion(getName());

        if (version == getMaxVersion()) {
            logger.debug("Skipping Migration Plugin: " + getName());
            return;
        }

        try {
            ActionFuture<GetIndexResponse> responseFuture = provider.getClient().admin().indices().getIndex(new GetIndexRequest());
            Observable
                .from(responseFuture)
                .flatMap(response -> {
                    List<String> indices = Arrays.asList(response.getIndices());
                    return Observable.from(indices);
                })

                .doOnNext(index -> {
                    createMappings(index);
                    observer.update(getMaxVersion(), "running update for " + index);
                })
                .doOnError(t -> {
                    observer.failed(getMaxVersion(),"failed to update",t);
                })
                .doOnCompleted(() -> {
                    migrationInfoSerialization.setVersion(getName(), getMaxVersion());
                    observer.complete();
                })
                .subscribe(); //should run through


        }catch (Exception ee){
            observer.failed(getMaxVersion(),"failed to update",ee);
            throw new RuntimeException(ee);
        }

    }


    /**
     * Setup ElasticSearch type mappings as a template that applies to all new indexes.
     * Applies to all indexes that* start with our prefix.
     */
    private void createMappings(final String indexName)  {

        //Added For Graphite Metrics
        PutMappingResponse pitr = provider.getClient().admin().indices().preparePutMapping( indexName ).setType( "entity" ).setSource(
            getMappingsContent() ).execute().actionGet();
        if ( !pitr.isAcknowledged() ) {
            throw new RuntimeException( "Unable to create default mappings" );
        }
    }


    /**
     * Get the content from our mappings file
     * @return
     */
    private String getMappingsContent(){
        URL url = Resources.getResource("org/apache/usergrid/persistence/index/usergrid-mappings.json");
        try {
            return Resources.toString(url, Charsets.UTF_8);
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Unable to read mappings file", e );
        }
    }

    @Override
    public int getMaxVersion() {
        return 2;//1->new storage format,2->remove uuid
    }

    @Override
    public PluginPhase getPhase() {
        return PluginPhase.BOOTSTRAP;
    }
}
