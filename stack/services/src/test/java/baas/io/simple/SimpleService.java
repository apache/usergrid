package baas.io.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.services.AbstractCollectionService;

public class SimpleService extends AbstractCollectionService {
	
	private static final Logger logger = LoggerFactory.getLogger(SimpleService.class);

	public SimpleService() {
		super();
		logger.info("/simple");
	}
}
