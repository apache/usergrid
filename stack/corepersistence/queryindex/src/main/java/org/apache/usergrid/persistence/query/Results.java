/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.usergrid.persistence.query;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;
import static org.apache.usergrid.persistence.query.SimpleEntityRef.ref;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


@XmlRootElement
public class Results implements Iterable<Entity> {


    public enum Level {
        IDS, REFS, CORE_PROPERTIES, ALL_PROPERTIES, LINKED_PROPERTIES
    }


    Level level = Level.IDS;
    Id id;
    List<Id> ids;
    Set<Id> idSet;

    EntityRef ref;
    List<EntityRef> refs;
    Map<Id, EntityRef> refsMap;
    Map<String, List<EntityRef>> refsByType;

    Entity entity;
    List<Entity> entities;
    Map<Id, Entity> entitiesMap;
    Map<String, List<Entity>> entitiesByType;

    List<ConnectionRef> connections;
    boolean forwardConnections = true;

    List<AggregateCounterSet> counters;

    Set<String> types;

    Map<Id, Map<String, Object>> metadata;
    boolean metadataMerged = true;

    Id nextResult;
    String cursor;

    Query query;
    Object data;
    String dataName;

    private SearchVisitor searchVisitor;


    public Results() {
    }


    public Results( Results r ) {
        if ( r != null ) {
            level = r.level;

            id = r.id;
            ids = r.ids;
            idSet = r.idSet;

            ref = r.ref;
            refs = r.refs;
            refsMap = r.refsMap;
            refsByType = r.refsByType;

            entity = r.entity;
            entities = r.entities;
            entitiesMap = r.entitiesMap;
            entitiesByType = r.entitiesByType;

            connections = r.connections;
            forwardConnections = r.forwardConnections;

            counters = r.counters;

            types = r.types;

            metadata = r.metadata;
            metadataMerged = r.metadataMerged;

            nextResult = r.nextResult;
            cursor = r.cursor;

            query = r.query;
            data = r.data;
            dataName = r.dataName;
        }
    }


    public void init() {
        level = Level.IDS;

        id = null;
        ids = null;
        idSet = null;

        ref = null;
        refs = null;
        refsMap = null;
        refsByType = null;

        entity = null;
        entities = null;
        entitiesMap = null;
        entitiesByType = null;

        connections = null;
        forwardConnections = true;

        counters = null;

        types = null;

        // metadata = null;
        metadataMerged = false;

        query = null;
        data = null;
        dataName = null;
    }


    public static Results fromIdList( List<Id> l ) {
        Results r = new Results();
        r.setIds( l );
        return r;
    }


    public static Results fromIdList( List<Id> l, String type ) {
        if ( type == null ) {
            return fromIdList( l );
        }
        List<EntityRef> refs = new ArrayList<EntityRef>();
        for ( Id u : l ) {
            refs.add( ref( u ) );
        }
        Results r = new Results();
        r.setRefs( refs );
        return r;
    }


    public static Results fromId( Id id ) {
        Results r = new Results();
        if ( id != null ) {
            List<Id> l = new ArrayList<Id>();
            l.add( id );
            r.setIds( l );
        }
        return r;
    }


    public static Results fromRefList( List<EntityRef> l ) {
        Results r = new Results();
        r.setRefs( l );
        return r;
    }


//    public static Results fromEntities( List<? extends Entity> l ) {
//        Results r = new Results();
//        r.setEntities( l );
//        return r;
//    }


    public static Results fromEntity( Entity e ) {
        Results r = new Results();
        r.setEntity( e );
        return r;
    }


