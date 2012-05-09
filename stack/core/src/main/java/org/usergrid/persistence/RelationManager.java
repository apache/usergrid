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
package org.usergrid.persistence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.Results.Level;

public interface RelationManager {

	public Set<String> getCollectionIndexes(String collectionName)
			throws Exception;

	public Map<String, Map<UUID, Set<String>>> getOwners() throws Exception;

	public Set<String> getCollections() throws Exception;

	public Results getCollection(String collectionName, UUID startResult,
			int count, Results.Level resultsLevel, boolean reversed)
			throws Exception;

//	T.N. This isn't used anywhere.  Removing for this release
//	public Results getCollection(String collectionName,
//			Map<String, Object> subkeyProperties, UUID startResult, int count,
//			Results.Level resultsLevel, boolean reversed) throws Exception;

	public Results getCollection(String collectionName, Query query,
			Results.Level resultsLevel) throws Exception;

	public Entity addToCollection(String collectionName, EntityRef itemRef)
			throws Exception;

	public Entity addToCollections(List<EntityRef> owners, String collectionName)
			throws Exception;

	public Entity createItemInCollection(String collectionName,
			String itemType, Map<String, Object> properties) throws Exception;

	public void removeFromCollection(String collectionName, EntityRef itemRef)
			throws Exception;

	public Results searchCollection(String collectionName, Query query)
			throws Exception;

	public ConnectionRef createConnection(ConnectionRef connection)
			throws Exception;

	public ConnectionRef createConnection(String connectionType,
			EntityRef connectedEntityRef) throws Exception;

	public ConnectionRef createConnection(String pairedConnectionType,
			EntityRef pairedEntity, String connectionType,
			EntityRef connectedEntityRef) throws Exception;

	public ConnectionRef createConnection(ConnectedEntityRef... connections)
			throws Exception;

	public ConnectionRef connectionRef(String connectionType,
			EntityRef connectedEntityRef) throws Exception;

	public ConnectionRef connectionRef(String pairedConnectionType,
			EntityRef pairedEntity, String connectionType,
			EntityRef connectedEntityRef) throws Exception;

	public ConnectionRef connectionRef(ConnectedEntityRef... connections);

	public void deleteConnection(ConnectionRef connectionRef) throws Exception;

	public boolean connectionExists(ConnectionRef connectionRef)
			throws Exception;

	public Set<String> getConnectionTypes(UUID connectedEntityId)
			throws Exception;

	public Set<String> getConnectionTypes() throws Exception;

	public Set<String> getConnectionTypes(boolean filterConnection)
			throws Exception;

	public Results getConnectedEntities(String connectionType,
			String connectedEntityType, Results.Level resultsLevel)
			throws Exception;

	public Results getConnectingEntities(String connectionType,
			String connectedEntityType, Results.Level resultsLevel)
			throws Exception;

	public List<ConnectedEntityRef> getConnections(Query query)
			throws Exception;

//	public Results searchConnectedEntitiesForProperty(String connectionType,
//			String connectedEntityType, String propertyName,
//			Object searchStartValue, Object searchFinishValue,
//			UUID startResult, int count, boolean reversed, Level resultsLevel)
//			throws Exception;

	public Results searchConnectedEntities(Query query) throws Exception;

	public List<ConnectionRef> searchConnections(Query query) throws Exception;

	public Set<String> getConnectionIndexes(String connectionType)
			throws Exception;

	public Object getAssociatedProperty(
			AssociatedEntityRef associatedEntityRef, String propertyName)
			throws Exception;

	public Map<String, Object> getAssociatedProperties(
			AssociatedEntityRef associatedEntityRef) throws Exception;

	public void setAssociatedProperty(AssociatedEntityRef associatedEntityRef,
			String propertyName, Object propertyValue) throws Exception;

	public long getCollectionSize(String collectionName) throws Exception;

}
