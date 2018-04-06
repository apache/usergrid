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
package org.apache.usergrid.tools;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.index.impl.EsProvider;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.services.assets.data.BinaryStore;
import org.apache.usergrid.tools.export.ExportEntity;
import org.apache.usergrid.utils.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Delete all entities and connections of a Usergrid app.
 */
public class AppDeleter extends ExportingToolBase {
    static final Logger logger = LoggerFactory.getLogger( AppDeleter.class );

    private static final String ORGANIZATION_NAME = "organizationName";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String DELETE_THREAD_COUNT = "deleteThreads";
    private static final String PERFORM_DELETE = "performDelete";
    private static final String LOG_EACH_ITEM = "logEachItem";

    private static final String ACCESS_ID_PROPNAME = "AWS_ACCESS_KEY_ID";
    private static final String SECRET_KEY_PROPNAME = "AWS_SECRET_KEY";
    private static final String BUCKET_NAME_PROPNAME = "usergrid.binary.bucketname";

    private static final String ALL_INDEXES = "*";
    private static final String SCROLL_TIMEOUT = "5m";
    private static final int SCROLL_SIZE = 10;

    String applicationName;
    String organizationName;

    AtomicInteger entitiesFound = new AtomicInteger(0);
    AtomicInteger entityDictionaryEntriesFound = new AtomicInteger(0);
    AtomicInteger appDictionaryEntriesFound = new AtomicInteger(0);
    AtomicInteger assetsFound = new AtomicInteger(0);
    AtomicInteger esDocsFound = new AtomicInteger(0);
    AtomicInteger orgAdminsFound = new AtomicInteger(0);

    Scheduler deleteScheduler;
    AmazonS3Client s3Client;
    EsProvider esProvider;
    IndexLocationStrategyFactory ilsf;

    ObjectMapper mapper = new ObjectMapper();
    Map<Thread, JsonGenerator> entityGeneratorsByThread  = new HashMap<Thread, JsonGenerator>();
    Map<Thread, JsonGenerator> connectionGeneratorsByThread = new HashMap<Thread, JsonGenerator>();

    int deleteThreadCount = 10; // set via CLI option

    BinaryStore binaryStore;

    String logLineSeparator = "-------------------------------------------------------------------";


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option orgNameOption = OptionBuilder.hasArg().isRequired(true).withType("")
            .withDescription( "Organization Name -" + ORGANIZATION_NAME ).create( ORGANIZATION_NAME );
        options.addOption( orgNameOption );

        Option appNameOption = OptionBuilder.hasArg().isRequired(false).withType("")
            .withDescription( "Application Name -" + APPLICATION_NAME ).create( APPLICATION_NAME );
        options.addOption( appNameOption );

        Option performDeleteOption = OptionBuilder.hasArg().isRequired(false)
                .withDescription("Perform Delete -" + PERFORM_DELETE).create(PERFORM_DELETE);
        options.addOption( performDeleteOption );

        Option deleteThreadsOption = OptionBuilder.hasArg().isRequired(false)
                .withType("")
                .withDescription( "Delete Threads -" + DELETE_THREAD_COUNT).create(DELETE_THREAD_COUNT);
        options.addOption( deleteThreadsOption );

        Option logEachItemOption = OptionBuilder.hasArg().isRequired(false)
                .withDescription("Log each item -" + LOG_EACH_ITEM).create(LOG_EACH_ITEM);
        options.addOption( logEachItemOption );

