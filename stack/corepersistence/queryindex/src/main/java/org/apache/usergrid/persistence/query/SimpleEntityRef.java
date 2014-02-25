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
package org.apache.usergrid.persistence.query;


import java.util.UUID;
import org.apache.usergrid.persistence.model.entity.Id;


public class SimpleEntityRef implements EntityRef {

    private final Id id;

    private final UUID version;


    public SimpleEntityRef( Id id, UUID version ) {
        this.id = id;
        this.version = version;
    }


    public SimpleEntityRef( EntityRef entityRef ) {
        this.id = entityRef.getId();
        this.version = entityRef.getVersion(); 
    }


    public static EntityRef ref() {
        return new SimpleEntityRef( null, null );
    }

    public static EntityRef ref( Id id ) {
        return new SimpleEntityRef( id, null );
    }

    public static EntityRef ref( Id id, UUID version ) {
        return new SimpleEntityRef(  id, version );
    }


    public static EntityRef ref( EntityRef ref ) {
        return new SimpleEntityRef( ref );
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }


    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        SimpleEntityRef other = ( SimpleEntityRef ) obj;
        if ( id == null ) {
            if ( other.id != null ) {
                return false;
            }
        }
        else if ( !id.equals( other.id ) ) {
            return false;
        }
        if ( version == null ) {
            if ( other.version != null ) {
                return false;
            }
        }
        else if ( !version.equals( other.version ) ) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return id.toString() + "|" + version.toString();
    }

    public static Id getId( EntityRef ref ) {
        if ( ref == null ) {
            return null;
        }
        return ref.getId();
    }


    public static String getType( EntityRef ref ) {
        if ( ref == null ) {
            return null;
        }
        return ref.getId().getType();
    }

    public Id getId() {
        return id;
    }

    public UUID getVersion() {
        return version;
    }
}
