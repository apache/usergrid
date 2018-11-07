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


import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USERGRID_BINARY_UPLOADER;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.export.ExportRequestBuilder;
import org.apache.usergrid.corepersistence.export.ExportRequestBuilderImpl;
import org.apache.usergrid.corepersistence.rx.impl.AllEntityIdsObservable;
import org.apache.usergrid.corepersistence.rx.impl.EdgeScope;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.services.assets.BinaryStoreFactory;
import org.apache.usergrid.services.assets.data.BinaryStore;
import org.apache.usergrid.tools.bean.ExportOrg;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;


public class Export extends ExportingToolBase {

    static final Logger logger = LoggerFactory.getLogger( Export.class );
    public static final String LAST_ID = "lastId";
    
    
    @Autowired
    private BinaryStoreFactory binaryStoreFactory;

    JsonFactory jsonFactory = new JsonFactory();
    
    private AllEntityIdsObservable allEntityIdsObs;
    private SimpleEdge lastEdge = null;
    
    //TODO : Add blocking queues for these executors where appropriate
    private ExecutorService orgAppCollParallelizer = Executors.newFixedThreadPool(3);
    private ExecutorService entityFetcher = Executors.newFixedThreadPool(10);
	private ExecutorService enitityMemberFetcher = Executors.newFixedThreadPool(10);
	private ExecutorService assetsFetcher = Executors.newFixedThreadPool(10);
	

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {
  
    	Options options = super.createOptions();
    	
    	Option lastId = OptionBuilder.withArgName( LAST_ID ).hasArg()
                .withDescription( "Last Entity Id to resume from" ).create( LAST_ID );
    	options.addOption( lastId);
    	
    	return options;
    }

    
    @Override
    public void runTool( CommandLine line ) throws Exception {
    	
        startSpring();
        setVerbose( line );

	    Gson gson = new GsonBuilder().create(); 
        
        this.allEntityIdsObs = injector.getInstance(AllEntityIdsObservable.class);
        applyExportParams(line);
        prepareBaseOutputFileName( line );
        
        
        if(lastEdgeJson != null) {
        	JSONObject lastEdgeJsonObj = new JSONObject(lastEdgeJson);
        	UUID uuid = UUID.fromString(lastEdgeJsonObj.getJSONObject("sourceNode").getString("uuid"));
        	Id sourceId = new SimpleId(uuid, lastEdgeJsonObj.getJSONObject("sourceNode").getString("type"));
        	uuid = UUID.fromString(lastEdgeJsonObj.getJSONObject("targetNode").getString("uuid"));
        	Id targetId = new SimpleId(uuid, lastEdgeJsonObj.getJSONObject("targetNode").getString("type"));
        	lastEdge = new SimpleEdge(sourceId, lastEdgeJsonObj.getString("type"), targetId, lastEdgeJsonObj.getLong("timestamp"));
        }
        
        outputDir = createOutputParentDir();
        logger.info( "Export directory: " + outputDir.getAbsolutePath() );
        

        // Export organizations separately.
        exportOrganizations();
        
        logger.info("Finished export waiting for threads to end.");

		while(true) {
			try {
				//Spinning to prevent program execution from ending.
				//Need to replace with some kind of countdown latch or task tracker
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.error("Exception while waiting for export to complete.",e);
			}
		}

    }
    
    
    
    private void exportOrganizations() throws Exception, UnsupportedEncodingException {

		for (Entry<UUID, String> organizationName : getOrgs().entrySet()) {

			// Let's skip the test entities.
			if (organizationName.equals(properties.getProperty("usergrid.test-account.organization"))) {
				continue;
			}

			OrganizationInfo orgInfo = managementService.getOrganizationByUuid(organizationName.getKey());
			logger.info("Exporting Organization: " + orgInfo.getName());

			ExportOrg exportOrg = new ExportOrg(orgInfo);

			List<UserInfo> users = managementService.getAdminUsersForOrganization(organizationName.getKey());

			for (UserInfo user : users) {
				exportOrg.addAdmin(user.getUsername());
			}

			File orgDir = createOrgDir(orgInfo.getName());

			// One file per Organization.
			saveOrganizationMetadata(orgDir, exportOrg);

			exportApplicationsForOrg(orgDir, organizationName.getKey(), organizationName.getValue());
		}
	}

