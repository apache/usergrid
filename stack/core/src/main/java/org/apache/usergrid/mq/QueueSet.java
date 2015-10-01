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
package org.apache.usergrid.mq;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;


@XmlRootElement
public class QueueSet {

    List<QueueInfo> queues = new ArrayList<QueueInfo>();
    boolean more;


    public QueueSet() {

    }


    public List<QueueInfo> getQueues() {
        return queues;
    }


    public void setQueues( List<QueueInfo> queues ) {
        if ( queues == null ) {
            queues = new ArrayList<QueueInfo>();
        }
        this.queues = queues;
    }


    public QueueSet addQueue( String queuePath, UUID queueId ) {
        QueueInfo queue = new QueueInfo( queuePath, queueId );
        queues.add( queue );
        return this;
    }


    public boolean isMore() {
        return more;
    }


    public void setMore( boolean more ) {
        this.more = more;
    }


    public boolean hasMore() {
        return more;
    }


    public int size() {
        return queues.size();
    }


    @XmlRootElement
    public static class QueueInfo {

        String path;
        UUID uuid;


        public QueueInfo() {
        }


        public QueueInfo( String path, UUID uuid ) {
            this.path = path;
            this.uuid = uuid;
        }


        public String getPath() {
            return path;
        }


        public void setPath( String path ) {
            this.path = path;
        }


        public UUID getUuid() {
            return uuid;
        }


        public void setUuid( UUID uuid ) {
            this.uuid = uuid;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = ( prime * result ) + ( ( path == null ) ? 0 : path.hashCode() );
            result = ( prime * result ) + ( ( uuid == null ) ? 0 : uuid.hashCode() );
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
            QueueInfo other = ( QueueInfo ) obj;
            if ( path == null ) {
                if ( other.path != null ) {
                    return false;
                }
            }
            else if ( !path.equals( other.path ) ) {
                return false;
            }
            if ( uuid == null ) {
                if ( other.uuid != null ) {
                    return false;
                }
            }
            else if ( !uuid.equals( other.uuid ) ) {
                return false;
            }
            return true;
        }


        @Override
        public String toString() {
            return "QueueInfo [path=" + path + ", uuid=" + uuid + "]";
        }
    }


    public void setCursorToLastResult() {
        // TODO Auto-generated method stub

    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Object getCursor() {
        // TODO Auto-generated method stub
        return null;
    }


    public void and( QueueSet r ) {
        Set<QueueInfo> oldSet = new HashSet<QueueInfo>( queues );
        List<QueueInfo> newList = new ArrayList<QueueInfo>();
        for ( QueueInfo q : r.getQueues() ) {
            if ( oldSet.contains( q ) ) {
                newList.add( q );
            }
        }
        queues = newList;
    }
}
