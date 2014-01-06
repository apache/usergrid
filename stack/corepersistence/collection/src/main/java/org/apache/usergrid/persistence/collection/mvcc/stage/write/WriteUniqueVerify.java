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
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;

import rx.util.functions.Func1;


/**
 * This phase should execute any verification on the MvccEntity
 */
@Singleton
public class WriteUniqueVerify implements Func1<CollectionIoEvent<MvccEntity>, CollectionIoEvent<MvccEntity>> {

    @Inject
    public WriteUniqueVerify() {
    }


    @Override
    public CollectionIoEvent<MvccEntity> call( final CollectionIoEvent<MvccEntity> mvccEntityIoEvent ) {

        ValidationUtils.verifyMvccEntityWithEntity( mvccEntityIoEvent.getEvent() );

        Entity entity = mvccEntityIoEvent.getEvent().getEntity().get();
        for ( Field field : entity.getFields() ) {

            if ( field.isUnique() ) {

            }
        }

                // search UniqueValues to ensure that no other entity has that name/value

                    // if other entity has that name/value then throw runtime exception

        //no op, just emit the value
        return mvccEntityIoEvent;
    }
}
