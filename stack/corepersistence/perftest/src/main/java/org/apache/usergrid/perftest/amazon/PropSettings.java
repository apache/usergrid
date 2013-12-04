package org.apache.usergrid.perftest.amazon;

import com.netflix.config.DynamicPropertyFactory;

/**
 * Easy access to dynamic properties. Make sure we do not cache values
 * and get every time.
 */
public class PropSettings implements Props {

    /**
     * Gets the dynamic property for the AWS S3 key.
     *
     * @return the AWS S3 key
     */
    public static String getAwsKey()
    {
        return DynamicPropertyFactory.getInstance().getStringProperty( AWS_KEY, "none" ).get();
    }


    /**
     * Gets the dynamic property for the AWS S3 secret.
     *
     * @return the AWS S3 secret
     */
    public static String getAwsSecret()
    {
        return DynamicPropertyFactory.getInstance().getStringProperty( AWS_SECRET_KEY, "none" ).get();
    }


    /**
     * Gets the dynamic property for the formation.
     *
     * @return the name of the formation
     */
    public static String getFormation()
    {
        return DynamicPropertyFactory.getInstance().getStringProperty( FORMATION_KEY, DEFAULT_FORMATION ).get();
    }


    /**
     * Gets the dynamic property for the bucket name.
     *
     * @return the name of the bucket
     */
    public static String getBucket()
    {
        return DynamicPropertyFactory.getInstance().getStringProperty( AWS_BUCKET_KEY, DEFAULT_BUCKET ).get();
    }


    /**
     * Gets the dynamic property for the "runners" container in our bucket.
     *
     * @return the name of the container used for runners in our bucket
     */
    public static String getRunners()
    {
        return DynamicPropertyFactory.getInstance().getStringProperty( RUNNERS_KEY, DEFAULT_RUNNERS ).get();
    }


    /**
     * Gets the dynamic property for the "tests" container in our bucket.
     *
     * @return the name of the container used for tests in our bucket
     */
    public static String getTests()
    {
        return DynamicPropertyFactory.getInstance().getStringProperty( TESTS_KEY, DEFAULT_TESTS ).get();
    }


    /**
     * Gets the dynamic property for the scan period in milliseconds.
     *
     * @return the time to wait between scans in milliseconds
     */
    public static long getScanPeriod()
    {
        return DynamicPropertyFactory.getInstance().getLongProperty( SCAN_PERIOD_KEY, DEFAULT_SCAN_PERIOD ).get();
    }

    public static int getServerPort() {
        return DynamicPropertyFactory.getInstance().getIntProperty( SERVER_PORT_KEY, DEFAULT_SERVER_PORT ).get();
    }
}
