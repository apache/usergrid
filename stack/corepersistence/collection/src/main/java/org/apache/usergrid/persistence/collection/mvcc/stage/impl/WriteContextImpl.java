package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.Collection;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.event.PostProcessListener;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;


/** @author tnine */
public class WriteContextImpl implements WriteContext {

    private final Collection<PostProcessListener> listeners;
    private final StagePipeline pipeline;
    private final CollectionContext context;

    private Object message;
    private WriteStage current;


    @Inject
    public WriteContextImpl( final Collection<PostProcessListener> listeners, final StagePipeline pipeline,
                             final CollectionContext context ) {
        this.listeners = listeners;
        this.pipeline = pipeline;
        this.context = context;
    }


    @Override
    public StagePipeline getStagePipeline() {
        return this.pipeline;
    }


    @Override
    public void performWrite( Object input ) {

        current = this.pipeline.first();

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
        WriteStage next = this.pipeline.nextStage( current );

        //Nothing to do
        if ( next == null ) {
            return;
        }

        current = next;
        current.performStage( this );
    }


    @Override
    public void stop() {
        //No op ATM
        current = null;
    }


    @Override
    public Collection<PostProcessListener> getPostProcessors() {
        return listeners;
    }


    @Override
    public CollectionContext getCollectionContext() {
        return this.context;
    }
}
