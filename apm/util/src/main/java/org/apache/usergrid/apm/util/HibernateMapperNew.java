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

