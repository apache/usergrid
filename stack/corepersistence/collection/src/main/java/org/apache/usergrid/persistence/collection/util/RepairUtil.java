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


    public static  ChangeLogGenerator changeLogGenerator;

    public static Entity repair(List<MvccEntity> results){
        //base case
        if(results.size() == 1) {
            return results.get( 0 ).getEntity().get();
        }

        for(int i = results.size()-1; i >= 0 ; i --){
            //checks closes instance of a complete entity.
           if(results.get( i ).getStatus() == MvccEntity.Status.COMPLETE){
                changeLogGenerator = new ChangeLogGeneratorImpl();
               List<ChangeLogEntry> chgPersist = changeLogGenerator.getChangeLog(results.subList( i, results.size() )
                       ,results.get( results.size()-1 ).getVersion()  );

               return entityRepair( chgPersist, results.subList( i+1,results.size() ),results.get( i ) );

           }
        }
        return null;
    }

    private static Entity entityRepair( List<ChangeLogEntry> changeLogEntryList, List<MvccEntity> results,
                                            MvccEntity completedEntity ) {
        int changeLogIndex = 0;
        for(int index = 0; index < results.size();index++){

            while(changeLogIndex != changeLogEntryList.size()){

                ChangeLogEntry changeLogEntry = changeLogEntryList.get( changeLogIndex );

                if(results.get( index ).getId().equals( changeLogEntry.getEntryId() )){

                    ChangeLogEntry.ChangeType changeType = changeLogEntry.getChangeType();

                    if(changeType.toString().equals("PROPERTY_DELETE")){
                        completedEntity.getEntity().get().getFields().remove( changeLogEntry.getField() );
                    }
                    else if(changeType.toString() .equals( "PROPERTY_WRITE" )){
                        completedEntity.getEntity().get().setField( changeLogEntry.getField() );
                    }
                    changeLogIndex++;
                }
                else
                    break;
            }
        }

        return completedEntity.getEntity().get();
    }
}
