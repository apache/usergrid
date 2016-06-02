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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByXPathMarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class HibernateProxyXPathMarshaller extends ReferenceByXPathMarshaller {

	public HibernateProxyXPathMarshaller(HierarchicalStreamWriter writer,
			ConverterLookup converterLookup, Mapper mapper, int mode) {
		super(writer, converterLookup, mapper, mode);
	}

	@Override
	public void convertAnother(Object item, Converter converter) {
		Object toConvert;
		if (HibernateProxy.class.isAssignableFrom(item.getClass())) {
			toConvert = ((HibernateProxy)item).getHibernateLazyInitializer().getImplementation();
		}
		else {
			toConvert = item;
		}
		super.convertAnother(toConvert, converter);
	}
	
	

}
