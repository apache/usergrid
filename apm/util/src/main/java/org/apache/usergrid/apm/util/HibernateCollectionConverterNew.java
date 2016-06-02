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
package org.apache.usergrid.apm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentSet;
import org.hibernate.collection.PersistentSortedSet;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter that strips HB collections specific information and retrieves the underlying 
 * collection which is then parsed by the delegated converter. This converter only takes care of the 
 * values inside the collections while the mapper takes care of the collections naming. 
 *  
 * @author Costin Leau
 *
 */
public class HibernateCollectionConverterNew implements Converter
{
    private Converter listSetConverter;
    private Converter mapConverter;
    private Converter treeMapConverter;
    private Converter treeSetConverter;
    private Converter defaultConverter;

    public HibernateCollectionConverterNew(ConverterLookup converterLookup)
    {
        listSetConverter = converterLookup
            .lookupConverterForType(ArrayList.class);
        mapConverter = converterLookup.lookupConverterForType(HashMap.class);
        treeMapConverter = converterLookup
            .lookupConverterForType(TreeMap.class);
        treeSetConverter = converterLookup
            .lookupConverterForType(TreeSet.class);
        defaultConverter = converterLookup.lookupConverterForType(Object.class);
    }

    /**
     * @see com.thoughtworks.xstream.converters.Converter#canConvert(java.lang.Class)
     */
    public boolean canConvert(Class type)
    {
        return PersistentCollection.class.isAssignableFrom(type);
    }

    /**
     * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
     */
    @SuppressWarnings("unchecked")
	public void marshal(Object source, HierarchicalStreamWriter writer,
        MarshallingContext context)
    {
        Object collection = source;
        
        if (source instanceof PersistentCollection)
        {
            PersistentCollection col = (PersistentCollection) source;
            col.forceInitialization();
//ToDo ES:       collection = col.getCollectionSnapshot().getSnapshot();
            collection = col.getStoredSnapshot();
        }
        
        // the set is returned as a map by Hibernate (unclear why exactly)
        if (source instanceof PersistentSortedSet)
        {
        	collection = new TreeSet(((HashMap)collection).values());
        }        
        else if (source instanceof PersistentSet)
        {
//            collection = new HashSet(((HashMap)collection).entrySet());
            collection = new HashSet(((HashMap)collection).values());
        }
        
        // delegate the collection to the approapriate converter
        if (listSetConverter.canConvert(collection.getClass()))
        {
            listSetConverter.marshal(collection, writer, context);
            return;
        }
        if (mapConverter.canConvert(collection.getClass()))
        {
            mapConverter.marshal(collection, writer, context);
            return;
        }
        if (treeMapConverter.canConvert(collection.getClass()))
        {
            treeMapConverter.marshal(collection, writer, context);
            return;
        }
        if (treeSetConverter.canConvert(collection.getClass()))
        {
            treeSetConverter.marshal(collection, writer, context);
            return;
        }

        defaultConverter.marshal(collection, writer, context);
    }
    
    /**
     * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
     */
    public Object unmarshal(HierarchicalStreamReader reader,
        UnmarshallingContext context)
    {
        return null;
    }
}
