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

package org.apache.usergrid.rest.management;

import com.amazonaws.SDKGlobalConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Module;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.management.importer.S3Upload;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.Organization;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;


public class ImportResourceIT extends AbstractRestIT {

    private static final Logger logger = LoggerFactory.getLogger(ImportResourceIT.class);


    private static String bucketPrefix;

    private String bucketName;

    boolean configured;


    public ImportResourceIT() throws Exception {

    }


    @ClassRule
    public static final ServiceITSetup setup =
        new ServiceITSetupImpl();

    @BeforeClass
    public static void setup() throws Exception {

        bucketPrefix = System.getProperty("bucketName");

        // start the scheduler after we're all set up
        JobSchedulerService jobScheduler = ConcurrentProcessSingleton.getInstance()
            .getSpringResource().getBean( JobSchedulerService.class );

        if (jobScheduler.state() != Service.State.RUNNING) {
            jobScheduler.startAsync();
            jobScheduler.awaitRunning();
        }

    }

    @Before
    public void before() {
        configured =
                   !StringUtils.isEmpty(System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ))
                && !StringUtils.isEmpty(System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ))
                && !StringUtils.isEmpty(System.getProperty("bucketName"));


        if (!configured) {
            logger.warn("Skipping test because {}, {} and bucketName not " +
                    "specified as system properties, e.g. in your Maven settings.xml file.",
                new Object[]{
                    "s3_key",
                    "s3_access_id"
                });
        }

