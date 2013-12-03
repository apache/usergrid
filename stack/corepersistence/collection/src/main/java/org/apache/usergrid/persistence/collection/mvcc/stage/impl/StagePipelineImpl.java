package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.Arrays;
import java.util.List;

import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;


/** @author tnine */
public class StagePipelineImpl implements StagePipeline {

    private int currentIndex = 0;
    private final List<WriteStage> stages;

    protected StagePipelineImpl(WriteStage[] stages){
        this.stages = Arrays.asList(stages);
    }

    @Override
    public WriteStage next() {

        if(currentIndex < stages.size()){

            //get our current stage and increment
            return stages.get( currentIndex ++);
        }

       return null;
    }


    @Override
    public void insert( final WriteStage stage ) {
        throw new UnsupportedOperationException("This needs written");

    }


    @Override
    public void addLast( final WriteStage stage ) {
       stages.add( stage );
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
