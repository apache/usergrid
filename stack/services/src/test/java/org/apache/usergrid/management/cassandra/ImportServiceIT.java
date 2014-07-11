/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.management.cassandra;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.export.S3Export;
import org.apache.usergrid.management.export.S3ExportImpl;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
@Concurrent
public class ImportServiceIT {

    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceIT.class);

    private static CassandraResource cassandraResource = ServiceITSuite.cassandraResource;

    // app-level data generated only once
    private static UserInfo adminUser;
    private static OrganizationInfo organization;
    private static UUID applicationId;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static final ServiceITSetup setup = new ServiceITSetupImpl( cassandraResource );


    @BeforeClass
    public static void setup() throws Exception {
        LOG.info( "in setup" );
        adminUser = setup.getMgmtSvc().createAdminUser( "pooja", "Pooja Jain", "pooja@jain.com", "test", false, false );
        organization = setup.getMgmtSvc().createOrganization( "pooja-organization", adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "pooja-application" ).getId();
    }



    // @Ignore //For this test please input your s3 credentials into settings.xml or Attach a -D with relevant fields.
    @Test
    public void testIntegration100EntitiesOn() throws Exception {

        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );



        String bucketName = System.getProperty( "bucketName" );
        String accessId = System.getProperty( "accessKey" );
        String secretKey = System.getProperty( "secretKey" );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );


        final Iterable<? extends Module> MODULES = ImmutableSet
                .of(new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
                        new NettyPayloadModule());

        BlobStoreContext context =
                ContextBuilder.newBuilder("s3").credentials( accessId, secretKey ).modules( MODULES )
                        .overrides( overrides ).buildView( BlobStoreContext.class );


        FileOutputStream fop = null;

        File ephemeral = new File("temp_file");
        ephemeral.deleteOnExit();
        try{
            BlobStore blobStore = context.getBlobStore();
            Blob blob = blobStore.getBlob(bucketName, s3Export.getFilename());

            fop = new FileOutputStream(ephemeral);
            blob.getPayload().writeTo(fop);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ContainerNotFoundException m) {
            m.printStackTrace();
        }
        assertNotNull( ephemeral );
    }

    /*Creates fake payload for testing purposes.*/
    public HashMap<String, Object> payloadBuilder() {
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        storage_info.put( "s3_key", System.getProperty( "secretKey" ) );
        storage_info.put( "s3_access_id", System.getProperty( "accessKey" ) );
        storage_info.put( "bucket_location", System.getProperty( "bucketName" ) );

        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );

        payload.put( "path", "test-organization/test-app" );
        payload.put( "properties", properties );
        return payload;
    }

}
