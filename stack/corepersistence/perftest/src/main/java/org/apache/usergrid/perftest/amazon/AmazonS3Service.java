package org.apache.usergrid.perftest.amazon;

/**
 * The S3 Service is used to register the node so other nodes in the same
 * perftest formation can access it.
 */
public interface AmazonS3Service {
    String AWS_BUCKET_KEY = "aws.s3.bucket";
    String AWS_KEY = "aws.s3.key";
    String AWS_SECRET = "aws.s3.secret";
    String FORMATION_KEY = "perftest.formation";
    String ARCHAIUS_CONTAINER_KEY = "com.netflix.config.blobstore.containerName";
    String PUBLIC_HOSTNAME_KEY = "public-hostname";
    String LOCAL_HOSTNAME_KEY = "local-hostname";
    String PUBLIC_IPV4_KEY = "public-ipv4";
    String LOCAL_IPV4_KEY = "local-ipv4";
    String INSTANCE_URL = "http://169.254.169.254/latest/meta-data";

    void start();

    boolean isStarted();

    void stop();
}