//        if (!StringUtils.isEmpty(bucketPrefix)) {
//            deleteBucketsWithPrefix();
//        }

        bucketName = bucketPrefix + RandomStringUtils.randomAlphanumeric(10).toLowerCase();
    }


    /**
     * Verify that we can get call the import endpoint and get the job state back.
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void importGetCollectionJobStatTest() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();

        ///management/orgs/orgname/apps/appname/import
        Entity entity = this.management()
            .orgs()
            .org( org )
            .app()
            .addToPath(app)
            .addToPath("imports")
            .post(Entity.class,payload);

        assertNotNull(entity);

        entity = this.management()
            .orgs()
            .org( org )
            .app()
            .addToPath(app)
            .addToPath("imports")
            .addToPath(entity.getUuid().toString())
            .get();

        assertNotNull(entity.getAsString("state"));
    }

    /**
     * Verify that import job can only be read with an authorized token and cannot be read
     * with an invalid/notAllowed token.
     */
    @Ignore
    @Test
    public void importTokenAuthorizationTest() throws Exception {

        // this test should post one import job with one token,
        // then try to read back the job with another token

        // create an import job
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Entity payload = payloadBuilder();

        // /management/orgs/orgname/apps/appname/import
        Entity entity = this.management().orgs().org( org ).app()
            .addToPath(app)
            .addToPath("imports")
            .post(Entity.class,payload);

        assertNotNull(entity);

        // test that you can access the organization using the currently set token.
        this.management().orgs().org( org ).app().addToPath(app)
            .addToPath("imports").addToPath(entity.getUuid().toString()).get();

        //create a new org/app
        String newOrgName = "org" + UUIDUtils.newTimeUUID();
        String newOrgUsername = "orgusername" + UUIDUtils.newTimeUUID();
        String newOrgEmail = UUIDUtils.newTimeUUID() + "@usergrid.com";
        String newOrgPassword = "password1";
        Organization orgPayload = new Organization(
            newOrgName, newOrgUsername, newOrgEmail, newOrgName, newOrgPassword, null);
        Organization orgCreatedResponse = clientSetup.getRestClient().management().orgs().post(orgPayload);
        this.refreshIndex();
        assertNotNull(orgCreatedResponse);


        //log into the new org/app and get a token
        Token tokenPayload = new Token("password", newOrgUsername, newOrgPassword);
        Token newOrgToken = clientSetup.getRestClient().management().token().post(Token.class,tokenPayload);

        //save the old token and set the newly issued token as current
        context().setToken(newOrgToken);


        //try to read with the new token, which should fail as unauthorized
        try {
            this.management().orgs().org( org ).app().addToPath(app)
                .addToPath("imports").addToPath(entity.getUuid().toString()).get();
            fail("Should not be able to read import job with unauthorized token");
        } catch (ClientErrorException ex) {
            errorParse(401, "unauthorized", ex);
        }

    }


    @Ignore
    @Test
    public void importPostApplicationNullPointerProperties() throws Exception {
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Response.Status responseStatus = Response.Status.OK;

        Entity payload = new Entity();

        try {
            this.management().orgs().org( org ).app().addToPath(app).addToPath("imports").post(Entity.class,payload);
        } catch (ClientErrorException uie) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertEquals(Response.Status.BAD_REQUEST, responseStatus);
    }

    @Ignore
    @Test
    public void importPostApplicationNullPointerStorageInfo() throws Exception {
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Response.Status responseStatus = Response.Status.OK;

        Entity payload = payloadBuilder();
        Entity properties = (Entity) payload.get("properties");
        //remove storage_info field
        properties.remove("storage_info");

        try {
            this.management().orgs().org( org ).app().addToPath(app).addToPath("imports").post(Entity.class,payload);
        } catch (ClientErrorException uie) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertEquals(Response.Status.BAD_REQUEST, responseStatus);
    }


    @Ignore
    @Test
    public void importPostApplicationNullPointerStorageProvider() throws Exception {
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Response.Status responseStatus = Response.Status.OK;

        Entity payload = payloadBuilder();
        Entity properties = (Entity) payload.get("properties");
        //remove storage_info field
        properties.remove("storage_provider");


        try {
            this.management().orgs().org( org ).app().addToPath(app).addToPath("imports").post(Entity.class,payload);
        } catch (ClientErrorException uie) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertEquals(Response.Status.BAD_REQUEST, responseStatus);
    }


    @Ignore
    @Test
    public void importPostApplicationNullPointerStorageVerification() throws Exception {
        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();
        Response.Status responseStatus = Response.Status.OK;

        Entity payload = payloadBuilder();

        Entity properties = (Entity) payload.get("properties");
        Entity storage_info = (Entity) properties.get("storage_info");
        //remove storage_key field
        storage_info.remove("s3_key");

        try {
            this.management().orgs().org( org ).app().addToPath(app).addToPath("imports").post(Entity.class,payload);
        } catch (ClientErrorException uie) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertEquals(Response.Status.BAD_REQUEST, responseStatus);

        payload = payloadBuilder();
        properties = (Entity) payload.get("properties");
        storage_info = (Entity) properties.get("storage_info");
        //remove storage_key field
        storage_info.remove("s3_access_id");

        try {
            this.management().orgs().org( org ).app().addToPath(app).addToPath("imports").post(Entity.class,payload);
        } catch (ClientErrorException uie) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertEquals(Response.Status.BAD_REQUEST, responseStatus);

        payload = payloadBuilder();
        properties = (Entity) payload.get("properties");
        storage_info = (Entity) properties.get("storage_info");
        //remove storage_key field
        storage_info.remove("bucket_location");

        try {
            this.management().orgs().org( org ).app().addToPath(app).addToPath("imports").post(Entity.class,payload);
        } catch (ClientErrorException uie) {
            responseStatus = Response.Status.fromStatusCode( uie.getResponse().getStatus() );
        }
        assertEquals(Response.Status.BAD_REQUEST, responseStatus);
    }

