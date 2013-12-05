package org.apache.usergrid.perftest.plugin;


import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;


public class PerftestMojoTest {
    @Test
    @Ignore
    public void testGetS3Client() {
        try {
            Method method =
                    PerftestMojo.class.getDeclaredMethod( "getS3Client", new Class[] { String.class, String.class } );
            method.setAccessible( true );
            AmazonS3 amazonS3 = ( AmazonS3 ) method.invoke( null, "***", "***" );
            Assert.assertNotNull( "AmazonS3 client should not be null", amazonS3 );
            List<Bucket> buckets = amazonS3.listBuckets();
            System.out.println( "There are " + buckets.size() + " buckets" );
            if ( buckets.size() != 0 ) {
                System.out.println( "First bucket name is " + buckets.get( 0 ).getName() );
            }
        }
        catch ( Exception e ) {
            Assert.fail( "Get AmazonS3 client failed : " + e.getMessage() );
        }
    }


    @Test
    @Ignore
    public void testExecute() {
        try {
            PerftestMojo uploader = new PerftestMojo();
            Field f1 = PerftestMojo.class.getDeclaredField( "accessKey" );
            Field f2 = PerftestMojo.class.getDeclaredField( "secretKey" );
            Field f3 = PerftestMojo.class.getDeclaredField( "bucketName" );
            Field f4 = PerftestMojo.class.getDeclaredField( "sourceFile" );
            Field f5 = PerftestMojo.class.getDeclaredField( "destinationFile" );

            f1.setAccessible( true );
            f2.setAccessible( true );
            f3.setAccessible( true );
            f4.setAccessible( true );
            f5.setAccessible( true );

            f1.set( uploader, "***" );
            f2.set( uploader, "***" );
            f3.set( uploader, "perftest-uploadplugin-test" );
            f4.set( uploader, "***/Documents/testfile.rtf" );
            f5.set( uploader, "test/testfile.rtf" );

            uploader.execute();
        }
        catch ( Exception e ) {
            Assert.fail( "Execute plugin failed : " + e.getMessage() );
        }
    }


    @Test
    @Ignore
    public void testExtractWar() {
        try {
            PerftestWarMojo u = new PerftestWarMojo();
            File war = new File(
                    "***/IdeaProjects/s3-upload-maven-plugin/S3UploadPluginWorker/target/S3UploadPluginWorker.war" );
            String extractRoot = "***/IdeaProjects/s3-upload-maven-plugin/S3UploadPluginWorker/target/extracted/";
            u.extractWar( war, extractRoot );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }


    @Test
    @Ignore
    public void testGetGitRemoteUrl() {
        try {
            PerftestWarMojo u = new PerftestWarMojo();
            String url = u.getGitRemoteUrl( "***/IdeaProjects/s3-upload-maven-plugin/.git" );
            System.out.println( url );
            Assert.assertTrue( url.startsWith( "http" ) && url.endsWith( ".git" ) );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }


    @Test
    @Ignore
    public void testIsCommitNecessary() {
        try {
            PerftestWarMojo u = new PerftestWarMojo();
            u.isCommitNecessary( "***/IdeaProjects/s3-upload-maven-plugin/.git" );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            Assert.fail( e.getMessage() );
        }
    }
}