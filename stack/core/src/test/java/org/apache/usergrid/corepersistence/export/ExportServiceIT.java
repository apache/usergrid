/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.export;

import com.google.inject.Injector;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.EntityWriteHelper;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;

import org.apache.usergrid.persistence.model.entity.Id;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import static org.junit.Assert.assertTrue;


public class ExportServiceIT extends AbstractCoreIT {


    @Test
    public void testExport() throws Exception {

        Injector injector =  SpringResource.getInstance().getBean(Injector.class);

        ExportService exportService = injector.getInstance(ExportService.class);

        final EntityManager em = app.getEntityManager();

        // create two types of entities

        final String type1 = "type1thing";
        final String type2 = "type2thing";
        final int size = 1;

        final Set<Id> type1Identities = EntityWriteHelper.createTypes( em, type1, size );
        final Set<Id> type2Identities = EntityWriteHelper.createTypes( em, type2, size );

        // connect the first type1 entity to all type2 entities

        final Id source = type1Identities.iterator().next();
        final Set<Id> connections = new HashSet<>();

        for ( Id target : type2Identities ) {
            em.createConnection( SimpleEntityRef.fromId( source ),
                "likes", SimpleEntityRef.fromId( target ) );
            connections.add( target );
        }

        ExportRequestBuilder builder = new ExportRequestBuilderImpl().withApplicationId(app.getId());

        // fill the output stream
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        exportService.export(builder, stream);

        // convert the output stream to an input stream and read it as a zip
        InputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
        ZipInputStream zip = new ZipInputStream(inputStream);


        boolean entityEntryExists = false;
        boolean connectionEntryExists = false;
        boolean statsEntryExists = false;
        boolean metaEntryExists = false;

        final String entityFile = "entities/entities.0.json";
        final String connectionFile = "connections/"+source.getUuid().toString()+"_"+"likes"+"_"+connections.iterator().next().getUuid().toString()+".json";
        final String statsFile = "stats.json";
        final String metaFile = "metadata.json";

        ZipEntry zipEntry;
        while ( (zipEntry = zip.getNextEntry()) != null ) {

                final String name = zipEntry.getName();

                if (name.equals(entityFile)) {
                    entityEntryExists = true;
                }
                if(name.equals(connectionFile)){
                    connectionEntryExists = true;
                }
                if(name.equals(statsFile)){
                    statsEntryExists = true;
                }
                if(name.equals(metaFile)){
                    metaEntryExists = true;
                }

        }

        assertTrue("Expected zip entries are missing", entityEntryExists && connectionEntryExists && statsEntryExists && metaEntryExists);

    }


}
