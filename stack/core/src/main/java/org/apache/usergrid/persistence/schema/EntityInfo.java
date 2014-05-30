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
package org.apache.usergrid.persistence.schema;


import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.usergrid.persistence.Schema;


public class EntityInfo {

    private String type;

    private String aliasProperty;

    private Map<String, PropertyInfo> properties = new TreeMap<String, PropertyInfo>( String.CASE_INSENSITIVE_ORDER );
    private final Set<String> indexed = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
    private final Set<String> basic = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
    private final Set<String> required = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
    private Map<String, DictionaryInfo> dictionaries =
            new TreeMap<String, DictionaryInfo>( String.CASE_INSENSITIVE_ORDER );
    private Map<String, CollectionInfo> collections =
            new TreeMap<String, CollectionInfo>( String.CASE_INSENSITIVE_ORDER );
    private final Set<String> fulltextIndexed = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );

    private boolean publicVisible = true;

    private boolean includedInExport = true;
    ;

    public EntityInfo() {
    }


    public String getType() {
        return type;
    }


    public void setType( String type ) {
        this.type = type;
    }


    public boolean hasProperty( String propertyName ) {
        return properties.containsKey( propertyName );
    }


    public PropertyInfo getProperty( String propertyName ) {
        return properties.get( propertyName );
    }


    public boolean hasProperties() {
        return !properties.isEmpty();
    }


    public Map<String, PropertyInfo> getProperties() {
        return properties;
    }


    public void setProperties( Map<String, PropertyInfo> properties ) {
        this.properties = new TreeMap<String, PropertyInfo>( String.CASE_INSENSITIVE_ORDER );
        this.properties.putAll( properties );
        for ( String key : properties.keySet() ) {
            PropertyInfo property = properties.get( key );
            property.setName( key );

            if ( property.isIndexed() ) {
                indexed.add( key );
            }

            if ( property.isRequired() ) {
                required.add( key );
                // logger.info("property " + key + " is required");
            }

            if ( property.isBasic() ) {
                basic.add( key );
            }


            if ( property.isFulltextIndexed() ) {
                fulltextIndexed.add( key );
            }

            if ( property.isAliasProperty() ) {
                aliasProperty = property.getName();
            }
        }
    }


    public boolean isPropertyMutable( String propertyName ) {
        PropertyInfo property = properties.get( propertyName );
        if ( property == null ) {
            return false;
        }
        return property.isMutable();
    }


    /** A property is unique if it has either been declared as unique, or as an alias property */
    public boolean isPropertyUnique( String propertyName ) {
        PropertyInfo property = properties.get( propertyName );
        if ( property == null ) {
            return false;
        }
        return property.isUnique() || property.isAliasProperty();
    }


    public boolean isPropertyTimestamp( String propertyName ) {
        PropertyInfo property = properties.get( propertyName );
        if ( property == null ) {
            return false;
        }
        return property.isTimestamp();
    }


    public boolean isPropertyRequired( String propertyName ) {
        return required.contains( propertyName );
    }


    public Set<String> getIndexedProperties() {
        return indexed;
    }


    public boolean isPropertyIndexed( String propertyName ) {
        return indexed.contains( propertyName );
    }


    public boolean isPropertyFulltextIndexed( String propertyName ) {
        return fulltextIndexed.contains( propertyName );
    }


    public Set<String> getRequiredProperties() {
        return required;
    }


    public boolean isPropertyBasic( String propertyName ) {
        return basic.contains( propertyName );
    }


    public Set<String> getBasicProperties() {
        return basic;
    }


    public boolean hasDictionary( String dictionaryName ) {
        return dictionaries.containsKey( dictionaryName );
    }


    public DictionaryInfo getDictionary( String dictionaryName ) {
        return dictionaries.get( dictionaryName );
    }


    public boolean hasDictionaries() {
        return !dictionaries.isEmpty();
    }


    public Map<String, DictionaryInfo> getDictionaries() {
        return dictionaries;
    }


    public void setDictionaries( Map<String, DictionaryInfo> dictionaries ) {
        this.dictionaries = new TreeMap<String, DictionaryInfo>( String.CASE_INSENSITIVE_ORDER );
        this.dictionaries.putAll( dictionaries );
        for ( String key : dictionaries.keySet() ) {
            DictionaryInfo dictionary = dictionaries.get( key );
            dictionary.setName( key );
        }

        for ( @SuppressWarnings( "rawtypes" ) Entry<String, Class> dictionaryEntry : Schema.DEFAULT_DICTIONARIES
                                                                                           .entrySet() ) {
            String dictionaryName = dictionaryEntry.getKey();
            if ( !this.dictionaries.containsKey( dictionaryName ) ) {
                DictionaryInfo dictionary = new DictionaryInfo();
                dictionary.setName( dictionaryName );
                dictionary.setKeyType( String.class );
                dictionary.setValueType( dictionaryEntry.getValue() );
                this.dictionaries.put( dictionaryName, dictionary );
            }
        }
    }


    public boolean hasCollection( String collectionName ) {
        return collections.containsKey( collectionName );
    }


    public CollectionInfo getCollection( String collectionName ) {
        return collections.get( collectionName );
    }


    public Map<String, CollectionInfo> getCollections() {
        return collections;
    }


    public void setCollections( Map<String, CollectionInfo> collections ) {
        this.collections = new TreeMap<String, CollectionInfo>( String.CASE_INSENSITIVE_ORDER );
        this.collections.putAll( collections );
    }


    public void mapCollectors( Schema schema, String entityType ) {

        setType( entityType );

        for ( String collectionName : collections.keySet() ) {
            CollectionInfo collection = collections.get( collectionName );
            collection.setContainer( this );
            collection.setName( collectionName );
            schema.mapCollector( collection.getType(), entityType, collectionName, collection );
        }
    }


    public String getAliasProperty() {
        return aliasProperty;
    }


    public void setAliasProperty( String nameProperty ) {
        aliasProperty = nameProperty;
    }


    public PropertyInfo getAliasPropertyObject() {
        if ( aliasProperty == null ) {
            return null;
        }
        return getProperty( aliasProperty );
    }


    public boolean isPublic() {
        return publicVisible;
    }


    public void setPublic( boolean publicVisible ) {
        this.publicVisible = publicVisible;
    }


    public void setIncludedInExport( boolean includedInExport ) {
        this.includedInExport = includedInExport;
    }


    public boolean isIncludedInExport() {
        return includedInExport;
    }


    @Override
    public String toString() {
        return "Entity [type=" + type + ", aliasProperty=" + aliasProperty + ", properties=" + properties + ", indexed="
                + indexed + ", required=" + required + ", sets=" + dictionaries + ", collections=" + collections
                + ", fulltextIndexed=" + fulltextIndexed + ", publicVisible=" + publicVisible + "]";
    }
}
