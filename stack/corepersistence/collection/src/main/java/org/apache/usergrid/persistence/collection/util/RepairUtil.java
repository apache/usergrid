package org.apache.usergrid.persistence.collection.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGenerator;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGeneratorImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;


/**
 * This class holds different methods pertaining to the consolidation of an mvccentity. 
 *
 */
public class RepairUtil {

    private static final ChangeLogGenerator changeLogGenerator = new ChangeLogGeneratorImpl();


    /**
     * Name of the operation to be done on the entity. Repair starts the process for determining whether an
     * entity needs repair or not.
     * @param results
     * @param collectionScope
     * @param entitySerializationStrategy
     * @return
     */
    public static MvccEntity repair( Iterator<MvccEntity> results, CollectionScope collectionScope,
                                     MvccEntitySerializationStrategy entitySerializationStrategy ) {

        //nothing to do, we didn't get a result back
        if ( !results.hasNext() ) {
            return null;
        }

        final MvccEntity partialEntity = results.next();
        List<MvccEntity> partialEntities = new ArrayList<>();

        //this entity has been marked as cleared.(deleted)
        //The version exists, but does not have entity data
        if ( !partialEntity.getEntity().isPresent() && ( partialEntity.getStatus() == MvccEntity.Status.DELETED
                || partialEntity.getStatus() == MvccEntity.Status.COMPLETE ) ) {
            return null;
        }

        if ( partialEntity.getStatus() == MvccEntity.Status.COMPLETE ) {
            return partialEntity;
        }

        partialEntities.add( partialEntity );


        while ( results.hasNext() ) {
           final MvccEntity previousEntity = results.next();
            partialEntities.add( previousEntity );

            if ( previousEntity.getStatus() != MvccEntity.Status.PARTIAL ) {
                return repairAndWrite( partialEntities, partialEntity, entitySerializationStrategy, collectionScope );
            }
        }
        return null;
    }


    /**
     * Repairs the entity then overwrites the previous entity to become the new completed entity.
     * @param partialEntities
     * @param targetEntity The entity that should ultimately contain all merged data
     * @param entitySerializationStrategy
     * @param collectionScope
     * @return
     */
    private static MvccEntity repairAndWrite( List<MvccEntity> partialEntities, MvccEntity targetEntity,
                                              MvccEntitySerializationStrategy entitySerializationStrategy,
                                              CollectionScope collectionScope ) {
        Collections.reverse( partialEntities );

        //repair
       final MvccEntity mergedEntity = entityRepair( changeLogGenerator.getChangeLog( partialEntities.iterator(),
                        partialEntities.get( partialEntities.size() - 1 ).getVersion() ), partialEntities, targetEntity
                                 );

        try {
            entitySerializationStrategy.write( collectionScope, mergedEntity ).execute();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Couldn't rewrite repaired entity", e );
        }
        return mergedEntity;
    }


    /**
     * Applies the changelog to the completed entity.
     * @param changeLogEntryList
     * @param results
     * @param completedEntity
     * @return
     */
    private static MvccEntity entityRepair( List<ChangeLogEntry> changeLogEntryList, List<MvccEntity> results,
                                            MvccEntity completedEntity ) {
        int changeLogIndex = 0;
        for ( MvccEntity result : results ) {

            while ( changeLogIndex != changeLogEntryList.size() ) {

                ChangeLogEntry changeLogEntry = changeLogEntryList.get( changeLogIndex );

                if ( result.getId().equals( changeLogEntry.getEntryId() ) ) {

                    ChangeLogEntry.ChangeType changeType = changeLogEntry.getChangeType();

                    if ( changeType.equals( ChangeLogEntry.ChangeType.PROPERTY_DELETE ) ) {
                        completedEntity.getEntity().get().getFields().remove( changeLogEntry.getField() );
                    }
                    else if ( changeType.equals( ChangeLogEntry.ChangeType.PROPERTY_WRITE ) ) {
                        completedEntity.getEntity().get().setField( changeLogEntry.getField() );
                    }
                    changeLogIndex++;
                }
                else {
                    break;
                }
            }
        }

        if ( !completedEntity.getEntity().isPresent() ) {
            return null;
        }

        return completedEntity;
    }
}
