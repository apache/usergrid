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

package org.apache.usergrid.rest.test.resource2point0.model;

import java.io.Serializable;
import java.util.*;

import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static org.apache.usergrid.persistence.Schema.PROPERTY_NAME;


/**
 * Contains a model that can be deconstructed from the api response. This is a base level value that contains the bare
 * minumum of what other classes use. Such as . users or groups.
 */
public class Entity implements Serializable, Map<String,Object> {


    protected Map<String, Object> dynamic_properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );

    ApiResponse response;

    public Entity(){}

    public Entity (Map<String,Object> payload){
        this.putAll(payload);
    }

    public Entity(ApiResponse response){
        this.response = response;

        if(response.getEntities() != null &&  response.getEntities().size()>=1){
            List<Entity>  entities =  response.getEntities();
            Map<String,Object> entity = entities.get(0);
            this.putAll(entity);
        }
        else if (response.getData() != null){

            if(response.getData() instanceof  LinkedHashMap) {
                LinkedHashMap dataResponse = ( LinkedHashMap ) response.getData();

                if(dataResponse.get( "user" )!=null){
                    this.putAll( ( Map<? extends String, ?> ) dataResponse.get( "user" ) );
                }
                else{
                    this.putAll( dataResponse );
                }
            }
            else if (response.getData() instanceof ArrayList){
                ArrayList<String> data = ( ArrayList<String> ) response.getData();
                Entity entity = new Entity();
                entity.put( "data", data.get( 0 ) );
                this.putAll( entity );
            }
        }
        //TODO: added bit for import tests and other tests that only put a single thing into properties
        else if (response.getProperties() != null){
            this.putAll( response.getProperties() );
        }
    }

    //For the owner , should have different cases that looks at the different types it could be
    protected Entity setResponse(final ApiResponse response, String key) {
        LinkedHashMap linkedHashMap = (LinkedHashMap) response.getData();

        if(linkedHashMap == null){
            linkedHashMap =  new LinkedHashMap( response.getProperties());
        }

        this.putAll((Map<? extends String, ?>) linkedHashMap.get(key));

        return this;
    }

    public void setProperties( Map<String, Object> properties ) {
        putAll( properties );
    }

    public Map<String, Object> getDynamicProperties() {
        return dynamic_properties;
    }

    @Override
    public int size() {
        return getDynamicProperties().size();
    }


    @Override
    public boolean isEmpty() {
        return getDynamicProperties().isEmpty();
    }


    @Override
    public boolean containsKey( final Object key ) {
        return getDynamicProperties().containsKey( key );
    }


    @Override
    public boolean containsValue( final Object value ) {
        return getDynamicProperties().containsValue( value );
    }


    @Override
    public Object get( final Object key ) {
        //All values are strings , so doing the cast here saves doing the cast elsewhere
        return getDynamicProperties().get( key );
    }

    public Map<String, Map<String, Object>> getMap(Object key){
        return (LinkedHashMap<String, Map<String, Object>>) getDynamicProperties().get( key );
    }

    public String getAsString( final Object key ) {
        //All values are strings , so doing the cast here saves doing the cast elsewhere
        return (String) getDynamicProperties().get( key );
    }

    public String getError () {
        return (String) this.get("error");
    }

    public String getErrorCode () {
        return (String)this.get("errorCode");
    }

    public String getErrorDescription () {
        return (String) this.get("errorDescription");
    }

    @Override
    public Object put( final String key, final Object value ) {
        return getDynamicProperties().put( key,value );
    }


    @Override
    public Object remove( final Object key ) {
        return getDynamicProperties().remove( key );
    }


    @Override
    public void putAll( final Map<? extends String, ?> m ) {
        getDynamicProperties().putAll( m );
    }


    @Override
    public void clear() {
        getDynamicProperties().clear();
    }


    @Override
    public Set<String> keySet() {
        return getDynamicProperties().keySet();
    }


    @Override
    public java.util.Collection<Object> values() {
        return getDynamicProperties().values();
    }


    @Override
    public Set<Entry<String, Object>> entrySet() {
        return getDynamicProperties().entrySet();
    }

    public UUID getUuid(){
        return UUID.fromString( ( String ) get( "uuid" ) );
    }

    public Entity chainPut(final String key, final Object value){
        put(key,value);
        return this;
    }

    public Entity withProp(final String key, final Object value){
        put(key,value);
        return this;
    }

    public ApiResponse getResponse(){
        return response;
    }
}
