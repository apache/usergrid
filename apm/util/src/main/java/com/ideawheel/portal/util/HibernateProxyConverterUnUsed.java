package com.ideawheel.portal.util;

import org.hibernate.proxy.HibernateProxy;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class HibernateProxyConverterUnUsed extends ReflectionConverter {


	public HibernateProxyConverterUnUsed(Mapper arg0, ReflectionProvider arg1) {
		super(arg0, arg1);

	}

	/**
	 * be responsible for hibernate proxy
	 */
	public boolean canConvert(Class clazz) {
		System.err.println("converter says can convert " + clazz + ":"+ HibernateProxy.class.isAssignableFrom(clazz));
		return HibernateProxy.class.isAssignableFrom(clazz);
	}

	public void marshal(Object arg0, HierarchicalStreamWriter arg1, MarshallingContext arg2) {	
		System.err.println("converter marshalls: "  + ((HibernateProxy)arg0).getHibernateLazyInitializer().getImplementation());
		super.marshal(((HibernateProxy)arg0).getHibernateLazyInitializer().getImplementation(), arg1, arg2);
	}

}
