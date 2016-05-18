package org.apache.usergrid.apm.service;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;

/**
 * This  xstream instance has overwritten converter and wrapper for hibernate collection so that you don't get
 * org.hibernate.collection.PersistentSet classes in generated xml. 
 * 
 * @author prabhat
 *
 */

public class HibernateXStream {

	public static XStream getHibernateXStream () {

		//Simpler version: This should serve our purpose.
		XStream xstream = new XStream();
//		XStream xstream = new XStream(new JettisonMappedXmlDriver());
//		xstream.setMode(XStream.NO_REFERENCES);
		xstream.addDefaultImplementation(
				org.hibernate.collection.PersistentSet.class, java.util.Set.class);


		xstream.registerConverter(new CollectionConverter(xstream.getMapper()) {
			public boolean canConvert(Class type) {
				return super.canConvert(type) || type.equals(org.hibernate.collection.PersistentSet.class);

			}
		});

		//Complex Version: In case above does not and we need to handle the cases for classes generated by Hibernate's CGLib proxy
		//See http://jira.codehaus.org/browse/XSTR-226 for details.
		/*
		final Mapper mapper = xstream.getMapper();

		xstream = new XStream() {
			protected MapperWrapper wrapMapper(MapperWrapper next) { return new HibernateMapperNew(next); }

			@SuppressWarnings("unused")
			protected Mapper buildMapper() { return new HibernateCollectionsMapperNew(mapper); }
		};

		xstream.registerConverter(new HibernateCollectionConverterNew(xstream.getConverterLookup()));

		xstream.registerConverter(new HibernateProxyConverterNew(
				xstream.getMapper(), new PureJavaReflectionProvider(),xstream.getConverterLookup()),
				XStream.PRIORITY_VERY_HIGH);
		xstream.setMarshallingStrategy(new XStreamMarshallingStrategy(XStreamMarshallingStrategy.RELATIVE));
		 */		
		return xstream;

	}

}
