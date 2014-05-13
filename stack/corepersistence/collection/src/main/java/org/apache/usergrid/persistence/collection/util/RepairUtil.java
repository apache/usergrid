package org.apache.usergrid.persistence.collection.util;


import java.util.List;

import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGenerator;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGeneratorImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;


/**
 *
 *
 */
public class RepairUtil {


    private static  ChangeLogGenerator changeLogGenerator;

    public static Entity repair(List<MvccEntity> results){
        //base case
        if(results.size() == 1) {
            return results.get( 0 ).getEntity().get();
        }
        //delete case
        if(!results.get( 0 ).getEntity().isPresent() && results.get( 0 ).getStatus() == MvccEntity.Status.COMPLETE){
            return null;
        }

        for( int i = 0; i < results.size(); i++) {
            if(results.get(i).getStatus() == MvccEntity.Status.COMPLETE){
                    changeLogGenerator = new ChangeLogGeneratorImpl();
                   List<ChangeLogEntry> chgPersist = changeLogGenerator.getChangeLog(results.subList( 0, i+1 )
                           ,results.get( 0 ).getVersion()  );

                   return entityRepair( chgPersist, results.subList( 0,i ),results.get( i ) );
            }
            else
                continue;
        }
        return null;
    }

    private static Entity entityRepair( List<ChangeLogEntry> changeLogEntryList, List<MvccEntity> results,
                                            MvccEntity completedEntity ) {
        int changeLogIndex = 0;
        for ( MvccEntity result : results ) {

            while ( changeLogIndex != changeLogEntryList.size() ) {

                ChangeLogEntry changeLogEntry = changeLogEntryList.get( changeLogIndex );

                if ( result.getId().equals( changeLogEntry.getEntryId() ) ) {

                    ChangeLogEntry.ChangeType changeType = changeLogEntry.getChangeType();

                    if ( changeType.toString().equals( "PROPERTY_DELETE" ) ) {
                        completedEntity.getEntity().get().getFields().remove( changeLogEntry.getField() );
                    }
                    else if ( changeType.toString().equals( "PROPERTY_WRITE" ) ) {
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
