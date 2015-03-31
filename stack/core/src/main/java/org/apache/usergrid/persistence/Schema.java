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
package org.apache.usergrid.persistence;


import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.reflect.FieldUtils;

import org.apache.usergrid.persistence.annotations.EntityCollection;
import org.apache.usergrid.persistence.annotations.EntityDictionary;
import org.apache.usergrid.persistence.annotations.EntityProperty;
import org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.exceptions.PropertyTypeConversionException;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.persistence.schema.DictionaryInfo;
import org.apache.usergrid.persistence.schema.EntityInfo;
import org.apache.usergrid.persistence.schema.PropertyInfo;
import org.apache.usergrid.utils.InflectionUtils;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.MapUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.ConversionUtils.string;
import static org.apache.usergrid.utils.ConversionUtils.uuid;
import static org.apache.usergrid.utils.InflectionUtils.pluralize;
import static org.apache.usergrid.utils.InflectionUtils.singularize;
import static org.apache.usergrid.utils.JsonUtils.toJsonNode;
import static org.apache.usergrid.utils.MapUtils.hashMap;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterLast;


/**
 * The controller class for determining Entity relationships as well as properties types. This class loads the entity
 * schema definition from a YAML file called usergrid-schema.yaml at the root of the classpath.
 *
 * @author edanuff
 */
public class Schema {

    private static final Logger logger = LoggerFactory.getLogger( Schema.class );

    public static final String DEFAULT_ENTITIES_PACKAGE = "org.apache.usergrid.persistence.entities";

    public static final String TYPE_APPLICATION = "application";
    public static final String TYPE_ENTITY = "entity";
    public static final String TYPE_ROLE = "role";
    public static final String TYPE_CONNECTION = "connection";
    public static final String TYPE_MEMBER = "member";

    public static final String PROPERTY_ACTIVATED = "activated";
    public static final String PROPERTY_APPLICATION_ID = "applicationId";
    public static final String PROPERTY_COLLECTION_NAME = "collectionName";
    public static final String PROPERTY_CREATED = "created";
    public static final String PROPERTY_CONFIRMED = "confirmed";
    public static final String PROPERTY_DISABLED = "disabled";
    public static final String PROPERTY_ADMIN = "admin";
    public static final String PROPERTY_UUID = "uuid";
    public static final String PROPERTY_EMAIL = "email";
    public static final String PROPERTY_ITEM = "item";
    public static final String PROPERTY_ITEM_TYPE = "itemType";
    public static final String PROPERTY_MEMBERSHIP = "membership";
    public static final String PROPERTY_METADATA = "metadata";
    public static final String PROPERTY_MODIFIED = "modified";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_OWNER = "owner";
    public static final String PROPERTY_OWNER_TYPE = "ownerType";
    public static final String PROPERTY_PATH = "path";
    public static final String PROPERTY_PICTURE = "picture";
    public static final String PROPERTY_PUBLISHED = "published";
    public static final String PROPERTY_SECRET = "secret";
    public static final String PROPERTY_TIMESTAMP = "timestamp";
    public static final String PROPERTY_TITLE = "title";
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_URI = "uri";
    public static final String PROPERTY_USERNAME = "username";
    public static final String PROPERTY_INACTIVITY = "inactivity";

    public static final String PROPERTY_CONNECTION = "connection";
    public static final String PROPERTY_ASSOCIATED = "associated";
    public static final String PROPERTY_CURSOR = "cursor";

    public static final String COLLECTION_ROLES = "roles";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_GROUPS = "groups";

    public static final String INDEX_COLLECTIONS = "collections";
    public static final String INDEX_CONNECTIONS = "connections";

    public static final String DICTIONARY_PROPERTIES = "properties";
    public static final String DICTIONARY_SETS = "sets";
    public static final String DICTIONARY_COLLECTIONS = "collections";
    public static final String DICTIONARY_CONNECTIONS = "connections";
    public static final String DICTIONARY_INDEXES = "indexes";
    public static final String DICTIONARY_CONNECTING_TYPES = "connecting_types";
    public static final String DICTIONARY_CONNECTING_ENTITIES = "connecting_entities";
    public static final String DICTIONARY_CONNECTED_TYPES = "connected_types";
    public static final String DICTIONARY_CONNECTED_ENTITIES = "connected_entities";
    public static final String DICTIONARY_CONTAINER_ENTITIES = "container_entities";
    public static final String DICTIONARY_CREDENTIALS = "credentials";
    public static final String DICTIONARY_ROLENAMES = "rolenames";
    public static final String DICTIONARY_ROLETIMES = "roletimes";
    public static final String DICTIONARY_PERMISSIONS = "permissions";
    public static final String DICTIONARY_ID_SETS = "id_sets";
    public static final String DICTIONARY_COUNTERS = "counters";
    public static final String DICTIONARY_GEOCELL = "geocell";

    private static final List<String> entitiesPackage = new ArrayList<String>();
    private static final List<String> entitiesScanPath = new ArrayList<String>();

    @SuppressWarnings("rawtypes")
    public static Map<String, Class> DEFAULT_DICTIONARIES =
            hashMap( DICTIONARY_PROPERTIES, ( Class ) String.class ).map( DICTIONARY_SETS, String.class )
                    .map( DICTIONARY_INDEXES, String.class ).map( DICTIONARY_COLLECTIONS, String.class )
                    .map( DICTIONARY_CONNECTIONS, String.class ).map( DICTIONARY_CONNECTING_TYPES, String.class )
                    .map( DICTIONARY_CONNECTING_ENTITIES, String.class ).map( DICTIONARY_CONNECTED_TYPES, String.class )
                    .map( DICTIONARY_CONNECTED_ENTITIES, String.class )
                    .map( DICTIONARY_CONTAINER_ENTITIES, String.class )
                    .map( DICTIONARY_CREDENTIALS, CredentialsInfo.class ).map( DICTIONARY_ROLENAMES, String.class )
                    .map( DICTIONARY_ROLETIMES, Long.class ).map( DICTIONARY_PERMISSIONS, String.class )
                    .map( DICTIONARY_ID_SETS, String.class );

    private static LoadingCache<String, String> baseEntityTypes =
            CacheBuilder.newBuilder().expireAfterAccess( 10, TimeUnit.MINUTES )
                        .build( new CacheLoader<String, String>() {
                            public String load( String key ) { // no checked exception
                                return createNormalizedEntityType( key, true );
                            }
                        } );

    private static LoadingCache<String, String> nonbaseEntityTypes =
            CacheBuilder.newBuilder().expireAfterAccess( 10, TimeUnit.MINUTES )
                        .build( new CacheLoader<String, String>() {
                            public String load( String key ) { // no checked exception
                                return createNormalizedEntityType( key, false );
                            }
                        } );