    public static Results fromRef( EntityRef ref ) {
        if ( ref instanceof Entity ) {
            return fromEntity( ( Entity ) ref );
        }
        Results r = new Results();
        r.setRef( ref );
        return r;
    }


//    public static Results fromData( Object obj ) {
//        Results r = new Results();
//        r.setData( obj );
//        return r;
//    }
//
//
//    public static Results fromCounters( AggregateCounterSet counters ) {
//        Results r = new Results();
//        List<AggregateCounterSet> l = new ArrayList<AggregateCounterSet>();
//        l.add( counters );
//        r.setCounters( l );
//        return r;
//    }
//
//
//    public static Results fromCounters( List<AggregateCounterSet> counters ) {
//        Results r = new Results();
//        r.setCounters( counters );
//        return r;
//    }
//
//
//    @SuppressWarnings("unchecked")
//    public static Results fromConnections( List<? extends ConnectionRef> connections ) {
//        Results r = new Results();
//        r.setConnections( ( List<ConnectionRef> ) connections, true );
//        return r;
//    }
//
//
//    @SuppressWarnings("unchecked")
//    public static Results fromConnections( List<? extends ConnectionRef> connections, boolean forward ) {
//        Results r = new Results();
//        r.setConnections( ( List<ConnectionRef> ) connections, forward );
//        return r;
//    }


