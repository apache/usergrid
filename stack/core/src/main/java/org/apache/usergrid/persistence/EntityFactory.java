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
package org.apache.usergrid.persistence;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** A factory for creating Entity objects. */
public class EntityFactory {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger( EntityFactory.class );


    /**
     * New entity.
     *
     * @param <A> the generic type
     * @param id the id
     * @param type the type
     * @param entityClass the entity class
     *
     * @return new entity
     */
    public static <A extends Entity> A newEntity( UUID id, String type, Class<A> entityClass ) {
        if ( type == null ) {
            String errorMsg = "Entity type cannot be null";
            logger.error( errorMsg );
            throw new IllegalArgumentException( errorMsg );
        }
        if ( "entity".equalsIgnoreCase( type ) || "dynamicentity".equalsIgnoreCase( type ) ) {
            String errorMsg = "Unable to instantiate entity (" + type + ") because that is not a valid type.";
            logger.error( errorMsg );
            throw new IllegalArgumentException( errorMsg );
        }
        Class<? extends Entity> expectedCls = Schema.getDefaultSchema().getEntityClass( type );

        if ( entityClass == null ) {
            if ( expectedCls != null ) {
                entityClass = ( Class<A> ) expectedCls;
            }
            else {
                entityClass = ( Class<A> ) DynamicEntity.class;
            }
        }

        if ( ( expectedCls != null ) && !Entity.class.isAssignableFrom( entityClass ) && !expectedCls
                .isAssignableFrom( entityClass ) ) {
            String errorMsg = "Unable to instantiate entity (" + type
                    + ") because type and entityClass do not match, either use DynamicClass as entityClass or fix " +
                    "mismatch.";
            logger.error( errorMsg );
            throw new IllegalArgumentException( errorMsg );
        }
        else {
            try {
                A entity = entityClass.newInstance();
                entity.setUuid( id );
                entity.setType( type );
                return entity;
            }
            catch ( IllegalAccessException e ) {
                String errorMsg = "Unable to access entity (" + type + "): " + e.getMessage();
                logger.error( errorMsg );
            }
            catch ( InstantiationException e ) {
                String errorMsg = "Unable to instantiate entity (" + type + "): " + e.getMessage();
                logger.error( errorMsg );
            }
        }

        return null;
    }


    /**
     * New entity.
     *
     * @param <A> the generic type
     * @param id the id
     * @param entityClass the entity class
     *
     * @return new entity
     */
    public static <A extends Entity> A newEntity( UUID id, Class<A> entityClass ) {

        if ( entityClass == DynamicEntity.class ) {
            return null;
        }

        String type = Schema.getDefaultSchema().getEntityType( entityClass );

        return newEntity( id, type, entityClass );
    }


    /**
     * New entity.
     *
     * @param id the id
     * @param type the type
     *
     * @return new entity
     */
    public static Entity newEntity( UUID id, String type ) {

        Class<? extends Entity> entityClass = Schema.getDefaultSchema().getEntityClass( type );
        if ( entityClass == null ) {
            entityClass = DynamicEntity.class;
        }

        return newEntity( id, type, entityClass );
    }
}
