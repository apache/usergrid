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
package org.apache.usergrid.services.assets.data;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.exceptions.RequiredPropertyNotFoundException;
import org.apache.usergrid.persistence.queue.impl.UsergridAwsCredentialsProvider;
import org.apache.usergrid.services.exceptions.AwsPropertiesNotFoundException;
import org.apache.usergrid.utils.StringUtils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.glacier.model.ListMultipartUploadsResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.google.common.primitives.Ints;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;


public class  AwsSdkS3BinaryStore implements BinaryStore {

    private static final Logger logger = LoggerFactory.getLogger(AwsSdkS3BinaryStore.class );
    private static final long FIVE_MB = ( FileUtils.ONE_MB * 5 );

    private AmazonS3 s3Client;
    private String accessId;
    private String secretKey;
    private String bucketName;
    private String regionName;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private Properties properties;

    public AwsSdkS3BinaryStore( ) {
    }

    //TODO: GREY rework how the s3 client works because currently it handles initlization and returning of the client
    //ideally it should only do one. and the client should be initlized at the beginning of the run.
    private AmazonS3 getS3Client() throws Exception{

        this.bucketName = properties.getProperty( "usergrid.binary.bucketname" );
        if(bucketName == null){
            logger.error( "usergrid.binary.bucketname  not properly set so amazon bucket is null" );
            throw new AwsPropertiesNotFoundException( "usergrid.binary.bucketname" );

        }

        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        AWSCredentials credentials = ugProvider.getCredentials();
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        s3Client = new AmazonS3Client(credentials, clientConfig);
        if(regionName != null)
            s3Client.setRegion( Region.getRegion(Regions.fromName(regionName)) );

        return s3Client;
    }

