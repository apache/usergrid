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

package org.apache.usergrid.management.importUG;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
public class S3ImportImpl implements S3Import {

    public File copyFromS3( final Map<String,Object> exportInfo, String filename ) {

        Map<String,Object> properties = ( Map<String, Object> ) exportInfo.get( "properties" );

        Map<String, Object> storage_info = (Map<String,Object>)properties.get( "storage_info" );

        String bucketName = ( String ) storage_info.get( "bucket_location" );
        String accessId = ( String ) storage_info.get( "s3_access_id" );
        String secretKey = ( String ) storage_info.get( "s3_key" );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        final Iterable<? extends Module> MODULES = ImmutableSet
                .of(new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
                        new NettyPayloadModule());

        BlobStoreContext context =
                ContextBuilder.newBuilder("s3").credentials( accessId, secretKey ).modules( MODULES )
                        .overrides( overrides ).buildView( BlobStoreContext.class );

        FileOutputStream fop = null;

        File ephemeral = new File("temp_file");
        ephemeral.deleteOnExit();
        try{
            BlobStore blobStore = context.getBlobStore();
            Blob blob = blobStore.getBlob(bucketName, filename);

            fop = new FileOutputStream(ephemeral);
            blob.getPayload().writeTo(fop);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ContainerNotFoundException m) {
            m.printStackTrace();
        }
        return ephemeral;

    }

    @Override
    public String getFilename() {
        return null;
    }

}