        return options;
    }


    /**
     * Tool entry point.
     */
    @Override
    public void runTool(CommandLine line) throws Exception {

        organizationName = line.getOptionValue( ORGANIZATION_NAME );
        applicationName = line.getOptionValue( APPLICATION_NAME );
        final boolean allApps = StringUtils.isEmpty(applicationName);

        String performDeleteOption = line.getOptionValue(PERFORM_DELETE);
        final boolean performDelete = StringUtils.isNotEmpty(performDeleteOption) && performDeleteOption.toLowerCase().equals("yes");

        String logEachItemOption = line.getOptionValue(LOG_EACH_ITEM);
        final boolean logEachItem = StringUtils.isNotEmpty(logEachItemOption) && logEachItemOption.toLowerCase().equals("yes");

        if (StringUtils.isNotEmpty( line.getOptionValue(DELETE_THREAD_COUNT) )) {
            try {
                deleteThreadCount = Integer.parseInt( line.getOptionValue(DELETE_THREAD_COUNT) );
            } catch (NumberFormatException nfe) {
                logger.error( "-" + DELETE_THREAD_COUNT + " must be specified as an integer. Aborting..." );
                return;
            }
        }

        startSpring();

        // S3 asset store
        String accessId = (String)properties.get(ACCESS_ID_PROPNAME);
        String secretKey = (String)properties.get(SECRET_KEY_PROPNAME);
        String bucketName = (String)properties.get(BUCKET_NAME_PROPNAME);

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        AWSCredentials credentials = new BasicAWSCredentials(accessId, secretKey);
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        s3Client = new AmazonS3Client(credentials, clientConfig);

        // ES
        ilsf = injector.getInstance(IndexLocationStrategyFactory.class);
        esProvider = injector.getInstance(EsProvider.class);

        ExecutorService deleteThreadPoolExecutor = Executors.newFixedThreadPool(deleteThreadCount);
        deleteScheduler = Schedulers.from( deleteThreadPoolExecutor );

        setVerbose( line );

        logger.info(logLineSeparator);

        boolean singleApp = false;
        String matchingAppPrefix = organizationName + "/";
        if (StringUtils.isNotEmpty(applicationName)) {
            singleApp = true;
            matchingAppPrefix += applicationName;
            logger.info("APPLICATION:");
        } else {
            logger.info("APPLICATIONS FOR ORG " + organizationName + ":");
        }

        boolean foundApps = false;
        Map<String, UUID> activeAppMap = new HashMap<>();
        for (Map.Entry<String, UUID> entry : emf.getApplications().entrySet()) {
            if (entry.getKey().startsWith(matchingAppPrefix)) {
                foundApps = true;
                logger.info("ACTIVE APP: {} - {}", entry.getKey(), entry.getValue().toString());
                activeAppMap.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, UUID> deletedAppMap = new HashMap<>();
        for (Map.Entry<String, UUID> entry : emf.getDeletedApplications().entrySet()) {
            if (entry.getKey().startsWith(matchingAppPrefix)) {
                foundApps = true;
                logger.info("DELETED APP: {} - {}", entry.getKey(), entry.getValue().toString());
                deletedAppMap.put(entry.getKey(), entry.getValue());
            }
        }
        logger.info(logLineSeparator);

        if (!foundApps) {
            if (singleApp) {
                throw new RuntimeException( "Cannot find application " + organizationName + "/" + applicationName );
            } else {
                throw new RuntimeException( "Cannot find applications for org " + organizationName );
            }
        }

        for (String name : activeAppMap.keySet()) {
            UUID applicationId = activeAppMap.get(name);
            final EntityManager em = emf.getEntityManager( applicationId );
            handleApp(applicationId, name, false, em, performDelete, bucketName, logEachItem);
        }
        for (String name : deletedAppMap.keySet()) {
            UUID applicationId = deletedAppMap.get(name);
            final EntityManager em = emf.getEntityManager( applicationId );
            handleApp(applicationId, name, true, em, performDelete, bucketName, logEachItem);
        }

        if (!singleApp) {
            // handle org
            handleOrg(organizationName, performDelete);
        }

    }


    private void handleOrg(String organizationName, boolean performDelete) throws Exception {
        OrganizationInfo orgInfo = managementService.getOrganizationByName(organizationName);
        UUID orgUUID = orgInfo.getUuid();

        logger.info(logLineSeparator);
        logger.info("ORGANIZATION: {}({})", organizationName, orgUUID);
        logger.info(logLineSeparator);

        if (performDelete) {
            try {
                String clientId = managementService.getClientIdForOrganization(orgUUID);
                String oldClientSecret = managementService.getClientSecretForOrganization(orgUUID);
                logger.info(logLineSeparator);
                logger.info("OLD ORG CLIENT ID: {}", clientId);
                logger.info("OLD ORG CLIENT SECRET: {}", oldClientSecret);
                String newClientSecret = managementService.newClientSecretForOrganization(orgInfo.getUuid());
                logger.info("NEW ORG CLIENT SECRET: {}", newClientSecret);
                logger.info(logLineSeparator);
            } catch (Exception e) {
                logger.error("FAILED TO CHANGE CREDENTIALS FOR ORG " + organizationName + ": " + e.getMessage(), e);
            }
        }

        List<UserInfo> userList = managementService.getAdminUsersForOrganization(orgInfo.getUuid());

        logger.info(logLineSeparator);
        logger.info("ORGANIZATION ADMINS: {}({})", organizationName, orgInfo.getUuid());
        logger.info(logLineSeparator);
        orgAdminsFound.set(0);
        for (UserInfo user : userList) {
            orgAdminsFound.incrementAndGet();
            BiMap<UUID, String> adminOrgs = managementService.getOrganizationsForAdminUser(user.getUuid());
            int numOrgs = adminOrgs.size();
            logger.info("ORGADMIN: {} ({}) - number of other orgs: {}", user.getUsername(), user.getEmail(), numOrgs - 1);
            if (performDelete) {
                managementService.removeAdminUserFromOrganization(user.getUuid(), orgInfo.getUuid(), true);
                if (numOrgs <= 1) {
                    logger.info("ORGADMIN {} is in no other orgs -- deleting", user.getUsername());
                    try {
                        boolean success = managementService.deleteAdminUser(user.getUuid());
                        if (!success) {
                            logger.info("ORGADMIN {} - failed to delete", user.getUsername());
                        }
                    } catch (Exception e) {
                        logger.info("ORGADMIN " + user.getUsername() + " - exception while deleting: " + e.getMessage(), e);
                    }
                }
            }
        }
        logger.info(logLineSeparator);
        logger.info("ORGANIZATION ADMINS {} DONE! OrgAdmins: {}", performDelete ? "DELETE" : "LIST", orgAdminsFound.get());
        logger.info(logLineSeparator);

    }


    private void handleApp(UUID appId, String orgAppName, boolean deletedApp,
                           EntityManager em, boolean performDelete, String bucketName,
                           boolean logEachItem) {
        logger.info(logLineSeparator);
        logger.info("APPLICATION: {}({}){}", orgAppName, appId.toString(), deletedApp ? " - DELETED" : "");
        logger.info(logLineSeparator);

        if (performDelete) {
            try {
                String clientId = managementService.getClientIdForApplication(appId);
                String oldClientSecret = managementService.getClientSecretForApplication(appId);
                logger.info(logLineSeparator);
                logger.info("OLD APP CLIENT ID: {}", clientId);
                logger.info("OLD APP CLIENT SECRET: {}", oldClientSecret);
                String newClientSecret = managementService.newClientSecretForApplication(appId);
                logger.info("NEW APP CLIENT SECRET: {}", newClientSecret);
                logger.info(logLineSeparator);
            } catch (Exception e) {
                logger.error("FAILED TO CHANGE CREDENTIALS FOR APP " + orgAppName + ": " + e.getMessage(), e);
            }
        }

        logger.info(logLineSeparator);
        logger.info("FINDING APP DICTIONARIES");
        logger.info(logLineSeparator);
        // check for entity dictionaries
        try {
            EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );

            Application application = em.getApplication();
            //logger.info("APP: {}", application.toString());

            for ( String dictionary : rootEm.getDictionaries( application ) ) {
                try {
                    //logger.info("DICTIONARY NAME: {}", dictionary);
                    Map<Object, Object> dictMap = rootEm.getDictionaryAsMap(application, dictionary, false);
                    for (Object key : dictMap.keySet()) {
                        appDictionaryEntriesFound.incrementAndGet();
                        if (logEachItem) {
                            logger.info("APP DICTIONARY {} ENTRY: ({})", dictionary, key.toString());
                        }
                    }
                }
                catch (Exception e) {
                    // ignore
                }
            }
        }
        catch (Exception e) {
            logger.error("APP DICTIONARY CHECK FOR APP " + orgAppName + " FAILED: " + e.getMessage(), e);
        }
        logger.info(logLineSeparator);
        logger.info("APP DICTIONARIES {} DONE! App Dictionary Entries found: {}", performDelete ? "DELETE" : "LIST", appDictionaryEntriesFound.get());
        logger.info(logLineSeparator);

        logger.info(logLineSeparator);
        logger.info("FINDING ENTITIES");
        logger.info(logLineSeparator);
        entitiesFound.set(0);
        Observable<String> collectionsObservable = Observable.create( new CollectionsObservable( em ) );

        collectionsObservable.flatMap( collection -> {

            return Observable.create( new EntityObservable( em, collection ) )
                .doOnNext( new EntityDeleteAction(em, performDelete, logEachItem) ).subscribeOn(deleteScheduler);


        } ).doOnCompleted( new EntityDeleteWrapUpAction(performDelete) ).toBlocking().lastOrDefault(null);


        logger.info(logLineSeparator);
        logger.info("FINDING ASSETS");
        logger.info(logLineSeparator);
        assetsFound.set(0);

        ObjectListing listing = null;
        try {
            listing = s3Client.listObjects(bucketName, appId.toString() + "/");
        }
        catch (Exception e) {
            logger.error("FAILED TO RETRIEVE ASSETS: ", e);
        }
        if (listing != null) {
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                String assetKey = summary.getKey();
                assetsFound.getAndIncrement();
                if (logEachItem) {
                    logger.info("ASSET: {}", assetKey);
                }
                if (performDelete) {
                    try {
                        s3Client.deleteObject(bucketName, assetKey);
                    }
                    catch (Exception e) {
                        logger.error("FAILED TO DELETE ASSET: " + assetKey, e);
                    }
                }
            }
        }
        logger.info(logLineSeparator);
        logger.info("Asset {} DONE! Assets: {}", performDelete ? "DELETE" : "LIST", assetsFound.get());
        logger.info(logLineSeparator);

        // Elasticsearch docs
        logger.info(logLineSeparator);
        logger.info("FINDING ES DOCS");
        logger.info(logLineSeparator);
        esDocsFound.set(0);

        ApplicationScope applicationScope = new ApplicationScopeImpl(new SimpleId(appId, "application"));
        // IndexLocationStrategy strategy = ilsf.getIndexLocationStrategy(applicationScope);

        QueryBuilder qb = QueryBuilders.matchQuery("applicationId", "appId(" + appId.toString() + ",application)");
        SearchResponse scrollResponse = esProvider.getClient()
            .prepareSearch(ALL_INDEXES)
            .setScroll(SCROLL_TIMEOUT)
            .setSearchType(SearchType.SCAN)
            .setQuery(qb)
            .setSize(SCROLL_SIZE)
            .setNoFields()
            .execute().actionGet();

        //logger.info(scrollResponse.toString());

        while (true) {
            BulkRequestBuilder bulkRequest = null;
            if (performDelete) {
                bulkRequest = esProvider.getClient().prepareBulk();
            }
            boolean docsToDelete = false;
            for (SearchHit hit : scrollResponse.getHits().getHits()) {
                esDocsFound.getAndIncrement();
                if (logEachItem) {
                    logger.info("ES DOC: {}", hit.getId());
                }
                if (performDelete) {
                    docsToDelete = true;
                    bulkRequest.add(esProvider.getClient()
                        .prepareDelete(hit.getIndex(), hit.getType(), hit.getId()));
                }
            }

            if (docsToDelete) {
                BulkResponse bulkResponse = bulkRequest.execute().actionGet();
                if (bulkResponse.hasFailures()) {
                    throw new RuntimeException(bulkResponse.buildFailureMessage());
                }
            }

            scrollResponse = esProvider.getClient().prepareSearchScroll(scrollResponse.getScrollId())
                .setScroll(SCROLL_TIMEOUT).execute().actionGet();

            //logger.info(scrollResponse.toString());

            if (scrollResponse.getHits().getHits().length == 0) {
                break;
            }
        }
        logger.info(logLineSeparator);
        logger.info("ES Doc {} DONE! ES Docs: {}", performDelete ? "DELETE" : "LIST", esDocsFound.get());
        logger.info(logLineSeparator);

    }



    // ----------------------------------------------------------------------------------------
    // reading data


    /**
     * Emits collection names found in application.
     */
    private class CollectionsObservable implements Observable.OnSubscribe<String> {
        EntityManager em;

        public CollectionsObservable(EntityManager em) {
            this.em = em;
        }

        public void call(Subscriber<? super String> subscriber) {

            int count = 0;
            try {
                Map<String, Object> collectionMetadata = em.getApplicationCollectionMetadata();

                logger.debug( "Emitting {} collection names for application {}",
                    collectionMetadata.size(), em.getApplication().getName() );

                for ( String collection : collectionMetadata.keySet() ) {
                    subscriber.onNext( collection );
                    count++;
                }

            } catch (Exception e) {
                subscriber.onError( e );
            }

            subscriber.onCompleted();
            logger.info( "Completed. Read {} collection names", count );
        }
    }


    /**
     * Emits entities of collection.
     */
    private class EntityObservable implements Observable.OnSubscribe<ExportEntity> {
        EntityManager em;
        String collection;

        public EntityObservable(EntityManager em, String collection) {
            this.em = em;
            this.collection = collection;
        }

        public void call(Subscriber<? super ExportEntity> subscriber) {

            logger.info("Starting to fetch entities of collection {}", collection);

            //subscriber.onStart();

            try {
                int count = 0;

                Query query = new Query();
                query.setLimit( MAX_ENTITY_FETCH );

                Results results = em.searchCollection( em.getApplicationRef(), collection, query );

                while (results.size() > 0) {
                    for (Entity entity : results.getEntities()) {
                        try {
                            Set<String> dictionaries = em.getDictionaries( entity );
                            Map dictionariesByName = new HashMap<String, Map<Object, Object>>();
                            for (String dictionary : dictionaries) {
                                Map<Object, Object> dict = em.getDictionaryAsMap( entity, dictionary );
                                if (dict.isEmpty()) {
                                    continue;
                                }
                                dictionariesByName.put( dictionary, dict );
                            }

                            ExportEntity exportEntity = new ExportEntity(
                                    organizationName,
                                    applicationName,
                                    entity,
                                    dictionariesByName );

                            subscriber.onNext( exportEntity );
                            count++;

                        } catch (Exception e) {
                            logger.error("Error reading entity " + entity.getUuid() +" from collection " + collection);
                        }
                    }
                    if (results.getCursor() == null) {
                        break;
                    }
                    query.setCursor( results.getCursor() );
                    results = em.searchCollection( em.getApplicationRef(), collection, query );
                }

                subscriber.onCompleted();
                logger.info("Completed collection {}. Read {} entities", collection, count);

            } catch ( Exception e ) {
                subscriber.onError(e);
            }
        }
    }


    // ----------------------------------------------------------------------------------------
    // writing data


    /**
     * Delete entities.
     */
    private class EntityDeleteAction implements Action1<ExportEntity> {

        EntityManager em;
        boolean performDelete;
        boolean logEachEntity;

        public EntityDeleteAction(EntityManager em, boolean performDelete, boolean logEachEntity) {
            this.em = em;
            this.performDelete = performDelete;
            this.logEachEntity = logEachEntity;
        }

        public void call(ExportEntity entity) {

            try {
                entitiesFound.getAndIncrement();
                if (logEachEntity) {
                    logger.info("ENTITY: {}", entity.getEntity().asId().toString());
                }

                // check for entity dictionaries
                if (entity.getDictionaries().size() > 0) {
                    for (String dictionaryName : entity.getDictionaries().keySet()) {
                        Map<Object, Object> dictMap = em.getDictionaryAsMap(entity.getEntity(), dictionaryName);
                        for (Object key : dictMap.keySet()) {
                            entityDictionaryEntriesFound.incrementAndGet();
                            if (logEachEntity) {
                                logger.info("ENTITY DICTIONARY ENTRY ({}-{}): ({}): ({})",
                                    entity.getEntity().asId().toString(),
                                    dictionaryName, key.toString(),
                                    dictMap.get(key).toString());
                            }
                        }
                    }
                }

                if (performDelete) {
                    em.delete(entity.getEntity());
                }

            } catch (IOException e) {
                throw new RuntimeException("Error deleting entity (IOException): " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Error deleting entity: " + e.getMessage(), e);
            }
        }
    }


    private class EntityDeleteWrapUpAction implements Action0 {

        boolean performDelete;

        public EntityDeleteWrapUpAction(boolean performDelete) {
            this.performDelete = performDelete;
        }

        @Override
        public void call() {

            logger.info(logLineSeparator);
            logger.info("Entity {} DONE! Entities: {}", performDelete ? "DELETE" : "LIST", entitiesFound.get());
            logger.info(logLineSeparator);
        }
    }
}
