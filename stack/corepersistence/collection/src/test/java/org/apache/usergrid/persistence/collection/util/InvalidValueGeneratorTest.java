/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.util;


import org.junit.Assert;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


@RunWith( Theories.class )
public class InvalidValueGeneratorTest {
    private static final Logger LOG = LoggerFactory.getLogger( InvalidValueGeneratorTest.class );

    @Theory
    public void testInvalidValues(
        @InvalidMvccEntityGenerator.IllegalFields final MvccEntity mvccEntityInvalid,
        @InvalidEntityGenerator.IllegalFields final Entity entityInvalid,
        @InvalidIdGenerator.IllegalFields final Id idInvalid) {

        Assert.assertNotNull( mvccEntityInvalid.getId() );

        Assert.assertNotNull( entityInvalid.getId() );

        Assert.assertNotNull( idInvalid.getUuid() );
        Assert.assertNotNull( idInvalid.getType() );
    }
}
