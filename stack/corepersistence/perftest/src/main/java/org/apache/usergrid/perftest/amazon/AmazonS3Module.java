/*
 * Created by IntelliJ IDEA.
 * User: akarasulu
 * Date: 12/2/13
 * Time: 3:17 AM
 */
package org.apache.usergrid.perftest.amazon;

import com.google.inject.AbstractModule;

public class AmazonS3Module extends AbstractModule {
    protected void configure() {
        bind( AmazonS3Service.class ).to( AmazonS3ServiceImpl.class ).asEagerSingleton();
    }
}
