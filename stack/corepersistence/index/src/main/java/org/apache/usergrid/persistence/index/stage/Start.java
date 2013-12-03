package org.apache.usergrid.persistence.index.stage;


import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;


/** This state should signal an index update has started */
public class Start implements WriteStage
{

    @Override
    public void performStage( final WriteContext context ) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
