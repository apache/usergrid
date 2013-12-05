package org.apache.usergrid.perftest.amazon;


import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Used to encapsulate the various S3 operations to perform.
 */
public class S3Operations {
    private static final Logger LOG = LoggerFactory.getLogger( S3Operations.class );
    private final AmazonS3Client client;


    @Inject
    public S3Operations( AmazonS3Client client ) {
        this.client = client;
    }


    public void register( Ec2Metadata metadata ) {
        StringBuilder sb = new StringBuilder();
        sb.append( PropSettings.getFormation() ).append('-').append( metadata.getPublicHostname() );
        String blobName = sb.toString();

        S3Object s3Object = null;

        try {
            s3Object = client.getObject( PropSettings.getBucket(), PropSettings.getRunners() + "/" );
        }
        catch ( Exception e ) {
            LOG.error( "Attempt to get object failed ... might not exist.", e );
        }

        if ( s3Object == null ) {
            LOG.info( "The runners container was not present: creating it ..." );
            createContainer( PropSettings.getRunners() );
        }
        else {
            try {
                s3Object.getObjectContent().close();
            }
            catch ( IOException e ) {
                LOG.error( "Failed to close object content stream.", e );
            }
        }

        try {
            PutObjectRequest putRequest = new PutObjectRequest( PropSettings.getBucket(),
                    PropSettings.getRunners() + "/" + blobName,
                    metadata.getPropertiesAsStream(), new ObjectMetadata() );
            client.putObject( putRequest );
        }
        catch ( IOException e ) {
            LOG.error( "Failed to create input stream for object.", e );
        }

        LOG.info( "Successfully registered {}", blobName);
    }


    public void createContainer( String name ) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength( 0 );

        InputStream emptyContent = new ByteArrayInputStream( new byte[0] );
        PutObjectRequest putRequest = new PutObjectRequest( PropSettings.getBucket(), name + "/",
                emptyContent, objectMetadata );
        client.putObject( putRequest );

        LOG.info( "Successfully created container {}", name );
    }


    public Set<String> getTests() {
        S3Object s3Object = null;

        try {
            s3Object = client.getObject( PropSettings.getBucket(), PropSettings.getTests() + "/" );
        }
        catch ( Exception e ) {
            LOG.error( "Attempt to get tests container object failed ... might not exist.", e );
        }

        if ( s3Object == null ) {
            LOG.info( "The tests container was not present: creating it ..." );
            createContainer( PropSettings.getTests() );
        }
        else {
            try {
                s3Object.getObjectContent().close();
            }
            catch ( IOException e ) {
                LOG.error( "Failed to close object content stream.", e );
            }
        }

        Set<String> tests = new HashSet<String>();
        ObjectListing listing = client.listObjects( PropSettings.getBucket(), PropSettings.getTests() + "/" );

        do {
            for ( S3ObjectSummary summary : listing.getObjectSummaries() ) {
                if ( summary.getKey().equals( PropSettings.getTests() + "/" ) )
                {
                    continue;
                }

                tests.add( summary.getKey() );
            }

            listing = client.listNextBatchOfObjects( listing );
        }
        while ( listing.isTruncated() );

        return tests;
    }


    public Map<String,Ec2Metadata> getRunners() {
        S3Object s3Object = null;

        try {
            s3Object = client.getObject( PropSettings.getBucket(), PropSettings.getRunners() + "/" );
        }
        catch ( Exception e ) {
            LOG.error( "Attempt to get runners container object failed ... might not exist.", e );
        }

        if ( s3Object == null ) {
            LOG.info( "The runners container was not present: creating it ..." );
            createContainer( PropSettings.getRunners() );
        }
        else {
            try {
                s3Object.getObjectContent().close();
            }
            catch ( IOException e ) {
                LOG.error( "Failed to close object content stream.", e );
            }
        }

        Map<String,Ec2Metadata> runners = new HashMap<String, Ec2Metadata>();
        ObjectListing listing = client.listObjects( PropSettings.getBucket(),
                PropSettings.getRunners() + "/" + PropSettings.getFormation() );

        do {
            for ( S3ObjectSummary summary : listing.getObjectSummaries() ) {
                String key = summary.getKey();

                LOG.debug( "Got key {} while scanning under runners container", key );

                s3Object = client.getObject( PropSettings.getBucket(), key );

                try {
                    runners.put( key, new Ec2Metadata( s3Object.getObjectContent() ) );
                }
                catch ( IOException e ) {
                    LOG.error( "Failed to load metadata for runner {}", key, e );
                }
            }

            listing = client.listNextBatchOfObjects( listing );
        }
        while ( listing.isTruncated() );

        return runners;
    }


    public File download( File tempDir, String perftest ) throws IOException, InterruptedException {
        File tempFile = File.createTempFile( "perftest", "war", tempDir );
        LOG.debug( "Created temporary file {} for new war download.", tempFile.getAbsolutePath() );

        S3Object s3Object = client.getObject( PropSettings.getBucket(), perftest );
        LOG.debug( "Got S3Object:\n{}", s3Object.toString() );

        // Download war file contents into temporary file
        S3ObjectInputStream in = s3Object.getObjectContent();
        FileOutputStream out = new FileOutputStream( tempFile );
        byte[] buffer = new byte[1024];
        int readAmount;

        while ( ( readAmount = in.read( buffer ) ) != -1 ) {
            out.write( buffer, 0, readAmount );
        }

        out.flush();
        out.close();
        in.close();
        LOG.info( "Successfully downloaded {} from S3 to {}.", perftest, tempFile.getAbsoluteFile() );
        return tempFile;
    }
}
