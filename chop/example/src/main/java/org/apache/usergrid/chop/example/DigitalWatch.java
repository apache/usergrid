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


import com.google.common.base.Preconditions;
import com.google.inject.Inject;


/**
 * A digital watch that runs on batteries.
 */
public class DigitalWatch implements Watch {
    private Battery battery;


    @Inject
    public void addPowerSource( PowerSource powerSource ) {
        Preconditions.checkState( powerSource.hasPower(), "Don't install a dead battery" );
        Preconditions.checkState( powerSource instanceof Battery );

        //noinspection ConstantConditions
        this.battery = ( Battery ) powerSource;
    }


    @Override
    public long getTime() {
        Preconditions.checkState( battery.hasPower(), "Can't tell time with a dead battery!" );
        return System.currentTimeMillis();
    }


    @Override
    public boolean isDead() {
        return ! battery.hasPower();
    }


    @Override
    public Type getType() {
        return Type.DIGITAL;
    }
}
