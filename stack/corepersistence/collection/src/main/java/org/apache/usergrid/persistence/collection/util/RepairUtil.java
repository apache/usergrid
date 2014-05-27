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
 *
 *
 */
public class RepairUtil {

    private static final ChangeLogGenerator changeLogGenerator = new ChangeLogGeneratorImpl();

    public static MvccEntity repair( Iterator<MvccEntity> results, CollectionScope collectionScope,
                                     MvccEntitySerializationStrategy entitySerializationStrategy ) {

        //nothing to do, we didn't get a result back
        if ( !results.hasNext() ) {
            return null;
        }

        MvccEntity mvccEntity = results.next();
        List<MvccEntity> partialEntities = new ArrayList<>();

        //this entity has been marked as cleared.(deleted)
        //The version exists, but does not have entity data
        if ( !mvccEntity.getEntity().isPresent() && ( mvccEntity.getStatus() == MvccEntity.Status.DELETED
                || mvccEntity.getStatus() == MvccEntity.Status.COMPLETE ) ) {
            return null;
        }

        if ( mvccEntity.getStatus() == MvccEntity.Status.COMPLETE ) {
            return mvccEntity;
        }
        partialEntities.add( mvccEntity );


        while ( results.hasNext() ) {
            mvccEntity = results.next();
            partialEntities.add( mvccEntity );

            if ( mvccEntity.getStatus() != MvccEntity.Status.PARTIAL ) {
                return repairAndWrite( partialEntities, mvccEntity, entitySerializationStrategy, collectionScope );
            }
        }
        return null;
    }


    private static MvccEntity repairAndWrite( List<MvccEntity> partialEntities, MvccEntity mvccEntity,
                                              MvccEntitySerializationStrategy entitySerializationStrategy,
                                              CollectionScope collectionScope ) {
        Collections.reverse( partialEntities );

        //repair
        mvccEntity = entityRepair( changeLogGenerator.getChangeLog( partialEntities.iterator(),
                        partialEntities.get( partialEntities.size() - 1 ).getVersion() ), partialEntities, mvccEntity
                                 );

        try {
            entitySerializationStrategy.write( collectionScope, mvccEntity ).execute();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Couldn't rewrite repaired entity", e );
        }
        return mvccEntity;
    }


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