    private static LoadingCache<String, String> collectionNameCache =
            CacheBuilder.newBuilder().maximumSize( 1000 ).build( new CacheLoader<String, String>() {
                public String load( String key ) { // no checked exception
                    return _defaultCollectionName( key );
                }
            } );

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unused")
    private final SmileFactory smile = new SmileFactory();

    private final Map<String, Class<? extends Entity>> typeToEntityClass =
            new ConcurrentHashMap<String, Class<? extends Entity>>();

    private final Map<Class<? extends Entity>, String> entityClassToType =
            new ConcurrentHashMap<Class<? extends Entity>, String>();

    private final Map<Class<? extends Entity>, Map<String, PropertyDescriptor>> entityClassPropertyToDescriptor =
            new ConcurrentHashMap<Class<? extends Entity>, Map<String, PropertyDescriptor>>();

    private final Map<Class<? extends Entity>, EntityInfo> registeredEntityClasses =
            new ConcurrentHashMap<Class<? extends Entity>, EntityInfo>();

    Map<String, EntityInfo> entityMap = new TreeMap<String, EntityInfo>( String.CASE_INSENSITIVE_ORDER );

    Map<String, Map<String, Set<CollectionInfo>>> entityContainerCollections =
            new TreeMap<String, Map<String, Set<CollectionInfo>>>( String.CASE_INSENSITIVE_ORDER );

    Map<String, Map<String, Set<CollectionInfo>>> entityContainerCollectionsIndexingProperties =
            new TreeMap<String, Map<String, Set<CollectionInfo>>>( String.CASE_INSENSITIVE_ORDER );
    Map<String, Map<String, Set<CollectionInfo>>> entityContainerCollectionsIndexingDictionaries =
            new TreeMap<String, Map<String, Set<CollectionInfo>>>( String.CASE_INSENSITIVE_ORDER );
    Map<String, Map<String, Set<CollectionInfo>>> entityContainerCollectionsIndexingDynamicDictionaries =
            new TreeMap<String, Map<String, Set<CollectionInfo>>>( String.CASE_INSENSITIVE_ORDER );

    Map<String, Map<String, Map<String, Set<CollectionInfo>>>> entityPropertyContainerCollectionsIndexingProperty =
            new TreeMap<String, Map<String, Map<String, Set<CollectionInfo>>>>( String.CASE_INSENSITIVE_ORDER );

    Map<String, Map<String, Map<String, Set<CollectionInfo>>>> entityDictionaryContainerCollectionsIndexingDictionary =
            new TreeMap<String, Map<String, Map<String, Set<CollectionInfo>>>>( String.CASE_INSENSITIVE_ORDER );

    Map<String, PropertyInfo> allIndexedProperties = new TreeMap<String, PropertyInfo>( String.CASE_INSENSITIVE_ORDER );

    Map<String, PropertyInfo> allProperties = new TreeMap<String, PropertyInfo>( String.CASE_INSENSITIVE_ORDER );

    private static Schema instance;

    boolean initialized = false;


