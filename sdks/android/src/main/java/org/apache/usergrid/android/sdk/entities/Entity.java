/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.android.sdk.entities;

import static org.apache.usergrid.android.sdk.utils.JsonUtils.getUUIDProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setBooleanProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setFloatProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setLongProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setStringProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.setUUIDProperty;
import static org.apache.usergrid.android.sdk.utils.JsonUtils.toJsonString;
import static org.apache.usergrid.android.sdk.utils.MapUtils.newMapWithoutKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.usergrid.android.sdk.UGClient;
import org.apache.usergrid.android.sdk.UGClient.Query;
import org.apache.usergrid.android.sdk.response.ApiResponse;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Models an entity of any type as a local object. Type-specific 
 * classes are extended from this class.
 *
 * @see <a href="http://apigee.com/docs/app-services/content/app-services-data-model-1">Usergrid data model documentation</a>
 */
public class Entity {

    public final static String PROPERTY_UUID      = "uuid";
    public final static String PROPERTY_TYPE      = "type";
    public final static String PROPERTY_NAME      = "name";
    public final static String PROPERTY_METADATA  = "metadata";
    public final static String PROPERTY_CREATED   = "created";
    public final static String PROPERTY_MODIFIED  = "modified";
    public final static String PROPERTY_ACTIVATED = "activated";
    

    protected Map<String, JsonNode> properties = new HashMap<String, JsonNode>();
    private UGClient client;

    public static Map<String, Class<? extends Entity>> CLASS_FOR_ENTITY_TYPE = new HashMap<String, Class<? extends Entity>>();
    static {
        CLASS_FOR_ENTITY_TYPE.put(User.ENTITY_TYPE, User.class);
    }

    /**
     * Default constructor for instantiating an Entity object.
     */
    public Entity() {	
    }
    
    /**
     * Constructor for instantiating an Entity with a UGClient.
     * @param  UGClient  a UGClient object
     */
    public Entity(UGClient client) {
    	this.client = client;
    }

    /**
     * Constructor for instantiating an Entity with a UGClient
     * and entity type. Normally this is the constructor that should
     * be used to model an entity locally.
     * @param  UGClient  a UGClient object
     * @param  type  the 'type' property of the entity
     */
    public Entity(UGClient client, String type) {
    	this.client = client;
        setType(type);
    }
    
    /**
     * Gets the UGClient currently saved in the Entity object.
     * @return the UGClient instance
     */
    public UGClient getUGClient() {
    	return client;
    }

    /**
     * Sets the UGClient in the Entity object.
     * @param  UGClient  the UGClient instance
     */
    public void setUGClient(UGClient client) {
        this.client = client;
    }

    /**
     * Gets the 'type' of the Entity object.
     * @return the 'type' of the entity
     */
    @JsonIgnore
    public String getNativeType() {
        return getType();
    }

    /**
     * Adds the type and UUID properties to the Entity object, then 
     * returns all object properties.
     * @return a List object with the entity UUID and type
     */
    @JsonIgnore
    public List<String> getPropertyNames() {
        List<String> properties = new ArrayList<String>();
        properties.add(PROPERTY_TYPE);
        properties.add(PROPERTY_UUID);
        return properties;
    }

    /**
     * Gets the String value of the specified Entity property.
     * @param  name  the name of the property
     * @return the property value. Returns null if the property has no value
     */
    public String getStringProperty(String name) {
        JsonNode val = this.properties.get(name);
        return val != null ? val.textValue() : null;
    }
    
    /**
     * Gets the boolean value of the specified Entity property.
     * @param  name  the name of the property
     * @return the property value
     */
    public boolean getBoolProperty(String name) {
    	return this.properties.get(name).booleanValue();
    }
    
    /**
     * Gets the Int value of the specified Entity property.
     * @param  name  the name of the property
     * @return the property value
     */
    public int getIntProperty(String name) {
    	return this.properties.get(name).intValue();
    }
    
    /**
     * Gets the Double value of the specified Entity property.
     * @param  name  the name of the property
     * @return the property value
     */
    public double getDoubleProperty(String name) {
    	return this.properties.get(name).doubleValue();
    }
    
    /**
     * Gets the long value of the specified Entity property.
     * @param  name  the name of the property
     * @return the property value
     */
    public long getLongProperty(String name) {
    	return this.properties.get(name).longValue();
    }

    /**
     * Gets the 'type' property of the Entity object.     
     * @return the Entity type
     */
    public String getType() {
        return getStringProperty(PROPERTY_TYPE);
    }

    /**
     * Sets the 'type' property of the Entity object.          
     * @param  type  the entity type
     */
    public void setType(String type) {
        setStringProperty(properties, PROPERTY_TYPE, type);
    }

    /**
     * Gets the 'uuid' property of the Entity object.     
     * @return the Entity UUID
     */
    public UUID getUuid() {
        return getUUIDProperty(properties, PROPERTY_UUID);
    }

