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


import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.subject.Subject;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.exceptions.UnexpectedEntityTypeException;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.security.shiro.principals.AdminUserPrincipal;
import org.apache.usergrid.security.shiro.principals.ApplicationPrincipal;
import org.apache.usergrid.security.shiro.principals.PrincipalIdentifier;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.services.ServiceResults.Type;
import org.apache.usergrid.services.exceptions.ForbiddenServiceOperationException;
import org.apache.usergrid.services.exceptions.ServiceResourceNotFoundException;

import static org.apache.usergrid.persistence.Schema.TYPE_APPLICATION;
import static org.apache.usergrid.utils.ClassUtils.cast;


public class AbstractCollectionService extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger( AbstractCollectionService.class );


    public AbstractCollectionService() {
        declareMetadataType( "indexes" );
    }

    public AbstractCollectionService(ServiceRequest serviceRequest){
        setServiceManager( serviceRequest.getServices() );
    }


    @Override
    public Entity getEntity( ServiceRequest request, UUID uuid ) throws Exception {
        if ( !isRootService() ) {
            return null;
        }
        Entity entity = em.get(new SimpleEntityRef(getEntityType(), uuid));
        if ( entity != null ) {
            entity = importEntity( request, entity );
        }
        return entity;
    }


    @Override
    public Entity getEntity( ServiceRequest request, String name ) throws Exception {
        if ( !isRootService() ) {
            return null;
        }
        String nameProperty = Schema.getDefaultSchema().aliasProperty( getEntityType() );
        if ( nameProperty == null ) {
            nameProperty = "name";
        }

        Entity entity = em.getUniqueEntityFromAlias( getEntityType(), name, false);
        if ( entity != null ) {
            entity = importEntity( request, entity );
        }
        return entity;
    }


    private EntityRef loadFromId( ServiceContext context, UUID id ) throws Exception {
        EntityRef entity = null;

        if ( !context.moreParameters() ) {
            entity = em.get( new SimpleEntityRef( getEntityType(), id) );

            entity = importEntity( context, ( Entity ) entity );
        }
        else {
            entity = em.get( new SimpleEntityRef( getEntityType(), id) );
        }

        if ( entity == null ) {
            if (logger.isTraceEnabled()) {
                logger.trace("miss on entityType: {} with uuid: {}", getEntityType(), id);
            }
            String msg = "Cannot find entity associated with uuid: " + id;
            throw new EntityNotFoundException( msg );
        }


        return entity;
    }


    private ServiceResults getItemById( ServiceContext context, UUID id, boolean skipPermissionCheck )
            throws Exception {

        EntityRef entity = loadFromId( context, id );

        validateEntityType( entity, id );

        if ( !skipPermissionCheck ) {
            checkPermissionsForEntity( context, entity );
        }

        // check ownership based on graph
        if ( !em.isCollectionMember( context.getOwner(), context.getCollectionName(), entity ) ) {

            // the entity is already loaded in the scope of the owner and type ( collection ) so it must exist at this point
            // if for some reason it's not a member of the collection, it should be and read repair it
            if( context.getOwner().getType().equals(TYPE_APPLICATION) ){
                logger.warn( "Edge missing for entity id {} with owner {}. Executing edge read repair to create new edge in " +
                    "collection {}", id, context.getOwner(), context.getCollectionName());

                em.addToCollection( context.getOwner(), context.getCollectionName(), entity);

                // do a final check to be absolutely sure we're good now before returning back to the client
                // TODO : Keep thinking if the double-check read after repair is necessary.  Favoring stability here
                if ( !em.isCollectionMember( context.getOwner(), context.getCollectionName(), entity ) ) {
                    logger.error( "Edge read repair failed for entity id {} with owner {} in collection {}",
                        id, context.getOwner(), context.getCollectionName());

                    throw new ServiceResourceNotFoundException( context );
                }

            }
            // if not head application, then we can't assume the ownership is meant to be there
            else{
                throw new ServiceResourceNotFoundException( context );
            }


        }

        List<ServiceRequest> nextRequests = context.getNextServiceRequests( entity );

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromRef( entity ), null, nextRequests );
    }


    @Override
    public ServiceResults getItemById( ServiceContext context, UUID id ) throws Exception {

        return getItemById( context, id, false );
    }


    @Override
    public ServiceResults getItemByName( ServiceContext context, String name ) throws Exception {

        // just get the UUID and then getItemById such that same results are being returned in both cases
        // don't use uniqueIndexRepair on read only logic
        UUID entityId = em.getUniqueIdFromAlias( getEntityType(), name, false);

        if ( entityId == null ) {

            if (logger.isTraceEnabled()) {
                logger.trace("Miss on entityType: {} with name: {}", getEntityType(), name);
            }

            String msg = "Cannot find entity with name: "+name;
            throw new EntityNotFoundException( msg );

        }

        return getItemById( context, entityId, false);

    }


    @Override
    public ServiceResults getItemsByQuery( ServiceContext context, Query query ) throws Exception {

        checkPermissionsForCollection( context );

        int count = 1;
        Level level = Level.REFS;

        if ( !context.moreParameters() ) {
            count = 0;
            level = Level.ALL_PROPERTIES;
        }

        if ( context.getRequest().isReturnsTree() ) {
            level = Level.ALL_PROPERTIES;
        }

        query = new Query( query );
        query.setResultsLevel( level );
        query.setLimit( query.getLimit( count ) );

        if ( !query.isReversedSet() ) {
            query.setReversed( isCollectionReversed( context ) );
        }


    /*
     * if (count > 0) { query.setMaxResults(count); }
     */

        Results r = em.searchCollection( context.getOwner(), context.getCollectionName(), query );

        List<ServiceRequest> nextRequests = null;
        if ( !r.isEmpty() ) {

            if ( !context.moreParameters() ) {
                importEntities( context, r );
            }

            nextRequests = context.getNextServiceRequests( r.getRefs() );
        }

        return new ServiceResults( this, context, Type.COLLECTION, r, null, nextRequests );
    }


    @Override
    public ServiceResults getCollection( ServiceContext context ) throws Exception {

        checkPermissionsForCollection( context );

        if ( getCollectionSort( context ) != null ) {
            return getItemsByQuery( context, new Query() );
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Limiting collection to {}", Query.DEFAULT_LIMIT);
        }

        int count = Query.DEFAULT_LIMIT;

        Results r = em.getCollection( context.getOwner(), context.getCollectionName(),
            null, count, Level.ALL_PROPERTIES, isCollectionReversed( context ) );

        importEntities( context, r );

    /*
     * if (r.isEmpty()) { throw new ServiceResourceNotFoundException(request); }
     */

        return new ServiceResults( this, context, Type.COLLECTION, r, null, null );
    }


    @Override
    public ServiceResults putItemById( ServiceContext context, UUID id ) throws Exception {

        if ( context.moreParameters() ) {
            return getItemById( context, id, true );
        }

        checkPermissionsForEntity( context, id );

        Entity item = em.get( id );

        if ( item != null ) {
            validateEntityType( item, id );

            if( context.getOwner().getType().equals(TYPE_APPLICATION)) {
                // this will repair any missing edges
                em.addToCollection(context.getOwner(), context.getCollectionName(), item);
            }

            updateEntity( context, item, context.getPayload() );
            item = importEntity( context, item );
        }
        else {
            String entityType = getEntityType();
            item = em.create( id, entityType, context.getPayload().getProperties() );
        }

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromEntity( item ), null, null );
    }


    @Override
    public ServiceResults putItemByName( ServiceContext context, String name ) throws Exception {

        if ( context.moreParameters() ) {
            return getItemByName( context, name );
        }

        // use unique index repair here before any write logic if there are problems
        Entity entity = em.getUniqueEntityFromAlias( getEntityType(), name, true);
        if ( entity == null ) {
            // null entity ref means we tried to put a non-existing entity
            // before we create a new entity for it, we should check for permission
            checkPermissionsForCollection(context);
            Map<String, Object> properties = context.getPayload().getProperties();
            if ( !properties.containsKey( "name" ) || !( ( String ) properties.get( "name" ) ).trim().equalsIgnoreCase(
                    name ) ) {
                properties.put( "name", name );
            }
            entity = em.create( getEntityType(), properties );
        }
        else {
            entity = importEntity( context, entity );
            checkPermissionsForEntity( context, entity );

            if( context.getOwner().getType().equals(TYPE_APPLICATION)) {
                // this will repair any missing edges
                em.addToCollection(context.getOwner(), context.getCollectionName(), entity);
            }

            updateEntity( context, entity );

        }

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromEntity( entity ), null, null );
    }


    @Override
    public ServiceResults putItemsByQuery( ServiceContext context, Query query ) throws Exception {

        checkPermissionsForCollection( context );

        if ( context.moreParameters() ) {
            return getItemsByQuery( context, query );
        }

        query = new Query( query );
        query.setResultsLevel( Level.ALL_PROPERTIES );
        query.setLimit( 1000 );
        if ( !query.isReversedSet() ) {
            query.setReversed( isCollectionReversed( context ) );
        }

        Results r = em.searchCollection( context.getOwner(), context.getCollectionName(), query );
        if ( r.isEmpty() ) {
            throw new ServiceResourceNotFoundException( context );
        }

        updateEntities( context, r );

        return new ServiceResults( this, context, Type.COLLECTION, r, null, null );
    }

    @Override
    public ServiceResults postCollectionSchema( ServiceRequest serviceRequest ) throws Exception {
        setServiceManager( serviceRequest.getServices() );
        ServiceContext context = serviceRequest.getAppContext();

        checkPermissionsForCollection( context );
        //TODO: write rest test for these line of codes
        Subject currentUser = SubjectUtils.getSubject();
        Object currentUserPrincipal =currentUser.getPrincipal();

        Map collectionSchema = null;

        if(currentUserPrincipal instanceof AdminUserPrincipal) {
            AdminUserPrincipal adminUserPrincipal = ( AdminUserPrincipal ) currentUserPrincipal;

            collectionSchema = em.createCollectionSchema( context.getCollectionName(),
                adminUserPrincipal.getUser().getEmail(), context.getProperties() );
        }
        else if(currentUserPrincipal instanceof ApplicationPrincipal){
            collectionSchema = em.createCollectionSchema( context.getCollectionName(),
                "app credentials", context.getProperties() );
        }
        else if ( currentUserPrincipal instanceof PrincipalIdentifier ) {
            collectionSchema = em.createCollectionSchema( context.getCollectionName(),
                "generic credentials", context.getProperties() );
        }

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromData( collectionSchema ), null, null );

    }

    @Override
    public ServiceResults getCollectionSchema( ServiceRequest serviceRequest ) throws Exception {
        setServiceManager( serviceRequest.getServices() );
        ServiceContext context = serviceRequest.getAppContext();
        context.setAction( ServiceAction.GET );
        checkPermissionsForCollection( context );

        Object collectionSchema = em.getCollectionSchema( context.getCollectionName() );

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromData( collectionSchema ), null, null );

    }


    @Override
    public ServiceResults postCollection( ServiceContext context ) throws Exception {

        checkPermissionsForCollection( context );

        if ( context.getPayload().isBatch() ) {
            List<Entity> entities = new ArrayList<Entity>();
            List<Map<String, Object>> batch = context.getPayload().getBatchProperties();

            if (logger.isTraceEnabled()) {
                logger.trace("Attempting to batch create {} entities in collection {}",
                        batch.size(), context.getCollectionName());
            }


            final Map<String, Boolean> nameValues = new HashMap<>(batch.size());

            for ( Map<String, Object> p : batch ) {

                // track unique name value in the batch to identify if duplicates are trying to be created
                String name = (String) p.get("name");
                if( name !=null && nameValues.get(name) !=null ){
                    logger.warn("Batch contains more than 1 entity with the same name: {}", name);
                }else{
                    nameValues.put(name, true);
                }


                if (logger.isTraceEnabled()) {
                    logger.trace("Creating entity [{}] in collection [{}]", p, context.getCollectionName());
                }


                Entity item = null;
                try {
                    item = em.createItemInCollection( context.getOwner(), context.getCollectionName(), getEntityType(),
                            p );
                }
                catch ( Exception e ) {

                    logger.error("Entity [{}] unable to be created in collection [{}] due to [{} - {}]", p, context.getCollectionName(),
                            e.getClass().getSimpleName(), e.getMessage());

                    // move on as we can't block the whole batch if only 1 failed
                    continue;
                }


                if (logger.isTraceEnabled()) {
                    logger.trace("Successfully created entity [{}] in collection [{}]", p, context.getCollectionName());
                }


                item = importEntity( context, item );
                entities.add( item );

            }
            return new ServiceResults( this, context, Type.COLLECTION, Results.fromEntities( entities ), null, null );
        }

        Entity item = em.createItemInCollection( context.getOwner(), context.getCollectionName(), getEntityType(),
                context.getProperties() );

        item = importEntity( context, item );

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromEntity( item ), null, null );
    }


    @Override
    public ServiceResults putCollection( ServiceContext context ) throws Exception {
        return postCollection( context );
    }


    @Override
    public ServiceResults postItemsByQuery( ServiceContext context, Query query ) throws Exception {
        if ( context.moreParameters() ) {
            return super.postItemsByQuery( context, query );
        }
        return postCollection( context );
    }


    @Override
    public ServiceResults postItemById( ServiceContext context, UUID id ) throws Exception {
        if ( context.moreParameters() ) {
            return getItemById( context, id, true );
        }
        checkPermissionsForEntity( context, id );

        Entity entity = em.get( new SimpleEntityRef( this.getEntityType(), id) );
        if ( entity == null ) {
            throw new ServiceResourceNotFoundException( context );
        }

        validateEntityType( entity, id );

        entity = importEntity( context, entity );

        em.addToCollection( context.getOwner(), context.getCollectionName(), entity );

        return new ServiceResults( null, context, Type.COLLECTION, Results.fromEntity( entity ), null, null );
    }


    @Override
    public ServiceResults postItemByName( ServiceContext context, String name ) throws Exception {

        if ( context.moreParameters() ) {
            return super.postItemByName( context, name );
        }

        // use unique index repair here before any write logic if there are problems
        Entity entity = em.getUniqueEntityFromAlias( getEntityType(), name, true);
        if ( entity == null ) {
            throw new ServiceResourceNotFoundException( context );
        }

        return postItemById( context, entity.getUuid() );
    }


    protected boolean isDeleteAllowed( ServiceContext context, Entity entity ) {
        return true;
    }


    protected void prepareToDelete( ServiceContext context, Entity entity ) {
        if ( !isDeleteAllowed( context, entity ) ) {
            throw new ForbiddenServiceOperationException( context );
        }
    }


    @Override
    public ServiceResults deleteItemById( ServiceContext context, UUID id ) throws Exception {

        checkPermissionsForEntity( context, id );

        if ( context.moreParameters() ) {
            return getItemById( context, id );
        }

        Entity item = em.get( new SimpleEntityRef( this.getEntityType(), id) );
        if ( item == null ) {
            throw new ServiceResourceNotFoundException( context );
        }

        validateEntityType( item, id );

        item = importEntity( context, item );

        prepareToDelete( context, item );

        em.removeFromCollection( context.getOwner(), context.getCollectionName(), item );

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromEntity( item ), null, null );
    }


    @Override
    public ServiceResults deleteItemByName( ServiceContext context, String name ) throws Exception {

        if ( context.moreParameters() ) {
            return getItemByName( context, name );
        }

        // use unique index repair here before any write logic if there are problems
        Entity entity = em.getUniqueEntityFromAlias( getEntityType(), name, true);
        if ( entity == null ) {
            throw new ServiceResourceNotFoundException( context );
        }
        entity = importEntity( context, entity );

        checkPermissionsForEntity( context, entity );

        prepareToDelete( context, entity );

        em.removeFromCollection( context.getOwner(), context.getCollectionName(), entity );

        return new ServiceResults( this, context, Type.COLLECTION, Results.fromEntity( entity ), null, null );
    }


    @Override
    public ServiceResults deleteItemsByQuery( ServiceContext context, Query query ) throws Exception {

        checkPermissionsForCollection( context );

        if ( context.moreParameters() ) {
            return getItemsByQuery( context, query );
        }

        query = new Query( query );
        query.setResultsLevel( Level.ALL_PROPERTIES );
        query.setLimit( query.getLimit() );

        if ( !query.isReversedSet() ) {
            query.setReversed( isCollectionReversed( context ) );
        }


        Results r = em.searchCollection( context.getOwner(), context.getCollectionName(), query );

        importEntities( context, r );

        for ( Entity entity : r ) {
            prepareToDelete( context, entity );
        }

        for ( Entity entity : r ) {
            em.removeFromCollection( context.getOwner(), context.getCollectionName(), entity );
        }

        return new ServiceResults( this, context, Type.COLLECTION, r, null, null );
    }


    @Override
    public ServiceResults getServiceMetadata( ServiceContext context, String metadataType ) throws Exception {

        if ( "indexes".equals( metadataType ) ) {
            Set<String> indexes = cast( em.getCollectionIndexes( context.getOwner(), context.getCollectionName() ) );

            return new ServiceResults( this,
                    context.getRequest().withPath( context.getRequest().getPath() + "/indexes" ),
                    context.getPreviousResults(), context.getChildPath(), Type.GENERIC, Results.fromData( indexes ),
                    null, null );
        }
        return null;
    }


    private void validateEntityType( EntityRef item, UUID id ) throws UnexpectedEntityTypeException {
        if ( !getEntityType().equalsIgnoreCase( item.getType() ) ) {
            throw new UnexpectedEntityTypeException(
                    "Entity " + id + " is not the expected type, expected " + getEntityType() + ", found " + item
                            .getType() );
        }
    }
}
