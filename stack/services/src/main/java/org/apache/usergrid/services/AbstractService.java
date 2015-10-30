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
package org.apache.usergrid.services;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.codahale.metrics.Timer;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.metrics.ObservableTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import org.apache.commons.lang.NotImplementedException;
import org.apache.shiro.subject.Subject;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.core.rx.RxSchedulerFig;
import org.apache.usergrid.persistence.core.rx.RxTaskScheduler;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.services.ServiceParameter.IdParameter;
import org.apache.usergrid.services.ServiceParameter.NameParameter;
import org.apache.usergrid.services.ServiceParameter.QueryParameter;
import org.apache.usergrid.services.ServiceResults.Type;
import org.apache.usergrid.services.exceptions.ServiceInvocationException;
import org.apache.usergrid.services.exceptions.ServiceResourceNotFoundException;
import org.apache.usergrid.services.exceptions.UnsupportedServiceOperationException;

import com.google.inject.Injector;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import static org.apache.usergrid.security.shiro.utils.SubjectUtils.getPermissionFromPath;
import static org.apache.usergrid.services.ServiceParameter.filter;
import static org.apache.usergrid.services.ServiceParameter.mergeQueries;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.InflectionUtils.pluralize;
import static org.apache.usergrid.utils.ListUtils.dequeueCopy;
import static org.apache.usergrid.utils.ListUtils.isEmpty;


/**
 * Implementation of Service inferface. Builds a method call fanout from the invoke() method so that overriding specific
 * behvaviors can be done easily.
 *
 * @author edanuff
 */
public abstract class AbstractService implements Service {

    private static final Logger logger = LoggerFactory.getLogger( AbstractService.class );

    private ServiceInfo info;

    protected EntityManager em;

    protected ServiceManager sm;

    protected Set<String> privateConnections;
    protected Set<String> declaredConnections;

    protected Set<String> privateCollections;
    protected Set<String> declaredCollections;

    protected Map<List<String>, List<String>> replaceParameters;

    protected Set<String> serviceCommands;
    protected Set<EntityDictionaryEntry> entityDictionaries;
    protected Set<String> metadataTypes;
    protected Set<String> entityCommands;

    protected Map<String, Object> defaultEntityMetadata;

    private Scheduler rxScheduler;
    private RxSchedulerFig rxSchedulerFig;
    private MetricsFactory metricsFactory;
    private Timer entityGetTimer;
    private Timer entitiesGetTimer;
    private Timer entitiesParallelGetTimer;
    private Timer invokeTimer;

    protected CacheFactory cacheFactory;

    public AbstractService() {

    }


    public void setServiceManager( ServiceManager sm ) {
        this.sm = sm;
        em = sm.getEntityManager();
        final Injector injector = sm.getApplicationContext().getBean( Injector.class );
        rxScheduler = injector.getInstance( RxTaskScheduler.class ).getAsyncIOScheduler();
        rxSchedulerFig = injector.getInstance(RxSchedulerFig.class);
        metricsFactory = injector.getInstance(MetricsFactory.class);
        this.entityGetTimer = metricsFactory.getTimer(this.getClass(), "importEntity.get");
        this.entitiesGetTimer = metricsFactory.getTimer(this.getClass(), "importEntities.get");
        this.entitiesParallelGetTimer = metricsFactory.getTimer( this.getClass(),"importEntitiesP.get" );
        this.invokeTimer = metricsFactory.getTimer( this.getClass(),"service.invoke" );

        this.cacheFactory = injector.getInstance( CacheFactory.class );
    }


    public ApplicationContext getApplicationContext() {
        return sm.getApplicationContext();
    }


    public void init( ServiceInfo info ) {
        this.info = info;
    }


    public ServiceInfo getServiceInfo() {
        return info;
    }


    @Override
    public String getServiceType() {
        if ( info == null ) {
            return null;
        }
        return info.getName();
    }


    @Override
    public Class<? extends Entity> getEntityClass() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getEntityType() {
        if ( info == null ) {
            return null;
        }
        return info.getItemType();
    }


    @Override
    public boolean isRootService() {
        if ( info == null ) {
            return false;
        }
        return info.isRootService();
    }


    public boolean isCollectionReversed( ServiceContext context ) {
        if ( info == null ) {
            return false;
        }
        if ( "application".equals( context.getOwner().getType() ) ) {
            return Schema.getDefaultSchema().isCollectionReversed("application", pluralize(info.getItemType()));
        }
        return Schema.getDefaultSchema().isCollectionReversed(info.getContainerType(), info.getCollectionName());
    }


