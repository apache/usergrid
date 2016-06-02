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
package org.apache.usergrid.apm.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.service.charts.service.AggregatedLogData;
import org.apache.usergrid.apm.service.charts.service.AggregatedNetworkData;
import org.apache.usergrid.apm.service.charts.service.AggregatedSessionData;
import org.apache.usergrid.apm.service.charts.service.AggregatorUtil;
import org.apache.usergrid.apm.service.charts.service.AttributeValueChartData;
import org.apache.usergrid.apm.service.charts.service.ChartService;
import org.apache.usergrid.apm.service.charts.service.CrashRawCriteria;
import org.apache.usergrid.apm.service.charts.service.LogChartCriteriaService;
import org.apache.usergrid.apm.service.charts.service.LogChartDTO;
import org.apache.usergrid.apm.service.charts.service.LogRawCriteria;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartCriteriaService;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartDTO;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsRawCriteria;
import org.apache.usergrid.apm.service.charts.service.NetworkRequestsErrorsChartData;
import org.apache.usergrid.apm.service.charts.service.SessionChartCriteriaService;
import org.apache.usergrid.apm.service.charts.service.SessionMetricsChartDTO;
import org.apache.usergrid.apm.model.CrashLogDetails;
import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.service.ApplicationService;
import org.apache.usergrid.apm.service.CrashLogDBService;
import org.apache.usergrid.apm.service.LogDBService;
import org.apache.usergrid.apm.service.NetworkMetricsDBService;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.apache.usergrid.apm.service.SessionDBService;
import com.sun.jersey.api.json.JSONWithPadding;



@Path("/{org}/{app}/apm")
public class ApigeeApmRestService {

	private static final Log logger = LogFactory.getLog(ApigeeApmRestService.class);

	ApplicationService appService = ServiceFactory.getApplicationService();	
	SessionChartCriteriaService sessionChartCriteriaService = ServiceFactory.getSessionChartCriteriaService();
	LogChartCriteriaService logChartCriteriaService = ServiceFactory.getLogChartCriteriaService();
	NetworkMetricsChartCriteriaService networkChartCriteriaService = ServiceFactory.getNetworkMetricsChartCriteriaService();
	ChartService chartDataService = ServiceFactory.getChartService();
	SessionDBService sessionDBService = ServiceFactory.getSessionDBService();
	Long demoAppId = null;

	/** This API is used by Usergrid Management Service to register newly added up into mAX/APM system.
	 * It's not exposed to SDK or UI.
	 * @param appFromUsergrid
	 * @return
	 * @throws Exception
	 */

	@POST
	@Path("/appConfig")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public App createApp(AppDetailsForAPM appFromUsergrid)  throws Exception {

		App app = new App ();		
		app.setApplicationUUID(appFromUsergrid.getAppUUID().toString());
		app.setOrganizationUUID(appFromUsergrid.getOrgUUID().toString());
		app.setAppName(appFromUsergrid.getAppName().toLowerCase());
		app.setOrgName(appFromUsergrid.getOrgName().toLowerCase());
		app.setAppOwner(appFromUsergrid.getAppAdminEmail());
		app.setFullAppName(app.getOrgName()+"_"+app.getAppName());
		app = appService.createApplication(app);

		return app;
	}
	/**
	 * This is an internal API to create demo app in this environment. See {@link org.apache.usergrid.apm.service.ApplicationService} for
	 * org name and app for Demo App.
	 * @return
	 * @throws Exception
	 */
	@POST
	@Path("/demoApp")	
	@Produces(MediaType.APPLICATION_JSON)
	public App createApp()  throws Exception {
		App app = appService.getDemoApp();
		if (app == null ) {
			app = new App ();		
			app.setApplicationUUID("DEMO_APP_UUID");
			app.setOrganizationUUID("DEMO_ORG_UUID");
			app.setAppName(ApplicationService.DEMO_APP_NAME.toLowerCase());
			app.setOrgName(ApplicationService.DEMO_APP_ORG.toLowerCase());
			app.setAppOwner(ApplicationService.ADMIN_EMAIL_NAME);
			app.setFullAppName(ApplicationService.DEMO_APP_FULL_NAME.toLowerCase());
			app = appService.createApplication(app); 
		}

		return app;
	}

