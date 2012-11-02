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
package org.usergrid.tools;

import static org.usergrid.utils.CompositeUtils.*;
import static java.util.Arrays.asList;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.usergrid.persistence.Schema.getDefaultSchema;
import static org.usergrid.persistence.cassandra.ApplicationCF.*;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX_ENTRIES;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.persistence.cassandra.CassandraService.INDEX_ENTRY_LIST_COUNT;
import static org.usergrid.persistence.cassandra.IndexUpdate.indexValueCode;
import static org.usergrid.persistence.cassandra.IndexUpdate.toIndexableValue;
import static org.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.usergrid.utils.UUIDUtils.newTimeUUID;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.AbstractComposite;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.AbstractComposite.Component;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.cassandra.ApplicationCF;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.EntityManagerImpl;
import org.usergrid.persistence.cassandra.IndexBucketScanner;
import org.usergrid.persistence.cassandra.IndexUpdate;
import org.usergrid.persistence.cassandra.IndexUpdate.IndexEntry;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.utils.UUIDUtils;

/**
 * This is a utility to audit all available entity ids in the secondary index.
 * It then checks to see if any index value is not present in the
 * Entity_Index_Entries. If it is not, the value from the index is removed, and
 * a forced re-index is triggered
 * 
 * USERGRID-323
 * 
 * @author tnine
 * 
 */
public class EntityIndexCleanup extends ToolBase {

  /**
     * 
     */
  private static final int PAGE_SIZE = 100;

  public static final ByteBufferSerializer be = new ByteBufferSerializer();

  private static final Logger logger = LoggerFactory.getLogger(EntityIndexCleanup.class);

  @Override
  @SuppressWarnings("static-access")
  public Options createOptions() {

    Option hostOption = OptionBuilder.withArgName("host").hasArg().isRequired(true).withDescription("Cassandra host")
        .create("host");

    Options options = new Options();
    options.addOption(hostOption);

    return options;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
   */
  @Override
  public void runTool(CommandLine line) throws Exception {
    startSpring();

    logger.info("Starting entity cleanup");

    List<UUID> ids = null;
    Query query = new Query();
    query.setLimit(PAGE_SIZE);
    String lastCursor = null;

    for (Entry<String, UUID> app : emf.getApplications().entrySet()) {

      logger.info("Starting cleanup for app {}", app.getKey());

      UUID applicationId = app.getValue();
      EntityManagerImpl em = (EntityManagerImpl) emf.getEntityManager(applicationId);

      CassandraService cass = em.getCass();
      IndexBucketLocator indexBucketLocator = em.getIndexBucketLocator();

      Keyspace ko = cass.getApplicationKeyspace(applicationId);

      UUID timestampUuid = newTimeUUID();
      long timestamp = getTimestampInMicros(timestampUuid);

      Set<String> collectionNames = em.getApplicationCollections();
  
      // go through each collection and audit the values
      for (String collectionName : collectionNames) {

       
        do {

          query.setCursor(lastCursor);
          // load all entity ids from the index itself.

          ids = cass.getIdList(cass.getApplicationKeyspace(applicationId),
              key(applicationId, DICTIONARY_COLLECTIONS, collectionName), query.getStartResult(), null,
              query.getLimit() + 1, false, indexBucketLocator, applicationId, collectionName);


          CollectionInfo collection = getDefaultSchema().getCollection("application", collectionName);
          

          //trim to set our cursor for next time around
          Results tempResults = Results.fromIdList(ids);
          tempResults.trim(query.getLimit());
          lastCursor = tempResults.getCursor();
          
          //We shouldn't have to do this, but otherwise the cursor won't work
          Set<String> indexed = collection.getPropertiesIndexed();

          // what's left needs deleted, do so

          logger.info("Auditing {} entities for collection {} in app {}", new Object[]{ids.size(), collectionName, app.getValue()});

          for (UUID id : ids) {
            boolean reIndex = false;

            Mutator<ByteBuffer> m = createMutator(ko, be);

            for (String prop : indexed) {

              Object key = key(applicationId, collection.getName(), prop);
              
              List<HColumn<ByteBuffer, ByteBuffer>> indexCols = scanIndexForAllTypes(ko, indexBucketLocator, applicationId, key, id, prop);

              // loop through the indexed values and verify them as present in
              // our entity_index_entries. If they aren't, we need to delete the
              // from the secondary index, and mark
              // this object for re-index via n update
              for (HColumn<ByteBuffer, ByteBuffer> index : indexCols) {

                DynamicComposite secondaryIndexValue = DynamicComposite.fromByteBuffer(index.getName().duplicate());

                Object code = secondaryIndexValue.get(0);
                Object propValue = secondaryIndexValue.get(1);
                UUID timestampId = (UUID) secondaryIndexValue.get(3);

                DynamicComposite existingEntryStart = new DynamicComposite(prop, code, propValue, timestampId);
                DynamicComposite existingEntryFinish = new DynamicComposite(prop, code, propValue, timestampId);

                setEqualityFlag(existingEntryFinish, ComponentEquality.GREATER_THAN_EQUAL);

                // now search our EntityIndexEntry for previous values, see if
                // they don't match this one

                List<HColumn<ByteBuffer, ByteBuffer>> entries = cass.getColumns(ko, ENTITY_INDEX_ENTRIES, id,
                    existingEntryStart, existingEntryFinish, INDEX_ENTRY_LIST_COUNT, false);

                // we wouldn't find this column in our entity_index_entries
                // audit. Delete it, then mark this entity for update
                if (entries.size() == 0) {
                  logger
                      .info(
                          "Could not find reference to value {} for property {} on entity {} in collection{}.  Forcing reindex",
                          new Object[] { propValue, prop, id, collectionName });
                
                  addDeleteToMutator(m, ENTITY_INDEX, key, index.getName().duplicate(), timestamp);
                  
                  reIndex = true;
                }
                
                if(entries.size() > 1){
                  reIndex = true;
                }

              }

            }

            //force this entity to be updated
            
            if(reIndex){
              Entity entity = em.get(id);
              
              em.update(entity);
              
              //now execute the cleanup. This way if the above update fails, we still have enough data to run again later
              m.execute();
            }
          


          }
          
         

          
        } while (ids.size() == PAGE_SIZE);
      }

    }

  }

  private List<HColumn<ByteBuffer, ByteBuffer>> scanIndexForAllTypes( Keyspace ko, IndexBucketLocator indexBucketLocator,
      UUID applicationId, Object key, UUID entityId, String prop) throws Exception {

    //TODO Determine the index bucket.  Scan the entire index for properties with this entityId.
    
    String bucket = indexBucketLocator.getBucket(applicationId, IndexType.COLLECTION, entityId, prop);
    
    Object rowKey = key(key, bucket);
   
    DynamicComposite start = null;
    
    List<HColumn<ByteBuffer, ByteBuffer>> cols;
    
    List<HColumn<ByteBuffer, ByteBuffer>> results = new ArrayList<HColumn<ByteBuffer,ByteBuffer>>();
    
   
    do{
      cols = cass.getColumns(ko, ENTITY_INDEX, rowKey, start, null, 100, false);
      
      for(HColumn<ByteBuffer, ByteBuffer> col: cols){
        DynamicComposite secondaryIndexValue = DynamicComposite.fromByteBuffer(col.getName().duplicate());

        UUID storedId = (UUID) secondaryIndexValue.get(2);
        
        //add it to the set.  We can't short circuit due to property ordering
        if(entityId.equals(storedId)){
          results.add(col);
        }
        
        start = secondaryIndexValue;
        
      }
    }while(cols.size() == 100);
   
    return results;

  }
}
