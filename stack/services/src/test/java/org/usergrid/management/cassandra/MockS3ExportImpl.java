package org.usergrid.management.cassandra;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.management.ExportInfo;
import org.usergrid.management.export.S3Export;


/**
 * Writes to file instead of s3.
 *
 */
public class MockS3ExportImpl implements S3Export {
    @Override
    public void copyToS3( final InputStream inputStream, final ExportInfo exportInfo, String filename ) {
        Logger logger = LoggerFactory.getLogger( MockS3ExportImpl.class );
        int read = 0;
        byte[] bytes = new byte[1024];
        OutputStream outputStream = null;
        //FileInputStream fis = new PrintWriter( inputStream );

        try {
            outputStream = new FileOutputStream( new File("test.json") );

        }
        catch ( FileNotFoundException e ) {
            e.printStackTrace();
        }


        try {
            while ( (read = (inputStream.read( bytes ))) != -1) {
                outputStream.write( bytes, 0, read );
            }
              
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }





        //        Logger logger = LoggerFactory.getLogger( ExportServiceImpl.class );
//        /*won't need any of the properties as I have the export info*/
//        String bucketName = exportInfo.getBucket_location();
//        String accessId = exportInfo.getS3_accessId();
//        String secretKey = exportInfo.getS3_key();
//
//        Properties overrides = new Properties();
//        overrides.setProperty( "s3" + ".identity", accessId );
//        overrides.setProperty( "s3" + ".credential", secretKey );
//
//        final Iterable<? extends Module> MODULES = ImmutableSet
//                .of( new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(), new NettyPayloadModule
//                        () );
//
//        BlobStoreContext context =
//                ContextBuilder.newBuilder( "s3" ).credentials( accessId, secretKey ).modules( MODULES )
//                              .overrides( overrides ).buildView( BlobStoreContext.class );
//
//        // Create Container (the bucket in s3)
//        try {
//            AsyncBlobStore blobStore = context.getAsyncBlobStore(); // it can be changed to sync
//            // BlobStore (returns false if it already exists)
//            ListenableFuture<Boolean> container = blobStore.createContainerInLocation( null, bucketName );
//            if ( container.get() ) {
//                logger.info( "Created bucket " + bucketName );
//            }
//        }
//        catch ( Exception ex ) {
//            logger.error( "Could not start binary service: {}", ex.getMessage() );
//            //throw new RuntimeException( ex );
//        }
//
//        try {
//
//
//            AsyncBlobStore blobStore = context.getAsyncBlobStore();
//            BlobBuilder blobBuilder =
//                    blobStore.blobBuilder( filename ).payload( inputStream ).calculateMD5().contentType( "text/plain" );
//
//
//            Blob blob = blobBuilder.build();
//
//            ListenableFuture<String> futureETag = blobStore.putBlob( bucketName, blob, PutOptions.Builder.multipart() );
//
//            logger.info( "Uploaded file etag=" + futureETag.get() );
//        }
//        catch ( Exception e ) {
//            logger.error( "Error uploading to blob store", e );
//        }

    }
}