	/**
	 * This API is not used directly by SDK or UI. It's more to verify that registration of new App in Usergrid with mAX system
	 * was successful
	 * @param org
	 * @param id
	 * @return
	 * @throws Exception
	 */

	@GET
	@Path("/appConfig/{instaOpsAppId}")
	@Produces(MediaType.APPLICATION_JSON)
	public App getApp(@PathParam("org") String org, @PathParam("instaOpsAppId") String id) throws Exception {
		logger.info("get called for app with org : " + org +  " instaOpsApplicationId : " + id);

		App app = new App ();
		app = appService.getApplication(new Long(id));
		if (app == null)
			throw new Exception("App does not exist in Apigee APM System.");
		else
			return app;
	}

	@GET
	@Path("/sessionChartCriteria/{instaOpsAppId}") 
	@Produces("application/x-javascript")
	public JSONWithPadding getSessionChartCriteria(
			@PathParam("instaOpsAppId") String id,	
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@QueryParam("callback") @DefaultValue("callback")  String callback) throws Exception {

		logger.info("getting session chart criteria for instaOpsApplicationId : " + id + " with demoApp set to " + demoApp);
		Long appId = null;
		if (demoApp) //requesting data for demo app so we are going to ignore instaOpsAppId path param 
			appId = getDemoAppId();
		else
			appId = new Long(id);

		List <SessionChartCriteria> cqs = sessionChartCriteriaService.getVisibleChartCriteriaForApp(appId);
		List<SimplifiedChartCriteria> chartCriterias = new ArrayList<SimplifiedChartCriteria>();
		SimplifiedChartCriteria scq;
		for (SessionChartCriteria cq : cqs) {
			scq = new SimplifiedChartCriteria();
			scq.setChartCriteriaId(cq.getId());
			scq.setInstaOpsApplicationId(cq.getAppId());
			scq.setChartName(cq.getChartName());
			scq.setChartDescription(cq.getDescription());
			scq.setType(SimplifiedChartCriteria.TYPE_SESSION);
			chartCriterias.add(scq);
		}	

		return new JSONWithPadding(new GenericEntity<List<SimplifiedChartCriteria>>(chartCriterias) {}, callback);

	}
	@GET
	@Path("/logChartCriteria/{instaOpsAppId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getLogChartCriteria(
			@PathParam("instaOpsAppId") String id, 
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@QueryParam("callback") @DefaultValue("callback")  String callback) throws Exception {
		logger.info("getting log chart criteria for instaOpsApplicationId : " + id + " with demoApp set as " + demoApp);
		Long appId = null;
		if (demoApp) //requesting data for demo app so we are going to ignore instaOpsAppId path param 
			appId = getDemoAppId();
		else
			appId = new Long(id);
		List<LogChartCriteria> cqs =  logChartCriteriaService.getVisibleChartCriteriaForApp(appId);

		List<SimplifiedChartCriteria> chartCriterias = new ArrayList<SimplifiedChartCriteria>();
		SimplifiedChartCriteria scq;
		for (LogChartCriteria cq : cqs) {
			scq = new SimplifiedChartCriteria();
			scq.setChartCriteriaId(cq.getId());
			scq.setInstaOpsApplicationId(cq.getAppId());
			scq.setChartName(cq.getChartName());
			scq.setChartDescription(cq.getDescription());
			scq.setType(SimplifiedChartCriteria.TYPE_LOG);
			chartCriterias.add(scq);
		}
		return new JSONWithPadding(new GenericEntity<List<SimplifiedChartCriteria>>(chartCriterias) {}, callback);
	}

	@GET
	@Path("/networkChartCriteria/{instaOpsAppId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getNetworkChartCriteria(
			@PathParam("instaOpsAppId")  String id, 
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@QueryParam("callback") @DefaultValue("callback")  String callback) throws Exception {
		logger.info("getting session chart criteria for instaOpsApplicationId : " + id + " with demoApp set to " + demoApp);
		Long appId = null;
		if (demoApp) //requesting data for demo app so we are going to ignore instaOpsAppId path param 
			appId = getDemoAppId();
		else
			appId = new Long(id);		
		List<MetricsChartCriteria> cqs = networkChartCriteriaService.getVisibleChartCriteriaForApp(appId);
		List<SimplifiedChartCriteria> chartCriterias = new ArrayList<SimplifiedChartCriteria>();
		SimplifiedChartCriteria scq;
		for (MetricsChartCriteria cq : cqs) {
			scq = new SimplifiedChartCriteria();
			scq.setChartCriteriaId(cq.getId());
			scq.setInstaOpsApplicationId(cq.getAppId());
			scq.setChartName(cq.getChartName());
			scq.setChartDescription(cq.getDescription());
			scq.setType(SimplifiedChartCriteria.TYPE_NETWORK);
			chartCriterias.add(scq);
		}
		return new JSONWithPadding(new GenericEntity<List<SimplifiedChartCriteria>>(chartCriterias) {}, callback);
	}

