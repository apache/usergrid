/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.example;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A mainspring used to power mechanical watches.
 */
public class Mainspring implements PowerSource {
    private static final Logger LOG = LoggerFactory.getLogger( Mainspring.class );
    private long energyLeftUntil = System.currentTimeMillis() + 1300L;


    public Mainspring() {
        LOG.debug( "Spring created with {} milliseconds of energy.", energyLeftUntil - System.currentTimeMillis() );
    }


    public void windSpring( long energyTime ) {
        energyLeftUntil = System.currentTimeMillis() + energyTime;
    }


    @Override
    public boolean hasPower() {
        return ( energyLeftUntil - System.currentTimeMillis() ) >= 0;
    }


    @Override
    public void refill( final long energyTime ) {
        windSpring( energyTime );
    }
}
