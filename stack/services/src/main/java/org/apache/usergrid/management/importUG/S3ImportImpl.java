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
import org.jclouds.blobstore.domain.MutableBlobMetadata;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
public class S3ImportImpl implements S3Import {

    private static BlobStore blobStore;
    private static ArrayList<Blob> blobs = new ArrayList<Blob>();
    private static ArrayList<File> files = new ArrayList<File>();
    private static int i=0;

    /**
     *
     * @param importInfo the information entered by the user required to perform import from S3
     * @param filename the filename generated based on the request URI
     * @param type  it indicates the type of import. 0 - Collection , 1 - Application and 2 - Organization
     * @return It returns an ArrayList of files i.e. the files downloaded from s3
     */
    public ArrayList<File> copyFromS3( final Map<String,Object> importInfo, String filename , int type) {

        Map<String,Object> properties = ( Map<String, Object> ) importInfo.get( "properties" );

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

        try{

            blobStore = context.getBlobStore();

            // gets all the files in the bucket recursively
            PageSet<? extends StorageMetadata> pageSet = blobStore.list(bucketName, new ListContainerOptions().recursive());

            Iterator itr = pageSet.iterator();

            while(itr.hasNext())
            {
                String fname = ((MutableBlobMetadata)itr.next()).getName();
                switch(type) {
                    // check if file is a collection file and is in format <org_name>/<app_name>.<collection_name>.[0-9]+.json
                    case 0:
                        if(fname.contains(filename))
                        {
                            copyFile(bucketName,fname);
                            i++;
                        }
                        break;
                    // check if file is an application file and is in format <org_name>/<app_name>.[0-9]+.json
                    case 1:
                        if(fname.matches(filename+"[0-9]+\\.json"))
                        {
                            copyFile(bucketName,fname);
                            i++;
                        }
                        break;
                    // check if file is an application file and is in format <org_name>/[-a-zA-Z0-9]+.[0-9]+.json
                    case 2:
                        if(fname.matches(filename+"[-a-zA-Z0-9]+\\.[0-9]+\\.json"))
                        {
                            copyFile(bucketName,fname);
                            i++;
                        }
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ContainerNotFoundException m) {
            m.printStackTrace();
        }
        return files;
    }

    /**
     *
     * @param bucketName the S3 bucket name from where files need to be imported
     * @param fname the filename by which the temp file should be created
     * @throws IOException
     */
    void copyFile(String bucketName, String fname) throws IOException {

        Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);
        Blob blob = blobStore.getBlob(bucketName, fname);
        blobs.add(blob);
        String[] fileOrg = fname.split("/");
        File organizationDirectory = new File(fileOrg[0]);

        if (!organizationDirectory.exists()) {
            try {
                organizationDirectory.mkdir();
            }catch(SecurityException se) {
                logger.error(se.getMessage());
            }
        }

        File ephemeral = new File(fname);

        FileOutputStream fop = new FileOutputStream(ephemeral);

        blobs.get(i).getPayload().writeTo(fop);

        files.add(ephemeral);

        organizationDirectory.deleteOnExit();
        ephemeral.deleteOnExit();
        fop.close();
    }
}