	//sessionChartData/121?chartCriteriaId=123&period=1h&reference=yesterday
	@GET
	@Path("/sessionChartData/{chartCriteriaId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getSessionChartData(			
			@PathParam("chartCriteriaId") Long chartCriteriaId,			
			@DefaultValue(ApigeeMobileAPMConstants.CHART_PERIOD_1HR) @QueryParam("period") String period,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_DATA_REFERENCE_POINT_NOW) @QueryParam("reference") String reference,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES) @QueryParam("chartType") String chartType,
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@DefaultValue("callback") @QueryParam("callback") String callback) throws Exception {
		logger.info("getting session chart data for chartCriteriaId : " + chartCriteriaId +
				" period " + period + " reference" + reference);
		SessionChartCriteria cq = sessionChartCriteriaService.getChartCriteria(chartCriteriaId);
		if (cq == null)
			throw new Exception("Invalid Chart Criteria parameter");
		cq = (SessionChartCriteria) ApmUtil.getChartCriteriaWithTimeRange(cq, period, reference);

		if (demoApp) {//update appId and chartCriteriaId of demo app so that queries are done for requested time period but for demo app
			cq.setAppId(getDemoAppId());
			cq.setId(sessionChartCriteriaService.getDefaultChartCriteriaByName(cq.getAppId(), cq.getChartName()).getId());
		}

		//session data is little bit different from log and network because of max concurrent session data count calculation
		//and more expensive as well.
		List<SessionMetricsChartDTO> data = chartDataService.getSessionMetricsChartData(cq);
		AggregatedSessionData summary = sessionDBService.getAggreateSessionData(cq);
		//sets up the max concurrent session count
		summary = AggregatorUtil.getSessionAggregatedData(data, summary);

		if (chartType.equals(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES)) {	
			ChartDataWrapper<AggregatedSessionData, SessionMetricsChartDTO> wrapped = new ChartDataWrapper<AggregatedSessionData, SessionMetricsChartDTO>();
			wrapped.setChartData(data);
			wrapped.setSummaryData(summary);
			return new JSONWithPadding(new GenericEntity<ChartDataWrapper<AggregatedSessionData, SessionMetricsChartDTO>>(wrapped) {}, callback);
		}
		else {
			List<AttributeValueChartData> chartData = sessionDBService.getCountFromSummarySessionMetrics(cq);
			//calculates the percentage and sets up "other" category for bar chart and pie chart
			chartData = AggregatorUtil.updatePercentage(chartData, chartType);
			ChartDataWrapper<AggregatedSessionData, AttributeValueChartData> wrapped = new ChartDataWrapper<AggregatedSessionData,AttributeValueChartData>();
			wrapped.setChartData(chartData);
			wrapped.setSummaryData(summary);
			return new JSONWithPadding(new GenericEntity<ChartDataWrapper<AggregatedSessionData,AttributeValueChartData>>(wrapped) {}, callback);
		}

	}

	@GET
	@Path("/logChartData/{chartCriteriaId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getLogChartData(			
			@PathParam("chartCriteriaId") Long chartCriteriaId,			
			@DefaultValue(ApigeeMobileAPMConstants.CHART_PERIOD_1HR) @QueryParam("period") String period,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_DATA_REFERENCE_POINT_NOW) @QueryParam("reference") String reference,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES) @QueryParam("chartType") String chartType,
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@DefaultValue("callback") @QueryParam("callback") String callback) throws Exception {
		logger.info("getting log chart data for chartCriteriaId : " + chartCriteriaId +
				" period " + period + " reference" + reference);
		LogChartCriteria cq = logChartCriteriaService.getChartCriteria(chartCriteriaId);
		if (cq == null)
			throw new Exception("Invalid Chart Criteria parameter");
		cq = (LogChartCriteria) ApmUtil.getChartCriteriaWithTimeRange(cq, period, reference);


		if (demoApp) {//update appId and chartCriteriaId of demo app so that queries are done for requested time period but for demo app
			cq.setAppId(getDemoAppId());
			cq.setId(logChartCriteriaService.getDefaultChartCriteriaByName(cq.getAppId(), cq.getChartName()).getId());
		}

		List<LogChartDTO> data = chartDataService.getLogChartData(cq);
		AggregatedLogData summary = AggregatorUtil.getLogAggregatedData(data);
		if (chartType.equalsIgnoreCase(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES)) {			
			ChartDataWrapper<AggregatedLogData, LogChartDTO> wrapped = new ChartDataWrapper<AggregatedLogData, LogChartDTO>();
			wrapped.setChartData(data);
			wrapped.setSummaryData(summary);
			return new JSONWithPadding(new GenericEntity<ChartDataWrapper<AggregatedLogData, LogChartDTO>>(wrapped) {}, callback);
		}
		else {//need to see if it's better to do in DB itself			
			List<AttributeValueChartData> chartData = AggregatorUtil.getRankedAppErrorData(data, chartType);
			ChartDataWrapper<AggregatedLogData,AttributeValueChartData> wrapped = new ChartDataWrapper<AggregatedLogData,AttributeValueChartData>();
			wrapped.setChartData(chartData);
			wrapped.setSummaryData(summary);
			return new JSONWithPadding(new GenericEntity<ChartDataWrapper<AggregatedLogData,AttributeValueChartData>>(wrapped) {}, callback);			
		}
	}

	@GET
	@Path("/crashChartData/{chartCriteriaId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getCrashChartData(			
			@PathParam("chartCriteriaId") Long chartCriteriaId,			
			@DefaultValue(ApigeeMobileAPMConstants.CHART_PERIOD_1HR) @QueryParam("period") String period,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_DATA_REFERENCE_POINT_NOW) @QueryParam("reference") String reference,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES) @QueryParam("chartType") String chartType,
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@DefaultValue("callback") @QueryParam("callback") String callback) throws Exception {
		logger.info("getting crash chart data for chartCriteriaId : " + chartCriteriaId +
				" period " + period + " reference" + reference);
		LogChartCriteria cq = logChartCriteriaService.getChartCriteria(chartCriteriaId);
		if (cq == null)
			throw new Exception("Invalid Chart Criteria parameter");
		cq = (LogChartCriteria) ApmUtil.getChartCriteriaWithTimeRange(cq, period, reference);

		if (demoApp) {//update appId and chartCriteriaId of demo app so that queries are done for requested time period but for demo app
			cq.setAppId(getDemoAppId());
			cq.setId(logChartCriteriaService.getDefaultChartCriteriaByName(cq.getAppId(), cq.getChartName()).getId());
		}
		List<LogChartDTO> data = chartDataService.getLogChartData(cq);
		AggregatedLogData summary = AggregatorUtil.getLogAggregatedData(data);
		if (chartType.equalsIgnoreCase(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES)) {			
			ChartDataWrapper<AggregatedLogData, LogChartDTO> wrapped = new ChartDataWrapper<AggregatedLogData, LogChartDTO>();
			wrapped.setChartData(data);
			wrapped.setSummaryData(summary);
			return new JSONWithPadding(new GenericEntity<ChartDataWrapper<AggregatedLogData, LogChartDTO>>(wrapped) {}, callback);
		}
		else {//need to see if it's better to do in DB itself			
			List<AttributeValueChartData> chartData = AggregatorUtil.getRankedCrashData(data, chartType);
			ChartDataWrapper<AggregatedLogData,AttributeValueChartData> wrapped = new ChartDataWrapper<AggregatedLogData,AttributeValueChartData> ();
			wrapped.setChartData(chartData);
			wrapped.setSummaryData(summary);
			return new JSONWithPadding(new GenericEntity<ChartDataWrapper<AggregatedLogData,AttributeValueChartData>>(wrapped) {}, callback);			
		}
	}


	@GET
	@Path("/networkChartData/{chartCriteriaId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getNetworkChartData(			
			@PathParam("chartCriteriaId") Long chartCriteriaId,			
			@DefaultValue(ApigeeMobileAPMConstants.CHART_PERIOD_1HR) @QueryParam("period") String period,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_DATA_REFERENCE_POINT_NOW) @QueryParam("reference") String reference,
			@DefaultValue(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES) @QueryParam("chartType") String chartType,
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@DefaultValue("callback") @QueryParam("callback") String callback) throws Exception {
		logger.info("getting network chart data for chartCriteriaId : " + chartCriteriaId +
				" period " + period + " reference" + reference);
		MetricsChartCriteria cq = networkChartCriteriaService.getChartCriteria(chartCriteriaId);
		if (cq == null)
			throw new Exception("Invalid Chart Criteria parameter");
		cq = (MetricsChartCriteria) ApmUtil.getChartCriteriaWithTimeRange(cq, period, reference);
		if (demoApp) {//update appId and chartCriteriaId of demo app so that queries are done for requested time period but for demo app
			cq.setAppId(getDemoAppId());
			cq.setId(networkChartCriteriaService.getDefaultChartCriteriaByName(cq.getAppId(), cq.getChartName()).getId());
		}

		List<NetworkMetricsChartDTO> data = chartDataService.getNetworkMetricsChartData(cq);
		AggregatedNetworkData summary = AggregatorUtil.getNetworkAggregatedData(data);

		if (chartType.equalsIgnoreCase(ApigeeMobileAPMConstants.CHART_VIEW_TIMESERIES)) {
			ChartDataWrapper<AggregatedNetworkData, NetworkMetricsChartDTO> wrapped = new ChartDataWrapper<AggregatedNetworkData, NetworkMetricsChartDTO>();
			wrapped.setChartData(data);
			wrapped.setSummaryData(summary);
			return new JSONWithPadding(new GenericEntity< ChartDataWrapper<AggregatedNetworkData, NetworkMetricsChartDTO>>(wrapped) {}, callback);	
		} else {
			List<AttributeValueChartData> avgResponseTime = AggregatorUtil.getRankedAvgResponseTimeData(data,chartType);
			List<NetworkRequestsErrorsChartData> requestErrors = AggregatorUtil.getRankedNetworkRequestsErrorsData(data,chartType);
			NetworkChartDataWrapper wrapped = new NetworkChartDataWrapper ();
			wrapped.setSummaryData(summary);
			wrapped.setResponseTimes(avgResponseTime);
			wrapped.setRequestErrorCounts(requestErrors);
			return new JSONWithPadding(new GenericEntity<NetworkChartDataWrapper>(wrapped) {}, callback);
		}
	}


	/**
	 * 
	 * @param chartCriteriaId This could be any log chartCriteriaId for a given app. I chose to stick with chartCriteriaId instead of instaOpsApplicationId because it allows us
	 * to be consistent with chartData API. C
	 *TODO : Search and filtering is not implemented yet
	 * @param period
	 * @param startRow
	 * @param count
	 * @param callback
	 * @return
	 * @throws Exception
	 */

	//logLevel, logMessage, tag, devicePlatform,deviceOperatingSystem,deviceId, deviceModel
	@GET
	@Path("/logRawData/{chartCriteriaId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getRawLogData (
			@PathParam("chartCriteriaId") Long chartCriteriaId,			
			@DefaultValue("1h") @QueryParam("period") String period,
			@DefaultValue("0") @QueryParam("start") Integer startRow,
			@DefaultValue("25") @QueryParam("rowCount") Integer count,
			@QueryParam("logLevel") String logLevel,
			@QueryParam("tag") String tag,
			@QueryParam("logMessage") String logMessage,
			@QueryParam("deviceModel") String deviceModel,
			@QueryParam("deviceId") String deviceId,
			@QueryParam("devicePlatform") String devicePlatform,
			@QueryParam("deviceOperatingSystem") String deviceOperatingSystem,
			@QueryParam("fixedTime") Long fixedTime, //java timestamp in milliseconds
			@DefaultValue("false")@QueryParam("excludeCrash") Boolean excludeCrash,
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@DefaultValue("callback") @QueryParam("callback") String callback) throws Exception {
		logger.info("Getting raw log data for period " + period + " start " + startRow + " count " + count);
		LogChartCriteria cq = logChartCriteriaService.getChartCriteria(chartCriteriaId);		
		if (cq == null)
			throw new Exception("Invalid Chart Criteria parameter");

		if (demoApp) //everything remains the same just appId changes
			cq.setAppId(getDemoAppId());

		List <ClientLog> data = null;
		LogDBService logDBService = ServiceFactory.getLogDBService();

		if (fixedTime != null && fixedTime > 0) {//if fixedTime is given then other query parameters dont take into effect
			data = logDBService.getRawClientLogForAPointInTime(cq.getAppId(), ApmUtil.getMinuteFromTime(fixedTime), count);
			return new JSONWithPadding(new GenericEntity<List<ClientLog>>(data) {}, callback);
		}

		//We only show last x hours of raw data as of now. Compare option does not apply here.		
		cq = (LogChartCriteria) ApmUtil.getChartCriteriaWithTimeRange(cq, period, ApigeeMobileAPMConstants.CHART_DATA_REFERENCE_POINT_NOW);
		cq.setDeviceModel(deviceModel);
		cq.setDeviceId(deviceId);
		cq.setDevicePlatform(devicePlatform);
		cq.setDeviceOS(deviceOperatingSystem);

		LogRawCriteria rcq = new LogRawCriteria(cq);
		rcq.setStartRow(startRow);
		rcq.setRowCount(count);
		rcq.setLogMessage(logMessage);
		rcq.setLogLevel(logLevel);
		rcq.setTag(tag); 
		if(tag != null && !tag.isEmpty()) //if there is non empty value for tag then we would overlook excludeCrash parameter
			rcq.setExcludeCrash(excludeCrash);

		data = logDBService.getRawLogData(rcq);
		return new JSONWithPadding(new GenericEntity<List<ClientLog>>(data) {}, callback);
	}

	/**
	 * 
	 * @param chartCriteriaId This could be any log chartCriteriaId for a given app. I chose to stick with chartCriteriaId instead of instaOpsApplicationId because it allows us
	 * to be consistent with chartData API.
	 * TODO: Search and filtering is not implemented yet
	 * @param period
	 * @param startRow
	 * @param count
	 * @param callback
	 * @return
	 * @throws Exception
	 */

	//crashSummary,devicePlatform,deviceOperatingSystem,deviceId, deviceModel
	@GET
	@Path("/crashRawData/{chartCriteriaId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getRawCrashData (
			@PathParam("chartCriteriaId") Long chartCriteriaId,			
			@DefaultValue("1h") @QueryParam("period") String period,
			@DefaultValue("0") @QueryParam("start") Integer startRow,
			@DefaultValue("25") @QueryParam("rowCount") Integer count,			
			@QueryParam("crashSummary") String crashSummary,
			@QueryParam("deviceModel") String deviceModel,
			@QueryParam("deviceId") String deviceId,
			@QueryParam("devicePlatform") String devicePlatform,
			@QueryParam("deviceOperatingSystem") String deviceOperatingSystem,
			@QueryParam("fixedTime") Long fixedTime, //java timestamp in milliseconds
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@DefaultValue("callback") @QueryParam("callback") String callback) throws Exception {
		logger.info("Getting raw log data for period " + period + " start " + startRow + " count " + count);
		LogChartCriteria cq = logChartCriteriaService.getChartCriteria(chartCriteriaId);		
		if (cq == null)
			throw new Exception("Invalid Chart Criteria parameter");

		if (demoApp) //everything remains the same just appId changes
			cq.setAppId(getDemoAppId());

		List <CrashLogDetails> data = null;
		CrashLogDBService crashDBService = ServiceFactory.getCrashLogDBService();
		if (fixedTime != null && fixedTime > 0) {//if fixedTime is given then other query parameters dont take into effect
			data = crashDBService.getRawCrashLogsForAPointInTime(cq.getAppId(), ApmUtil.getMinuteFromTime(fixedTime), count);
			return new JSONWithPadding(new GenericEntity<List<CrashLogDetails>>(data) {}, callback);
		}

		//We only show last x hours of raw data as of now. Compare option does not apply here.		
		cq = (LogChartCriteria) ApmUtil.getChartCriteriaWithTimeRange(cq, period, ApigeeMobileAPMConstants.CHART_DATA_REFERENCE_POINT_NOW);
		cq.setDeviceModel(deviceModel);
		cq.setDeviceId(deviceId);
		cq.setDevicePlatform(devicePlatform);
		cq.setDeviceOS(deviceOperatingSystem);

		CrashRawCriteria rcq = new CrashRawCriteria(cq);
		rcq.setCrashSummary(crashSummary);
		rcq.setStartRow(startRow);
		rcq.setRowCount(count);

		data = crashDBService.getCrashLogs(rcq);
		return new JSONWithPadding(new GenericEntity<List<CrashLogDetails>>(data) {}, callback);
	}

	/**
	 * 
	 * @param chartCriteriaId This could be any chartCriteriaId for a given app. I chose to stick with chartCriteriaId instead of instaOpsApplicationId because it allows us
	 * to be consistent with chartData API.
	 * @param period
	 * @param startRow
	 * @param count
	 * @param callback
	 * @return
	 * @throws Exception
	 */
	//url, networkType, networkCarrier, devicePlatform,deviceOperatingSystem,deviceId, deviceModel
	@GET
	@Path("/networkRawData/{chartCriteriaId}")
	@Produces("application/x-javascript")
	public JSONWithPadding getRawNetworkData (
			@PathParam("chartCriteriaId") Long chartCriteriaId,			
			@DefaultValue("1h") @QueryParam("period") String period,
			@DefaultValue("0") @QueryParam("start") Integer startRow,
			@DefaultValue("25") @QueryParam("rowCount") Integer count,
			@QueryParam("url") String url,
			@QueryParam("httpStatusCode") Long httpStatusCode,
			@QueryParam("networkCarrier") String networkCarrier,
			@QueryParam("networkType") String networkType,
			@QueryParam("deviceModel") String deviceModel,
			@QueryParam("deviceId") String deviceId,
			@QueryParam("devicePlatform") String devicePlatform,
			@QueryParam("deviceOperatingSystem") String deviceOperatingSystem,
			@QueryParam("latency") Long latency,
			@QueryParam("fixedTime") Long fixedTime, //java timestamp in milliseconds
			@DefaultValue("false") @QueryParam("demoApp") Boolean demoApp,
			@DefaultValue("callback") @QueryParam("callback") String callback) throws Exception {
		logger.info("Getting raw network data for period " + period + " start " + startRow + " count " + count);
		MetricsChartCriteria cq = networkChartCriteriaService.getChartCriteria(chartCriteriaId);		
		if (cq == null)
			throw new Exception("Invalid Chart Criteria parameter");

		if (demoApp) //everything remains the same just appId changes
			cq.setAppId(getDemoAppId());

		NetworkMetricsDBService networkDBService = ServiceFactory.getMetricsDBServiceInstance();
		List <ClientNetworkMetrics> data = null;
		if (fixedTime != null && fixedTime > 0) {//if fixedTime is given then other query parameters dont take into effect
			data = networkDBService.getRawNetworkMetricsDataForAPointInTime(cq.getAppId(), ApmUtil.getMinuteFromTime(fixedTime), count);
			return new JSONWithPadding(new GenericEntity<List<ClientNetworkMetrics>>(data) {}, callback);
		}
		//We only show last x hours of raw data as of now. Compare option does not apply here.
		cq = (MetricsChartCriteria) ApmUtil.getChartCriteriaWithTimeRange(cq, period, ApigeeMobileAPMConstants.CHART_DATA_REFERENCE_POINT_NOW);
		cq.setDeviceModel(deviceModel);
		cq.setDeviceId(deviceId);
		cq.setDevicePlatform(devicePlatform);
		cq.setDeviceOS(deviceOperatingSystem);
		cq.setNetworkCarrier(networkCarrier);
		cq.setNetworkType(networkType);
		cq.setUrl(url);
		if(latency != null && latency > 0)
			cq.setUpplerLatency(latency);
		NetworkMetricsRawCriteria rcq = new NetworkMetricsRawCriteria(cq);
		if (httpStatusCode != null && httpStatusCode > 0)
			rcq.setHttpStatusCode(httpStatusCode);
		rcq.setStartRow(startRow);
		rcq.setRowCount(count);

		data = networkDBService.getRawNetworkMetricsData(rcq);
		return new JSONWithPadding(new GenericEntity<List<ClientNetworkMetrics>>(data) {}, callback);
	}

	public Long getDemoAppId() throws Exception {
		App demoApp = appService.getDemoApp();
		if (demoApp != null)
			return demoApp.getInstaOpsApplicationId();
		else
			throw new Exception("Trying to get data for Demo App when such app does not exist");
	}

}
