package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.stage.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;

import com.google.inject.Singleton;


/** This phase should execute any verification on the MvccEntity */
@Singleton
public class Verify implements Stage {


    public Verify() {
    }


    @Override
    public void performStage( final ExecutionContext executionContext ) {
        //TODO no op for now, just continue to the next stage.  Verification logic goes in here

        executionContext.proceed();
    }
}