    @Override
    public void write( final UUID appId, final Entity entity, InputStream inputStream ) throws Exception {

        String uploadFileName = AssetUtils.buildAssetKey( appId, entity );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long written = IOUtils.copyLarge( inputStream, baos, 0, FIVE_MB );

        byte[] data = baos.toByteArray();

        InputStream awsInputStream = new ByteArrayInputStream(data);

        final Map<String, Object> fileMetadata = AssetUtils.getFileMetadata( entity );
        fileMetadata.put( AssetUtils.LAST_MODIFIED, System.currentTimeMillis() );

        String mimeType = AssetMimeHandler.get().getMimeType( entity, data );

        Boolean overSizeLimit = false;

        if ( written < FIVE_MB ) { // total smaller than 5mb

            ObjectMetadata om = new ObjectMetadata();
            om.setContentLength(written);
            om.setContentType( mimeType );
            PutObjectResult result = null;
            result = getS3Client().putObject( bucketName, uploadFileName, awsInputStream, om );

            String md5sum = Hex.encodeHexString( Base64.decodeBase64(result.getContentMd5()) );
            String eTag = result.getETag();

            fileMetadata.put( AssetUtils.CONTENT_LENGTH, written );

            if(md5sum != null)
                fileMetadata.put( AssetUtils.CHECKSUM, md5sum );
            fileMetadata.put( AssetUtils.E_TAG, eTag );
        }
        else { // bigger than 5mb... dump 5 mb tmp files and upload from them
            written = 0; //reset written to 0, we still haven't wrote anything in fact
            int partNumber = 1;
            int firstByte = 0;
            Boolean isFirstChunck = true;
            List<PartETag> partETags = new ArrayList<PartETag>();


            //get the s3 client in order to initialize the multipart request
            getS3Client();
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest( bucketName, uploadFileName );
            InitiateMultipartUploadResult initResponse = getS3Client().initiateMultipartUpload( initRequest );


            InputStream firstChunck = new ByteArrayInputStream(data);
            PushbackInputStream chunckableInputStream = new PushbackInputStream(inputStream, 1);

            // determine max size file allowed, default to 50mb
            long maxSizeBytes = 50 * FileUtils.ONE_MB;
            String maxSizeMbString = properties.getProperty( "usergrid.binary.max-size-mb", "50" );
            if ( StringUtils.isNumeric( maxSizeMbString )) {
                maxSizeBytes = Long.parseLong( maxSizeMbString ) * FileUtils.ONE_MB;
            }

            // always allow files up to 5mb
            if (maxSizeBytes < 5 * FileUtils.ONE_MB ) {
                maxSizeBytes = 5 * FileUtils.ONE_MB;
            }

            while (-1 != (firstByte = chunckableInputStream.read())) {
                long partSize = 0;
                chunckableInputStream.unread(firstByte);
                File tempFile = File.createTempFile( entity.getUuid().toString().concat("-part").concat(String.valueOf(partNumber)), "tmp" );

                tempFile.deleteOnExit();
                OutputStream os = null;
                try {
                    os = new BufferedOutputStream( new FileOutputStream( tempFile.getAbsolutePath() ) );

                    if(isFirstChunck == true) {
                        partSize = IOUtils.copyLarge( firstChunck, os, 0, ( FIVE_MB ) );
                        isFirstChunck = false;
                    }
                    else {
                        partSize = IOUtils.copyLarge( chunckableInputStream, os, 0, ( FIVE_MB ) );
                    }
                    written += partSize;

                    if(written> maxSizeBytes){
                        overSizeLimit = true;
                        logger.error( "OVERSIZED FILE. STARTING ABORT" );
                        break;
                        //set flag here and break out of loop to run abort
                    }
                }
                finally {
                    IOUtils.closeQuietly( os );
                }

                FileInputStream chunk = new FileInputStream(tempFile);

                Boolean isLastPart = -1 == (firstByte = chunckableInputStream.read());
                if(!isLastPart)
                    chunckableInputStream.unread(firstByte);

                UploadPartRequest uploadRequest = new UploadPartRequest().withUploadId(initResponse.getUploadId())
                                                                         .withBucketName(bucketName)
                                                                         .withKey(uploadFileName)
                                                                         .withInputStream(chunk)
                                                                         .withPartNumber(partNumber)
                                                                         .withPartSize(partSize)
                                                                         .withLastPart(isLastPart);
                partETags.add( getS3Client().uploadPart(uploadRequest).getPartETag() );
                partNumber++;
            }

            //check for flag here then abort.
            if(overSizeLimit) {

                EntityManager em = emf.getEntityManager( appId );
                AbortMultipartUploadRequest abortRequest =
                    new AbortMultipartUploadRequest( bucketName, uploadFileName, initResponse.getUploadId() );

                ListMultipartUploadsRequest listRequest = new ListMultipartUploadsRequest( bucketName );

                MultipartUploadListing listResult = getS3Client().listMultipartUploads( listRequest );

                //upadte the entity with the error.
                try {
                    logger.error("starting update of entity due to oversized asset");
                    fileMetadata.put( "error", "Asset size is larger than max size of " + maxSizeBytes );
                    em.update( entity );
                }
                catch ( Exception e ) {
                    logger.error( "Error updating entity with error message", e );
                }

                int timesIterated = 20;
                //loop and abort all the multipart uploads
                while ( listResult.getMultipartUploads().size()!=0  && timesIterated > 0) {

                    getS3Client().abortMultipartUpload( abortRequest );
                    Thread.sleep( 1000 );
                    timesIterated--;
                    listResult = getS3Client().listMultipartUploads( listRequest );
                    logger.debug( "Files that haven't been aborted are: ",listResult.getMultipartUploads().listIterator().toString() );

                }
                if ( timesIterated == 0 ){
                    logger.error( "Files parts that couldn't be aborted in 20 seconds are:" );
                    Iterator<MultipartUpload> multipartUploadIterator = listResult.getMultipartUploads().iterator();
                    while(multipartUploadIterator.hasNext()){
                        logger.error( multipartUploadIterator.next().getKey() );
                    }
                }
            }
            else {
                CompleteMultipartUploadRequest request =
                    new CompleteMultipartUploadRequest( bucketName, uploadFileName, initResponse.getUploadId(),
                        partETags );
                CompleteMultipartUploadResult amazonResult = getS3Client().completeMultipartUpload( request );
                fileMetadata.put( AssetUtils.CONTENT_LENGTH, written );
                fileMetadata.put( AssetUtils.E_TAG, amazonResult.getETag() );
            }
        }
    }


    @Override
    public InputStream read( UUID appId, Entity entity, long offset, long length ) throws Exception {

        S3Object object = getS3Client().getObject( bucketName, AssetUtils.buildAssetKey( appId, entity ) );

        byte data[] = null;

        if ( offset == 0 && length == FIVE_MB ) {
            return object.getObjectContent();
        }
        else {
            object.getObjectContent().read(data, Ints.checkedCast(offset), Ints.checkedCast(length));
        }

        return new ByteArrayInputStream(data);
    }


    @Override
    public InputStream read( UUID appId, Entity entity ) throws Exception {
        return read( appId, entity, 0, FIVE_MB );
    }


    @Override
    public void delete( UUID appId, Entity entity ) throws Exception {
        getS3Client().deleteObject(new DeleteObjectRequest(bucketName, AssetUtils.buildAssetKey( appId, entity )));
    }
}
