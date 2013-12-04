package org.apache.usergrid.perftest.amazon;

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
}
