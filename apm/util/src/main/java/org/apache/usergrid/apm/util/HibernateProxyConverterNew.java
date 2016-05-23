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

