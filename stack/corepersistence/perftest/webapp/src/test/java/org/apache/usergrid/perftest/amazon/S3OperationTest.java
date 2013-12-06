package org.apache.usergrid.perftest.amazon;


import com.google.inject.Guice;
import org.junit.Ignore;
import org.junit.Test;


/**
 */
public class S3OperationTest {
    S3Operations operations = Guice.createInjector(
            new AmazonS3Module() ).getInstance( S3Operations.class );

    @Test @Ignore
    public void testRunnersListing() {
        operations.getRunners();
    }


    @Test @Ignore
    public void testRegister() {
        Ec2Metadata metadata = new Ec2Metadata();
        metadata.setProperty( "foo", "bar" );
        operations.register( metadata );
    }
}