    public String getCollectionSort( ServiceContext context ) {
        if ( info == null ) {
            return null;
        }
        if ( "application".equals( context.getOwner().getType() ) ) {
            return Schema.getDefaultSchema().getCollectionSort("application", pluralize(info.getItemType()));
        }
        return Schema.getDefaultSchema().getCollectionSort(info.getContainerType(), info.getCollectionName());
    }


    public void makeConnectionPrivate( String connection ) {
        if ( privateConnections == null ) {
            privateConnections = new LinkedHashSet<String>();
        }
        privateConnections.add(connection);
    }


    public void makeConnectionsPrivate( List<String> connections ) {
        if ( privateConnections == null ) {
            privateConnections = new LinkedHashSet<String>();
        }
        privateConnections.addAll(connections);
    }


    public void declareConnection( String connection ) {
        if ( declaredConnections == null ) {
            declaredConnections = new LinkedHashSet<String>();
        }
        declaredConnections.add(connection);
    }


    public void declareConnections( List<String> connections ) {
        if ( declaredConnections == null ) {
            declaredConnections = new LinkedHashSet<String>();
        }
        declaredConnections.addAll(connections);
    }


    public void makeCollectionPrivate( String collection ) {
        if ( privateCollections == null ) {
            privateCollections = new LinkedHashSet<String>();
        }
        privateCollections.add(collection);
    }


    public void makeCollectionsPrivate( List<String> collections ) {
        if ( privateCollections == null ) {
            privateCollections = new LinkedHashSet<String>();
        }
        privateCollections.addAll(collections);
    }


    public void declareVirtualCollection( String collection ) {
        if ( declaredCollections == null ) {
            declaredCollections = new LinkedHashSet<String>();
        }
        declaredCollections.add(collection);
    }


    public void declareVirtualCollections( List<String> collections ) {
        if ( declaredCollections == null ) {
            declaredCollections = new LinkedHashSet<String>();
        }
        declaredCollections.addAll(collections);
    }


    public void addReplaceParameters( List<String> find, List<String> replace ) {
        if ( replaceParameters == null ) {
            replaceParameters = new LinkedHashMap<List<String>, List<String>>();
        }
        replaceParameters.put(find, replace);
    }


    public void declareServiceCommands( String command ) {
        if ( serviceCommands == null ) {
            serviceCommands = new LinkedHashSet<String>();
        }
        serviceCommands.add(command);
    }


    public void declareServiceCommands( List<String> commands ) {
        if ( serviceCommands == null ) {
            serviceCommands = new LinkedHashSet<String>();
        }
        serviceCommands.addAll(commands);
    }


    public void declareEntityDictionary( EntityDictionaryEntry dictionary ) {
        if ( entityDictionaries == null ) {
            entityDictionaries = new LinkedHashSet<EntityDictionaryEntry>();
        }
        entityDictionaries.add(dictionary);
    }


    public void declareEntityDictionary( String dictionary ) {
        if ( entityDictionaries == null ) {
            entityDictionaries = new LinkedHashSet<EntityDictionaryEntry>();
        }
        entityDictionaries.add( new EntityDictionaryEntry( dictionary ) );
    }


    public void declareEntityDictionaries( List<String> dictionaries ) {
        if ( entityDictionaries == null ) {
            entityDictionaries = new LinkedHashSet<EntityDictionaryEntry>();
        }
        for ( String dict : dictionaries ) {
            entityDictionaries.add( new EntityDictionaryEntry( dict ) );
        }
    }


    public void declareMetadataType( String type ) {
        if ( metadataTypes == null ) {
            metadataTypes = new LinkedHashSet<String>();
        }
        metadataTypes.add(type);
    }


    public void declareMetadataTypes( List<String> typeList ) {
        if ( metadataTypes == null ) {
            metadataTypes = new LinkedHashSet<String>();
        }
        metadataTypes.addAll(typeList);
    }


    public void declareEntityCommand( String command ) {
        if ( entityCommands == null ) {
            entityCommands = new LinkedHashSet<String>();
        }
        entityCommands.add(command);
    }


    public void declareEntityCommands( List<String> commands ) {
        if ( entityCommands == null ) {
            entityCommands = new LinkedHashSet<String>();
        }
        entityCommands.addAll(commands);
    }


