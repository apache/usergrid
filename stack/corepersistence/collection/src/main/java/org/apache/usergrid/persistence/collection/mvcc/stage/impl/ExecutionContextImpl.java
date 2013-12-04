package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;


/** @author tnine */
public class ExecutionContextImpl implements ExecutionContext {

    private final StagePipeline pipeline;
    private final CollectionContext context;

    private Object message;
    private Stage current;


    @Inject
    public ExecutionContextImpl( final StagePipeline pipeline, final CollectionContext context ) {
        Preconditions.checkNotNull( pipeline, "pipeline cannot be null" );
        Preconditions.checkNotNull( context, "context cannot be null" );

        this.pipeline = pipeline;
        this.context = context;
    }


    @Override
    public void execute( Object input ) {

        current = this.pipeline.first();

        setMessage( input );

        current.performStage( this );
    }


    @Override
    public <T> T getMessage( final Class<T> clazz ) {
        Preconditions.checkNotNull( clazz, "Class must be specified" );

        if ( message == null ) {
            return null;
        }

        if ( !clazz.isInstance( message ) ) {
            throw new ClassCastException(
                    "Message must be an instance of class " + clazz + ".  However it was of type '" + message.getClass()
                            + "'" );
        }


        return ( T ) message;
    }


    @Override
    public Object setMessage( final Object object ) {
        Object original = message;

        this.message = object;

        return original;
    }


    @Override
    public void proceed() {
        Stage next = this.pipeline.nextStage( current );

        //Nothing to do
        if ( next == null ) {
            return;
        }

        current = next;
        current.performStage( this );
    }



    @Override
    public CollectionContext getCollectionContext() {
        return this.context;
    }
}
