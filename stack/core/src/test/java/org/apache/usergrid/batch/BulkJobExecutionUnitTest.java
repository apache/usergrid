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


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.batch.JobExecution.Status;
import org.apache.usergrid.batch.repository.JobDescriptor;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.entities.JobStat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author zznate
 * @author tnine
 */
public class BulkJobExecutionUnitTest {

    @Test
    public void transitionsOk() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );
        assertEquals( JobExecution.Status.NOT_STARTED, bje.getStatus() );
        bje.start( 1 );
        assertEquals( JobExecution.Status.IN_PROGRESS, bje.getStatus() );
        bje.completed();
        assertEquals( JobExecution.Status.COMPLETED, bje.getStatus() );
    }


    @Test
    public void transitionsDead() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );
        assertEquals( JobExecution.Status.NOT_STARTED, bje.getStatus() );
        bje.start( 1 );
        assertEquals( JobExecution.Status.IN_PROGRESS, bje.getStatus() );
        bje.killed();
        assertEquals( JobExecution.Status.DEAD, bje.getStatus() );
    }


    @Test
    public void transitionsRetry() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );
        assertEquals( JobExecution.Status.NOT_STARTED, bje.getStatus() );
        bje.start( JobExecution.FOREVER );
        assertEquals( JobExecution.Status.IN_PROGRESS, bje.getStatus() );
        bje.failed();
        assertEquals( JobExecution.Status.FAILED, bje.getStatus() );
    }


    @Test
    public void transitionFail() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );
        try {
            bje.completed();
            fail( "Should have throw ISE on NOT_STARTED to IN_PROGRESS" );
        }
        catch ( IllegalStateException ise ) {
        }

        try {
            bje.failed();
            fail( "Should have thrown ISE on NOT_STARTED to FAILED" );
        }
        catch ( IllegalStateException ise ) {
        }
        bje.start( 1 );

        bje.completed();
        try {
            bje.failed();
            fail( "Should have failed failed after complete call" );
        }
        catch ( IllegalStateException ise ) {
        }
    }


    @Test
    public void transitionFailOnDeath() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );
        try {
            bje.completed();
            fail( "Should have throw ISE on NOT_STARTED to IN_PROGRESS" );
        }
        catch ( IllegalStateException ise ) {
        }

        try {
            bje.failed();
            fail( "Should have thrown ISE on NOT_STARTED to FAILED" );
        }
        catch ( IllegalStateException ise ) {
        }
        bje.start( 1 );

        bje.killed();
        try {
            bje.killed();
            fail( "Should have failed failed after complete call" );
        }
        catch ( IllegalStateException ise ) {
        }
    }


    @Test
    public void failureTriggerCount() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );

        bje.start( 1 );

        assertEquals( Status.IN_PROGRESS, bje.getStatus() );
        assertEquals( 1, stat.getRunCount() );


        bje.failed();

        assertEquals( Status.FAILED, bje.getStatus() );
        assertEquals( 1, stat.getRunCount() );


        // now fail again, we should trigger a state change
        bje = new JobExecutionImpl( jobDescriptor );
        bje.start( 1 );

        assertEquals( Status.DEAD, bje.getStatus() );
        assertEquals( 2, stat.getRunCount() );
    }


    @Test
    public void failureTriggerNoTrip() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );

        bje.start( JobExecution.FOREVER );

        assertEquals( Status.IN_PROGRESS, bje.getStatus() );
        assertEquals( 1, stat.getRunCount() );

        bje.failed();

        assertEquals( Status.FAILED, bje.getStatus() );
        assertEquals( 1, stat.getRunCount() );

        // now fail again, we should trigger a state change
        bje = new JobExecutionImpl( jobDescriptor );
        bje.start( JobExecution.FOREVER );

        assertEquals( Status.IN_PROGRESS, bje.getStatus() );
        assertEquals( 2, stat.getRunCount() );

        bje.failed();

        assertEquals( Status.FAILED, bje.getStatus() );
        assertEquals( 2, stat.getRunCount() );
    }


    @Test
    public void doubleInvokeFail() {
        JobData data = new JobData();
        JobStat stat = new JobStat();
        JobDescriptor jobDescriptor = new JobDescriptor( "", UUID.randomUUID(), UUID.randomUUID(), data, stat, null );
        JobExecution bje = new JobExecutionImpl( jobDescriptor );
        bje.start( 1 );
        try {
            bje.start( 1 );
            fail( "Should have failed on double start() call" );
        }
        catch ( IllegalStateException ise ) {
        }

        bje.completed();
        try {
            bje.completed();
            fail( "Should have failed on double complete call" );
        }
        catch ( IllegalStateException ise ) {
        }
    }
}
