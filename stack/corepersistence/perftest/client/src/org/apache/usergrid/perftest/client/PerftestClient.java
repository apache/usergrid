package org.apache.usergrid.perftest.client;


/**
 * A client to interact with the perftest web application and it's
 * S3 bucket. The S3 bucket operations allow:
 *
 * <ul>
 *     <li>listing registered runners in the cluster</li>
 *     <li>listing and deleting uploaded test jars and their test information</li>
 *     <li>downloading and collating test run results from runners</li>
 *     <li></li>
 * </ul>
 */
public interface PerftestClient {

}
