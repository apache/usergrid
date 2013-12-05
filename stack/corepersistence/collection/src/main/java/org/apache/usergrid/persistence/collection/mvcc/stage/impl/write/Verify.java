package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionStage;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;

import com.google.inject.Singleton;


/** This phase should execute any verification on the MvccEntity */
@Singleton
public class Verify implements ExecutionStage {


    public Verify() {
    }


    @Override
    public void performStage( final ExecutionContext executionContext ) {
        //TODO no op for now, just continue to the next stage.  Verification logic goes in here

        executionContext.proceed();
    }
}
