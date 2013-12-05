package org.apache.usergrid.perftest.plugin;


import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;


@Mojo(name = "perftest")
public class PerftestMojo extends AbstractMojo {
    /** Access key for S3. */
    @Parameter(property = "perftest.accessKey")
    protected String accessKey;

    /** Secret key for S3. */
    @Parameter(property = "perftest.secretKey")
    protected String secretKey;

    /** Execute all steps up except the upload to the S3.  This can be set to true to perform a "dryRun" execution. */
    @Parameter(property = "s3repo.doNotUpload", defaultValue = "false")
    protected boolean doNotUpload;

    /** The file to upload. */
    @Parameter(property = "perftest.sourceFile", required = true)
    protected String sourceFile;

    /** The bucket to upload into. */
    @Parameter(property = "perftest.bucketName", required = true)
    protected String bucketName;

    /** The file (in the bucket) to create. */
    @Parameter(property = "perftest.destinationFile")
    protected String destinationFile;

    /** Force override of endpoint for S3 regions such as EU. */
    @Parameter(property = "perftest.endpoint")
    protected String endpoint;


    @Override
    public void execute() throws MojoExecutionException {
        File source = new File( sourceFile );
        if ( !source.exists() ) {
            throw new MojoExecutionException( "File doesn't exist: " + sourceFile );
        }

        AmazonS3 s3 = getS3Client( accessKey, secretKey );
        if ( endpoint != null ) {
            s3.setEndpoint( endpoint );
        }

        if ( !s3.doesBucketExist( bucketName ) ) {
            throw new MojoExecutionException( "Bucket doesn't exist: " + bucketName );
        }

        boolean success = upload( s3, bucketName, destinationFile, source );
        if ( !success ) {
            throw new MojoExecutionException( "Unable to upload file to S3." );
        }

        getLog().info( "File " + source + " uploaded to s3://" + bucketName + "/" + destinationFile );
    }


    protected static AmazonS3 getS3Client( String accessKey, String secretKey ) {
        AWSCredentialsProvider provider;
        if ( accessKey != null && secretKey != null ) {
            AWSCredentials credentials = new BasicAWSCredentials( accessKey, secretKey );
            provider = new StaticCredentialsProvider( credentials );
        }
        else {
            provider = new DefaultAWSCredentialsProviderChain();
        }

        return new AmazonS3Client( provider );
    }


    protected static boolean upload( AmazonS3 s3, String bucketName, String destinationFile, File source ) {
        TransferManager mgr = new TransferManager( s3 );
        Upload upload = mgr.upload( bucketName, destinationFile, source );

        try {
            upload.waitForUploadResult();
        }
        catch ( InterruptedException e ) {
            return false;
        }

        return true;
    }
}
