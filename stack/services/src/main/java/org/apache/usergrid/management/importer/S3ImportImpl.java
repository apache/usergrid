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

package org.apache.usergrid.management.importer;

import com.amazonaws.SDKGlobalConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.lucene.document.StringField;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
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

import java.io.*;
import java.nio.file.Files;
import java.util.*;


public class S3ImportImpl implements S3Import {
    private static final Logger logger = LoggerFactory.getLogger(S3ImportImpl.class);


    public File copyFileFromBucket(
        String blobFileName, String bucketName, String accessId, String secretKey ) throws Exception {

        // setup to use JCloud BlobStore interface to AWS S3

        Properties overrides = new Properties();
        overrides.setProperty("s3" + ".identity", accessId);
        overrides.setProperty("s3" + ".credential", secretKey);

        final Iterable<? extends Module> MODULES = ImmutableSet.of(
            new JavaUrlHttpCommandExecutorServiceModule(),
            new Log4JLoggingModule(),
            new NettyPayloadModule());

        BlobStoreContext context = ContextBuilder.newBuilder("s3")
            .credentials(accessId, secretKey)
            .modules(MODULES)
            .overrides(overrides)
            .buildView(BlobStoreContext.class);
        BlobStore blobStore = context.getBlobStore();

        // get file from configured bucket, copy it to local temp file

        Blob blob = blobStore.getBlob(bucketName, blobFileName);
        if ( blob == null) {
            throw new RuntimeException(
                "Blob file name " + blobFileName + " not found in bucket " + bucketName );
        }

        FileOutputStream fop = null;
        File tempFile;
        try {
            tempFile = File.createTempFile(bucketName, RandomStringUtils.randomAlphabetic(10));
            tempFile.deleteOnExit();
            fop = new FileOutputStream(tempFile);
            InputStream is = blob.getPayload().openStream();
            IOUtils.copyLarge(is, fop);
            return tempFile;

        } finally {
            if ( fop != null ) {
                fop.close();
            }
        }
    }


    @Override
    public List<String> getBucketFileNames(
        String bucketName, String endsWith, String accessId, String secretKey ) {

        // get setup to use JCloud BlobStore interface to AWS S3

        Properties overrides = new Properties();
        overrides.setProperty("s3" + ".identity", accessId);
        overrides.setProperty("s3" + ".credential", secretKey);

        final Iterable<? extends Module> MODULES = ImmutableSet.of(
            new JavaUrlHttpCommandExecutorServiceModule(),
            new Log4JLoggingModule(),
            new NettyPayloadModule());

        BlobStoreContext context = ContextBuilder.newBuilder("s3")
            .credentials(accessId, secretKey)
            .modules(MODULES)
            .overrides(overrides)
            .buildView(BlobStoreContext.class);
        BlobStore blobStore = context.getBlobStore();

        // gets all the files in the configured bucket recursively

        PageSet<? extends StorageMetadata> pageSets =
            blobStore.list(bucketName, new ListContainerOptions().recursive());
        logger.debug("   Found {} files in bucket {}", pageSets.size(), bucketName);

        List<String> blobFileNames = new ArrayList<>();
        for ( Object pageSet : pageSets ) {
            String blobFileName = ((MutableBlobMetadata)pageSet).getName();
            if ( blobFileName.endsWith( endsWith )) {
                blobFileNames.add(blobFileName);
            }
        }

        return blobFileNames;
    }


}