    @Override
    public Entity getEntity( ServiceRequest request, UUID uuid ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Entity getEntity( ServiceRequest request, String name ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }


    public Entity importEntity( ServiceContext context, Entity entity ) throws Exception {
        return importEntity( context.getRequest(), entity );
    }


    @Override
    public Entity importEntity( ServiceRequest request, Entity entity ) throws Exception {
        Timer.Context getEntityTimer = entityGetTimer.time();
        try {
            if (entity == null) {
                return null;
            }

            if (!isRootService()) {
                return sm.importEntity(request, entity);
            }


            String path = request.getPath() + "/" + entity.getUuid();
            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            metadata.put("path", path);

            if (defaultEntityMetadata != null) {
                metadata.putAll(defaultEntityMetadata);
            }

            if (request.isReturnsOutboundConnections()) {
                Set<Object> connections = getConnectedTypesSet(entity);
                if (connections != null) {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    for (Object n : connections) {
                        m.put(n.toString(), path + "/" + n);
                    }
                    metadata.put("connections", m);
                }
            }

            if (request.isReturnsInboundConnections()) {
                Set<Object> connecting = getConnectingTypesSet(entity);
                if (connecting != null) {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    for (Object n : connecting) {
                        m.put(n.toString(), path + "/connecting/" + n);
                    }
                    metadata.put("connecting", m);
                }
            }

            Set<String> collections = getCollectionSet(entity);
            if (collections != null) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                for (Object n : collections) {
                    m.put(n.toString(), path + "/" + n);
                }
                metadata.put("collections", m);
            }

            if (entityDictionaries != null) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                for (EntityDictionaryEntry dict : entityDictionaries) {
                    m.put(dict.getName(), path + "/" + dict.getPath());
                }
                metadata.put("sets", m);
            }

            if (metadata.size() > 0) {
                entity.mergeMetadata(metadata);
            }
            return entity;
        }finally {
            getEntityTimer.stop();
        }
    }


    public void importEntities( ServiceRequest request, Results results ) throws Exception {
        Timer.Context timer = entitiesGetTimer.time();
        try {
            List<Entity> entities = results.getEntities();
            if (entities != null) {
                importEntitiesParallel(request, results);
            }
        }finally {
            timer.stop();
        }
    }


    /**
     * Import entities in parallel
     * @param request
     * @param results
     */
    private void importEntitiesParallel(final ServiceRequest request, final Results results ) {
        //create our tuples
        final Observable<EntityTuple> tuples = Observable.create(new Observable.OnSubscribe<EntityTuple>() {
            @Override
            public void call(final Subscriber<? super EntityTuple> subscriber) {
                subscriber.onStart();

                final List<Entity> entities = results.getEntities();
                final int size = entities.size();
                for (int i = 0; i < size && !subscriber.isUnsubscribed(); i++) {
                    subscriber.onNext(new EntityTuple(i, entities.get(i)));
                }

                subscriber.onCompleted();
            }
        });

        //now process them in parallel up to 10 threads

        Observable tuplesObservable = tuples.flatMap(tuple -> {
            //map the entity into the tuple
            return Observable.just(tuple).doOnNext(parallelTuple -> {
                //import the entity and set it at index
                try {

                    final Entity imported = importEntity(request, parallelTuple.entity);

                    if (imported != null) {
                        results.setEntity(parallelTuple.index, imported);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).subscribeOn(rxScheduler);
        }, rxSchedulerFig.getImportThreads());

        ObservableTimer.time(tuplesObservable, entitiesParallelGetTimer).toBlocking().lastOrDefault(null);
    }


    /**
     * Simple tuple representing and entity and it's location within the results
     */
    private static final class EntityTuple {
        private final int index;
        private final Entity entity;


        private EntityTuple( final int index, final Entity entity ) {
            this.index = index;
            this.entity = entity;
        }
    }


    public void importEntities( ServiceContext context, Results results ) throws Exception {
        importEntities( context.getRequest(), results );
    }


    @Override
    public Entity writeEntity( ServiceRequest request, Entity entity ) throws Exception {
        if ( !isRootService() ) {
            return sm.writeEntity( request, entity );
        }
        return entity;
    }


    public void writeEntities( ServiceRequest request, Results results ) throws Exception {

        List<Entity> entities = results.getEntities();
        if ( entities != null ) {
            for ( Entity entity : entities ) {
                writeEntity( request, entity );
            }
        }
    }


    public Entity updateEntity( ServiceContext context, EntityRef ref, ServicePayload payload ) throws Exception {
        return updateEntity( context.getRequest(), ref, payload );
    }


    public Entity updateEntity( ServiceContext context, EntityRef ref ) throws Exception {
        return updateEntity( context.getRequest(), ref, context.getPayload() );
    }


    @Override
    public Entity updateEntity( ServiceRequest request, EntityRef ref, ServicePayload payload ) throws Exception {
        if ( !isRootService() ) {
            return sm.updateEntity( request, ref, payload );
        }

        if ( ref instanceof Entity ) {
            Entity entity = ( Entity ) ref;
            em.updateProperties( entity, payload.getProperties() );
            entity.addProperties( payload.getProperties() );
            return entity;
        }
        logger.error("Attempted update of entity reference rather than full entity, currently unsupport - MUSTFIX");
        throw new NotImplementedException();
    }


    public void updateEntities( ServiceContext context, Results results, ServicePayload payload ) throws Exception {
        updateEntities( context.getRequest(), results, payload );
    }


    public void updateEntities( ServiceContext context, Results results ) throws Exception {
        updateEntities( context.getRequest(), results, context.getPayload() );
    }


    public void updateEntities( ServiceRequest request, Results results, ServicePayload payload ) throws Exception {

        List<Entity> entities = results.getEntities();
        if ( entities != null ) {
            for ( Entity entity : entities ) {
                updateEntity( request, entity, payload );
            }
        }
    }


    public Set<Object> getConnectedTypesSet( EntityRef ref ) throws Exception {
        final Set<String> connections = em.getConnectionsAsSource(ref);

        if ( connections == null ) {
            return null;
        }
        if ( connections.size() > 0 ) {
            connections.remove( "connection" );
            if ( privateConnections != null ) {
                connections.removeAll( privateConnections );
            }
            if ( connections.size() > 0 ) {
                return new HashSet<Object>( connections );
            }
        }
        return null;
    }


    public Set<Object> getConnectingTypesSet( EntityRef ref ) throws Exception {
        final Set<String> connections = em.getConnectionsAsTarget(ref);

        if ( connections == null ) {
            return null;
        }
        if ( connections.size() > 0 ) {
            connections.remove( "connection" );
            if ( privateConnections != null ) {
                connections.removeAll( privateConnections );
            }
            if ( connections.size() > 0 ) {
                return new LinkedHashSet<Object>( connections );
            }
        }
        return null;
    }


    public Set<String> getCollectionSet( EntityRef ref ) {
        Set<String> set = Schema.getDefaultSchema().getCollectionNames(ref.getType());
        set = new LinkedHashSet<String>( set );
        if ( declaredCollections != null ) {
            set.addAll( declaredCollections );
        }
        if ( privateCollections != null ) {
            set.removeAll( privateCollections );
        }
        if ( set.size() > 0 ) {
            return set;
        }
        return null;
    }


    @Override
    public ServiceResults invoke( ServiceAction action, ServiceRequest request, ServiceResults previousResults,
                                  ServicePayload payload ) throws Exception {

        ServiceContext context = getContext(action, request, previousResults, payload);

        return invoke( context );
    }


    /**
     * Create context from parameter queue. Returns context containing a query object that represents the parameters in
     * the queue. Remaining parameters are left for next service request to allow for request chaining.
     */

    public ServiceContext getContext( ServiceAction action, ServiceRequest request, ServiceResults previousResults,
                                      ServicePayload payload ) throws Exception {

        EntityRef owner = request.getOwner();
        String collectionName =
                "application".equals( owner.getType() ) ? pluralize( info.getItemType() ) : info.getCollectionName();
        List<ServiceParameter> parameters = filter(request.getParameters(), replaceParameters);

        ServiceParameter first_parameter = null;
        if ( !isEmpty( parameters ) ) {
            first_parameter = parameters.get( 0 );
            parameters = dequeueCopy( parameters );
        }

        if ( first_parameter instanceof NameParameter ) {
            if ( hasServiceMetadata( first_parameter.getName() ) ) {
                return new ServiceContext( this, action, request, previousResults, owner, collectionName, parameters,
                        payload ).withServiceMetadata( first_parameter.getName() );
            }
            else if ( hasServiceCommand( first_parameter.getName() ) ) {
                return new ServiceContext( this, action, request, previousResults, owner, collectionName, parameters,
                        payload ).withServiceCommand( first_parameter.getName() );
            }
        }

        Query query = null;
        if ( first_parameter instanceof QueryParameter ) {
            query = first_parameter.getQuery();
        }
        parameters = mergeQueries(query, parameters);

        if ( first_parameter instanceof IdParameter ) {
            UUID id = first_parameter.getId();
            return new ServiceContext( this, action, request, previousResults, owner, collectionName,
                    Query.fromUUID( id ), parameters, payload );
        }
        else if ( first_parameter instanceof NameParameter ) {
            String name = first_parameter.getName();
            return new ServiceContext( this, action, request, previousResults, owner, collectionName,
                    Query.fromIdentifier( name ), parameters, payload );
        }
        else if ( query != null ) {
            return new ServiceContext( this, action, request, previousResults, owner, collectionName, query, parameters,
                    payload );
        }
        else if ( first_parameter == null ) {
            return new ServiceContext( this, action, request, previousResults, owner, collectionName, null, null,
                    payload );
        }

        return null;
    }


    public ServiceResults invoke( ServiceContext context ) throws Exception {
        ServiceResults results = null;
        Timer.Context time = invokeTimer.time();
        String metadataType = checkForServiceMetadata( context );
        if ( metadataType != null ) {
            return handleServiceMetadata( context, metadataType );
        }

        String serviceCommand = checkForServiceCommand( context );
        if ( serviceCommand != null ) {
            return handleServiceCommand( context, serviceCommand );
        }

        EntityDictionaryEntry entityDictionary = checkForEntityDictionaries( context );
        String entityCommand = checkForEntityCommands( context );

        if ( context.isByQuery() ) {
            results = invokeItemsWithQuery( context, context.getQuery() );
        }
        else if ( context.isByName() ) {
            results = invokeItemWithName( context, context.getName() );
        }
        else if ( context.isByUuid() ) {
            results = invokeItemWithId( context, context.getUuid() );
        }
        else {
            results = invokeCollection( context );
        }

        results = handleEntityDictionary( context, results, entityDictionary );
        results = handleEntityCommand(context, results, entityCommand);

        time.stop();
        return results;
    }


    public ServiceResults invokeItemWithId( ServiceContext context, UUID id ) throws Exception {

        switch ( context.getAction() ) {
            case GET:
                return getItemById(context, id);

            case POST:
                return postItemById(context, id);

            case PUT:
                return putItemById(context, id);

            case DELETE:
                return deleteItemById(context, id);

            case HEAD:
                return headItemById(context, id);
        }

        throw new ServiceInvocationException( context, "Request action unhandled " + context.getAction() );
    }


    public ServiceResults invokeItemWithName( ServiceContext context, String name ) throws Exception {
        switch (context.getAction()) {
            case GET:
                return getItemByName(context, name);
            case POST:
                return postItemByName(context, name);
            case PUT:
                return putItemByName(context, name);
            case DELETE:
                return deleteItemByName(context, name);
            case HEAD:
                return headItemByName(context, name);
            default:
                throw new ServiceInvocationException(context, "Request action unhandled " + context.getAction());
        }
    }


    public ServiceResults invokeItemsWithQuery( ServiceContext context, Query query ) throws Exception {

        switch ( context.getAction() ) {
            case GET:
                return getItemsByQuery( context, query );

            case POST:
                return postItemsByQuery( context, query );

            case PUT:
                return putItemsByQuery( context, query );

            case DELETE:
                return deleteItemsByQuery( context, query );

            case HEAD:
                return headItemsByQuery( context, query );
        }

        throw new ServiceInvocationException( context, "Request action unhandled " + context.getAction() );
    }


    public ServiceResults invokeCollection( ServiceContext context ) throws Exception {

        switch ( context.getAction() ) {
            case GET:
                return getCollection( context );

            case POST:
                return postCollection( context );

            case PUT:
                return putCollection( context );

            case DELETE:
                return deleteCollection( context );

            case HEAD:
                return headCollection( context );
        }

        throw new ServiceInvocationException( context, "Request action unhandled " + context.getAction() );
    }


    public ServiceResults getItemById( ServiceContext context, UUID id ) throws Exception {
        throw new ServiceResourceNotFoundException( context );
    }


    public ServiceResults getItemByName( ServiceContext context, String name ) throws Exception {
        throw new ServiceResourceNotFoundException( context );
    }


    public ServiceResults getItemsByQuery( ServiceContext context, Query query ) throws Exception {
        throw new ServiceResourceNotFoundException( context );
    }


    public ServiceResults getCollection( ServiceContext context ) throws Exception {
        throw new ServiceResourceNotFoundException( context );
    }


    public ServiceResults putItemById( ServiceContext context, UUID id ) throws Exception {
        return getItemById( context, id );
    }


    public ServiceResults putItemByName( ServiceContext context, String name ) throws Exception {
        return getItemByName( context, name );
    }


    public ServiceResults putItemsByQuery( ServiceContext context, Query query ) throws Exception {
        return getItemsByQuery( context, query );
    }


    public ServiceResults putCollection( ServiceContext context ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {
        return getItemById( context, id );
    }


    public ServiceResults postItemByName( ServiceContext context, String name ) throws Exception {
        return getItemByName( context, name );
    }


    public ServiceResults postItemsByQuery( ServiceContext context, Query query ) throws Exception {
        return getItemsByQuery( context, query );
    }


    public ServiceResults postCollection( ServiceContext context ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults deleteItemById( ServiceContext context, UUID id ) throws Exception {
        return getItemById( context, id );
    }


    public ServiceResults deleteItemByName( ServiceContext context, String name ) throws Exception {
        return getItemByName( context, name );
    }


    public ServiceResults deleteItemsByQuery( ServiceContext context, Query query ) throws Exception {
        return getItemsByQuery( context, query );
    }


    public ServiceResults deleteCollection( ServiceContext context ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults headItemById( ServiceContext context, UUID id ) throws Exception {
        return getItemById( context, id );
    }


    public ServiceResults headItemByName( ServiceContext context, String name ) throws Exception {
        return getItemByName( context, name );
    }


    public ServiceResults headItemsByQuery( ServiceContext context, Query query ) throws Exception {
        return getItemsByQuery( context, query );
    }


    public ServiceResults headCollection( ServiceContext context ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public boolean hasServiceCommand( String command ) {
        return ( serviceCommands != null ) && ( command != null ) && serviceCommands.contains( command );
    }


    public String checkForServiceCommand( ServiceContext context ) {
        if ( serviceCommands == null ) {
            return null;
        }

        if ( !context.moreParameters() ) {
            return null;
        }

        String name = null;
        if ( context.firstParameterIsName() ) {
            name = context.firstParameter().getName();
            if ( serviceCommands.contains( name ) ) {
                return name;
            }
        }

        return null;
    }


    public ServiceResults handleServiceCommand( ServiceContext context, String command ) throws Exception {
        switch ( context.getAction() ) {
            case GET:
                return getServiceCommand( context, command );

            case POST:
                return postServiceCommand( context, command, context.getPayload() );

            case PUT:
                return putServiceCommand( context, command, context.getPayload() );

            case DELETE:
                return deleteServiceCommand( context, command );

            case HEAD:
                return headServiceCommand( context, command );
        }

        throw new ServiceInvocationException( context, "Request action unhandled " + context.getAction() );
    }


    public ServiceResults getServiceCommand( ServiceContext context, String command ) throws Exception {

        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults putServiceCommand( ServiceContext context, String command, ServicePayload payload )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults postServiceCommand( ServiceContext context, String command, ServicePayload payload )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults deleteServiceCommand( ServiceContext context, String command ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults headServiceCommand( ServiceContext context, String command ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public boolean hasEntityDictionary( String dictionary ) {
        if ( entityDictionaries == null ) {
            return false;
        }
        for ( EntityDictionaryEntry entry : entityDictionaries ) {
            if ( entry.getName().equalsIgnoreCase( dictionary ) ) {
                return true;
            }
        }
        return false;
    }


    public EntityDictionaryEntry checkForEntityDictionaries( ServiceContext context ) {
        if ( entityDictionaries == null ) {
            return null;
        }

        if ( !context.moreParameters() ) {
            return null;
        }

        String name = null;
        if ( context.firstParameterIsName() ) {
            name = context.firstParameter().getName();
            for ( EntityDictionaryEntry entry : entityDictionaries ) {
                if ( entry.getName().equalsIgnoreCase( name ) ) {
                    return entry;
                }
            }
        }

        return null;
    }


    public ServiceResults handleEntityDictionary( ServiceContext context, ServiceResults results,
                                                  EntityDictionaryEntry dictionary ) throws Exception {
        if ( dictionary != null ) {
            if ( results.size() == 1 ) {
                results = handleEntityDictionary( context, results.getRef(), dictionary );
            }
            else if ( results.size() > 1 ) {
                results = handleEntityDictionary( context, results.getRefs(), dictionary );
            }
        }
        return results;
    }


    public ServiceResults handleEntityDictionary( ServiceContext context, EntityRef ref,
                                                  EntityDictionaryEntry dictionary ) throws Exception {
        if ( ref == null ) {
            throw new UnsupportedServiceOperationException( context );
        }
        List<EntityRef> refs = new ArrayList<EntityRef>();
        refs.add( ref );
        return handleEntityDictionary( context, refs, dictionary );
    }


    public ServiceResults handleEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                  EntityDictionaryEntry dictionary ) throws Exception {
        if ( ( refs == null ) || ( refs.size() == 0 ) ) {
            throw new UnsupportedServiceOperationException( context );
        }
        switch ( context.getAction() ) {
            case GET:
                return getEntityDictionary( context, refs, dictionary );

            case POST:
                return postEntityDictionary( context, refs, dictionary, context.getPayload() );

            case PUT:
                return putEntityDictionary( context, refs, dictionary, context.getPayload() );

            case DELETE:
                return deleteEntityDictionary( context, refs, dictionary );

            case HEAD:
                return headEntityDictionary( context, refs, dictionary );
        }

        throw new ServiceInvocationException( context, "Request action unhandled " + context.getAction() );
    }


    public ServiceResults getEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary ) throws Exception {

        for ( EntityDictionaryEntry entry : entityDictionaries ) {
            if ( entry.getName().equalsIgnoreCase( dictionary.getName() ) ) {
                EntityRef entityRef = refs.get( 0 );
                checkPermissionsForEntitySubPath( context, entityRef, entry.getPath() );
                Set<String> items = cast( em.getDictionaryAsSet( entityRef, entry.getName() ) );

                return new ServiceResults( this, context, Type.GENERIC, Results.fromData( items ), null, null );
            }
        }

        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults putEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary, ServicePayload payload )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults postEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                EntityDictionaryEntry dictionary, ServicePayload payload )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults deleteEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                  EntityDictionaryEntry dictionary ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults headEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                                EntityDictionaryEntry dictionary ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public boolean hasEntityCommand( String command ) {
        return ( entityCommands != null ) && ( command != null ) && entityCommands.contains( command );
    }


    public String checkForEntityCommands( ServiceContext context ) {
        if ( entityCommands == null ) {
            return null;
        }

        if ( !context.moreParameters() ) {
            return null;
        }

        String name = null;
        if ( context.firstParameterIsName() ) {
            name = context.firstParameter().getName();
            if ( entityCommands.contains( name ) ) {
                return name;
            }
        }

        return null;
    }


    public ServiceResults handleEntityCommand( ServiceContext context, ServiceResults results, String command )
            throws Exception {
        if ( command != null ) {
            if ( results.size() == 1 ) {
                results = handleEntityCommand( context, results.getRef(), command );
            }
            else if ( results.size() > 1 ) {
                results = handleEntityCommand( context, results.getRefs(), command );
            }
        }
        return results;
    }


    public ServiceResults handleEntityCommand( ServiceContext context, EntityRef ref, String command )
            throws Exception {
        if ( ref == null ) {
            throw new UnsupportedServiceOperationException( context );
        }
        List<EntityRef> refs = new ArrayList<EntityRef>();
        refs.add( ref );
        return handleEntityCommand( context, refs, command );
    }


    public ServiceResults handleEntityCommand( ServiceContext context, List<EntityRef> refs, String command )
            throws Exception {
        if ( ( refs == null ) || ( refs.size() == 0 ) ) {
            throw new UnsupportedServiceOperationException( context );
        }
        switch ( context.getAction() ) {
            case GET:
                return getEntityCommand( context, refs, command );

            case POST:
                return postEntityCommand( context, refs, command, context.getPayload() );

            case PUT:
                return putEntityCommand( context, refs, command, context.getPayload() );

            case DELETE:
                return deleteEntityCommand( context, refs, command );

            case HEAD:
                return headEntityCommand( context, refs, command );
        }

        throw new ServiceInvocationException( context, "Request action unhandled " + context.getAction() );
    }


    public ServiceResults getEntityCommand( ServiceContext context, List<EntityRef> refs, String command )
            throws Exception {

        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults putEntityCommand( ServiceContext context, List<EntityRef> refs, String command,
                                            ServicePayload payload ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults postEntityCommand( ServiceContext context, List<EntityRef> refs, String command,
                                             ServicePayload payload ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults deleteEntityCommand( ServiceContext context, List<EntityRef> refs, String command )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults headEntityCommand( ServiceContext context, List<EntityRef> refs, String command )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public boolean hasServiceMetadata( String metadata ) {
        return ( metadataTypes != null ) && ( metadata != null ) && metadataTypes.contains( metadata );
    }


    public String checkForServiceMetadata( ServiceContext context ) {
        if ( metadataTypes == null ) {
            return null;
        }

        if ( context.getServiceMetadata() == null ) {
            return null;
        }

        if ( metadataTypes.contains( context.getServiceMetadata() ) ) {
            return context.getServiceMetadata();
        }

        return null;
    }


    public ServiceResults handleServiceMetadata( ServiceContext context, String metadataType ) throws Exception {
        switch ( context.getAction() ) {
            case GET:
                return getServiceMetadata( context, metadataType );

            case POST:
                return postServiceMetadata( context, metadataType, context.getPayload() );

            case PUT:
                return putServiceMetadata( context, metadataType, context.getPayload() );

            case DELETE:
                return deleteServiceMetadata( context, metadataType );

            case HEAD:
                return headServiceMetadata( context, metadataType );
        }

        throw new ServiceInvocationException( context, "Request action unhandled " + context.getAction() );
    }


    public ServiceResults getServiceMetadata( ServiceContext context, String metadataType ) throws Exception {

        if ( metadataTypes.contains( metadataType ) ) {
            // return new ServiceResults(this, context, Type.GENERIC,
            // Results.fromData(items), null, null);
        }

        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults putServiceMetadata( ServiceContext context, String metadataType, ServicePayload payload )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults postServiceMetadata( ServiceContext context, String metadataType, ServicePayload payload )
            throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults deleteServiceMetadata( ServiceContext context, String metadataType ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public ServiceResults headServiceMetadata( ServiceContext context, String metadataType ) throws Exception {
        throw new UnsupportedServiceOperationException( context );
    }


    public void checkPermissionsForCollection( ServiceContext context ) {
        String path = context.getPath();
        checkPermissionsForPath( context, path );
    }


    public void checkPermissionsForEntity( ServiceContext context, UUID entityId ) {
        String path = context.getPath( entityId );
        checkPermissionsForPath( context, path );
    }


    public void checkPermissionsForEntity( ServiceContext context, EntityRef entity ) {
        String path = context.getPath( entity );
        checkPermissionsForPath( context, path );
    }


    public void checkPermissionsForEntitySubPath( ServiceContext context, UUID entityId, String subPath ) {
        String path = context.getPath( entityId, subPath );
        checkPermissionsForPath( context, path );
    }


    public void checkPermissionsForEntitySubPath( ServiceContext context, EntityRef entity, String subPath ) {
        String path = context.getPath( entity, subPath );
        checkPermissionsForPath( context, path );
    }


    public void checkPermissionsForPath( ServiceContext context, String path ) {
        Subject currentUser = SubjectUtils.getSubject();
        if ( currentUser == null ) {
            return;
        }
        String perm =
                getPermissionFromPath( em.getApplicationRef().getUuid(), context.getAction().toString().toLowerCase(),
                        path );
        boolean permitted = currentUser.isPermitted( perm );
        if ( logger.isDebugEnabled() ) {
            logger.debug( PATH_MSG, new Object[] { path, context.getAction(), perm, permitted } );
        }
        SubjectUtils.checkPermission( perm );
        Subject subject = SubjectUtils.getSubject();
        logger.debug( "Checked subject {} for perm {}", subject != null ? subject.toString() : "", perm );
        logger.debug( "------------------------------------------------------------------------------" );
    }


    private static final String PATH_MSG =
            "---- Checked permissions for path --------------------------------------------\n" + "Requested path: {} \n"
                    + "Requested action: {} \n" + "Requested permission: {} \n" + "Permitted: {} \n";


    /** Purpose is to enable entity dictionary entries to have name not equal to path segment. */
    protected static class EntityDictionaryEntry {
        private String name;
        private String path; // path segment used in URL


        public EntityDictionaryEntry( String name ) {
            this.name = this.path = name;
        }


        public EntityDictionaryEntry( String name, String path ) {
            this.name = name;
            this.path = path;
        }


        public String getName() {
            return name;
        }


        public String getPath() {
            return path;
        }
    }
}
