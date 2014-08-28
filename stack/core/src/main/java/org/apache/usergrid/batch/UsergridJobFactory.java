/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.batch;


import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.apache.usergrid.batch.repository.JobDescriptor;


/** @author tnine */
public class UsergridJobFactory implements JobFactory {

    @Autowired
    private ApplicationContext context;

    private Logger logger = LoggerFactory.getLogger( UsergridJobFactory.class );


    @Override
    public Job jobsFrom( JobDescriptor descriptor ) throws JobNotFoundException {

        Job job = context.getBean( descriptor.getJobName(), Job.class );

        if ( job == null ) {
            String error =
                    String.format( "Could not find job implementation for job name %s", descriptor.getJobName() );
            logger.error( error );
            throw new JobNotFoundException( error );
        }

        return job;
    }
}
