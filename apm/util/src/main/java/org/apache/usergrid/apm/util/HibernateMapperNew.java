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
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.collection.PersistentBag;
import org.hibernate.collection.PersistentList;
import org.hibernate.collection.PersistentMap;
import org.hibernate.collection.PersistentSet;
import org.hibernate.collection.PersistentSortedMap;
import org.hibernate.collection.PersistentSortedSet;
import org.hibernate.proxy.HibernateProxy;

import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;


public class HibernateMapperNew extends MapperWrapper {

	Map collectionMap = new HashMap();
	public HibernateMapperNew(Mapper arg0) {
		super(arg0);
		init();
	}
	@SuppressWarnings("unchecked")
	public void init() {
		collectionMap.put(PersistentBag.class,ArrayList.class);
		collectionMap.put(PersistentList.class,ArrayList.class);
		collectionMap.put(PersistentMap.class,HashMap.class);
		collectionMap.put(PersistentSet.class,HashSet.class);
		collectionMap.put(PersistentSortedMap.class,TreeMap.class);
		collectionMap.put(PersistentSortedSet.class,TreeSet.class);
	}	

	public Class defaultImplementationOf(Class clazz) {
//		System.err.println("checking class:" + clazz);
		if(collectionMap.containsKey(clazz)) {
//			System.err.println("** substituting "  + clazz  + " with " + collectionMap.get(clazz)); 
			return (Class) collectionMap.get(clazz);	
		}

		return super.defaultImplementationOf(clazz);
	}

	public String serializedClass(Class clazz) {
		// check whether we are hibernate proxy and substitute real name
		for(int i = 0; i < clazz.getInterfaces().length;i++) {
			if(HibernateProxy.class.equals(clazz.getInterfaces()[i])){
//				System.err.println("resolving to class name:" + clazz.getSuperclass().getName());
				return clazz.getSuperclass().getName();
			}
		}
		if(collectionMap.containsKey(clazz)) {
//			System.err.println("** substituting "  + clazz  + " with " + collectionMap.get(clazz)); 
			return ((Class) collectionMap.get(clazz)).getName();	
		}

		return super.serializedClass(clazz);
	}

}