//    @Test
//    public void testExportImportCollection() throws Exception {
//        Assume.assumeTrue( configured );
//        // create a collection of "thing" entities in the first application, export to S3
//        try {
//
//            Map<UUID, org.apache.usergrid.persistence.Entity> thingsMap = new HashMap<>();
//            List<org.apache.usergrid.persistence.Entity> things = new ArrayList<>();
//            createTestEntities(emApp1, thingsMap, things, "thing");
//
//            deleteBucket();
//            exportCollection( emApp1, "things" );
//
//            // create new second application, import the data from S3
//
//            final UUID appId2 = setup.getMgmtSvc().createApplication(
//                organization.getUuid(), "second").getId();
//
//            final EntityManager emApp2 = setup.getEmf().getEntityManager(appId2);
//            importCollection( emApp2, "things" );
//
//
//            // make sure that it worked
//
//            logger.debug("\n\nCheck connections\n");
//
//            List<org.apache.usergrid.persistence.Entity> importedThings = emApp2.getCollection(
//                appId2, "things", null, Query.Level.ALL_PROPERTIES).getEntities();
//            assertTrue( !importedThings.isEmpty() );
//
//            // two things have connections
//
//            int conCount = 0;
//            for ( org.apache.usergrid.persistence.Entity e : importedThings ) {
//                Results r = emApp2.getTargetEntities( e, "related", null, Query.Level.IDS);
//                List<ConnectionRef> connections = r.getConnections();
//                conCount += connections.size();
//            }
//            assertEquals( 2, conCount );
//
//            logger.debug("\n\nCheck dictionaries\n");
//
//            // first two items have things in dictionary
//
//            EntityRef entity0 = importedThings.get(0);
//            Map connected0 = emApp2.getDictionaryAsMap(entity0, "connected_types");
//            Map connecting0 = emApp2.getDictionaryAsMap(entity0, "connected_types");
//            Assert.assertEquals( 1, connected0.size() );
//            Assert.assertEquals( 1, connecting0.size() );
//
//            EntityRef entity1 = importedThings.get(1);
//            Map connected1 = emApp2.getDictionaryAsMap(entity1, "connected_types");
//            Map connecting1 = emApp2.getDictionaryAsMap(entity1, "connected_types");
//            Assert.assertEquals( 1, connected1.size() );
//            Assert.assertEquals( 1, connecting1.size() );
//
//            // the rest rest do not have connections
//
//            EntityRef entity2 = importedThings.get(2);
//            Map connected2 = emApp2.getDictionaryAsMap(entity2, "connected_types");
//            Map connecting2 = emApp2.getDictionaryAsMap(entity2, "connected_types");
//            Assert.assertEquals( 0, connected2.size() );
//            Assert.assertEquals( 0, connecting2.size() );
//
//            // if entities are deleted from app1, they still exist in app2
//
//            logger.debug("\n\nCheck dictionary\n");
//            for ( org.apache.usergrid.persistence.Entity importedThing : importedThings ) {
//                emApp1.delete( importedThing );
//            }
//            emApp1.refreshIndex();
//            emApp2.refreshIndex();
//
//            importedThings = emApp2.getCollection(
//                appId2, "things", null, Query.Level.ALL_PROPERTIES).getEntities();
//            assertTrue( !importedThings.isEmpty() );
//
//        } finally {
//            deleteBucket();
//        }
//    }


    /**
     * TODO: Test that importing bad JSON will result in an informative error message.
     */
    @Ignore
    @Test
    public void testImportGoodJson() throws Exception {
        // import from a bad JSON file
        Assume.assumeTrue(configured);

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();

        String basePath = System.getProperty("target.directory")
            + File.separator + "test-classes" + File.separator;

        List<String> filenames = new ArrayList<>( 1 );
        filenames.add( basePath  + "testimport-correct-testcol.1.json");

        // create 10 applications each with collection of 10 things, export all to S3
        S3Upload s3Upload = new S3Upload();
        s3Upload.copyToS3(
            System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ),
            System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ),
            bucketName, filenames);

        // import all those exports from S3 into the default test application

        Entity importEntity = importCollection();

        Entity importGet = this.management().orgs().org( org ).app()
            .addToPath(app)
            .addToPath( "imports" )
            .addToPath(importEntity.getUuid().toString())
            .get();

        refreshIndex();

        Entity importGetIncludes = this.management().orgs().org( org ).app()
            .addToPath(app)
            .addToPath("imports")
            .addToPath(importEntity.getUuid().toString())
            .addToPath("files")
            .get();

        ApiResponse importGetIncludesResponse = importGetIncludes.getResponse();

        assertNotNull(importGet);
        assertNotNull( importGetIncludes );
        assertEquals( 1,importGetIncludesResponse.getEntityCount());


        final Entity includesEntity = importGetIncludesResponse.getEntities().get( 0 );

        assertTrue( includesEntity.getAsString("fileName").endsWith("testimport-correct-testcol.1.json"));

        assertEquals(1, includesEntity.get( "importedConnectionCount" ));
        assertEquals(1, includesEntity.get( "importedEntityCount" ));

        assertEquals("FINISHED", importGet.get("state"));
        assertEquals(1, importGet.get("fileCount"));

        Collection collection = this.app().collection("things").get();

        assertNotNull(collection);
        assertEquals(1, collection.getNumOfEntities());
        assertEquals("thing0", collection.getResponse().getEntities().get(0).get("name"));


        //TODO: make sure it checks the actual imported entities. And the progress they have made.

    }

    /**
     * TODO: Test that importing bad JSON will result in an informative error message.
     */
    @Ignore
    @Test
    public void testImportOneGoodOneBad() throws Exception {

        // import from a bad JSON file
        Assume.assumeTrue(configured);

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();

        String basePath = System.getProperty("target.directory")
            + File.separator + "test-classes" + File.separator;

        List<String> filenames = new ArrayList<>( 2 );
        filenames.add( basePath + "testimport-correct-testcol.1.json");
        filenames.add( basePath + "testImport.testApplication.2.json");

        // create 10 applications each with collection of 10 things, export all to S3
        S3Upload s3Upload = new S3Upload();
        s3Upload.copyToS3(
            System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ),
            System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ),
            bucketName, filenames);

        // import all those exports from S3 into the default test application

        Entity importEntity = importCollection();

        Entity importGet = this.management().orgs().org( org ).app()
            .addToPath(app)
            .addToPath("imports")
            .addToPath(importEntity.getUuid().toString()).get();

        assertNotNull(importGet);

        assertEquals("FAILED", importGet.get("state"));
        assertEquals(2, importGet.get("fileCount"));

        Collection collection = this.app().collection("things").get();

        assertNotNull(collection);
        assertEquals(1, collection.getNumOfEntities());
        assertEquals("thing0", collection.getResponse().getEntities().get(0).get("name"));
    }

    /**
     * TODO: Test that importing bad JSON will result in an informative error message.
     */
    @Ignore
    @Test
    public void testImportOneBadFile() throws Exception {
        // import from a bad JSON file
        Assume.assumeTrue(configured);

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();

        String basePath = System.getProperty("target.directory")
            + File.separator + "test-classes" + File.separator;

        List<String> filenames = new ArrayList<>( 1 );
        filenames.add( basePath + "testimport-bad-json.json");

        // create 10 applications each with collection of 10 things, export all to S3
        S3Upload s3Upload = new S3Upload();
        s3Upload.copyToS3(
            System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ),
            System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ),
            bucketName, filenames);

        // import all those exports from S3 into the default test application

        Entity importEntity = importCollection();

        Entity importGet = this.management().orgs().org( org ).app()
            .addToPath(app)
            .addToPath("imports" )
            .addToPath(importEntity.getUuid().toString() )
            .get();

        assertNotNull(importGet);

        assertEquals("FAILED", importGet.get("state"));
        assertEquals(1, importGet.get("fileCount"));

        Collection collection = this.app().collection("things").get();

        assertNotNull(collection);
        assertEquals(0, collection.getNumOfEntities());


    }


    /**
     * TODO: Test that importing bad JSON will result in an informative error message.
     */
    @Ignore
    @Test
    public void testImportBadJson() throws Exception {

        // import from a bad JSON file
        Assume.assumeTrue(configured);

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();

        String basePath = System.getProperty("target.directory")
            + File.separator + "test-classes" + File.separator;

        List<String> filenames = new ArrayList<>( 1 );
        filenames.add( basePath + "testimport-bad-json-testapp.3.json");

        // create 10 applications each with collection of 10 things, export all to S3
        S3Upload s3Upload = new S3Upload();
        s3Upload.copyToS3(
            System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ),
            System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ),
            bucketName, filenames);

        // import all those exports from S3 into the default test application

        Entity importEntity = importCollection();

        // we should now have 100 Entities in the default app

        Entity importGet = this.management().orgs().org( org ).app()
            .addToPath( app )
            .addToPath("imports")
            .addToPath( importEntity.getUuid().toString() )
            .get();

        Entity importGetIncludes = this.management().orgs().org( org ).app()
            .addToPath(app)
            .addToPath("imports" )
            .addToPath(importEntity.getUuid().toString() )
            .addToPath("files" )
            .get();

        assertNotNull(importGet);

        //TODO: needs better error checking
        assertNotNull(importGetIncludes);

        // check that error message indicates JSON parsing error
    }

    /**
     * Call importService to import files from the configured S3 bucket.
     */
    private Entity importCollection() throws Exception {

        String org = clientSetup.getOrganizationName();
        String app = clientSetup.getAppName();

        logger.debug("\n\nImport into new app {}\n", app);

        Entity importPayload = new Entity(new HashMap<String, Object>() {{
            put("properties", new HashMap<String, Object>() {{
                put("storage_provider", "s3");
                put("storage_info", new HashMap<String, Object>() {{
                    put("s3_key",
                        System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ));
                    put("s3_access_id",
                        System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ));
                    put("bucket_location", bucketName);
                }});
            }});
        }});

        Entity importEntity = this.management().orgs().org( org ).app()
            .addToPath(app)
            .addToPath("imports")
            .post(Entity.class,importPayload);

        int maxRetries = 120;
        int retries = 0;

        while (retries++ < maxRetries) {

            Entity importGet = this.management().orgs().org( org ).app()
                .addToPath( app )
                .addToPath( "imports")
                .addToPath( importEntity.getUuid().toString() )
                .get();

            if (importGet.get("state").equals("FINISHED") || importGet.get( "state" ).equals( "FAILED" )) {
                break;
            }

            logger.debug("Waiting for import...");
            Thread.sleep(1000);
        }

        refreshIndex();

        return importEntity;
    }

    /**
     * Create test entities of a specified type.
     * First two entities are connected.
     */
    private void createTestEntities() throws Exception {

        logger.debug("\n\nCreating users in application {}\n",
            clientSetup.getAppName());

        List<org.apache.usergrid.persistence.Entity> created = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String name = "test" + i;
            Entity payload = new Entity();
            payload.put("name", name);
            payload.put("username", name);
            payload.put("email", name + "@test.com");
            this.app().collection("users").post(payload);


        }

        this.refreshIndex();

