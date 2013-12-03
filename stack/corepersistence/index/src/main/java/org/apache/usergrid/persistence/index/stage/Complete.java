package org.apache.usergrid.persistence.index.stage;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;


/**
 *
 * @author: tnine
 *
 */
public class Complete implements WriteStage
{

    @Override
    public void performStage( WriteContext context, final MvccEntity entity ) {

    }

}