    /**
     * Sets the 'uuid' property of the Entity object.     
     * @param  uuid  the entity UUID
     */
    public void setUuid(UUID uuid) {
        setUUIDProperty(properties, PROPERTY_UUID, uuid);
    }

    /**
     * Returns a HashMap of the Entity properties without keys.
     *
     * @return a HashMap object with no keys and the value of the Entity properties
     */
    @JsonAnyGetter
    public Map<String, JsonNode> getProperties() {
        return newMapWithoutKeys(properties, getPropertyNames());
    }

    /**
     * Adds a property to the Entity object.
     *
     * @param  name  the name of the property to be set
     * @param  value the value of the property as a JsonNode object.
     *      If the value is null, the property will be removed from the object.
     * @see  <a href="http://jackson.codehaus.org/1.0.1/javadoc/org/codehaus/jackson/JsonNode.html">JsonNode</a> 
     */
    @JsonAnySetter
    public void setProperty(String name, JsonNode value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }
    
    /**
     * Removes all properties from the Entity object, then adds multiple properties.
     *
     * @param  newProperties  a Map object that contains the 
     *      property names as keys and their values as values.
     *      Property values must be JsonNode objects. If the value 
     *      is null, the property will be removed from the object.
     * @see  <a href="http://jackson.codehaus.org/1.0.1/javadoc/org/codehaus/jackson/JsonNode.html">JsonNode</a> 
     */
    public void setProperties(Map<String,JsonNode> newProperties) {
    	properties.clear();
    	Set<String> keySet = newProperties.keySet();
    	Iterator<String> keySetIter = keySet.iterator();
    	
    	while( keySetIter.hasNext() ) {
    		String key = keySetIter.next();
    		setProperty(key, newProperties.get(key));
    	}
    }
  
    /**
     * Adds a property to the Entity object with a String value.
     * 
     * @param  name  the name of the property to be set
     * @param  value  the String value of the property
     */
    public void setProperty(String name, String value) {
        setStringProperty(properties, name, value);
    }

    /**
     * Adds a property to the Entity object with a boolean value.
     * 
     * @param  name  the name of the property to be set
     * @param  value  the boolean value of the property
     */
    public void setProperty(String name, boolean value) {
        setBooleanProperty(properties, name, value);
    }

    /**
     * Adds a property to the Entity object with a long value.
     * 
     * @param  name  the name of the property to be set
     * @param  value  the long value of the property
     */
    public void setProperty(String name, long value) {
        setLongProperty(properties, name, value);
    }

    /**
     * Adds a property to the Entity object with a int value.
     * 
     * @param  name  the name of the property to be set
     * @param  value  the int value of the property
     */
    public void setProperty(String name, int value) {
        setProperty(name, (long) value);
    }

    /**
     * Adds a property to the Entity object with a float value.
     * 
     * @param  name  the name of the property to be set
     * @param  value  the float value of the property
     */
    public void setProperty(String name, float value) {
        setFloatProperty(properties, name, value);
    }

    /**
     * Returns the Entity object as a JSON-formatted string
     */
    @Override
    public String toString() {
        return toJsonString(this);
    }

    /**
     * @y.exclude
     */
    public <T extends Entity> T toType(Class<T> t) {
        return toType(this, t);
    }

