package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;

import com.google.inject.Singleton;


/** This phase should execute any verification on the MvccEntity */
@Singleton
public class Verify implements WriteStage {


    public Verify() {
    }


    @Override
    public void performStage( final WriteContext writeContext ) {
        //TODO no op for now, just continue to the next stage.  Verification logic goes in here

        writeContext.proceed();
    }
}
