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
package org.apache.usergrid.mq.cassandra.io;


import java.util.UUID;


public class QueueBounds
{

    private final UUID oldest;
    private final UUID newest;


    public QueueBounds( UUID oldest, UUID newest )
    {
        this.oldest = oldest;
        this.newest = newest;
    }


    public UUID getOldest()
    {
        return oldest;
    }


    public UUID getNewest()
    {
        return newest;
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = ( prime * result ) + ( ( newest == null ) ? 0 : newest.hashCode() );
        result = ( prime * result ) + ( ( oldest == null ) ? 0 : oldest.hashCode() );
        return result;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        QueueBounds other = ( QueueBounds ) obj;
        if ( newest == null )
        {
            if ( other.newest != null )
            {
                return false;
            }
        }
        else if ( !newest.equals( other.newest ) )
        {
            return false;
        }
        if ( oldest == null )
        {
            if ( other.oldest != null )
            {
                return false;
            }
        }
        else if ( !oldest.equals( other.oldest ) )
        {
            return false;
        }
        return true;
    }


    @Override
    public String toString()
    {
        return "QueueBounds [oldest=" + oldest + ", newest=" + newest + "]";
    }
}