//        // first two things are related to each other
//        em.createConnection(new SimpleEntityRef(type, created.get(0).getUuid()),
//            "related", new SimpleEntityRef(type, created.get(1).getUuid()));
//        em.createConnection(new SimpleEntityRef(type, created.get(1).getUuid()),
//            "related", new SimpleEntityRef(type, created.get(0).getUuid()));
//
//        em.refreshIndex();
    }

    /**
     * Delete the configured s3 bucket.
     */
    public void deleteBucket() {

        logger.debug("\n\nDelete bucket\n");

        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );

        Properties overrides = new Properties();
        overrides.setProperty("s3" + ".identity", accessId);
        overrides.setProperty("s3" + ".credential", secretKey);

        final Iterable<? extends Module> MODULES = ImmutableSet.of(new JavaUrlHttpCommandExecutorServiceModule(),
            new Log4JLoggingModule(), new NettyPayloadModule());

        BlobStoreContext context =
            ContextBuilder.newBuilder("s3").credentials(accessId, secretKey).modules(MODULES)
                .overrides(overrides ).buildView(BlobStoreContext.class);

        BlobStore blobStore = context.getBlobStore();
        blobStore.deleteContainer(bucketName);
    }

    // might be handy if you need to clean up buckets
    private static void deleteBucketsWithPrefix() {

        logger.debug("\n\nDelete buckets with prefix {}\n", bucketPrefix);

        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );

        Properties overrides = new Properties();
        overrides.setProperty("s3" + ".identity", accessId);
        overrides.setProperty("s3" + ".credential", secretKey);

        final Iterable<? extends Module> MODULES = ImmutableSet
            .of(new JavaUrlHttpCommandExecutorServiceModule(),
                new Log4JLoggingModule(),
                new NettyPayloadModule());

        BlobStoreContext context =
            ContextBuilder.newBuilder("s3").credentials(accessId, secretKey).modules(MODULES)
                .overrides(overrides ).buildView(BlobStoreContext.class);

        BlobStore blobStore = context.getBlobStore();
        final PageSet<? extends StorageMetadata> blobStoreList = blobStore.list();

        for (Object o : blobStoreList.toArray()) {
            StorageMetadata s = (StorageMetadata) o;

            if (s.getName().startsWith(bucketPrefix)) {
                try {
                    blobStore.deleteContainer(s.getName());
                } catch (ContainerNotFoundException cnfe) {
                    logger.warn("Attempted to delete bucket {} but it is already deleted", cnfe);
                }
                logger.debug("Deleted bucket {}", s.getName());
            }
        }
    }


    /*Creates fake payload for testing purposes.*/
    public Entity payloadBuilder() {

        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );

        Entity storage_info = new Entity();
        storage_info.put("s3_key", secretKey);
        storage_info.put("s3_access_id", accessId);
        storage_info.put("bucket_location", bucketName) ;

        Entity properties = new Entity();
        properties.put("storage_provider", "s3");
        properties.put("storage_info", storage_info);

        Entity payload = new Entity();
        payload.put("properties", properties);

        return payload;
    }
}
