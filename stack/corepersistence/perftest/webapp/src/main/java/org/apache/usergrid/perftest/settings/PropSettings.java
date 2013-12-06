package org.apache.usergrid.perftest.settings;

import com.netflix.config.DynamicPropertyFactory;
import org.apache.usergrid.perftest.NoopPerftestModule;
import org.apache.usergrid.perftest.settings.Props;

import java.util.Date;

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


    public static String getManagerEndpoint() {
        return DynamicPropertyFactory.getInstance().getStringProperty( MANAGER_ENDPOINT_KEY,
                DEFAULT_MANAGER_ENDPOINT ).get();
    }


    public static String getManagerAppUsername() {
        return DynamicPropertyFactory.getInstance().getStringProperty( MANAGER_APP_USERNAME_KEY, "admin" ).get();
    }


    public static String getManagerAppPassword() {
        return DynamicPropertyFactory.getInstance().getStringProperty( MANAGER_APP_PASSWORD_KEY, "secret" ).get();
    }


    public static int getRecoveryRetryCount() {
        return DynamicPropertyFactory.getInstance().getIntProperty(RECOVERY_RETRY_COUNT_KEY,
                DEFAULT_RECOVERY_RETRY_COUNT ).get();
    }


    public static long getRecoveryRetryDelay() {
        return DynamicPropertyFactory.getInstance().getLongProperty( DELAY_RETRY_KEY, DEFAULT_DELAY_RETRY ).get();
    }


    public static String getPerftestVersion() {
        return DynamicPropertyFactory.getInstance().getStringProperty( PERFTEST_VERSION_KEY, "1.0" ).get();
    }


    public static String getCreateTimestamp() {
        return DynamicPropertyFactory.getInstance().getStringProperty( CREATE_TIMESTAMP_KEY,  "none" ).get();
    }


    public static String getGitUuid() {
        return DynamicPropertyFactory.getInstance().getStringProperty( GIT_UUID_KEY, "none" ).get();
    }


    public static String getGitUrl() {
        return DynamicPropertyFactory.getInstance().getStringProperty( GIT_URL_KEY, "none" ).get();
    }


    public static String getGroupId() {
        return DynamicPropertyFactory.getInstance().getStringProperty( GROUP_ID_KEY, "none" ).get();
    }


    public static String getArtifactId() {
        return DynamicPropertyFactory.getInstance().getStringProperty( ARTIFACT_ID_KEY, "none" ).get();
    }


    public static String getTestModuleFqcn() {
        return DynamicPropertyFactory.getInstance().getStringProperty( TEST_MODULE_FQCN_KEY,
                DEFAULT_TEST_MODULE ).get();
    }
}