    /**
     * Serialize an Organization into a json file.
     * @param orgDir 
     *
     * @param acc OrganizationInfo
     */
    private void saveOrganizationMetadata( File orgDir, ExportOrg acc ) {
    	
        try {

            File outFile = createOutputFile( orgDir, "organization", acc.getName() );
            com.fasterxml.jackson.core.JsonGenerator jg = getJsonGenerator( outFile );
            jg.writeObject( acc );
            jg.close();
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }


    private Map<UUID, String> getOrgs() throws Exception {
        // Loop through the organizations
        Map<UUID, String> organizationNames = null;

        if ( orgId == null && (orgName == null || orgName.trim().equals(""))) {
            organizationNames = managementService.getOrganizations();
        }
        else {
            OrganizationInfo info = null;
            
            if( orgId != null ) {
            	info = managementService.getOrganizationByUuid( orgId );
            }
            else  {
            	info = managementService.getOrganizationByName( orgName );
            }

            if ( info == null ) {
                logger.error( "Organization info is null!" );
                System.exit( 1 );
            }

            organizationNames = new HashMap<UUID, String>();
            organizationNames.put( info.getUuid(), info.getName() );
        }

        return organizationNames;
    }
    
    private void exportApplicationsForOrg(File orgDir, UUID orgId, String orgName ) throws Exception {
        
    	logger.info("Exporting applications for {} : {} ",orgId, orgName);

        // Loop through the applications per organization
        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( orgId );
        
        if ( applicationId == null && (applicationName == null || applicationName.trim().equals(""))) {
        	//export all apps as appId or name is not provided
        	
        	Observable.from(applications.entrySet())
        	.subscribeOn(Schedulers.from(orgAppCollParallelizer))
        	.subscribe(appEntry -> {
        		UUID appId = appEntry.getKey();
	        	String appName = appEntry.getValue().split("/")[1];
	        	try {
					exportApplication(orgDir, appId, appName);
				} catch (Exception e) {
					logger.error("There was an exception exporting application {} : {}",appName, appId, e);
				}
        	});
        	
        }
        else {
        	
        	UUID appId = applicationId;
        	String appName = applicationName;
        	
        	if( applicationId != null ) {
            	appName = applications.get(appId);
            }
            else  {
            	appId = applications.inverse().get(orgName+'/'+appName);
            }
	        
        	try {
				exportApplication(orgDir, appId, appName);
			} catch (Exception e) {
				logger.error("There was an exception exporting application {} : {}",appName, appId, e);
			}

        }
    }
    
    private void exportApplication(File orgDir, UUID appId, String appName) throws Exception {
    
    	
    	logger.info( "Starting application export for {} : {} ",appName, appId );
    	File appDir = createApplicationDir(orgDir, appName);
    	
    	JsonGenerator jg =
                getJsonGenerator( createOutputFile( appDir, "application", appName) );

        // load the dictionary
        EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );

        Entity appEntity = rootEm.get( new SimpleEntityRef( "org_application", appId));

        Map<String, Object> dictionaries = new HashMap<String, Object>();
        
        for ( String dictionary : rootEm.getDictionaries( appEntity ) ) {
            Map<Object, Object> dict = rootEm.getDictionaryAsMap( appEntity, dictionary );

            // nothing to do
            if ( dict.isEmpty() ) {
                continue;
            }

            dictionaries.put( dictionary, dict );
        }

        EntityManager em = emf.getEntityManager( appId);

        // Get application
        Entity nsEntity = em.get( new SimpleEntityRef( "application", appId));

        Set<String> collections = em.getApplicationCollections();

        // load app counters

        Map<String, Long> entityCounters = em.getApplicationCounters();

        //nsEntity.setMetadata( "organization", orgName );
        nsEntity.setMetadata( "dictionaries", dictionaries );
        // counters for collections
        nsEntity.setMetadata( "counters", entityCounters );
        nsEntity.setMetadata( "collections", collections );

        jg.writeStartArray();
        jg.writeObject( nsEntity );
        jg.close();
        
        if ( collNames == null || collNames.length <= 0) {
        	//export all collections as collection names are not provided
        	
        	Observable.from(collections)
        	.subscribeOn(Schedulers.from(orgAppCollParallelizer))
        	.subscribe(collectionName -> {
        		exportCollection(appDir, appId, collectionName, em);
        	});
        	
        }
        else {
        	Observable.from(collNames)
        	.subscribeOn(Schedulers.from(orgAppCollParallelizer))
        	.subscribe(collectionName -> {
        		if(collections.contains(collectionName)) {
        			exportCollection(appDir, appId, collectionName, em);
        		}
        	});
        }
		
	}

