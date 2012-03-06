/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.utils;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.utils.ClassUtils.cast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author edanuff
 * 
 */
public class MapUtils extends org.apache.commons.collections.MapUtils {

	/**
	 * @param <A>
	 * @param <B>
	 * @param map
	 * @param a
	 * @param b
	 */
	public static <A, B> void addMapSet(Map<A, Set<B>> map, A a, B b) {
		addMapSet(map, false, a, b);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param map
	 * @param ignore_case
	 * @param a
	 * @param b
	 */
	@SuppressWarnings("unchecked")
	public static <A, B> void addMapSet(Map<A, Set<B>> map,
			boolean ignore_case, A a, B b) {

		Set<B> set_b = map.get(a);
		if (set_b == null) {
			if (ignore_case && (b instanceof String)) {
				set_b = (Set<B>) new TreeSet<String>(
						String.CASE_INSENSITIVE_ORDER);
			} else {
				set_b = new LinkedHashSet<B>();
			}
			map.put(a, set_b);
		}
		set_b.add(b);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param map
	 * @param a
	 * @param b
	 * @param c
	 */
	public static <A, B, C> void addMapMapSet(Map<A, Map<B, Set<C>>> map, A a,
			B b, C c) {
		addMapMapSet(map, false, a, b, c);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param map
	 * @param ignore_case
	 * @param a
	 * @param b
	 * @param c
	 */
	@SuppressWarnings("unchecked")
	public static <A, B, C> void addMapMapSet(Map<A, Map<B, Set<C>>> map,
			boolean ignore_case, A a, B b, C c) {

		Map<B, Set<C>> map_b = map.get(a);
		if (map_b == null) {
			if (ignore_case && (b instanceof String)) {
				map_b = (Map<B, Set<C>>) new TreeMap<String, Set<C>>(
						String.CASE_INSENSITIVE_ORDER);
			} else {
				map_b = new LinkedHashMap<B, Set<C>>();
			}
			map.put(a, map_b);
		}
		addMapSet(map_b, ignore_case, b, c);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param map
	 * @param a
	 * @param b
	 * @return
	 */
	public static <A, B, C> Set<C> getMapMapSet(Map<A, Map<B, Set<C>>> map,
			A a, B b) {

		Map<B, Set<C>> map_b = map.get(a);
		if (map_b == null) {
			return null;
		}
		Set<C> list_c = map_b.get(b);
		return list_c;
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 * @param map
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 */
	public static <A, B, C, D> void addMapMapMapSet(
			Map<A, Map<B, Map<C, Set<D>>>> map, A a, B b, C c, D d) {
		addMapMapMapSet(map, false, a, b, c, d);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 * @param map
	 * @param ignore_case
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 */
	@SuppressWarnings("unchecked")
	public static <A, B, C, D> void addMapMapMapSet(
			Map<A, Map<B, Map<C, Set<D>>>> map, boolean ignore_case, A a, B b,
			C c, D d) {
		Map<B, Map<C, Set<D>>> map_b = map.get(a);
		if (map_b == null) {
			if (ignore_case && (b instanceof String)) {
				map_b = (Map<B, Map<C, Set<D>>>) new TreeMap<String, Map<C, Set<D>>>(
						String.CASE_INSENSITIVE_ORDER);
			} else {
				map_b = new LinkedHashMap<B, Map<C, Set<D>>>();
			}
			map.put(a, map_b);
		}
		addMapMapSet(map_b, ignore_case, b, c, d);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 * @param map
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	public static <A, B, C, D> Set<D> getMapMapMapSet(
			Map<A, Map<B, Map<C, Set<D>>>> map, A a, B b, C c) {

		Map<B, Map<C, Set<D>>> map_b = map.get(a);
		if (map_b == null) {
			return null;
		}
		return getMapMapSet(map_b, b, c);
	}

	public static <A, B, C> void putMapMap(Map<A, Map<B, C>> map, A a, B b, C c) {
		putMapMap(map, false, a, b, c);
	}

	@SuppressWarnings("unchecked")
	public static <A, B, C> void putMapMap(Map<A, Map<B, C>> map,
			boolean ignore_case, A a, B b, C c) {

		Map<B, C> map_b = map.get(a);
		if (map_b == null) {
			if (ignore_case && (b instanceof String)) {
				map_b = (Map<B, C>) new TreeMap<String, C>(
						String.CASE_INSENSITIVE_ORDER);
			} else {
				map_b = new LinkedHashMap<B, C>();
			}
			map.put(a, map_b);
		}
		map_b.put(b, c);
	}

	public static <A, B, C> C getMapMap(Map<A, Map<B, C>> map, A a, B b) {

		Map<B, C> map_b = map.get(a);
		if (map_b == null) {
			return null;
		}
		return map_b.get(b);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param map
	 * @param a
	 * @param b
	 */
	public static <A, B> void addMapList(Map<A, List<B>> map, A a, B b) {

		List<B> list_b = map.get(a);
		if (list_b == null) {
			list_b = new ArrayList<B>();
			map.put(a, list_b);
		}
		list_b.add(b);
	}

	public static <A, B> void addListToMapList(Map<A, List<B>> map, A a,
			List<B> b) {

		List<B> list_b = map.get(a);
		if (list_b == null) {
			list_b = new ArrayList<B>();
			map.put(a, list_b);
		}
		list_b.addAll(b);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param map
	 * @param a
	 * @param b
	 * @param c
	 */
	public static <A, B, C> void addMapMapList(Map<A, Map<B, List<C>>> map,
			A a, B b, C c) {
		addMapMapList(map, false, a, b, c);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param map
	 * @param ignore_case
	 * @param a
	 * @param b
	 * @param c
	 */
	@SuppressWarnings("unchecked")
	public static <A, B, C> void addMapMapList(Map<A, Map<B, List<C>>> map,
			boolean ignore_case, A a, B b, C c) {

		Map<B, List<C>> map_b = map.get(a);
		if (map_b == null) {
			if (ignore_case && (b instanceof String)) {
				map_b = (Map<B, List<C>>) new TreeMap<String, List<C>>(
						String.CASE_INSENSITIVE_ORDER);
			} else {
				map_b = new LinkedHashMap<B, List<C>>();
			}
			map.put(a, map_b);
		}

		addMapList(map_b, b, c);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param map
	 * @param a
	 * @param b
	 * @return
	 */
	public static <A, B, C> List<C> getMapMapList(Map<A, Map<B, List<C>>> map,
			A a, B b) {

		Map<B, List<C>> map_b = map.get(a);
		if (map_b == null) {
			return null;
		}
		List<C> list_c = map_b.get(b);
		return list_c;
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 * @param map
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 */
	public static <A, B, C, D> void addMapMapMapList(
			Map<A, Map<B, Map<C, List<D>>>> map, A a, B b, C c, D d) {
		addMapMapMapList(map, false, a, b, c, d);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 * @param map
	 * @param ignore_case
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 */
	@SuppressWarnings("unchecked")
	public static <A, B, C, D> void addMapMapMapList(
			Map<A, Map<B, Map<C, List<D>>>> map, boolean ignore_case, A a, B b,
			C c, D d) {

		Map<B, Map<C, List<D>>> map_b = map.get(a);
		if (map_b == null) {
			if (ignore_case && (b instanceof String)) {
				map_b = (Map<B, Map<C, List<D>>>) new TreeMap<String, Map<C, List<D>>>(
						String.CASE_INSENSITIVE_ORDER);
			} else {
				map_b = new LinkedHashMap<B, Map<C, List<D>>>();
			}
			map.put(a, map_b);
		}

		addMapMapList(map_b, ignore_case, b, c, d);
	}

	/**
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 * @param <D>
	 * @param map
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	public static <A, B, C, D> List<D> getMapMapMapList(
			Map<A, Map<B, Map<C, List<D>>>> map, A a, B b, C c) {

		Map<B, Map<C, List<D>>> map_b = map.get(a);
		if (map_b == null) {
			return null;
		}
		return getMapMapList(map_b, b, c);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> V getValue(Map<K, ?> map, K k) {
		V v = null;
		try {
			v = (V) map.get(k);
		} catch (ClassCastException e) {

		}

		return v;
	}

	public static <A> Map<String, A> merge(Map<String, A>... maps) {
		Map<String, A> merged = new TreeMap<String, A>(
				String.CASE_INSENSITIVE_ORDER);
		for (Map<String, A> m : maps) {
			merged.putAll(m);
		}
		return merged;
	}

	public static <K, V> Map<K, V> superMerge(Map<K, V> a, Map<K, V> b) {

		for (K k : b.keySet()) {
			V v = b.get(k);

			if (!a.containsKey(k)) {
				a.put(k, v);
				return a;
			} else {
				V av = a.get(k);
				if (av instanceof Map) {

				}
			}
		}
		return a;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<?, ?> map(Object... objects) {
		Map<K, V> map = new LinkedHashMap<K, V>();
		int i = 0;
		while (i < objects.length) {
			if (objects[i] instanceof Map.Entry) {
				Map.Entry<K, V> entry = (Entry<K, V>) objects[i];
				map.put(entry.getKey(), entry.getValue());
				i++;
			} else if (objects[i] instanceof Map) {
				map.putAll((Map<? extends K, ? extends V>) objects[i]);
				i++;
			} else if (i < (objects.length - 1)) {
				K k = (K) objects[i];
				V v = (V) objects[i + 1];
				map.put(k, v);
				i += 2;
			} else {
				break;
			}
		}
		return map;
	}

	private static class SimpleMapEntry<K, V> implements Map.Entry<K, V> {

		private final K k;
		private V v;

		public SimpleMapEntry(K k, V v) {
			this.k = k;
			this.v = v;
		}

		@Override
		public K getKey() {
			return k;
		}

		@Override
		public V getValue() {
			return v;
		}

		@Override
		public V setValue(V v) {
			V oldV = this.v;
			this.v = v;
			return oldV;
		}

	}

	public static <K, V> Map.Entry<K, V> entry(K k, V v) {
		return new SimpleMapEntry<K, V>(k, v);
	}

	public static Object trimSingleKeyMap(Object obj) {
		if (obj == null) {
			return null;
		}
		if (!(obj instanceof Map)) {
			return obj;
		}
		@SuppressWarnings("unchecked")
		Map<Object, Object> map = (Map<Object, Object>) obj;
		if (map.size() == 1) {
			Iterator<Map.Entry<Object, Object>> i = map.entrySet().iterator();
			return i.next().getValue();
		}
		return map;
	}

	public static Object trimAllParentSingleKeyMaps(Object obj) {
		Object prev_obj = obj;
		Object new_obj = trimSingleKeyMap(prev_obj);
		while ((new_obj != prev_obj) && (new_obj != null)) {
			prev_obj = new_obj;
			new_obj = trimSingleKeyMap(prev_obj);
		}
		return new_obj;
	}

	public static <K, V> Entry<K, V> getFirst(Map<K, V> map) {
		if (map == null) {
			return null;
		}
		return map.entrySet().iterator().next();
	}

	public static <K, V> K getFirstKey(Map<K, V> map) {
		if (map == null) {
			return null;
		}
		Entry<K, V> e = map.entrySet().iterator().next();
		if (e != null) {
			return e.getKey();
		}
		return null;
	}

	public static <V> Map<String, V> filter(Map<String, V> map, String prefix,
			boolean remove_prefix) {
		Map<String, V> filteredMap = new LinkedHashMap<String, V>();
		for (Entry<String, V> entry : map.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				if (remove_prefix) {
					filteredMap.put(entry.getKey().substring(prefix.length()),
							entry.getValue());
				} else {
					filteredMap.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return filteredMap;
	}

	public static <V> Map<String, V> filter(Map<String, V> map, String prefix) {
		return filter(map, prefix, false);
	}

	public static Properties filter(Properties properties, String prefix,
			boolean remove_prefix) {
		Properties filteredProperties = new Properties();
		for (Entry<String, String> entry : asMap(properties).entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				if (remove_prefix) {
					filteredProperties.put(
							entry.getKey().substring(prefix.length()),
							entry.getValue());
				} else {
					filteredProperties.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return filteredProperties;
	}

	public static Properties filter(Properties properties, String prefix) {
		return filter(properties, prefix, false);
	}

	public static Properties asProperties(Map<String, String> map) {
		Properties properties = new Properties();
		properties.putAll(map);
		return properties;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> asMap(Properties properties) {
		return (Map<String, String>) cast(properties);
	}

	public static <S, T> HashMapBuilder<S, T> hashMap(S key, T value) {
		return new HashMapBuilder<S, T>().map(key, value);
	}

	public static class HashMapBuilder<S, T> extends HashMap<S, T> {
		private static final long serialVersionUID = 1L;

		public HashMapBuilder() {
		}

		public HashMapBuilder<S, T> map(S key, T value) {
			put(key, value);
			return this;
		}
	}

	public static <S, T> ConcurrentHashMapBuilder<S, T> concurrentHashMap(
			S key, T value) {
		return new ConcurrentHashMapBuilder<S, T>().map(key, value);
	}

	public static class ConcurrentHashMapBuilder<S, T> extends HashMap<S, T> {
		private static final long serialVersionUID = 1L;

		public ConcurrentHashMapBuilder() {
		}

		public ConcurrentHashMapBuilder<S, T> map(S key, T value) {
			put(key, value);
			return this;
		}
	}

	public static <S, T> LinkedHashMapBuilder<S, T> linkedHashMap(S key, T value) {
		return new LinkedHashMapBuilder<S, T>().map(key, value);
	}

	public static class LinkedHashMapBuilder<S, T> extends HashMap<S, T> {
		private static final long serialVersionUID = 1L;

		public LinkedHashMapBuilder() {
		}

		public LinkedHashMapBuilder<S, T> map(S key, T value) {
			put(key, value);
			return this;
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, List<?>> toMapList(Map<String, ?> m) {
		Map<String, List<Object>> mapList = new LinkedHashMap<String, List<Object>>();

		for (Entry<String, ?> e : m.entrySet()) {
			if (e.getValue() instanceof List) {
				addListToMapList(mapList, e.getKey(),
						(List<Object>) e.getValue());
			} else {
				addMapList(mapList, e.getKey(), e.getValue());
			}
		}

		return cast(mapList);
	}

	public static Map<String, ?> putPath(String path, Object value) {
		return putPath(null, path, value);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, ?> putPath(Map<String, ?> map, String path,
			Object value) {

		if (map == null) {
			map = new HashMap<String, Object>();
		}

		int i = path.indexOf('.');
		if (i < 0) {
			((Map<String, Object>) map).put(path, value);
			return map;
		}
		String segment = path.substring(0, i).trim();
		if (isNotBlank(segment)) {
			Object o = map.get(segment);
			if ((o != null) && (!(o instanceof Map))) {
				return map;
			}
			Map<String, Object> subMap = (Map<String, Object>) o;
			if (subMap == null) {
				subMap = new HashMap<String, Object>();
				((Map<String, Object>) map).put(segment, subMap);
			}
			String subPath = path.substring(i + 1);
			if (isNotBlank(subPath)) {
				putPath(subMap, subPath, value);
			}
		}

		return map;
	}

	public static <K, V> Map<K, V> emptyMapWithKeys(Map<K, V> map) {
		Map<K, V> newMap = new HashMap<K, V>();

		for (K k : map.keySet()) {
			newMap.put(k, null);
		}

		return newMap;
	}

}