    /**
     * @y.exclude
     */
    public static <T extends Entity> T toType(Entity entity, Class<T> t) {
        if (entity == null) {
            return null;
        }
        T newEntity = null;
        if (entity.getClass().isAssignableFrom(t)) {
            try {
                newEntity = (t.newInstance());
                if ((newEntity.getNativeType() != null)
                        && newEntity.getNativeType().equals(entity.getType())) {
                    newEntity.properties = entity.properties;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return newEntity;
    }

    /**
     * @y.exclude
     */
    public static <T extends Entity> List<T> toType(List<Entity> entities,
            Class<T> t) {
        List<T> l = new ArrayList<T>(entities != null ? entities.size() : 0);
        if (entities != null) {
            for (Entity entity : entities) {
                T newEntity = entity.toType(t);
                if (newEntity != null) {
                    l.add(newEntity);
                }
            }
        }
        return l;
    }
    
    /**
     * Fetches the current state of the entity from the server and saves
     * it in the Entity object. Runs synchronously.
     *      
     * @return an ApiResponse object
     */
    public ApiResponse fetch() {
    	ApiResponse response = new ApiResponse();
        String type = this.getType();
        UUID uuid = this.getUuid(); // may be NULL
        String entityId = null;
        if ( uuid != null ) {        	
        	entityId = uuid.toString();
        } else {
        	if (User.isSameType(type)) {
                String username = this.getStringProperty(User.PROPERTY_USERNAME);
                if ((username != null) && (username.length() > 0)) {            	    
            	    entityId = username;
                } else {
                    String error = "no_username_specified";
                    this.client.writeLog(error);
                    response.setError(error);
                    //response.setErrorCode(error);
                    return response;
                }
            } else {
                String name = this.getStringProperty(PROPERTY_NAME);
                if ((name != null) && (name.length() > 0)) {                    
                    entityId = name;
                } else {
                    String error = "no_name_specified";
                    this.client.writeLog(error);
                    response.setError(error);
                    //response.setErrorCode(error);
                    return response;
                }
            }
        }
        
        Query q = this.client.queryEntitiesRequest("GET", null, null,
                this.client.getOrganizationId(),  this.client.getApplicationId(), type, entityId);
        response = q.getResponse();
        if (response.getError() != null) {
            this.client.writeLog("Could not get entity.");
        } else {
            if ( response.getUser() != null ) {
        	    this.addProperties(response.getUser().getProperties());
            } else if ( response.getEntityCount() > 0 ) {
        	    Entity entity = response.getFirstEntity();
        	    this.setProperties(entity.getProperties());
            }
        }
        
        return response;
    }
    
    /**
     * Saves the Entity object as an entity on the server. Any
     * conflicting properties on the server will be overwritten. Runs synchronously.
     *      
     * @return  an ApiResponse object
     */
    public ApiResponse save() {
    	ApiResponse response = null;
        UUID uuid = this.getUuid();
        boolean entityAlreadyExists = false;
        
        if (client.isUuidValid(uuid)) {
            entityAlreadyExists = true;
        }
        
        // copy over all properties except some specific ones
        Map<String,Object> data = new HashMap<String,Object>();
        Set<String> keySet = this.properties.keySet();
        Iterator<String> keySetIter = keySet.iterator();
        
        while(keySetIter.hasNext()) {
        	String key = keySetIter.next();
        	if (!key.equals(PROPERTY_METADATA) &&
        		!key.equals(PROPERTY_CREATED) &&
        		!key.equals(PROPERTY_MODIFIED) &&
        		!key.equals(PROPERTY_ACTIVATED) &&
        		!key.equals(PROPERTY_UUID)) {
        		data.put(key, this.properties.get(key));
        	}
        }
        
        if (entityAlreadyExists) {
        	// update it
        	response = this.client.updateEntity(uuid.toString(), data);
        } else {
        	// create it
        	response = this.client.createEntity(data);
        }

        if ( response.getError() != null ) {
            this.client.writeLog("Could not save entity.");
        } else {
        	if (response.getEntityCount() > 0) {
        		Entity entity = response.getFirstEntity();
        		this.setProperties(entity.getProperties());
        	}
        }
        
        return response;    	
    }
    
    /**
     * Deletes the entity on the server.
     *     
     * @return  an ApiResponse object
     */
    public ApiResponse destroy() {
    	ApiResponse response = new ApiResponse();
        String type = getType();
        String uuidAsString = null;
        UUID uuid = getUuid();
        if ( uuid != null ) {
        	uuidAsString = uuid.toString();
        } else {
        	String error = "Error trying to delete object: No UUID specified.";
        	this.client.writeLog(error);
        	response.setError(error);
        	//response.setErrorCode(error);
        	return response;
        }
        
        response = this.client.removeEntity(type, uuidAsString);
        
        if( (response != null) && (response.getError() != null) ) {
        	this.client.writeLog("Entity could not be deleted.");
        } else {
        	this.properties.clear();
        }
        
        return response;
    }
    
    /**
     * Adds multiple properties to the Entity object. Pre-existing properties will
     * be preserved, unless there is a conflict, then the pre-existing property
     * will be overwritten.
     *
     * @param  properties  a Map object that contains the 
     *      property names as keys and their values as values.
     *      Property values must be JsonNode objects. If the value 
     *      is null, the property will be removed from the object.
     * @see  <a href="http://jackson.codehaus.org/1.0.1/javadoc/org/codehaus/jackson/JsonNode.html">JsonNode</a> 
     */
    public void addProperties(Map<String, JsonNode> properties) {
    	Set<String> keySet = properties.keySet();
    	Iterator<String> keySetIter = keySet.iterator();
    	
    	while( keySetIter.hasNext() ) {
    		String key = keySetIter.next();
    		setProperty(key, properties.get(key));
    	}
    }
    
    /**
     * Creates a connection between two entities.
     *
     * @param  connectType  the type of connection
     * @param  targetEntity  the UUID of the entity to connect to
     * @return an ApiResponse object
     */
    public ApiResponse connect(String connectType, Entity targetEntity) {
    	return this.client.connectEntities(this.getType(),
				this.getUuid().toString(),
				connectType,
				targetEntity.getUuid().toString());
    }
    
    /**
     * Destroys a connection between two entities.
     *
     * @param  connectType  the type of connection
     * @param  targetEntity  the UUID of the entity to disconnect from
     * @return  an ApiResponse object
     */
    public ApiResponse disconnect(String connectType, Entity targetEntity) {
    	return this.client.disconnectEntities(this.getType(),
    												this.getUuid().toString(),
    												connectType,
    												targetEntity.getUuid().toString());
    }

}
