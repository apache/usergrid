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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.rx.impl.EdgeScope;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSet;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.utils.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;

import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

public class UniqueValueRepairer extends ExportingToolBase {

	static final Logger logger = LoggerFactory.getLogger(UniqueValueRepairer.class);

	JsonFactory jsonFactory = new JsonFactory();
	public static final String LAST_ID = "lastId";

	public static final String FIND_MISSING_UNIQUE_VALUES = "findMissingUniqueValues";
	public static final String FIX_MISSING_VALUES = "fixUniqueValues";

	private boolean findMissingUniqueValues = false;
	private boolean fixMissingValue = false;

	private AllEntityIdsObservable allEntityIdsObs;
	private SimpleEdge lastEdge = null;

	private ExecutorService entityFetcher = Executors.newFixedThreadPool(10);
	private ExecutorService uniqueValueChecker = Executors.newFixedThreadPool(50);

	private Session session;
	private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
	private MvccEntitySerializationStrategy mvccEntitySerializationStrategy;

	@Override
	@SuppressWarnings("static-access")
	public Options createOptions() {

		Options options = super.createOptions();

		Option findMissingUniqueValues = OptionBuilder
				.withDescription("Find entities with missing unique value entry  -findMissingUniqueValues")
				.create(FIND_MISSING_UNIQUE_VALUES);
		Option fixMissingUniqueValueEntries = OptionBuilder
				.withDescription("Fix entities with missing unique value entry  -fixUniqueValues")
				.create(FIX_MISSING_VALUES);

		options.addOption(findMissingUniqueValues);
		options.addOption(fixMissingUniqueValueEntries);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {

		startSpring();
		setVerbose(line);

		this.allEntityIdsObs = injector.getInstance(AllEntityIdsObservable.class);
		applyInputParams(line);

		mvccEntitySerializationStrategy = injector.getInstance(MvccEntitySerializationStrategy.class);
		uniqueValueSerializationStrategy = injector.getInstance(UniqueValueSerializationStrategy.class);
		session = injector.getInstance(Session.class);

		startEntityScan();

		logger.info("Finished checking entities. Waiting for threads to complete execution.");

		while (true) {
			try {
				// Spinning to prevent program execution from ending.
				// Need to replace with some kind of countdown latch or task tracker
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.error("Exception while waiting for unique check to complete.", e);
			}
		}
	}

	private void startEntityScan() throws Exception, UnsupportedEncodingException {

		for (Entry<UUID, String> organizationName : getOrgs().entrySet()) {

			// Let's skip the test entities.
			if (organizationName.equals(properties.getProperty("usergrid.test-account.organization"))) {
				continue;
			}
			fetchApplicationsForOrgs(organizationName.getKey(), organizationName.getValue());
		}
	}

	private Map<UUID, String> getOrgs() throws Exception {
		// Loop through the organizations
		Map<UUID, String> organizationNames = null;

		if (orgId == null && (orgName == null || orgName.trim().equals(""))) {
			organizationNames = managementService.getOrganizations();
		} else {
			OrganizationInfo info = null;

			if (orgId != null) {
				info = managementService.getOrganizationByUuid(orgId);
			} else {
				info = managementService.getOrganizationByName(orgName);
			}

			if (info == null) {
				logger.error("Organization info is null!");
				System.exit(1);
			}

			organizationNames = new HashMap<UUID, String>();
			organizationNames.put(info.getUuid(), info.getName());
		}

		return organizationNames;
	}

	private void fetchApplicationsForOrgs(UUID orgId, String orgName) throws Exception {

		logger.info("Fetch applications for {} : {} ", orgId, orgName);

		// Loop through the applications per organization
		BiMap<UUID, String> applications = managementService.getApplicationsForOrganization(orgId);

		if (applicationId == null && (applicationName == null || applicationName.trim().equals(""))) {
			// export all apps as appId or name is not provided

			Observable.from(applications.entrySet()).subscribe(appEntry -> {
				UUID appId = appEntry.getKey();
				String appName = appEntry.getValue().split("/")[1];
				try {
					fetchApplications(appId, appName);
				} catch (Exception e) {
					logger.error("There was an exception fetching application {} : {}", appName, appId, e);
				}
			});

		} else {

			UUID appId = applicationId;
			String appName = applicationName;

			if (applicationId != null) {
				appName = applications.get(appId);
			} else {
				appId = applications.inverse().get(orgName + '/' + appName);
			}

			try {
				fetchApplications(appId, appName);
			} catch (Exception e) {
				logger.error("There was an exception fetching application {} : {}", appName, appId, e);
			}

		}
	}

	private void fetchApplications(UUID appId, String appName) throws Exception {

		logger.info("Fetching application for {} : {} ", appName, appId);

		EntityManager em = emf.getEntityManager(appId);

		Set<String> collections = em.getApplicationCollections();

		if (collNames == null || collNames.length <= 0) {
			logger.info("Please pass collection name ( -collectionName testCollection ) ");
		} else {
			Observable.from(collNames).subscribe(collectionName -> {
				if (collections.contains(collectionName)) {
					fetchCollections(appId, collectionName, em);
				}
			});
		}

	}

	private void fetchCollections(UUID appId, String collectionName, EntityManager em) {
		extractEntitiesForCollection(appId, collectionName);
	}

	private void extractEntitiesForCollection(UUID applicationId, String collectionName) {

		AtomicInteger batch = new AtomicInteger(1);

		final EntityManager rootEm = emf.getEntityManager(applicationId);

		ExecutorService edgeScopeFetcher = Executors.newFixedThreadPool(1);
		allEntityIdsObs
				.getEdgesToEntities(Observable.just(CpNamingUtils.getApplicationScope(applicationId)),
						Optional.fromNullable(
								CpNamingUtils.getEdgeTypeFromCollectionName(collectionName.toLowerCase())),
						(lastEdge == null ? Optional.absent() : Optional.fromNullable(lastEdge)))
				.buffer(1000).finallyDo(() -> {
					edgeScopeFetcher.shutdown();
					logger.info("Finished fetching entity ids for {}. Shutting down entity edge scope fetcher ",
							collectionName);
					while (!edgeScopeFetcher.isTerminated()) {
						try {
							edgeScopeFetcher.awaitTermination(10, TimeUnit.SECONDS);
						} catch (InterruptedException e) {
						}
					}
					logger.info("Entity edge scope fetcher terminated after shutdown for {}", collectionName);
				}).subscribe(edges -> {

					logger.info("For collection {}", collectionName);
					Integer batchId = batch.getAndIncrement();
					logger.info("Started fetching details for collection {} batch {} ", collectionName, batchId);
					Observable.just(edges).subscribeOn(Schedulers.from(edgeScopeFetcher)).subscribe(edgeScopes -> {

						List<UUID> entityIds = new ArrayList<UUID>(1000);

						for (EdgeScope edgeScope : edgeScopes) {
							Id entityId = edgeScope.getEdge().getTargetNode();
							if (entityId != null) {
								entityIds.add(entityId.getUuid());
							} else {
								edgeScopes.remove(edgeScope);
							}
						}
						try {
							String type = edgeScopes.get(0).getEdge().getTargetNode().getType();

							Observable.just(entityIds).subscribeOn(Schedulers.from(entityFetcher)) // change to
									.subscribe(entIds -> {

										logger.info("Fetched {} entity id's of type {} for batch ID {}", entIds.size(),
												type, batchId);
										Results entities = rootEm.getEntities(entIds, type);
										logger.info("Fetched {} entities of type {} for batch ID {}", entities.size(),
												type, batchId);
										try {

											ConnectableObservable<Entity> entityObs = Observable
													.from(entities.getEntities()).publish();
											entityObs.subscribeOn(Schedulers.from(uniqueValueChecker));
											entityObs.subscribe(t -> {
												logger.info("Fetched entity with UUID : {}", t.getUuid());
												if (findMissingUniqueValues) {
													String fieldValue = null;
													//We can search entity with UUID or name/email based on the entity type. 
													//This mapping between unique value field(name/email etc) and UUID,
													//is stored in unique value table. This can either be name / email or any other type.
													//This value is being passed as field type. 
										            //The code below takes the parameter and retrieves the value of the field using the getter method. 
													if (fieldType == null || fieldType.equals("")
															|| fieldType.equals("name")) {
														fieldType = "name";
														fieldValue = t.getName();
													} else {
														try {
															Method method = t.getClass()
																	.getMethod("get"
																			+ fieldType.substring(0, 1).toUpperCase()
																			+ fieldType.substring(1));
															fieldValue = (String) method.invoke(t);
														} catch (Exception e1) {
															logger.error(
																	"Exception while trying to fetch field value of type {} for entity {} batch {}",
																	fieldType, t.getUuid(), batchId, e1);
														}
													}
													try {
														if (fieldValue != null) {

															Entity e = rootEm.getUniqueEntityFromAlias(t.getType(),
																	fieldValue, false);

															if (e == null) {
																logger.info(
																		"No entity found for field type {} and field value {} but exists for UUID {}",
																		fieldType, fieldValue, t.getUuid());
																if (fixMissingValue) {
																	logger.info(
																			"Trying to repair unique value mapping for {} ",
																			t.getUuid());
																	UniqueValueSet uniqueValueSet = uniqueValueSerializationStrategy
																			.load(new ApplicationScopeImpl(new SimpleId(
																					applicationId, "application")),
																					ConsistencyLevel
																							.valueOf(System.getProperty(
																									"usergrid.read.cl",
																									"LOCAL_QUORUM")),
																					t.getType(),
																					Collections.singletonList(
																							new StringField(fieldType,
																									fieldValue)),
																					false);

																	ApplicationScope applicationScope = new ApplicationScopeImpl(
																			new SimpleId(applicationId, "application"));
																	com.google.common.base.Optional<MvccEntity> entity = mvccEntitySerializationStrategy
																			.load(applicationScope, new SimpleId(
																					t.getUuid(), t.getType()));

																	if (!entity.isPresent()
																			|| !entity.get().getEntity().isPresent()) {
																		throw new RuntimeException(
																				"Unable to update unique value index because supplied UUID "
																						+ t.getUuid()
																						+ " does not exist");
																	}
																	logger.info("Delete unique value: {}",
																			uniqueValueSet.getValue(fieldType));
																	try {
																		session.execute(uniqueValueSerializationStrategy
																				.deleteCQL(applicationScope,
																						uniqueValueSet
																								.getValue(fieldType)));
																	} catch (Exception ex) {
																		logger.error(
																				"Exception while trying to delete the Unique value for {}. Will proceed with creating new entry",
																				t.getUuid(), ex);
																	}

																	UniqueValue newUniqueValue = new UniqueValueImpl(
																			new StringField(fieldType, fieldValue),
																			entity.get().getId(),
																			entity.get().getVersion());
																	logger.info("Writing new unique value: {}",
																			newUniqueValue);
																	session.execute(uniqueValueSerializationStrategy
																			.writeCQL(applicationScope, newUniqueValue,
																					-1));
																}

															} else {
																logger.info(
																		"Found entity {} for field type {} and field value {}",
																		e.getUuid(), fieldType, fieldValue);
															}
														} else {
															logger.info("No value found for field {} for entity {}",
																	fieldType, t.getUuid());
														}
													} catch (Exception e) {
														logger.error(
																"Error while checking unique values for batch id : {} for entity {}",
																batchId, t.getUuid(), e);
													}
												}
											});
											entityObs.connect();

										} catch (Exception e) {
											logger.error(
													"Error while checking unique values for batch id : {} for collection {}",
													batchId, collectionName, e);
										}
									});

						} catch (Exception e) {
							logger.error("Exception while traversing entities " + edgeScopes.get(0).getEdge(), e);
							System.exit(0);
						}
					});
					logger.info("Finished entity walk for collection {} for batch {}", collectionName, batchId);
				});
		logger.info("Exiting extractEntitiesForCollection() method.");
	}

	protected void applyInputParams(CommandLine line) {

		if (line.hasOption(ORG_ID)) {
			orgId = ConversionUtils.uuid(line.getOptionValue(ORG_ID));
		} else if (line.hasOption(ORG_NAME)) {
			orgName = line.getOptionValue(ORG_NAME);
		}

		if (line.hasOption(APP_ID)) {
			applicationId = ConversionUtils.uuid(line.getOptionValue(APP_ID));
		} else if (line.hasOption(APP_NAME)) {
			applicationName = line.getOptionValue(APP_NAME);
		}
		if (line.hasOption(COLL_NAMES)) {
			collNames = line.getOptionValue(COLL_NAMES).split(",");
		}
		if (line.hasOption(COLLECTION_NAME)) {
			collNames = new String[] { line.getOptionValue(COLLECTION_NAME) };
		}
		findMissingUniqueValues = line.hasOption(FIND_MISSING_UNIQUE_VALUES);
		fixMissingValue = line.hasOption(FIX_MISSING_VALUES);

	}
}
