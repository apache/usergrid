package io.baas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.clustering.hazelcast.HazelcastTest;
import org.usergrid.persistence.TypedEntity;

public class Simple extends TypedEntity {

	private static final Logger logger = LoggerFactory.getLogger(HazelcastTest.class);
	
	public Simple() {
		super();
		logger.info("simple entity");
	}
}
