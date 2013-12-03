package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.Arrays;
import java.util.List;

import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;


/** @author tnine */
public class StagePipelineImpl implements StagePipeline {

    private final List<WriteStage> stages;
    private WriteStage current;

    protected StagePipelineImpl(WriteStage[] stages){
        this.stages = Arrays.asList(stages);
    }

    @Override
    public WriteStage first() {

        if(stages.size() == 0){
            return null;
        }

        return stages.get( 0 );
    }


    @Override
    public WriteStage last() {
        if(stages.size() == 0){
            return null;
        }

        return stages.get( stages.size()-1 );
    }


    @Override
    public WriteStage current() {
        return current;
    }


    @Override
    public void insert( final WriteStage stage ) {
        throw new UnsupportedOperationException("This needs implemented");

    }


    @Override
    public void addLast( final WriteStage stage ) {
       stages.add( stage );
    }


    @Override
    public WriteStage nextStage( final WriteStage stage ) {
        int index = stages.indexOf( stage );

        //we're done, do nothing
        if(index == stages.size()){
            return null;
        }

        current = stages.get( index+1 );

        return current;
    }



    /**
     * Factory to create a new instance.
     * @param stages
     * @return
     */
    public static StagePipelineImpl fromStages(WriteStage... stages){
        return new StagePipelineImpl( stages );
    }
}
