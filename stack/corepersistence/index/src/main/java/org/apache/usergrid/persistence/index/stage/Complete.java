package org.apache.usergrid.persistence.index.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;


/**
 *
 * @author: tnine
 *
 */
public class Complete implements WriteStage
{

    @Override
    public MvccEntity performStage( final MvccEntity entity ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
