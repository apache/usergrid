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

import org.hibernate.proxy.HibernateProxy;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public	class HibernateProxyConverterNew extends ReflectionConverter {

		
		private ConverterLookup converterLookup;

		public HibernateProxyConverterNew (Mapper arg0, ReflectionProvider arg1, ConverterLookup converterLookup) {
			super(arg0, arg1);
			this.converterLookup = converterLookup;
		}

		/**
		 * be responsible for hibernate proxy
		 */
		public boolean canConvert(Class clazz) {
//			System.err.println("converter says can convert " + clazz + ":"+ HibernateProxy.class.isAssignableFrom(clazz));
			return HibernateProxy.class.isAssignableFrom(clazz);
		}

		public void marshal(Object arg0, HierarchicalStreamWriter writer, MarshallingContext context) {	
//			System.err.println("converter marshalls: "  + ((HibernateProxy)arg0).getHibernateLazyInitializer().getImplementation());
			Object item = ((HibernateProxy)arg0).getHibernateLazyInitializer().getImplementation();
			converterLookup.lookupConverterForType(item.getClass()).marshal(item, writer, context);
		}
    	
    }

