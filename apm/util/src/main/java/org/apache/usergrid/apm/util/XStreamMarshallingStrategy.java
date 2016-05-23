package org.apache.usergrid.apm.util;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByXPathMarshallingStrategy;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class XStreamMarshallingStrategy extends
		ReferenceByXPathMarshallingStrategy {
	
	

	public XStreamMarshallingStrategy(int mode) {
		super(mode);
	}

	@Override
	protected TreeMarshaller createMarshallingContext(
			HierarchicalStreamWriter writer, ConverterLookup converterLookup,
			Mapper mapper) {
        return new HibernateProxyXPathMarshaller(writer, converterLookup, mapper, RELATIVE);
	}


}
