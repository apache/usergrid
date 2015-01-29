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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class S3ImportImpl implements S3Import {
    private static final Logger logger = LoggerFactory.getLogger(S3ImportImpl.class);


    /**
     * Downloads the files from s3 into temp local files.
     *
     * @param importInfo     the information entered by the user required to perform import from S3
     * @param filenamePrefix generated based on the request URI
     * @param type           indicates the type of import
     * @return An ArrayList of files i.e. the files downloaded from s3
     */
    public ArrayList<File> copyFromS3(
        final Map<String, Object> importInfo, String filenamePrefix, ImportService.ImportType type) {

        logger.debug("copyFileFromS3(): copying file={} type={}", filenamePrefix, type.toString());

        ArrayList<File> files = new ArrayList<>();

        Map<String, Object> properties = (Map<String, Object>) importInfo.get("properties");

        Map<String, Object> storage_info = (Map<String, Object>) properties.get("storage_info");

        String bucketName = (String) storage_info.get("bucket_location");
        String accessId = (String) storage_info.get("s3_access_id");
        String secretKey = (String) storage_info.get("s3_key");

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

        try {

            BlobStore blobStore = context.getBlobStore();

            // gets all the files in the bucket recursively
            PageSet<? extends StorageMetadata> pageSet =
                blobStore.list(bucketName, new ListContainerOptions().recursive());

            logger.debug("   Found {} files in bucket {}", pageSet.size(), bucketName);

            Iterator itr = pageSet.iterator();

            while (itr.hasNext()) {

                String blobStoreFileName = ((MutableBlobMetadata) itr.next()).getName();
                ParsedFileName pfn = new ParsedFileName(blobStoreFileName);

                switch (type) {

                    // collection file in format <org_name>/<app_name>.<collection_name>.[0-9]+.json
                    case COLLECTION: {
                        List<String> errors = new ArrayList<>();
                        if (pfn.organizationName == null) {
                            errors.add("Filename does not specify organization name");
                        }
                        if (pfn.applicationName == null) {
                            errors.add("Filename does not specify application name");
                        }
                        if (pfn.collectionName == null) {
                            errors.add("Filename does not specify collection name");

                        } else if (!pfn.collectionName.equals(importInfo.get("collectionName"))) {
                            errors.add("Collection name in input file should be " + pfn.collectionName);
                        }
                        if (!errors.isEmpty()) {
                            throw new IllegalArgumentException("Input errors " + errors.toString());
                        }
                        files.add(copyFile(blobStore, bucketName, blobStoreFileName));
                        break;
                    }

                    // application file in format <org_name>/<app_name>.[0-9]+.json
                    case APPLICATION: {
                        List<String> errors = new ArrayList<>();
                        if (pfn.organizationName == null) {
                            errors.add("Filename does not specify organization name");
                        }
                        if (pfn.applicationName == null) {
                            errors.add("Filename does not specify application name");
                        }
                        if (!errors.isEmpty()) {
                            throw new IllegalArgumentException("Input errors " + errors.toString());
                        }

                        files.add(copyFile(blobStore, bucketName, blobStoreFileName));
                        break;
                    }

                    // is an application file in format <org_name>/[-a-zA-Z0-9]+.[0-9]+.json
                    case ORGANIZATION: {
                        List<String> errors = new ArrayList<>();
                        if (pfn.organizationName == null) {
                            errors.add("Filename does not specify organization name");
                        }
                        if (!errors.isEmpty()) {
                            throw new IllegalArgumentException("Input errors " + errors.toString());
                        }
                        files.add(copyFile(blobStore, bucketName, blobStoreFileName));
                        break;
                    }

                    default: {
                        throw new IllegalArgumentException(
                            "Unrecognized import type " + type.toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.debug("   Returning {} files", files.size());
        return files;
    }


    /**
     * Copy the file from s3 into a temp local file.
     *
     * @param bucketName the S3 bucket name from where files need to be imported
     * @param fileName   the filename by which the temp file should be created
     */
    private File copyFile(BlobStore blobStore, String bucketName, String fileName) throws IOException {

        Blob blob = blobStore.getBlob(bucketName, fileName);

        String[] fileOrg = fileName.split("/");
        File organizationDirectory = new File(fileOrg[0]);

        if (!organizationDirectory.exists()) {
            try {
                organizationDirectory.mkdir();

            } catch (SecurityException se) {
                logger.error(se.getMessage());
            }
        }

        File ephemeral = new File(fileName);
        FileOutputStream fop = new FileOutputStream(ephemeral);
        blob.getPayload().writeTo(fop);
        fop.close();

        organizationDirectory.deleteOnExit();
        ephemeral.deleteOnExit();

        return ephemeral;
    }


    /**
     * Break filename down into parts.
     */
    class ParsedFileName {
        String fileName;
        String applicationName;
        String collectionName;
        String organizationName;
        long fileNumber = -1L;

        public ParsedFileName(String fileName) {

            this.fileName = fileName;

            if (fileName.endsWith("\\.json")) {
                logger.debug("Bad filename " + fileName);
                throw new IllegalArgumentException("Import filenames must end with .json");
            }

            if (fileName.contains("/")) {
                String[] parts = fileName.split("/");
                organizationName = parts[0];

                if (parts.length > 1) {
                    String[] secondParts = parts[1].split("\\.");
                    applicationName = secondParts[0];

                    if (secondParts.length > 1) {
                        collectionName = secondParts[1];
                    }

                    if (secondParts.length > 2) {
                        fileNumber = Long.parseLong(secondParts[2]);
                    }
                }
            }

            if (applicationName == null
                && collectionName == null
                && organizationName == null) {
                throw new IllegalArgumentException("Unable to parse import filename " + fileName);
            }

            logger.debug("Parsed " + toString());
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("org: ").append(organizationName);
            sb.append(" app: ").append(applicationName);
            sb.append(" col: ").append(collectionName);
            sb.append(" num: ").append(fileNumber);
            return sb.toString();
        }
    }
}