	private void exportCollection(File appDir, UUID appId, String collectionName, EntityManager em) {
		File collectionDir = createCollectionDir(appDir, collectionName);
		extractEntityIdsForCollection(collectionDir, appId, collectionName);
	}
	
	private void extractEntityIdsForCollection(File collectionDir, UUID applicationId, String collectionName) {
		
		AtomicInteger batch = new AtomicInteger(1);
		
		final EntityManager rootEm = emf.getEntityManager(applicationId);
		final Gson gson = new GsonBuilder().create();
		ManagerCache managerCache = injector.getInstance(ManagerCache.class);
		ExportRequestBuilder builder = new ExportRequestBuilderImpl().withApplicationId(applicationId);
		final ApplicationScope appScope = builder.getApplicationScope().get();
		GraphManager gm = managerCache.getGraphManager(appScope);
		EntityCollectionManagerFactory entityCollectionManagerFactory = injector
				.getInstance(EntityCollectionManagerFactory.class);
		final EntityCollectionManager ecm = entityCollectionManagerFactory.createCollectionManager(appScope);

		ExecutorService entityIdWriter = Executors.newFixedThreadPool(1);
		allEntityIdsObs
		.getEdgesToEntities(Observable.just(CpNamingUtils.getApplicationScope(applicationId)),Optional.fromNullable(CpNamingUtils.getEdgeTypeFromCollectionName(collectionName.toLowerCase())),(this.lastEdge == null ? Optional.absent() : Optional.fromNullable(lastEdge)))
		.buffer(1000)
		.finallyDo(()-> {
			entityIdWriter.shutdown();
			logger.info("Finished fetching entity ids for {}. Shutting down entity id writer executor ", collectionName);
			while(!entityIdWriter.isTerminated()) {
				try {
					entityIdWriter.awaitTermination(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
				}
			}
			logger.info("Entity id writer executor terminated after shutdown for {}", collectionName);
		})
		.subscribe(edges -> {
			
			logger.info("For collection {}" , collectionName);
			Integer batchId = batch.getAndIncrement();
			logger.info("Started fetching details for collection {} batch {} ", collectionName, batchId);
			Observable.just(edges)
			.subscribeOn(Schedulers.from(entityIdWriter)) 
			.subscribe(edgeScopes -> {

				List<UUID> entityIds = new ArrayList<UUID>(1000);
	
				for (EdgeScope edgeScope : edgeScopes) {
					// write to file
					Id entityId = edgeScope.getEdge().getTargetNode();
					if (entityId != null) {
						entityIds.add(entityId.getUuid());
					} else {
						edgeScopes.remove(edgeScope);
					}
				}
				// extract name for this batch
				try {
					writeEntityIdsBatch(collectionDir, edgeScopes, batchId, collectionName);
					String type = edgeScopes.get(0).getEdge().getTargetNode().getType();
					
					Observable.just(entityIds)
					.subscribeOn(Schedulers.from(entityFetcher)) // change to
					.subscribe(entIds -> {
						
						// get entities here
						logger.info("entIds count {} for type {}", entIds.size(), type);
						Results entities = rootEm.getEntities(entIds, type);
						int size = entities.getEntities().size();
						logger.info("Got {} entities.", size);
						
						if(!skipConnections || !skipDictionaries || !skipAssets) {
							
							ConnectableObservable<Results> entityObs = Observable.just(entities)
									.publish();
							entityObs.subscribeOn(Schedulers.from(enitityMemberFetcher));
	
							
							// fetch and write connections
							if(!skipConnections) {
								entityObs.subscribe(entity -> {
									fetchConnections(gm, ecm, entity, collectionDir, collectionName, batchId, gson);
	
								});
							}
							// fetch and write dictionaries
							if(!skipDictionaries) {
								entityObs.subscribe(entity -> {
									fetchDictionaries(collectionDir, collectionName, rootEm,
										entity, gson, batchId);
								});
							}
							
							if(!skipAssets) {
								File assetsDir = createDir(collectionDir.getAbsolutePath(), "files"); 
								entityObs.subscribe(entity -> {
									try {
										fetchAssets(assetsDir, applicationId, collectionName, batchId, entities);
									} catch (Exception e) {
										logger.error("Exception while trying to fetch assets for app {}, collection {}, batch {} ",
												applicationId, collectionName, batchId, e);
									}
								});
							}
							entityObs.connect();
						}
						writeEntities(collectionDir, entities, batchId, collectionName, gson);
					});
	
				} catch (Exception e) {
					logger.error("There was an error writing entity ids to file for "
							+ edgeScopes.get(0).getEdge(), e);
					// since entity id writing has failed, we need to see how we can not exit the
					// whole program
					System.exit(0);
				}
			});

			logger.info("Finished fetching details for collection {} for batch {}", collectionName, batchId);
		});
		logger.info("Exiting extractEntityIdsForCollection() method.");
	}

	private void fetchAssets(File assetsDir, UUID applicationId, String collectionName, Integer batchId,
			Results entities) throws Exception {

		List<Entity> entitiesWithAssets = new ArrayList<>();

		for (Entity e : entities.getEntities()) {
			if (e.getProperty("file-metadata") != null) {
				entitiesWithAssets.add(e);
			}
		}

		if (!entitiesWithAssets.isEmpty()) {

			writeAssets(assetsDir, collectionName, batchId, entitiesWithAssets);

			ConnectableObservable<Entity> entityAssets = Observable.from(entitiesWithAssets).publish();
			entityAssets.subscribeOn(Schedulers.from(assetsFetcher));
			entityAssets.subscribe(e -> {
				// Write code to fetch these assets from entity store.
				BinaryStore binaryStore = null;
				try {
					binaryStore = binaryStoreFactory
							.getBinaryStore(properties.getProperty(PROPERTIES_USERGRID_BINARY_UPLOADER));
				} catch (Exception e2) {
					logger.error("Except on while trying to get binary store for property {}, ", properties.getProperty(PROPERTIES_USERGRID_BINARY_UPLOADER), e2 );
				}

						File file = new File(assetsDir + "/" + collectionName + "_assets_" + e.getUuid());
						try (InputStream in = binaryStore.read(applicationId, e);
								OutputStream out = new BufferedOutputStream(new FileOutputStream(file));) {

							int read = -1;

							while ((read = in.read()) != -1) {
								out.write(read);
							}

						} catch (Exception e1) {
							logger.error("Exception while to write assets file for entity {}", e.getUuid(), e1);
						}

			});
			entityAssets.connect();
		}
	}

	private void writeAssets(final File collectionDir, final String collectionName, final Integer batchId,
			List<Entity> entitiesWithAssets2) {

		try (BufferedWriter assetsWriter = new BufferedWriter(
				new FileWriter(new File(collectionDir + "/" + collectionName + "_assets_" + batchId + ".json")));) {
			for (Entity e : entitiesWithAssets2) {
				JSONObject object = new JSONObject();
				object.put("uuid", e.getUuid());
				object.put("type", e.getType());
				object.put("file-metadata", e.getProperty("file-metadata"));
				object.put("file", (e.getProperty("file") != null) ? e.getProperty("file") : null);
				assetsWriter.write(object.toString());
				assetsWriter.newLine();
			}
		} catch (Exception ex) {
			logger.error("Exception while trying to write entities collection {} batch {}", collectionName, batchId,
					ex);
		}
	}

	private void fetchDictionaries(File collectionDir, String collectionName, final EntityManager rootEm,
			Results entity, Gson gson, Integer batchId) {
		
		//TODO : still using JsonGenerator 
		JsonGenerator jgDictionaries = null;
		try {
			jgDictionaries = getJsonGenerator(new File(collectionDir + "/" + collectionName + "_" + "dictionaries_" + batchId));

			for (Entity et : entity.getEntities()) {
				Set<String> dictionaries;
				try {
					dictionaries = rootEm.getDictionaries(et);
					
					jgDictionaries.writeStartArray();
					if (dictionaries != null && !dictionaries.isEmpty()) {
						for (String dictionary : dictionaries) {
							Map<Object, Object> dict = rootEm.getDictionaryAsMap(et, dictionary);
							if (dict != null && dict.isEmpty()) {
								continue;
							}
							
							jgDictionaries.writeStartObject();
							jgDictionaries.writeObjectField(dictionary, dict);
							jgDictionaries.writeEndObject();
						}
						jgDictionaries.writeEndArray();
					}
				} catch (Exception e) {
					logger.error("Exception while trying to fetch dictionaries.", e);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while trying to fetch dictionaries.", e);
		} finally {
			if (jgDictionaries != null) {
				try {
					jgDictionaries.close();
				} catch (IOException e) {
					logger.error("Exception while trying to close dictionaries writer.", e);
				}
			}
		}
	}

	private void fetchConnections(GraphManager gm, final EntityCollectionManager ecm, Results entity,
			File collectionDir, String collectionName, Integer batchId, Gson gson) {
		
		try(BufferedWriter bufferedWriter = new BufferedWriter(
				new FileWriter(new File(collectionDir + "/" + collectionName + "_" + "connections_" + batchId)));){
			
			for (Entity et : entity.getEntities()) {
				
				List<ConnectionPojo> connections = new ArrayList<>();
				
				SimpleId id = new SimpleId();
				id.setType(et.getType());
				id.setUuid(et.getUuid());

				gm.getEdgeTypesFromSource(CpNamingUtils.createConnectionTypeSearch(id))
						.flatMap(emittedEdgeType -> {
							logger.debug("loading edges of type {} from node {}", emittedEdgeType, id);
							return gm.loadEdgesFromSource(new SimpleSearchByEdgeType(id, emittedEdgeType,
									Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING, Optional.absent()));
						}).map(markedEdge -> {

							if (!markedEdge.isDeleted() && !markedEdge.isTargetNodeDeleted()
									&& markedEdge.getTargetNode() != null) {

								// doing the load to just again make sure bad
								// connections are not exported
								org.apache.usergrid.persistence.model.entity.Entity en = ecm
										.load(markedEdge.getTargetNode()).toBlocking().lastOrDefault(null);

								if (en != null) {

									try {
										
										ConnectionPojo connectionPojo = new ConnectionPojo();
										connectionPojo.setRelationship(CpNamingUtils
														.getConnectionNameFromEdgeName(markedEdge.getType()));
										connectionPojo.setSourceNodeUUID(markedEdge.getSourceNode().getUuid().toString());
										connectionPojo.setTargetNodeUUID(markedEdge.getTargetNode().getUuid().toString());

										connections.add(connectionPojo);
										
									} catch (Exception e) {
										logger.error("Exception while trying process connection entity", e);
									}
								} else {
									logger.warn(
											"Exported connection has a missing target node, not creating connection in export. Edge: {}",
											markedEdge);
								}
							}
							return null;

						}).toBlocking().lastOrDefault(null);
				
				for(ConnectionPojo c : connections) {
					bufferedWriter.write(gson.toJson(c));
					bufferedWriter.newLine();
				}
			}
		}catch (Exception e) {
			logger.error("Exception while trying to write connection to file.", e);
		}
		logger.info("Finished fetching details for collection {} batch {}", collectionName, batchId);
	}
	
	
	class ConnectionPojo {
		private String sourceNodeUUID;
		private String relationship;
		private String targetNodeUUID;
		public String getSourceNodeUUID() {
			return sourceNodeUUID;
		}
		public void setSourceNodeUUID(String sourceNodeUUID) {
			this.sourceNodeUUID = sourceNodeUUID;
		}
		public String getRelationship() {
			return relationship;
		}
		public void setRelationship(String relationship) {
			this.relationship = relationship;
		}
		public String getTargetNodeUUID() {
			return targetNodeUUID;
		}
		public void setTargetNodeUUID(String targetNodeUUID) {
			this.targetNodeUUID = targetNodeUUID;
		}
		
	}

	private void writeEntities(File collectionDir, Results entities, Integer batchId, String collectionName, Gson gson) {
		logger.info("Started writing entities for collection {} batch {} ", collectionName, batchId);
		
		try(BufferedWriter bufferedWriter = new BufferedWriter(
				new FileWriter(new File(collectionDir + "/" + collectionName + "_data_" + batchId + ".json")));) {
			
			logger.info("Got count {} entities for file writing", entities.getEntities().size());
			for(Entity e : entities.getEntities()) {
				bufferedWriter.write(gson.toJson(e));
				bufferedWriter.newLine();
			}
			
		} catch (Exception e) {
			logger.error("Exception while trying to write entities collection {} batch {}", collectionName, batchId, e);
		}
		
		logger.info("Finised writing entities for collection {} batch {} ", collectionName, batchId);
	}

	private void writeEntityIdsBatch(File collectionDir, List<EdgeScope> edgeScopes, Integer batchId,
			String collectionName) throws Exception {
		logger.info("Started writing ids for collection {} batch {} ", collectionName, batchId);
		try (BufferedWriter bufferedWriter = new BufferedWriter(
				new FileWriter(new File(collectionDir + "/" + collectionName + "_" + batchId)));) {
			for (EdgeScope es : edgeScopes) {
				bufferedWriter.write(es.getEdge().toString());
				bufferedWriter.newLine();
			}
		} catch (Exception e) {
			logger.error("Exception while tryign to write entity ids for collection {} batch {}", collectionName, batchId, e);
		}
		logger.info("Finished writing ids for collection {} batch {} ", collectionName, batchId);
	}

}
