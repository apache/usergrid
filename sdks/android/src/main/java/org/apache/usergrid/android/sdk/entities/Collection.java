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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.android.sdk.UGClient;
import org.apache.usergrid.android.sdk.UGClient.Query;
import org.apache.usergrid.android.sdk.response.ApiResponse;

/**
 * Models a collection of entities as a local object. Collections
 * are the primary way that entities are organized in Usergrid.
 *
 * @see <a href="http://apigee.com/docs/app-services/content/collections">Collections documentation</a>
 */
public class Collection
{
	private UGClient client;
	private String type;
	private Map<String,Object> qs;

	private ArrayList<Entity> list;
	private int iterator;
	private ArrayList<String> previous;
	private String next;
	private String cursor;

	/**
	 * Default constructor for a Collection object.
	 *
	 * @param  UGClient  an instance of the UGClient class
	 * @param  type  the entity 'type' associated with the colleciton
	 * @param  qs  optional Map object of query parameters to apply to the collection retrieval
	 */	
	public Collection(UGClient client, String type, Map<String,Object> qs) {
	    this.client = client;
	    this.type = type;
	    
	    if( qs == null )
	    {
	    	this.qs = new HashMap<String,Object>();
	    }
	    else
	    {
	    	this.qs = qs;
	    }

	    this.list = new ArrayList<Entity>();
	    this.iterator = -1;

	    this.previous = new ArrayList<String>();
	    this.next = null;
	    this.cursor = null;

	    this.fetch();
	}

	/**
	 * Gets the entity 'type' associated with the collection.
	 *
	 * @return the collection type
	 */	
	public String getType(){
	   return this.type;
	}
	
	/**
	 * Sets the entity 'type' associated with the collection.
	 *
	 * @param  type  the collection type
	 */	
	public void setType(String type){
	   this.type = type;
	}

	/**
	 * Retrieves the current state of the collection from the server, and populates
	 * an the Collection object with the returned set of entities. Executes synchronously.
	 *
	 * @return  an ApiResponse object
	 */	
	public ApiResponse fetch() {
	    if (this.cursor != null) {
	    	this.qs.put("cursor", this.cursor);
	    }
	    
	    Query query = this.client.queryEntitiesRequest("GET", this.qs, null,
                this.client.getOrganizationId(),  this.client.getApplicationId(), this.type);
	    ApiResponse response = query.getResponse();
	    if (response.getError() != null) {
	    	this.client.writeLog("Error getting collection.");
	    } else {
	    	String theCursor = response.getCursor();
    		int count = response.getEntityCount();
    		
    		UUID nextUUID = response.getNext();
    		if( nextUUID != null ) {
    			this.next = nextUUID.toString();
    		} else {
    			this.next = null;
    		}
    		this.cursor = theCursor;

	    	this.saveCursor(theCursor);
	    	if ( count > 0 ) {
	    		this.resetEntityPointer();
	    		this.list = new ArrayList<Entity>();
	    		List<Entity> retrievedEntities = response.getEntities();
	    		
	    		for ( Entity retrievedEntity : retrievedEntities ) {
	    			if( retrievedEntity.getUuid() != null ) {
	    				retrievedEntity.setType(this.type);
	    				this.list.add(retrievedEntity);
	    			}
	    		}
	    	}
	    }
	    
	    return response;
	}

	/**
	 * Adds an entity to the Collection object.
	 *
	 * @param  entityData  a Map object of entity properties to be saved in the entity
	 * @return  an Entity object that represents the newly added entity. Must include
	 *		a 'type' property. Executes synchronously.
	 */	
	public Entity addEntity(Map<String,Object> entityData) {
		Entity entity = null;		
		ApiResponse response = this.client.createEntity(entityData);
		if( (response != null) && (response.getError() == null) && (response.getEntityCount() > 0) ) {
			entity = response.getFirstEntity();
			if (entity != null) {
				this.list.add(entity);
			}
		}
		return entity;
	}

