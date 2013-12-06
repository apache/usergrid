package org.apache.usergrid.perftest.settings;

/**
 *
 */
public interface Props {
    String FORMATION_KEY = "perftest.formation";
    String DEFAULT_FORMATION = "default";

    String ARCHAIUS_CONTAINER_KEY = "com.netflix.config.blobstore.containerName";

    String AWS_KEY = "aws.s3.key";
    String AWS_SECRET_KEY = "aws.s3.secret";

    String AWS_BUCKET_KEY = "aws.s3.bucket";
    String DEFAULT_BUCKET = "perftest-bucket";

    String RUNNERS_KEY = "runners.container";
    String DEFAULT_RUNNERS = "runners";

    String TESTS_KEY = "tests.container";
    String DEFAULT_TESTS = "tests";

    String SCAN_PERIOD_KEY = "scan.period.milliseconds";
    long DEFAULT_SCAN_PERIOD = 300000L;

    String SERVER_PORT_KEY = "server.port";
    int DEFAULT_SERVER_PORT = 8080;

    String SERVER_INFO_KEY = "server.info";
    String CONTEXT_PATH = "context.path";

    String CONTEXT_TEMPDIR_KEY = "javax.servlet.context.tempdir";
    String MANAGER_ENDPOINT_KEY = "manager.endpoint";
    String DEFAULT_MANAGER_ENDPOINT = "http://localhost:8080/manager/text";
    String MANAGER_APP_PASSWORD_KEY = "manager.app.password";
    String MANAGER_APP_USERNAME_KEY = "manager.app.username";

    /** prop key for number of times to retry recovery operations */
    String RECOVERY_RETRY_COUNT_KEY = "recovery.retry.count";
    /** default for number of times to retry recovery operations */
    int DEFAULT_RECOVERY_RETRY_COUNT = 3;

    /** prop key for the time to wait between retry recovery operations */
    String DELAY_RETRY_KEY = "recovery.retry.delay";
    /** default for the time to wait in milliseconds between retry recovery operations */
    long DEFAULT_DELAY_RETRY = 10000;


    String PERFTEST_VERSION_KEY = "perftest.version";

    String CREATE_TIMESTAMP_KEY = "create.timestamp";

    String GIT_UUID_KEY = "git.uuid";

    String GIT_URL_KEY = "git.url";

    String GROUP_ID_KEY = "group.id";

    String ARTIFACT_ID_KEY = "artifact.id";

    String TEST_MODULE_FQCN_KEY = "test.module.fqcn";

    String DEFAULT_TEST_MODULE = "org.apache.usergrid.perftest.NoopPerftestModule";
}