    public Level getLevel() {
        return level;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Query getQuery() {
        return query;
    }


    public void setQuery( Query query ) {
        this.query = query;
    }


    public Results withQuery( Query query ) {
        this.query = query;
        return this;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Id getId() {
        if ( id != null ) {
            return id;
        }
        if ( entity != null ) {
            id = entity.getId();
            return id;
        }
        if ( ( ids != null ) && ( ids.size() > 0 ) ) {
            id = ids.get( 0 );
            return id;
        }
        if ( ( entities != null ) && ( entities.size() > 0 ) ) {
            entity = entities.get( 0 );
            id = entity.getId();
            return id;
        }
        if ( ( refs != null ) && ( refs.size() > 0 ) ) {
            EntityRef ref = refs.get( 0 );
            id = ref.getId();
        }
        return id;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<Id> getIds() {
        if ( ids != null ) {
            return ids;
        }
        /*
         * if (connectionTypeAndEntityTypeToEntityIdMap != null) { ids = new
         * ArrayList<Id>(); Set<Id> entitySet = new LinkedHashSet<Id>();
         * for (String ctype : connectionTypeAndEntityTypeToEntityIdMap
         * .keySet()) { Map<String, List<Id>> m =
         * connectionTypeAndEntityTypeToEntityIdMap .get(ctype); for (String
         * etype : m.keySet()) { List<Id> l = m.get(etype); for (Id id : l)
         * { if (!entitySet.contains(id)) { ids.add(id); } } } } return ids; }
         */
        if ( connections != null ) {
            ids = new ArrayList<Id>();
            for ( ConnectionRef connection : connections ) {
                if ( forwardConnections ) {
                    ConnectedEntityRef c = connection.getConnectedEntity();
                    if ( c != null ) {
                        ids.add( c.getId() );
                    }
                }
                else {
                    EntityRef c = connection.getConnectingEntity();
                    if ( c != null ) {
                        ids.add( c.getId() );
                    }
                }
            }
            return ids;
        }
        if ( ( entities != null )
        /* || (connectionTypeAndEntityTypeToEntityMap != null) */ ) {
            // getEntities();
            ids = new ArrayList<Id>();
            for ( Entity entity : entities ) {
                ids.add( entity.getId() );
            }
            return ids;
        }
        if ( refs != null ) {
            ids = new ArrayList<Id>();
            for ( EntityRef ref : refs ) {
                ids.add( ref.getId() );
            }
            return ids;
        }
        if ( id != null ) {
            ids = new ArrayList<Id>();
            ids.add( id );
            return ids;
        }
        if ( entity != null ) {
            ids = new ArrayList<Id>();
            ids.add( entity.getId() );
            return ids;
        }
        return new ArrayList<Id>();
    }


    public void setIds( List<Id> resultsIds ) {
        init();
        ids = resultsIds;
        level = Level.IDS;
    }


    public Results withIds( List<Id> resultsIds ) {
        setIds( resultsIds );
        return this;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Set<Id> getIdSet() {
        if ( idSet != null ) {
            return idSet;
        }
        getIds();
        if ( ids != null ) {
            idSet = new LinkedHashSet<Id>();
            idSet.addAll( ids );
            return idSet;
        }
        return new LinkedHashSet<Id>();
    }


//    @JsonSerialize(include = Inclusion.NON_NULL)
//    @SuppressWarnings("unchecked")
//    public List<EntityRef> getRefs() {
//        if ( refs != null ) {
//            return refs;
//        }
//        List<?> l = getEntities();
//        if ( ( l != null ) && ( l.size() > 0 ) ) {
//            return ( List<EntityRef> ) l;
//        }
//        if ( connections != null ) {
//            refs = new ArrayList<EntityRef>();
//            for ( ConnectionRef connection : connections ) {
//                if ( forwardConnections ) {
//                    ConnectedEntityRef c = connection.getConnectedEntity();
//                    if ( c != null ) {
//                        refs.add( c );
//                    }
//                }
//                else {
//                    EntityRef c = connection.getConnectingEntity();
//                    if ( c != null ) {
//                        refs.add( c );
//                    }
//                }
//            }
//            return refs;
//        }
//        if ( ref != null ) {
//            refs = new ArrayList<EntityRef>();
//            refs.add( ref );
//            return refs;
//        }
//        return new ArrayList<EntityRef>();
//    }


    public void setRefs( List<EntityRef> resultsRefs ) {
        init();
        refs = resultsRefs;
        level = Level.REFS;
    }


    public Results withRefs( List<EntityRef> resultsRefs ) {
        setRefs( resultsRefs );
        return this;
    }


    public void setRef( EntityRef ref ) {
        init();
        this.ref = ref;
        level = Level.REFS;
    }


    public Results withRef( EntityRef ref ) {
        setRef( ref );
        return this;
    }


//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public EntityRef getRef() {
//        if ( ref != null ) {
//            return ref;
//        }
//        Entity e = getEntity();
//        if ( e != null ) {
//            return ref( e.getId(), e.getVersion() );
//        }
//        Id u = getId();
//        if ( u != null ) {
//            return ref( u );
//        }
//        return null;
//    }

//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public Map<Id, EntityRef> getRefsMap() {
//        if ( refsMap != null ) {
//            return refsMap;
//        }
//        getEntitiesMap();
//        if ( entitiesMap != null ) {
//            refsMap = cast( entitiesMap );
//            return refsMap;
//        }
//        getRefs();
//        if ( refs != null ) {
//            refsMap = new LinkedHashMap<Id, EntityRef>();
//            for ( EntityRef ref : refs ) {
//                refsMap.put( ref.getId(), ref );
//            }
//        }
//        return refsMap;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public Entity getEntity() {
//        mergeEntitiesWithMetadata();
//        if ( entity != null ) {
//            return entity;
//        }
//        if ( ( entities != null ) && ( entities.size() > 0 ) ) {
//            entity = entities.get( 0 );
//            return entity;
//        }
//        return null;
//    }


    public void setEntity( Entity resultEntity ) {
        init();
        entity = resultEntity;
        level = Level.CORE_PROPERTIES;
    }


    public Results withEntity( Entity resultEntity ) {
        setEntity( resultEntity );
        return this;
    }


    public Iterator<Id> idIterator() {
        List<Id> l = getIds();
        if ( l != null ) {
            return l.iterator();
        }
        return ( new ArrayList<Id>( 0 ) ).iterator();
    }


//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public List<Entity> getEntities() {
//        mergeEntitiesWithMetadata();
//        if ( entities != null ) {
//            return entities;
//        }
//        /*
//         * if (connectionTypeAndEntityTypeToEntityMap != null) { entities = new
//         * ArrayList<Entity>(); Map<Id, Entity> eMap = new LinkedHashMap<Id,
//         * Entity>(); for (String ctype :
//         * connectionTypeAndEntityTypeToEntityMap.keySet()) { Map<String,
//         * List<Entity>> m = connectionTypeAndEntityTypeToEntityMap .get(ctype);
//         * for (String etype : m.keySet()) { List<Entity> l = m.get(etype); for
//         * (Entity e : l) { if (!eMap.containsKey(e.getUuid())) { entities.add(e);
//         * eMap.put(e.getUuid(), e); } } } } return entities; }
//         */
//        if ( entity != null ) {
//            entities = new ArrayList<Entity>();
//            entities.add( entity );
//            return entities;
//        }
//        return new ArrayList<Entity>();
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public Map<Id, Entity> getEntitiesMap() {
//        if ( entitiesMap != null ) {
//            return entitiesMap;
//        }
//        if ( entities != null ) {
//            entitiesMap = new LinkedHashMap<Id, Entity>();
//            for ( Entity entity : entities ) {
//                entitiesMap.put( entity.getId(), entity );
//            }
//        }
//        return entitiesMap;
//    }
//
//
//    public List<EntityRef> getEntityRefsByType( String type ) {
//        if ( entitiesByType != null ) {
//            return refsByType.get( type );
//        }
//        List<EntityRef> l = cast( getEntitiesByType( type ) );
//        if ( l != null ) {
//            return l;
//        }
//        getRefs();
//        if ( refs == null ) {
//            return null;
//        }
//        refsByType = new LinkedHashMap<String, List<EntityRef>>();
//        for ( Entity entity : entities ) {
//            l = refsByType.get( entity.getId().getType() );
//            if ( l == null ) {
//                l = new ArrayList<EntityRef>();
//                refsByType.put( entity.getId().getType(), l );
//            }
//            l.add( entity );
//        }
//        return l;
//    }
//
//
//    public List<Entity> getEntitiesByType( String type ) {
//        if ( entitiesByType != null ) {
//            return entitiesByType.get( type );
//        }
//        getEntities();
//        if ( entities == null ) {
//            return null;
//        }
//        List<Entity> l = null;
//        entitiesByType = new LinkedHashMap<String, List<Entity>>();
//        for ( Entity entity : entities ) {
//            l = entitiesByType.get( entity.getType() );
//            if ( l == null ) {
//                l = new ArrayList<Entity>();
//                entitiesByType.put( entity.getType(), l );
//            }
//            l.add( entity );
//        }
//        return l;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public Set<String> getTypes() {
//        if ( types != null ) {
//            return types;
//        }
//        getEntityRefsByType( "entity" );
//        if ( entitiesByType != null ) {
//            types = entitiesByType.keySet();
//        }
//        else if ( refsByType != null ) {
//            types = refsByType.keySet();
//        }
//        return types;
//    }
//
//
//    public void merge( Results results ) {
//        getEntitiesMap();
//        results.getEntitiesMap();
//        if ( entitiesMap != null || results.entitiesMap != null ) {
//
//            level = Level.ALL_PROPERTIES;
//
//            // do nothing, nothing to union
//            if ( entitiesMap != null && results.entitiesMap == null ) {
//                return;
//                // other side has the results, assign and return
//            }
//            else if ( entitiesMap == null && results.entitiesMap != null ) {
//                entities = results.entities;
//                return;
//            }
//
//            entitiesMap.putAll( results.entitiesMap );
//            entities = new ArrayList<Entity>( entitiesMap.values() );
//
//            return;
//        }
//
//        getRefsMap();
//        results.getRefsMap();
//        if ( ( refsMap != null ) || ( results.refsMap != null ) ) {
//
//            level = Level.REFS;
//
//            // do nothing, nothing to union
//            if ( refsMap != null && results.refsMap == null ) {
//                return;
//                // other side has the results, assign and return
//            }
//            else if ( refsMap == null && results.refsMap != null ) {
//                refs = results.refs;
//                return;
//            }
//
//            refsMap.putAll( results.refsMap );
//            refs = new ArrayList<EntityRef>( refsMap.values() );
//
//            return;
//        }
//
//        getIdSet();
//        results.getIdSet();
//        if ( ( idSet != null ) && ( results.idSet != null ) ) {
//
//            level = Level.IDS;
//
//            // do nothing, nothing to union
//            if ( idSet != null && results.idSet == null ) {
//                return;
//                // other side has the results, assign and return
//            }
//            else if ( idSet == null && results.idSet != null ) {
//                ids = results.ids;
//                return;
//            }
//
//            idSet.addAll( results.idSet );
//            ids = new ArrayList<Id>( idSet );
//
//            return;
//        }
//    }
//
//
//    /** Remove the passed in results from the current results */
//    public void subtract( Results results ) {
//        getEntitiesMap();
//        results.getEntitiesMap();
//
//        if ( ( entitiesMap != null ) && ( results.entitiesMap != null ) ) {
//            Map<Id, Entity> newMap = new LinkedHashMap<Id, Entity>();
//            for ( Map.Entry<Id, Entity> e : entitiesMap.entrySet() ) {
//                if ( !results.entitiesMap.containsKey( e.getKey() ) ) {
//                    newMap.put( e.getKey(), e.getValue() );
//                }
//            }
//            entitiesMap = newMap;
//            entities = new ArrayList<Entity>( entitiesMap.values() );
//            level = Level.ALL_PROPERTIES;
//            return;
//        }
//
//        getRefsMap();
//        results.getRefsMap();
//        if ( ( refsMap != null ) && ( results.refsMap != null ) ) {
//            Map<Id, EntityRef> newMap = new LinkedHashMap<Id, EntityRef>();
//            for ( Map.Entry<Id, EntityRef> e : refsMap.entrySet() ) {
//                if ( !results.refsMap.containsKey( e.getKey() ) ) {
//                    newMap.put( e.getKey(), e.getValue() );
//                }
//            }
//            refsMap = newMap;
//            refs = new ArrayList<EntityRef>( refsMap.values() );
//            level = Level.REFS;
//            return;
//        }
//
//        getIdSet();
//        results.getIdSet();
//        if ( ( idSet != null ) && ( results.idSet != null ) ) {
//            Set<Id> newSet = new LinkedHashSet<Id>();
//            for ( Id uuid : idSet ) {
//                if ( !results.idSet.contains( uuid ) ) {
//                    newSet.add( uuid );
//                }
//            }
//            idSet = newSet;
//            ids = new ArrayList<Id>( idSet );
//            level = Level.IDS;
//            return;
//        }
//    }
//
//
//    /** Perform an intersection of the 2 results */
//    public void and( Results results ) {
//        getEntitiesMap();
//        results.getEntitiesMap();
//
//        if ( ( entitiesMap != null ) && ( results.entitiesMap != null ) ) {
//            Map<Id, Entity> newMap = new LinkedHashMap<Id, Entity>();
//            for ( Map.Entry<Id, Entity> e : entitiesMap.entrySet() ) {
//                if ( results.entitiesMap.containsKey( e.getKey() ) ) {
//                    newMap.put( e.getKey(), e.getValue() );
//                }
//            }
//            entitiesMap = newMap;
//            entities = new ArrayList<Entity>( entitiesMap.values() );
//            level = Level.ALL_PROPERTIES;
//            return;
//        }
//
//        getRefsMap();
//        results.getRefsMap();
//        if ( ( refsMap != null ) && ( results.refsMap != null ) ) {
//            Map<Id, EntityRef> newMap = new LinkedHashMap<Id, EntityRef>();
//            for ( Map.Entry<Id, EntityRef> e : refsMap.entrySet() ) {
//                if ( results.refsMap.containsKey( e.getKey() ) ) {
//                    newMap.put( e.getKey(), e.getValue() );
//                }
//            }
//            refsMap = newMap;
//            refs = new ArrayList<EntityRef>( refsMap.values() );
//            level = Level.REFS;
//            ids = null;
//            return;
//        }
//
//        getIdSet();
//        results.getIdSet();
//        if ( ( idSet != null ) && ( results.idSet != null ) ) {
//            Set<Id> newSet = new LinkedHashSet<Id>();
//            for ( Id uuid : idSet ) {
//                if ( results.idSet.contains( uuid ) ) {
//                    newSet.add( uuid );
//                }
//            }
//            idSet = newSet;
//            ids = new ArrayList<Id>( idSet );
//            level = Level.IDS;
//            return;
//        }
//
//        // should be empty
//        init();
//    }
//
//
//    public void replace( Entity entity ) {
//        entitiesMap = null;
//        if ( ( this.entity != null ) && ( this.entity.getUuid().equals( entity.getUuid() ) ) ) {
//            this.entity = entity;
//        }
//        if ( entities != null ) {
//            ListIterator<Entity> i = entities.listIterator();
//            while ( i.hasNext() ) {
//                Entity e = i.next();
//                if ( e.getUuid().equals( entity.getUuid() ) ) {
//                    i.set( entity );
//                }
//            }
//        }
//    }
//
//
//    public Results startingFrom( Id entityId ) {
//        if ( entities != null ) {
//            for ( int i = 0; i < entities.size(); i++ ) {
//                Entity entity = entities.get( i );
//                if ( entityId.equals( entity.getUuid() ) ) {
//                    if ( i == 0 ) {
//                        return this;
//                    }
//                    return Results.fromEntities( entities.subList( i, entities.size() ) );
//                }
//            }
//        }
//        if ( refs != null ) {
//            for ( int i = 0; i < refs.size(); i++ ) {
//                EntityRef entityRef = refs.get( i );
//                if ( entityId.equals( entityRef.getUuid() ) ) {
//                    if ( i == 0 ) {
//                        return this;
//                    }
//                    return Results.fromRefList( refs.subList( i, refs.size() ) );
//                }
//            }
//        }
//        if ( ids != null ) {
//            for ( int i = 0; i < ids.size(); i++ ) {
//                Id uuid = ids.get( i );
//                if ( entityId.equals( uuid ) ) {
//                    if ( i == 0 ) {
//                        return this;
//                    }
//                    return Results.fromIdList( ids.subList( i, ids.size() ) );
//                }
//            }
//        }
//        return this;
//    }
//
//
//    @SuppressWarnings("unchecked")
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public <E extends Entity> List<E> getList() {
//        List<Entity> l = getEntities();
//        return ( List<E> ) l;
//    }
//
//
//    public <E extends Entity> Iterator<E> iterator( Class<E> cls ) {
//        List<E> l = getList();
//        if ( l != null ) {
//            return l.iterator();
//        }
//        return ( new ArrayList<E>( 0 ) ).iterator();
//    }
//

    @Override
    public Iterator<Entity> iterator() {
        List<Entity> l = null; // getEntities();
        if ( l != null ) {
            return l.iterator();
        }
        return ( new ArrayList<Entity>( 0 ) ).iterator();
    }

//
//    public Results findForProperty( String propertyName, Object propertyValue ) {
//        return findForProperty( propertyName, propertyValue, 1 );
//    }
//
//
//    public Results findForProperty( String propertyName, Object propertyValue, int count ) {
//        if ( propertyValue == null ) {
//            return new Results();
//        }
//        List<Entity> l = getEntities();
//        if ( l == null ) {
//            return new Results();
//        }
//        List<Entity> found = new ArrayList<Entity>();
//        for ( Entity e : l ) {
//            if ( propertyValue.equals( e.getProperty( propertyName ) ) ) {
//                found.add( e );
//                if ( ( count > 0 ) && ( found.size() == count ) ) {
//                    break;
//                }
//            }
//        }
//        return Results.fromEntities( found );
//    }
//
//
//    @SuppressWarnings("unchecked")
//    public void setEntities( List<? extends Entity> resultsEntities ) {
//        init();
//        entities = ( List<Entity> ) resultsEntities;
//        level = Level.CORE_PROPERTIES;
//    }
//
//
//    public Results withEntities( List<? extends Entity> resultsEntities ) {
//        setEntities( resultsEntities );
//        return this;
//    }
//
//
//    public boolean hasConnections() {
//        return connections != null;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public List<ConnectionRef> getConnections() {
//        return connections;
//    }
//
//
//    private void setConnections( List<ConnectionRef> connections, boolean forwardConnections ) {
//        init();
//        this.connections = connections;
//        this.forwardConnections = forwardConnections;
//        level = Level.REFS;
//        for ( ConnectionRef connection : connections ) {
//            if ( forwardConnections ) {
//                this.setMetadata( connection.getConnectedEntity().getUuid(), "connection",
//                        connection.getConnectionType() );
//            }
//            else {
//                this.setMetadata( connection.getConnectingEntity().getUuid(), "connection",
//                        connection.getConnectionType() );
//            }
//        }
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public Object getObject() {
//        if ( data != null ) {
//            return data;
//        }
//        if ( entities != null ) {
//            return entities;
//        }
//        if ( ids != null ) {
//            return ids;
//        }
//        if ( entity != null ) {
//            return entity;
//        }
//        if ( id != null ) {
//            return id;
//        }
//        if ( counters != null ) {
//            return counters;
//        }
//        return null;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public String getObjectName() {
//        if ( dataName != null ) {
//            return dataName;
//        }
//        if ( entities != null ) {
//            return "entities";
//        }
//        if ( ids != null ) {
//            return "ids";
//        }
//        if ( entity != null ) {
//            return "entity";
//        }
//        if ( id != null ) {
//            return "id";
//        }
//        return null;
//    }
//
//
//    public void setDataName( String dataName ) {
//        this.dataName = dataName;
//    }
//
//
//    public Results withDataName( String dataName ) {
//        this.dataName = dataName;
//        return this;
//    }
//
//
//    public boolean hasData() {
//        return data != null;
//    }
//
//
//    public void setData( Object data ) {
//        this.data = data;
//    }
//
//
//    public Results withData( Object data ) {
//        this.data = data;
//        return this;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public Object getData() {
//        return data;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public List<AggregateCounterSet> getCounters() {
//        return counters;
//    }
//
//
//    public void setCounters( List<AggregateCounterSet> counters ) {
//        this.counters = counters;
//    }
//
//
//    public Results withCounters( List<AggregateCounterSet> counters ) {
//        this.counters = counters;
//        return this;
//    }
//
//
//    public int size() {
//        if ( entities != null ) {
//            return entities.size();
//        }
//        if ( refs != null ) {
//            return refs.size();
//        }
//        if ( ids != null ) {
//            return ids.size();
//        }
//        if ( entity != null ) {
//            return 1;
//        }
//        if ( ref != null ) {
//            return 1;
//        }
//        if ( id != null ) {
//            return 1;
//        }
//        return 0;
//    }
//
//
//    public boolean isEmpty() {
//        return size() == 0;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public Id getNextResult() {
//        return nextResult;
//    }
//
//
//    public Results excludeCursorMetadataAttribute() {
//        if ( metadata != null ) {
//            for ( Entry<Id, Map<String, Object>> entry : metadata.entrySet() ) {
//                Map<String, Object> map = entry.getValue();
//                if ( map != null ) {
//                    map.remove( Schema.PROPERTY_CURSOR );
//                }
//            }
//        }
//        return new Results( this );
//    }
//
//
//    public Results trim( int count ) {
//        if ( count == 0 ) {
//            return this;
//        }
//
//        int size = size();
//        if ( size <= count ) {
//            return this;
//        }
//
//        List<Id> ids = getIds();
//        Id nextResult = null;
//        String cursor = null;
//        if ( ids.size() > count ) {
//            nextResult = ids.get( count );
//            ids = ids.subList( 0, count );
//            if ( metadata != null ) {
//                cursor = StringUtils.toString( MapUtils.getMapMap( metadata, nextResult, "cursor" ) );
//            }
//            if ( cursor == null ) {
//                cursor = encodeBase64URLSafeString( bytes( nextResult ) );
//            }
//        }
//
//        Results r = new Results( this );
//        if ( r.entities != null ) {
//            r.entities = r.entities.subList( 0, count );
//        }
//        if ( r.refs != null ) {
//            r.refs = r.refs.subList( 0, count );
//        }
//        if ( r.ids != null ) {
//            r.ids = r.ids.subList( 0, count );
//        }
//        r.setNextResult( nextResult );
//        r.setCursor( cursor );
//
//        return r;
//    }
//
//
//    public boolean hasMoreResults() {
//        return nextResult != null;
//    }
//
//
//    public void setNextResult( Id nextResult ) {
//        this.nextResult = nextResult;
//    }
//
//
//    public Results withNextResult( Id nextResult ) {
//        this.nextResult = nextResult;
//        return this;
//    }
//
//
//    @JsonSerialize(include = Inclusion.NON_NULL)
//    public String getCursor() {
//        return cursor;
//    }
//
//
//    public boolean hasCursor() {
//        return cursor != null && cursor.length() > 0;
//    }
//
//
//    public void setCursor( String cursor ) {
//        this.cursor = cursor;
//    }
//
//
//    public Results withCursor( String cursor ) {
//        this.cursor = cursor;
//        return this;
//    }
//
//
//    public void setMetadata( Id id, String name, Object value ) {
//        if ( metadata == null ) {
//            metadata = new LinkedHashMap<Id, Map<String, Object>>();
//        }
//        Map<String, Object> entityMetadata = metadata.get( id );
//        if ( entityMetadata == null ) {
//            entityMetadata = new LinkedHashMap<String, Object>();
//            metadata.put( id, entityMetadata );
//        }
//        entityMetadata.put( name, value );
//        metadataMerged = false;
//        // updateIndex(id, name, value);
//    }
//
//
//    public Results withMetadata( Id id, String name, Object value ) {
//        setMetadata( id, name, value );
//        return this;
//    }
//
//
//    public void setMetadata( Id id, Map<String, Object> data ) {
//        if ( metadata == null ) {
//            metadata = new LinkedHashMap<Id, Map<String, Object>>();
//        }
//        Map<String, Object> entityMetadata = metadata.get( id );
//        if ( entityMetadata == null ) {
//            entityMetadata = new LinkedHashMap<String, Object>();
//            metadata.put( id, entityMetadata );
//        }
//        entityMetadata.putAll( data );
//        metadataMerged = false;
//        /*
//         * for (Entry<String, Object> m : data.entrySet()) { updateIndex(id,
//         * m.getKey(), m.getValue()); }
//         */
//    }
//
//
//    public Results withMetadata( Id id, Map<String, Object> data ) {
//        setMetadata( id, data );
//        return this;
//    }
//
//
//    public void setMetadata( Map<Id, Map<String, Object>> metadata ) {
//        this.metadata = metadata;
//    }
//
//
//    public Results withMetadata( Map<Id, Map<String, Object>> metadata ) {
//        this.metadata = metadata;
//        return this;
//    }
//
//
//    public void mergeEntitiesWithMetadata() {
//        if ( metadataMerged ) {
//            return;
//        }
//        if ( metadata == null ) {
//            return;
//        }
//        metadataMerged = true;
//        getEntities();
//        if ( entities != null ) {
//            for ( Entity entity : entities ) {
//                entity.clearMetadata();
//                Map<String, Object> entityMetadata = metadata.get( entity.getUuid() );
//                if ( entityMetadata != null ) {
//                    entity.mergeMetadata( entityMetadata );
//                }
//            }
//        }
//    }
//
//
//    protected QueryProcessor getQueryProcessor() {
//        return queryProcessor;
//    }
//
//
//    public void setQueryProcessor( QueryProcessor queryProcessor ) {
//        this.queryProcessor = queryProcessor;
//    }
//
//
//    public void setSearchVisitor( SearchVisitor searchVisitor ) {
//        this.searchVisitor = searchVisitor;
//    }
//
//
//    /** uses cursor to get next batch of Results (returns null if no cursor) */
//    public Results getNextPageResults() throws Exception {
//        if ( !hasCursor() ) {
//            return null;
//        }
//
//        Query q = new Query( query );
//        q.setCursor( getCursor() );
//        queryProcessor.setQuery( q );
//
//        return queryProcessor.getResults( searchVisitor );
//    }
}