	/**
	 * Deletes the provided entity on the server, then updates the
	 * Collection object by calling fetch(). Executes synchronously.
	 *
	 * @param entity an Entity object that contains a 'type' and 'uuid' property
	 */	
	public ApiResponse destroyEntity(Entity entity) {
		ApiResponse response = entity.destroy();
		if (response.getError() != null) {
			this.client.writeLog("Could not destroy entity.");
		} else {
			response = this.fetch();
		}
	    
		return response;
	}

	/**
	 * Retrieves an entity from the server.
	 *
	 * @param uuid the UUID of the entity to retrieve
	 * @return an ApiResponse object
	 */	
	public ApiResponse getEntityByUuid(UUID uuid) {
		Entity entity = new Entity(this.client);
	    entity.setType(this.type);
	    entity.setUuid(uuid);
	    return entity.fetch();
	}

	/**
	 * Gets the first entity in the Collection object.
	 *
	 * @return  an Entity object
	 */	
	public Entity getFirstEntity() {
		return ((this.list.size() > 0) ? this.list.get(0) : null);
	}

	/**
	 * Gets the last entity in the Collection object.
	 *
	 * @return  an Entity object
	 */	
	public Entity getLastEntity() {
		return ((this.list.size() > 0) ? this.list.get(this.list.size()-1) : null);
	}

	/**
	 * Checks if there is another entity in the Collection after the current pointer position.
	 *
	 * @return  Boolean true/false
	 */	
	public boolean hasNextEntity() {
		int next = this.iterator + 1;
		return ((next >= 0) && (next < this.list.size()));
	}

	/**
	 * Checks if there is an entity in the Collection before the current pointer position.
	 *
	 * @return  Boolean true/false
	 */	
	public boolean hasPrevEntity() {
		int prev = this.iterator - 1;
		return ((prev >= 0) && (prev < this.list.size()));
	}

	/**
	 * Checks if there is an entity in the Collection after the current
	 * pointer position, and returns it.
	 *
	 * @return an Entity object
	 */	
	public Entity getNextEntity() {
		if (this.hasNextEntity()) {
			this.iterator++;
			return this.list.get(this.iterator);
		}
		return null;
	}

	/**
	 * Checks if there is an entity in the Collection before the current
	 * pointer position, and returns it.
	 *
	 * @return an Entity object
	 */	
	public Entity getPrevEntity() {
		if (this.hasPrevEntity()) {
			this.iterator--;
			return this.list.get(this.iterator);
		}
		return null;
	}

	/**
	 * Resets the pointer to the start of the Collection.
	 */
	public void resetEntityPointer() {
		this.iterator = -1;
	}

	/**
	 * Saves a pagination cursor.	 
	 */
	public void saveCursor(String cursor) {
		this.next = cursor;
	}

	/**
	 * Clears the currently saved pagination cursor from the Collection.
	 */
	public void resetPaging() {
		this.previous.clear();
		this.next = null;
		this.cursor = null;
	}

	/**
	 * Checks if a pagination cursor for the next result set is 
	 * present in the Collection.
	 *
	 * @return Boolean true/false
	 */
	public boolean hasNextPage() {
		return this.next != null;
	}

	/**
	 * Checks if a pagination cursor for the previous result set is 
	 * present in the Collection
	 *
	 * @return  Boolean true/false
	 */
	public boolean hasPrevPage() {
		return !this.previous.isEmpty();
	}

	/**
	 * Checks if a pagination cursor for the next result set is 
	 * present in the Collection, then fetches it.
	 *
	 * @return an ApiResponse object if a cursor is present, otherwise null
	 */
	public ApiResponse getNextPage() {
		if (this.hasNextPage()) {
			this.previous.add(this.cursor);
			this.cursor = this.next;
			this.list.clear();
			return this.fetch();
		}
		  
		return null;
	}

	/**
	 * Checks if a pagination cursor for the previous result set is 
	 * present in the Collection, then fetches it.
	 *
	 * @return  an ApiResponse object if a cursor is present, otherwise null
	 */
	public ApiResponse getPrevPage() {
		if (this.hasPrevPage()) {
			this.next = null;
			int indexOfLastObject = this.previous.size() - 1;
			this.cursor = this.previous.get(indexOfLastObject);
			this.previous.remove(indexOfLastObject);
			this.list.clear();
			return this.fetch();
		}
		  
		return null;
	}

}
