package org.apache.usergrid.persistence.collection.util;


import java.util.ArrayList;
import java.util.Collections;
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
            partialEntities.add( mvccEntity );
            if(mvccEntity.getStatus() == MvccEntity.Status.PARTIAL){
                continue;
            }
            else{

                //reverse the list
                Collections.reverse(partialEntities);

                //
                List<ChangeLogEntry> chgPersist = new ArrayList<>();
                //Create a sublist of 2 containing completed entity and partial entity
                List<MvccEntity> subEntList =  new ArrayList<>(  );

                for(int chg = 1; chg <= partialEntities.size()-1; chg++){
                    subEntList.clear();
                    chgPersist.clear();
                    subEntList.add( mvccEntity );
                    subEntList.add( partialEntities.get( chg ) );
                    chgPersist.addAll( changeLogGenerator.getChangeLog(subEntList.iterator()
                            ,subEntList.get( subEntList.size()-1 ).getVersion() ) );

                    mvccEntity = entityRepair( chgPersist,subEntList,mvccEntity );
                }

                return mvccEntity.getEntity().get();
            }

        }
        return null;
    }

    //TODO: change the list to be an iterator
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

        return completedEntity;
    }
}
