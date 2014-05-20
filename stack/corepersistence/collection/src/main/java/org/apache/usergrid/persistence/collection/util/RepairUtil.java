package org.apache.usergrid.persistence.collection.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.iterators.PushbackIterator;

import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGenerator;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGeneratorImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;


/**
 *
 *
 */
public class RepairUtil {

    private static  ChangeLogGenerator changeLogGenerator = new ChangeLogGeneratorImpl();

    public static Entity repair(Iterator<MvccEntity> results){

        //nothing to do, we didn't get a result back
        if ( !results.hasNext() ) {
            return null;
        }

        org.apache.commons.collections4.iterators.PushbackIterator<MvccEntity> iter = new PushbackIterator<>( results );
        MvccEntity mvccEntity = iter.next();
        final Optional<Entity> targetVersion = mvccEntity.getEntity();
        List<MvccEntity> partialEntities = new ArrayList<>();

        //this entity has been marked as cleared.(deleted)
        //The version exists, but does not have entity data
        if ( !targetVersion.isPresent() && (mvccEntity.getStatus() == MvccEntity.Status.DELETED
                                            || mvccEntity.getStatus() == MvccEntity.Status.COMPLETE)) {
            return null;
        }

        //TODO: make this return a mvcc entity?
        if ( mvccEntity.getStatus() == MvccEntity.Status.COMPLETE ) {
            return targetVersion.get();
        }

        iter.pushback( mvccEntity );
        while(iter.hasNext()){
            mvccEntity = iter.next();
            if(mvccEntity.getStatus() == MvccEntity.Status.PARTIAL){
                partialEntities.add( mvccEntity );
            }
            else{
                partialEntities.add(mvccEntity);
                List<ChangeLogEntry> chgPersist = changeLogGenerator.getChangeLog(partialEntities.iterator()
                        ,partialEntities.get( 0 ).getVersion() );

                return entityRepair( chgPersist,partialEntities,mvccEntity );
            }

        }
        return null;
    }

    //TODO: change the list to be an iterator
    private static Entity entityRepair( List<ChangeLogEntry> changeLogEntryList, List<MvccEntity> results,
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
                    else if ( changeType.equals( ChangeLogEntry.ChangeType.PROPERTY_WRITE) ) {
                        completedEntity.getEntity().get().setField( changeLogEntry.getField() );
                    }
                    changeLogIndex++;
                }
                else {
                    break;
                }
            }
        }

        if(!completedEntity.getEntity().isPresent()){
            return null;
        }

        return completedEntity.getEntity().get();
    }
}
