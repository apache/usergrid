package com.apigee.apm.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.usergrid.apm.service.ApplicationService;
import org.apache.usergrid.apm.service.ServiceFactory;




@Path("/apm")
public class StatusRestService {

	private static final Log logger = LogFactory.getLog(StatusRestService.class);	
	ApplicationService appService = ServiceFactory.getApplicationService();	

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public StatusBean getStatus() throws Exception {

		logger.info("Getting Apigee APM overall status");

		StatusBean status = new StatusBean();

		long startTime = System.currentTimeMillis();

		//verifies that we are able to connect to app config database

		status.setNumActiveApps (appService.getTotalApplicationsCount());


		//verifies that we are able to connect to analytics database
		status.setClientSessionMetricsCount (ServiceFactory.getSessionDBService().getClientSessionMetricsRowCount());

		//verifies that we are able to connect to analytics database and CEP is running
		status.setCompactSessionMetricsCount (ServiceFactory.getSessionDBService().getCompactSessionMetricsRowCount());

		long endTime = System.currentTimeMillis();
		long queryTime = (endTime - startTime) ; //seconds
		if (queryTime > 10000 || status.getNumActiveApps () == 0 || status.getClientSessionMetricsCount() == 0 ||
				status.getCompactSessionMetricsCount () == 0)
			status.setOverallStatus("RED");
		else if (queryTime > 5)
			status.setOverallStatus ("YELLOW" );
		else
			status.setOverallStatus("GREEN");
		return status;
	}
}