    public Schema() {
        setDefaultSchema( this );

        mapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false );
    }


    public static final Object initLock = new Object();


    public static void setDefaultSchema( Schema instance ) {
        synchronized ( initLock ) {
            if ( Schema.instance == null ) {
                Schema.instance = instance;
            }
        }
    }


    public static Schema getDefaultSchema() {
        if ( instance == null ) {
            synchronized ( initLock ) {
                if ( instance == null ) {
                    logger.info( "Initializing schema..." );
                    instance = new Schema();
                    instance.init();
                    logger.info( "Schema initialized" );
                }
            }
        }
        return instance;
    }


    public void mapCollector( String entityType, String containerType, String collectionName,
                              CollectionInfo collection ) {

        MapUtils.addMapMapSet( entityContainerCollections, true, entityType, containerType, collection );

        if ( !collection.getPropertiesIndexed().isEmpty() ) {
            MapUtils.addMapMapSet( entityContainerCollectionsIndexingProperties, true, entityType, containerType,
                    collection );
            for ( String propertyName : collection.getPropertiesIndexed() ) {
                MapUtils.addMapMapMapSet( entityPropertyContainerCollectionsIndexingProperty, true, entityType,
                        propertyName, containerType, collection );
            }
        }

        if ( !collection.getDictionariesIndexed().isEmpty() ) {
            MapUtils.addMapMapSet( entityContainerCollectionsIndexingDictionaries, true, entityType, containerType,
                    collection );
            for ( String dictionaryName : collection.getDictionariesIndexed() ) {
                MapUtils.addMapMapMapSet( entityDictionaryContainerCollectionsIndexingDictionary, true, entityType,
                        dictionaryName, containerType, collection );
            }
        }

        if ( collection.isIndexingDynamicDictionaries() ) {
            MapUtils.addMapMapSet( entityContainerCollectionsIndexingDynamicDictionaries, true, entityType,
                    containerType, collection );
        }
    }


    private <T extends Annotation> T getAnnotation( Class<? extends Entity> entityClass, PropertyDescriptor descriptor,
                                                    Class<T> annotationClass ) {
        try {
            if ( ( descriptor.getReadMethod() != null ) && descriptor.getReadMethod()
                                                                     .isAnnotationPresent( annotationClass ) ) {
                return descriptor.getReadMethod().getAnnotation( annotationClass );
            }
            if ( ( descriptor.getWriteMethod() != null ) && descriptor.getWriteMethod()
                                                                      .isAnnotationPresent( annotationClass ) ) {
                return descriptor.getWriteMethod().getAnnotation( annotationClass );
            }
            Field field = FieldUtils.getField( entityClass, descriptor.getName(), true );
            if ( field != null ) {
                if ( field.isAnnotationPresent( annotationClass ) ) {
                    return field.getAnnotation( annotationClass );
                }
            }
        }
        catch ( Exception e ) {
            logger.error( "Could not retrieve the annotations", e );
        }
        return null;
    }


    public synchronized void registerEntity( Class<? extends Entity> entityClass ) {
        logger.info( "Registering {}", entityClass );
        EntityInfo e = registeredEntityClasses.get( entityClass );
        if ( e != null ) {
            return;
        }

        Map<String, PropertyDescriptor> propertyDescriptors = entityClassPropertyToDescriptor.get( entityClass );
        if ( propertyDescriptors == null ) {
            EntityInfo entity = new EntityInfo();

            String type = getEntityType( entityClass );

            propertyDescriptors = new LinkedHashMap<String, PropertyDescriptor>();
            Map<String, PropertyInfo> properties = new TreeMap<String, PropertyInfo>( String.CASE_INSENSITIVE_ORDER );
            Map<String, CollectionInfo> collections =
                    new TreeMap<String, CollectionInfo>( String.CASE_INSENSITIVE_ORDER );
            Map<String, DictionaryInfo> sets = new TreeMap<String, DictionaryInfo>( String.CASE_INSENSITIVE_ORDER );

            PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors( entityClass );

            for ( PropertyDescriptor descriptor : descriptors ) {
                String name = descriptor.getName();

                EntityProperty propertyAnnotation = getAnnotation( entityClass, descriptor, EntityProperty.class );
                if ( propertyAnnotation != null ) {
                    if ( isNotBlank( propertyAnnotation.name() ) ) {
                        name = propertyAnnotation.name();
                    }
                    propertyDescriptors.put( name, descriptor );

                    PropertyInfo propertyInfo = new PropertyInfo( propertyAnnotation );
                    propertyInfo.setName( name );
                    propertyInfo.setType( descriptor.getPropertyType() );

                    properties.put( name, propertyInfo );
                    // logger.info(propertyInfo);
                }

                EntityCollection collectionAnnotation =
                        getAnnotation( entityClass, descriptor, EntityCollection.class );
                if ( collectionAnnotation != null ) {
                    CollectionInfo collectionInfo = new CollectionInfo( collectionAnnotation );
                    collectionInfo.setName( name );
                    collectionInfo.setContainer( entity );

                    collections.put( name, collectionInfo );
                    // logger.info(collectionInfo);
                }

                EntityDictionary setAnnotation = getAnnotation( entityClass, descriptor, EntityDictionary.class );
                if ( setAnnotation != null ) {
                    DictionaryInfo setInfo = new DictionaryInfo( setAnnotation );
                    setInfo.setName( name );
                    // setInfo.setType(descriptor.getPropertyType());
                    sets.put( name, setInfo );
                    // logger.info(setInfo);
                }
            }

            if ( !DynamicEntity.class.isAssignableFrom( entityClass ) ) {
                entity.setProperties( properties );
                entity.setCollections( collections );
                entity.setDictionaries( sets );
                entity.mapCollectors( this, type );

                entityMap.put( type, entity );

                allProperties.putAll( entity.getProperties() );

                Set<String> propertyNames = entity.getIndexedProperties();
                for ( String propertyName : propertyNames ) {
                    PropertyInfo property = entity.getProperty( propertyName );
                    if ( ( property != null ) && !allIndexedProperties.containsKey( propertyName ) ) {
                        allIndexedProperties.put( propertyName, property );
                    }
                }
            }

            entityClassPropertyToDescriptor.put( entityClass, propertyDescriptors );

            registeredEntityClasses.put( entityClass, entity );
        }
    }


    public synchronized void init() {
        if ( !initialized ) {
            initialized = true;
            addEntitiesPackage( DEFAULT_ENTITIES_PACKAGE );
            scanEntities();
        }
    }


    @SuppressWarnings("unchecked")
    public void scanEntities() {
        synchronized ( entitiesScanPath ) {
            for ( String path : entitiesScanPath ) {
                ClassPathScanningCandidateComponentProvider provider =
                        new ClassPathScanningCandidateComponentProvider( true );
                provider.addIncludeFilter( new AssignableTypeFilter( TypedEntity.class ) );

                Set<BeanDefinition> components = provider.findCandidateComponents( path );
                for ( BeanDefinition component : components ) {
                    try {
                        Class<?> cls = Class.forName( component.getBeanClassName() );
                        if ( Entity.class.isAssignableFrom( cls ) ) {
                            registerEntity( ( Class<? extends Entity> ) cls );
                        }
                    }
                    catch ( ClassNotFoundException e ) {
                        logger.error( "Unable to get entity class ", e );
                    }
                }
                registerEntity( DynamicEntity.class );
            }
        }
    }


    public void addEntitiesPackage( String entityPackage ) {
        if ( !entitiesPackage.contains( entityPackage ) ) {
            entitiesPackage.add( entityPackage );
            String path = entityPackage.replaceAll( "\\.", "/" );
            synchronized ( entitiesScanPath ) {
                entitiesScanPath.add( path );
            }
        }
    }


    public void removeEntitiesPackage( String entityPackage ) {
        entitiesPackage.remove( entityPackage );
        String path = entityPackage.replaceAll( "\\.", "/" );
        synchronized ( entitiesScanPath ) {
            entitiesScanPath.remove( path );
        }
    }


    @SuppressWarnings("unchecked")
    public List<String> getEntitiesPackage() {
        return ( List<String> ) ( ( ArrayList<String> ) entitiesPackage ).clone();
    }


    /** @return value */
    public Map<String, PropertyInfo> getAllIndexedProperties() {

        return allIndexedProperties;
    }


    public Set<String> getAllIndexedPropertyNames() {

        return allIndexedProperties.keySet();
    }


    public Set<String> getAllPropertyNames() {

        return allProperties.keySet();
    }


    public String[] getAllPropertyNamesAsArray() {

        Set<String> strings = allProperties.keySet();
        return strings.toArray(new String[strings.size()]);
    }


    /** @return value */
    public EntityInfo getEntityInfo( String entityType ) {

        if ( entityType == null ) {
            return null;
        }

        entityType = normalizeEntityType( entityType );

        if ( "dynamicentity".equalsIgnoreCase( entityType ) ) {
            throw new IllegalArgumentException( entityType + " is not a valid entity type" );
        }

        EntityInfo entity = entityMap.get( entityType );
        if ( entity == null ) {
            return getDynamicEntityInfo( entityType );
        }
        return entity;
    }


    public JsonNode getEntityJsonSchema( String entityType ) {
        Class<?> cls = getEntityClass( entityType );
        if ( cls == null ) {
            cls = DynamicEntity.class;
        }
        try {
            JsonNode schemaNode = mapper.generateJsonSchema( cls ).getSchemaNode();
            if ( schemaNode != null ) {
                JsonNode properties = schemaNode.get( "properties" );
                if ( properties instanceof ObjectNode ) {
                    Set<String> fieldsToRemove = new LinkedHashSet<String>();
                    Iterator<String> i = properties.fieldNames();
                    while ( i.hasNext() ) {
                        String propertyName = i.next();
                        if ( !hasProperty( entityType, propertyName ) ) {
                            fieldsToRemove.add( propertyName );
                        }
                        else {
                            ObjectNode property = ( ObjectNode ) properties.get( propertyName );
                            if ( isRequiredProperty( entityType, propertyName ) ) {
                                property.put( "optional", false );
                            }
                        }
                    }
                    ( ( ObjectNode ) properties ).remove( fieldsToRemove );
                }
            }
            return schemaNode;
        }
        catch ( Exception e ) {
            logger.error( "Unable to get schema for entity type " + entityType, e );
        }
        return null;
    }


    public String getEntityType( Class<? extends Entity> cls ) {
        String type = entityClassToType.get( cls );
        if ( type != null ) {
            return type;
        }

        String className = cls.getName();
        boolean finded = false;
        for ( String entityPackage : entitiesPackage ) {
            String entityPackagePrefix = entityPackage + ".";
            if ( className.startsWith( entityPackagePrefix ) ) {
                type = className.substring( entityPackagePrefix.length() );
                type = InflectionUtils.underscore( type );
                finded = true;
            }
        }

        if ( !finded ) {
            type = className;
        }

        typeToEntityClass.put( type, cls );
        entityClassToType.put( cls, type );
        return type;
    }


    @SuppressWarnings("unchecked")
    private Class<? extends Entity> entityClassForName( String className ) {
        try {
            @SuppressWarnings("rawtypes") Class cls = Class.forName( className );
            if ( Entity.class.isAssignableFrom( cls ) ) {
                return cls;
            }
        }
        catch ( ClassNotFoundException e ) {
        }
        return null;
    }


    public Class<? extends Entity> getEntityClass( String type ) {
        type = getAssociatedEntityType( type );
        Class<? extends Entity> cls = typeToEntityClass.get( type );
        if ( cls != null ) {
            return cls;
        }

        for ( String entityPackage : entitiesPackage ) {
            String entityPackagePrefix = entityPackage + ".";
            cls = entityClassForName( entityPackagePrefix + InflectionUtils.camelCase( type, true ) );
            if ( cls == null ) {
                cls = entityClassForName( entityPackagePrefix + type );
            }

            if ( cls == null ) {
                cls = entityClassForName( type );
            }

            if ( cls != null ) {
                break;
            }
        }

        if ( cls == null ) {
            cls = DynamicEntity.class;
        }

        typeToEntityClass.put( type, cls );
        entityClassToType.put( cls, type );
        return cls;
    }


    /** @return value */
    public boolean hasProperties( String entityType ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.hasProperties();

    }


    /** @return value */
    public Set<String> getPropertyNames( String entityType ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return null;
        }

        return entity.getProperties().keySet();
    }


    /** @return value */
    public String[] getPropertyNamesAsArray( String entityType ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return new String[0];
        }

        Set<String> strings = entity.getProperties().keySet();
        return strings.toArray(new String[strings.size()]);
    }


    /** @return value */
    public boolean hasProperty( String entityType, String propertyName ) {

        if ( propertyName.equals( PROPERTY_UUID ) || propertyName.equals( PROPERTY_TYPE ) ) {
            return true;
        }

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.hasProperty(propertyName);

    }


    public String aliasProperty( String entityType ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return null;
        }

        return entity.getAliasProperty();
    }


    /** @return value */
    public boolean isPropertyMutable( String entityType, String propertyName ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.isPropertyMutable(propertyName);

    }


    public boolean isPropertyUnique( String entityType, String propertyName ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.isPropertyUnique(propertyName);

    }


    public boolean isPropertyIndexed( String entityType, String propertyName ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity == null || !entity.hasProperty(propertyName) || entity.isPropertyIndexed(propertyName);

    }


    public boolean isPropertyFulltextIndexed( String entityType, String propertyName ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity == null || !entity.hasProperty(propertyName) || entity.isPropertyFulltextIndexed(propertyName);

    }


    public boolean isPropertyTimestamp( String entityType, String propertyName ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.isPropertyTimestamp(propertyName);

    }


    /** @return value */
    public Set<String> getRequiredProperties( String entityType ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return null;
        }

        return entity.getRequiredProperties();
    }


    /** @return value */
    public boolean isRequiredProperty( String entityType, String propertyName ) {

        if ( propertyName.equals( PROPERTY_UUID ) || propertyName.equals( PROPERTY_TYPE ) ) {
            return true;
        }

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.isPropertyRequired(propertyName);

    }


    /** @return value */
    public Class<?> getPropertyType( String entityType, String propertyName ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return null;
        }

        PropertyInfo property = entity.getProperty( propertyName );
        if ( property == null ) {
            return null;
        }

        return property.getType();
    }


    /** @return value */
    public boolean isPropertyIndexedInCollection( String containerType, String collectionName, String propertyName ) {

        CollectionInfo collection = getCollection( containerType, collectionName );
        return collection != null && collection.isPropertyIndexed(propertyName);

    }


    /** @return value */
    public boolean hasDictionaries( String entityType ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.hasDictionaries();

    }


    /** @return value */
    public Set<String> getDictionaryNames( String entityType ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return null;
        }

        return entity.getDictionaries().keySet();
    }


    /** @return value */
    public boolean hasDictionary( String entityType, String dictionaryName ) {

        EntityInfo entity = getEntityInfo( entityType );
        return entity != null && entity.hasDictionary(dictionaryName);

    }


    /** @return value */
    public Class<?> getDictionaryKeyType( String entityType, String dictionaryName ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return null;
        }

        DictionaryInfo set = entity.getDictionary( dictionaryName );
        if ( set == null ) {
            return null;
        }

        return set.getKeyType();
    }


    public Class<?> getDictionaryValueType( String entityType, String dictionaryName ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return null;
        }

        DictionaryInfo dictionary = entity.getDictionary( dictionaryName );
        if ( dictionary == null ) {
            return null;
        }

        return dictionary.getValueType();
    }


    /** @return value */
    public boolean isDictionaryIndexedInConnections( String entityType, String dictionaryName ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return false;
        }

        DictionaryInfo dictionary = entity.getDictionary( dictionaryName );
        return dictionary != null && dictionary.isKeysIndexedInConnections();

    }


    /** @return value */
    public boolean isDictionaryIndexedInCollection( String containerType, String collectionName,
                                                    String dictionaryName ) {

        CollectionInfo collection = getCollection( containerType, collectionName );
        return collection != null && collection.isDictionaryIndexed(dictionaryName);

    }


    /** @return value */
    public boolean hasCollection( String containerType, String collectionName ) {

        return getCollection( containerType, collectionName ) != null;
    }


    public boolean isCollectionPathBased( String containerType, String collectionName ) {

        CollectionInfo collection = getCollection( containerType, collectionName );
        if ( collection == null ) {
            return false;
        }

        EntityInfo item = getEntityInfo( collection.getType() );
        if ( item == null ) {
            return false;
        }

        PropertyInfo property = item.getAliasPropertyObject();
        return property != null && property.isPathBasedName();

    }


    public boolean isCollectionReversed( String containerType, String collectionName ) {

        CollectionInfo collection = getCollection( containerType, collectionName );
        return collection != null && collection.isReversed();

    }


    public String getCollectionSort( String containerType, String collectionName ) {

        CollectionInfo collection = getCollection( containerType, collectionName );
        if ( collection == null ) {
            return null;
        }

        return collection.getSort();
    }


    /** @return value */
    public CollectionInfo getCollection( String containerType, String collectionName ) {

        containerType = normalizeEntityType( containerType, true );

        EntityInfo entity = getEntityInfo( containerType );
        if ( entity == null ) {
            return null;
        }

        CollectionInfo collection = entity.getCollection( collectionName );

        if ( ( collection == null ) && ( Application.ENTITY_TYPE.equalsIgnoreCase( containerType ) ) ) {
            collection = getDynamicApplicationCollection( collectionName );
        }

        return collection;
    }


    private CollectionInfo getDynamicApplicationCollection( String collectionName ) {
        EntityInfo entity = getEntityInfo( Application.ENTITY_TYPE );
        if ( entity == null ) {
            return null;
        }

        CollectionInfo collection = entity.getCollection( collectionName );

        if ( collection != null ) {
            return collection;
        }

        collection = new CollectionInfo();
        collection.setName( collectionName );
        collection.setContainer( entity );
        collection.setType( normalizeEntityType( collectionName ) );
        Set<String> properties = new LinkedHashSet<String>();
        properties.add( PROPERTY_NAME );
        properties.add( PROPERTY_CREATED );
        properties.add( PROPERTY_MODIFIED );
        collection.setPropertiesIndexed( properties );
        // entity.getCollections().put(collectionName, collection);
        // mapCollector(collection.getType(), Application.ENTITY_TYPE,
        // collectionName, collection);

        return collection;
    }


    public String getCollectionType( String containerType, String collectionName ) {

        containerType = normalizeEntityType( containerType );

        CollectionInfo collection = getCollection( containerType, collectionName );

        if ( collection == null ) {

            if ( Application.ENTITY_TYPE.equalsIgnoreCase( containerType ) ) {
                return normalizeEntityType( collectionName );
            }
            return null;
        }

        return collection.getType();
    }


    /** @return value */
    public Map<String, CollectionInfo> getCollections( String entityType ) {

        EntityInfo entity = getEntityInfo( normalizeEntityType( entityType, true ) );
        if ( entity == null ) {
            return null;
        }

        return entity.getCollections();
    }


    public Set<String> getCollectionNames( String entityType ) {

        EntityInfo entity = getEntityInfo( normalizeEntityType( entityType, true ) );
        if ( entity == null ) {
            return null;
        }

        Map<String, CollectionInfo> map = entity.getCollections();

        if ( map != null ) {
            return map.keySet();
        }

        return null;
    }


    public java.util.List<String> getCollectionNamesAsList( String entityType ) {
        Set<String> set = getCollectionNames( normalizeEntityType( entityType, true ) );
        if ( set != null ) {
            return new ArrayList<String>( set );
        }
        return null;
    }


    private Map<String, Set<CollectionInfo>> addDynamicApplicationCollectionAsContainer(
            Map<String, Set<CollectionInfo>> containers, String entityType ) {

        Map<String, Set<CollectionInfo>> copy =
                new TreeMap<String, Set<CollectionInfo>>( String.CASE_INSENSITIVE_ORDER );
        if ( containers != null ) {
            copy.putAll( containers );
        }
        containers = copy;

        if ( !containers.containsKey( Application.ENTITY_TYPE ) ) {
            MapUtils.addMapSet( containers, true, Application.ENTITY_TYPE,
                    getCollection( Application.ENTITY_TYPE, defaultCollectionName( entityType ) ) );
        }

        return containers;
    }


    /** @return value */
    public Map<String, Set<CollectionInfo>> getContainers( String entityType ) {

        entityType = normalizeEntityType( entityType );

        // Add the application as a container to all entities
        return addDynamicApplicationCollectionAsContainer( entityContainerCollections.get( entityType ), entityType );
    }


    /** @return value */
    public CollectionInfo getContainerCollectionLinkedToCollection( String containerType, String collectionName ) {

        CollectionInfo collection = getCollection( containerType, collectionName );
        if ( collection == null ) {
            return null;
        }

        String linkedCollection = collection.getLinkedCollection();
        if ( linkedCollection == null ) {
            return null;
        }

        return getCollection( collection.getType(), linkedCollection );
    }


    /** @return value */
    public Map<String, Set<CollectionInfo>> getContainersIndexingProperties( String entityType ) {

        entityType = normalizeEntityType( entityType );

        // Add the application as a container indexing some properties by
        // default
        return addDynamicApplicationCollectionAsContainer(
                entityContainerCollectionsIndexingProperties.get( entityType ), entityType );
    }


    /** @return value */
    public Map<String, Set<CollectionInfo>> getContainersIndexingDictionaries( String entityType ) {

        entityType = normalizeEntityType( entityType );

        // Application does index any sets by default
        return entityContainerCollectionsIndexingDictionaries.get( entityType );
    }


    /** @return value */
    public Map<String, Set<CollectionInfo>> getContainersIndexingDynamicSetInfos( String entityType ) {

        entityType = normalizeEntityType( entityType );

        // Application does index dynamic sets by default
        return entityContainerCollectionsIndexingDynamicDictionaries.get( entityType );
    }


    /** @return value */
    public Map<String, Set<CollectionInfo>> getContainersIndexingProperty( String entityType, String propertyName ) {

        entityType = normalizeEntityType( entityType );

        Map<String, Map<String, Set<CollectionInfo>>> propertyContainerCollectionsIndexingPropertyInfo =
                entityPropertyContainerCollectionsIndexingProperty.get( entityType );

        // Application indexes name property by default
        if ( propertyName.equalsIgnoreCase( PROPERTY_NAME ) || propertyName.equalsIgnoreCase( PROPERTY_CREATED )
                || propertyName.equalsIgnoreCase( PROPERTY_MODIFIED ) ) {
            return addDynamicApplicationCollectionAsContainer(
                    propertyContainerCollectionsIndexingPropertyInfo != null ?
                    propertyContainerCollectionsIndexingPropertyInfo.get( propertyName ) : null, entityType );
        }

        if ( propertyContainerCollectionsIndexingPropertyInfo == null ) {
            return null;
        }

        return propertyContainerCollectionsIndexingPropertyInfo.get( propertyName );
    }


    /** @return value */
    public Map<String, Set<CollectionInfo>> getContainersIndexingDictionary( String entityType,
                                                                             String dictionaryName ) {

        entityType = normalizeEntityType( entityType );

        /*
         * if (entityType == null) { return null; }
         */

        Map<String, Map<String, Set<CollectionInfo>>> dictionaryContainerCollectionsIndexingDictionary =
                entityDictionaryContainerCollectionsIndexingDictionary.get( entityType );

        if ( dictionaryContainerCollectionsIndexingDictionary == null ) {
            return null;
        }

        // Application does index any set by default
        return dictionaryContainerCollectionsIndexingDictionary.get( dictionaryName );
    }


    public static String defaultCollectionName( String entityType ) {
        try {
            return collectionNameCache.get( entityType );
        }
        catch ( ExecutionException ex ) {
            ex.printStackTrace();
        }
        return _defaultCollectionName( entityType );
    }


    private static String _defaultCollectionName( String entityType ) {
        entityType = normalizeEntityType( entityType );
        return pluralize( entityType );
    }


    public static String normalizeEntityType( String entityType ) {
        return normalizeEntityType( entityType, false );
    }


    public static String getAssociatedEntityType( String entityType ) {
        if ( entityType == null ) {
            return null;
        }
        entityType = stringOrSubstringAfterLast( entityType, ':' );
        return normalizeEntityType( entityType, false );
    }


    public static String normalizeEntityType( String entityType, boolean baseType ) {
        if ( entityType == null ) {
            return null;
        }
        return baseType ? baseEntityTypes.getUnchecked( entityType ) : nonbaseEntityTypes.getUnchecked( entityType );
    }


    /** uncached - use normalizeEntityType() */
    private static String createNormalizedEntityType( String entityType, boolean baseType ) {
        if ( baseType ) {
            int i = entityType.indexOf( ':' );
            if ( i >= 0 ) {
                entityType = entityType.substring( 0, i );
            }
        }
        entityType = entityType.toLowerCase();
        if ( entityType.startsWith( "org.apache.usergrid.persistence" ) ) {
            entityType = stringOrSubstringAfterLast( entityType, '.' );
        }
        entityType = singularize( entityType );

        if ( "dynamicentity".equalsIgnoreCase( entityType ) ) {
            throw new IllegalArgumentException( entityType + " is not a valid entity type" );
        }

        // entityType = capitalizeDelimiter(entityType, '.', '_');
        return entityType;
    }


    public static boolean isAssociatedEntityType( String entityType ) {
        return entityType != null && entityType.contains(":");
    }


    /** @return value */
    public EntityInfo getDynamicEntityInfo( String entityType ) {

        entityType = normalizeEntityType( entityType );

        EntityInfo entity = new EntityInfo();
        entity.setType( entityType );

        Map<String, PropertyInfo> properties = new LinkedHashMap<String, PropertyInfo>();
        PropertyInfo property = new PropertyInfo();
        property.setName( PROPERTY_UUID );
        property.setRequired( true );
        property.setType( UUID.class );
        property.setMutable( false );
        property.setBasic( true );
        properties.put( PROPERTY_UUID, property );

        property = new PropertyInfo();
        property.setName( PROPERTY_TYPE );
        property.setRequired( true );
        property.setType( String.class );
        property.setMutable( false );
        property.setBasic( true );
        properties.put( PROPERTY_TYPE, property );

        property = new PropertyInfo();
        property.setName( PROPERTY_NAME );
        property.setRequired( false );
        property.setType( String.class );
        property.setMutable( false );
        property.setAliasProperty( true );
        property.setIndexed( true );
        property.setBasic( true );
        property.setUnique( true );
        properties.put( PROPERTY_NAME, property );

        property = new PropertyInfo();
        property.setName( PROPERTY_CREATED );
        property.setRequired( true );
        property.setType( Long.class );
        property.setMutable( false );
        property.setIndexed( true );
        properties.put( PROPERTY_CREATED, property );

        property = new PropertyInfo();
        property.setName( PROPERTY_MODIFIED );
        property.setRequired( true );
        property.setType( Long.class );
        property.setIndexed( true );
        properties.put( PROPERTY_MODIFIED, property );

        property = new PropertyInfo();
        property.setName( PROPERTY_ITEM );
        property.setRequired( false );
        property.setType( UUID.class );
        property.setMutable( false );
        property.setAliasProperty( false );
        property.setIndexed( false );
        properties.put( PROPERTY_ITEM, property );

        property = new PropertyInfo();
        property.setName( PROPERTY_ITEM_TYPE );
        property.setRequired( false );
        property.setType( String.class );
        property.setMutable( false );
        property.setAliasProperty( false );
        property.setIndexed( false );
        properties.put( PROPERTY_ITEM_TYPE, property );

        property = new PropertyInfo();
        property.setName( PROPERTY_COLLECTION_NAME );
        property.setRequired( false );
        property.setType( String.class );
        property.setMutable( false );
        property.setAliasProperty( false );
        property.setIndexed( false );
        properties.put( PROPERTY_COLLECTION_NAME, property );

        entity.setProperties( properties );

        Map<String, DictionaryInfo> sets = new LinkedHashMap<String, DictionaryInfo>();

        DictionaryInfo set = new DictionaryInfo();
        set.setName( DICTIONARY_CONNECTIONS );
        set.setKeyType( String.class );
        sets.put( DICTIONARY_CONNECTIONS, set );

        entity.setDictionaries( sets );

        return entity;
    }


    public Map<String, Object> cleanUpdatedProperties( String entityType, Map<String, Object> properties ) {
        return cleanUpdatedProperties( entityType, properties, false );
    }


    public Map<String, Object> cleanUpdatedProperties( String entityType, Map<String, Object> properties,
                                                       boolean create ) {

        if ( properties == null ) {
            return null;
        }

        entityType = normalizeEntityType( entityType );

        properties.remove( PROPERTY_UUID );
        properties.remove( PROPERTY_TYPE );
        properties.remove( PROPERTY_METADATA );
        properties.remove( PROPERTY_MEMBERSHIP );
        properties.remove( PROPERTY_CONNECTION );

        Iterator<Entry<String, Object>> iterator = properties.entrySet().iterator();
        while ( iterator.hasNext() ) {
            Entry<String, Object> entry = iterator.next();
            if ( hasProperty( entityType, entry.getKey() ) ) {
                if ( !create && !isPropertyMutable( entityType, entry.getKey() ) ) {
                    iterator.remove();
                    continue;
                }
                Object propertyValue = entry.getValue();
                if ( ( propertyValue instanceof String ) && (propertyValue.equals("")) ) {
                    propertyValue = null;
                }
                if ( ( propertyValue == null ) && isRequiredProperty( entityType, entry.getKey() ) ) {
                    iterator.remove();
                }
            }
        }
        return properties;
    }


    public Object validateEntityPropertyValue( String entityType, String propertyName, Object propertyValue )
            throws PropertyTypeConversionException {

        entityType = normalizeEntityType( entityType );

        if ( ( propertyValue instanceof String ) && propertyValue.equals("") ) {
            propertyValue = null;
        }

        if ( !hasProperty( entityType, propertyName ) ) {
            return propertyValue;
        }

        /*
         * if (PROPERTY_TYPE.equals(propertyName)) { return
         * string(propertyValue); } else if (PROPERTY_ID.equals(propertyName)) {
         * return uuid(propertyValue); }
         */

        Class<?> type = getPropertyType( entityType, propertyName );
        if ( type != null ) {
            // propertyValue = coerce(type, propertyValue);
            try {
                propertyValue = mapper.convertValue( propertyValue, type );
            }
            catch ( Exception e ) {
                throw new PropertyTypeConversionException( entityType, propertyName, propertyValue, type, e );
            }
        }

        return propertyValue;
    }


    public Object validateEntitySetValue( String entityType, String dictionaryName, Object elementValue ) {

        entityType = normalizeEntityType( entityType );

        if ( ( elementValue instanceof String ) && elementValue.equals("") ) {
            elementValue = null;
        }

        if ( !hasDictionary( entityType, dictionaryName ) ) {
            return elementValue;
        }

        Class<?> type = getDictionaryKeyType( entityType, dictionaryName );
        if ( type != null ) {
            // elementValue = coerce(type, elementValue);
            elementValue = mapper.convertValue( elementValue, type );
        }

        return elementValue;
    }


    public Entity toEntity( Map<String, Object> map ) {
        Class<? extends Entity> entityClass = DynamicEntity.class;
        String type = ( String ) map.get( PROPERTY_TYPE );
        if ( type != null ) {
            entityClass = getEntityClass( type );
        }
        if ( entityClass == null ) {
            entityClass = DynamicEntity.class;
        }
        return mapper.convertValue( map, entityClass );
    }

    /*
     * public Entity toEntity(Reader reader) { Entity entity =
     * mapper.convertValue(reader, Entity.class); return entity; }
     *
     * public Entity toEntity(InputStream input) { Entity entity =
     * mapper.convertValue(input, Entity.class); return entity; }
     *
     * public Entity toEntity(String string) { Entity entity =
     * mapper.convertValue(string, Entity.class); return entity; }
     */


    public Map<String, Object> toMap( Entity entity ) {
        return mapper.convertValue( entity, new TypeReference<Map<String, Object>>() {} );
    }


    public Object convertToPropertyType( Class<? extends Entity> entityClass, String property, Object value ) {
        Class<?> cls = getPropertyType( getEntityType( entityClass ), property );
        if ( cls != null ) {
            return mapper.convertValue( value, cls );
        }
        return value;
    }


    public Object convertToPropertyType( String type, String property, Object value ) {
        Class<?> cls = getPropertyType( type, property );
        if ( cls != null ) {
            return mapper.convertValue( value, cls );
        }
        return value;
    }


    public PropertyDescriptor getDescriptorForEntityProperty( Class<? extends Entity> entityClass, String property ) {
        Map<String, PropertyDescriptor> propertyDescriptors = entityClassPropertyToDescriptor.get( entityClass );
        if ( propertyDescriptors == null ) {
            return null;
        }
        return propertyDescriptors.get( property );
    }


    public void setEntityProperty( Entity entity, String property, Object value ) {
        PropertyDescriptor descriptor = getDescriptorForEntityProperty( entity.getClass(), property );
        if ( descriptor != null ) {
            Class<?> cls = descriptor.getPropertyType();
            if ( cls != null ) {
                if ( ( value == null ) || ( cls.isAssignableFrom( value.getClass() ) ) ) {
                    try {
                        descriptor.getWriteMethod().invoke( entity, value );
                        return;
                    }
                    catch ( Exception e ) {
                        logger.error( "Unable to set entity property " + property, e );
                    }
                }
                try {
                    descriptor.getWriteMethod().invoke( entity, mapper.convertValue( value, cls ) );
                    return;
                }
                catch ( Exception e ) {
                    logger.error( "Unable to set entity property " + property, e );
                }
            }
        }
        entity.setDynamicProperty( property, value );
    }


    public Object getEntityProperty( Entity entity, String property ) {
        PropertyDescriptor descriptor = getDescriptorForEntityProperty( entity.getClass(), property );
        if ( descriptor != null ) {
            try {
                return descriptor.getReadMethod().invoke( entity );
            }
            catch ( Exception e ) {
                logger.error( "Unable to get entity property " + property, e );
            }
            return null;
        }
        Map<String, Object> properties = entity.getDynamicProperties();
        if ( properties != null ) {
            return properties.get( property );
        }
        return null;
    }


    public Map<String, Object> getEntityProperties( Entity entity ) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        Map<String, PropertyDescriptor> propertyDescriptors = entityClassPropertyToDescriptor.get( entity.getClass() );

        if ( propertyDescriptors == null ) {
            registerEntity( entity.getClass() );
            propertyDescriptors = entityClassPropertyToDescriptor.get( entity.getClass() );
        }

        for ( Entry<String, PropertyDescriptor> propertyEntry : propertyDescriptors.entrySet() ) {
            String property = propertyEntry.getKey();
            PropertyDescriptor descriptor = propertyEntry.getValue();
            if ( descriptor != null ) {
                try {
                    Object value = descriptor.getReadMethod().invoke( entity );
                    if ( value != null ) {
                        properties.put( property, value );
                    }
                }
                catch ( Exception e ) {
                    logger.error( "Unable to get entity property " + property, e );
                }
            }
        }
        Map<String, Object> dynamicProperties = entity.getDynamicProperties();
        if ( dynamicProperties != null ) {
            properties.putAll( dynamicProperties );
        }
        return properties;
    }


    public static Map<String, Object> deserializeEntityProperties( Row<UUID, String, ByteBuffer> row ) {
        if ( row == null ) {
            return null;
        }
        ColumnSlice<String, ByteBuffer> slice = row.getColumnSlice();
        if ( slice == null ) {
            return null;
        }
        return deserializeEntityProperties( slice.getColumns(), true, false );
    }


    /** @return entity properties from columns as a map */
    public static Map<String, Object> deserializeEntityProperties( List<HColumn<String, ByteBuffer>> columns ) {
        return deserializeEntityProperties( CassandraPersistenceUtils.asMap( columns ), true, false );
    }


    public static Map<String, Object> deserializeEntityProperties( Map<String, ByteBuffer> columns ) {
        return deserializeEntityProperties( columns, true, false );
    }


    public static Map<String, Object> deserializeEntityProperties( List<HColumn<String, ByteBuffer>> columns,
                                                                   boolean checkId, boolean checkRequired ) {
        return deserializeEntityProperties( CassandraPersistenceUtils.asMap( columns ), checkId, checkRequired );
    }


    /** @return entity properties from columns as a map */
    public static Map<String, Object> deserializeEntityProperties( Map<String, ByteBuffer> columns, boolean checkId,
                                                                   boolean checkRequired ) {

        if ( columns == null ) {
            return null;
        }

        String entityType = string( columns.get( PROPERTY_TYPE ) );
        if ( entityType == null ) {
            logger.debug( "deserializeEntityProperties(): No type for entity found, entity probably doesn't exist" );
            return null;
        }
        if ( checkId && !columns.containsKey( PROPERTY_UUID ) ) {
            logger.error( "No id for entity ( {} ) found!", entityType );
            return null;
        }

        if ( checkRequired ) {
            Set<String> required_properties = Schema.getDefaultSchema().getRequiredProperties( entityType );
            if ( required_properties != null ) {
                for ( String property_name : required_properties ) {
                    if ( !columns.containsKey( property_name ) ) {
                        logger.error( "Entity (" + entityType + ") missing required property: " + property_name,
                                new Throwable() );
                        return null;
                    }
                }
            }
        }

        Map<String, Object> properties_map = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );
        for ( Entry<String, ByteBuffer> column : columns.entrySet() ) {
            String propertyName = column.getKey();
            Object propertyValue = deserializeEntityProperty( entityType, propertyName, column.getValue() );
            properties_map.put( propertyName, propertyValue );
        }
        return properties_map;
    }


    /** @return object of correct type deserialize from column bytes */
    public static Object deserializeEntityProperty( String entityType, String propertyName, ByteBuffer bytes ) {
        Object propertyValue = null;
        if ( PROPERTY_UUID.equals( propertyName ) ) {
            propertyValue = uuid( bytes );
        }
        else if ( PROPERTY_TYPE.equals( propertyName ) ) {
            propertyValue = string( bytes );
        }
        else {
            if ( Schema.getDefaultSchema().isPropertyEncrypted( entityType, propertyName ) ) {
                bytes = decrypt( bytes );
            }
            propertyValue = Schema.deserializePropertyValueFromJsonBinary( bytes );
        }
        return propertyValue;
    }


    public static ByteBuffer serializeEntityProperty( String entityType, String propertyName, Object propertyValue ) {
        ByteBuffer bytes = null;
        if ( PROPERTY_UUID.equals( propertyName ) ) {
            bytes = bytebuffer( uuid( propertyValue ) );
        }
        else if ( PROPERTY_TYPE.equals( propertyName ) ) {
            bytes = bytebuffer( string( propertyValue ) );
        }
        else {
            bytes = Schema.serializePropertyValueToJsonBinary( toJsonNode( propertyValue ) );
            if ( Schema.getDefaultSchema().isPropertyEncrypted( entityType, propertyName ) ) {
                bytes.rewind();
                bytes = encrypt( bytes );
            }
        }
        return bytes;
    }


    public static ByteBuffer serializePropertyValueToJsonBinary( Object obj ) {
        return JsonUtils.toByteBuffer( obj );
    }


    public static Object deserializePropertyValueFromJsonBinary( ByteBuffer bytes ) {
        return JsonUtils.normalizeJsonTree( JsonUtils.fromByteBuffer( bytes ) );
    }


    public static Object deserializePropertyValueFromJsonBinary( ByteBuffer bytes, Class<?> classType ) {
        return JsonUtils.normalizeJsonTree( JsonUtils.fromByteBuffer( bytes, classType ) );
    }


    public boolean isPropertyEncrypted( String entityType, String propertyName ) {

        EntityInfo entity = getEntityInfo( entityType );
        if ( entity == null ) {
            return false;
        }

        PropertyInfo property = entity.getProperty( propertyName );
        return property != null && property.isEncrypted();

    }


    private static final byte[] DEFAULT_ENCRYPTION_SEED =
            "oWyWX?I2kZAhkKb_jQ8SZvjmgkiF4eGSjsfIkhnRetD4Dvtx2J".getBytes();
    private static byte[] encryptionSeed =
            ( System.getProperty( "encryptionSeed" ) != null ) ? System.getProperty( "encryptionSeed" ).getBytes() :
            DEFAULT_ENCRYPTION_SEED;


    public static ByteBuffer encrypt( ByteBuffer clear ) {
        if ( clear == null || !clear.hasRemaining() ) {
            return clear;
        }
        try {
            SecretKeySpec sKeySpec = new SecretKeySpec( getRawKey( encryptionSeed ), "AES" );
            Cipher cipher = Cipher.getInstance( "AES" );
            cipher.init( Cipher.ENCRYPT_MODE, sKeySpec );
            ByteBuffer encrypted = ByteBuffer.allocate( cipher.getOutputSize( clear.remaining() ) );
            cipher.doFinal( clear, encrypted );
            encrypted.rewind();
            return encrypted;
        }
        catch ( Exception e ) {
            throw new IllegalStateException( e );
        }
    }


    public static ByteBuffer decrypt( ByteBuffer encrypted ) {
        if ( encrypted == null || !encrypted.hasRemaining() ) {
            return encrypted;
        }
        try {
            SecretKeySpec sKeySpec = new SecretKeySpec( getRawKey( encryptionSeed ), "AES" );
            Cipher cipher = Cipher.getInstance( "AES" );
            cipher.init( Cipher.DECRYPT_MODE, sKeySpec );
            ByteBuffer decrypted = ByteBuffer.allocate( cipher.getOutputSize( encrypted.remaining() ) );
            cipher.doFinal( encrypted, decrypted );
            decrypted.rewind();
            return decrypted;
        }
        catch ( Exception e ) {
            throw new IllegalStateException( e );
        }
    }


    private static byte[] getRawKey( byte[] seed ) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance( "AES" );
        SecureRandom sr = SecureRandom.getInstance( "SHA1PRNG" );
        sr.setSeed( seed );
        keyGenerator.init( 128, sr ); // 192 and 256 bits may not be available
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey.getEncoded();
    }
}
