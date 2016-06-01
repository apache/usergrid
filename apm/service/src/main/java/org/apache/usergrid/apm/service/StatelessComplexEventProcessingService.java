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
package org.apache.usergrid.apm.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.conf.MBeansOption;
import org.drools.io.ResourceFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.drools.time.SessionClock;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ClientMetricsEnvelope;
import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartCriteriaService;
import org.apache.usergrid.apm.model.CompactClientLog;
import org.apache.usergrid.apm.model.CompactNetworkMetrics;
import org.apache.usergrid.apm.model.CompactSessionMetrics;
import org.apache.usergrid.apm.model.SummarySessionMetrics;

public class StatelessComplexEventProcessingService {


	private static final String[] ASSET_FILES = {
		"/rules/StatelessActiveSessionsProcessor.drl", 
		"/rules/StatelessActiveSessionsByApplicationVersionProcessor.drl",
		"/rules/StatelessActiveSessionsByApplicationConfigTypeProcessor.drl",
		"/rules/StatelessActiveSessionsByDeviceModelProcessor.drl",
		"/rules/StatelessActiveSessionsByDevicePlatformProcessor.drl",
		"/rules/StatelessActiveSessionsByOSVersionProcessor.drl",
		"/rules/AppErrorsProcessor.drl",
		"/rules/AppErrorsByApplicationVersionProcessor.drl",
		"/rules/AppErrorsByApplicationConfigProcessor.drl",
		"/rules/AppErrorsByDeviceModelProcessor.drl",
		"/rules/AppErrorsByDevicePlatformProcessor.drl", 
		"/rules/AppErrorsByOSVersionProcessor.drl",
		"/rules/NetworkMetricsProcessor.drl",		
		"/rules/NetworkMetricsProcessorByNetworkType.drl",
		"/rules/NetworkMetricsProcessorByCarrier.drl",
		"/rules/NetworkMetricsProcessorByOSVersion.drl",
		"/rules/NetworkMetricsProcessorByPlatform.drl",
		"/rules/NetworkMetricsProcessorByDomain.drl",
		"/rules/AppErrorsAlarmEngine.drl"
	};
	/**Disabled for now since these chart are not set visible by default and will reenable them if there are enough requests
	"/rules/NetworkMetricsPoorPerformanceProcessor.drl",
	"/rules/NetworkMetricsProcessorByAppVersion.drl",
	"/rules/NetworkMetricsProcessorByConfigType.drl",
	"/rules/NetworkMetricsProcessorPoorPerfByAppVersion.drl",
	"/rules/NetworkMetricsProcessorPoorPerfByNetworkType.drl",
	"/rules/NetworkMetricsProcessorPoorPerfByCarrier.drl",
	"/rules/NetworkMetricsProcessorPoorPerfByConfigType.drl",
	 */




	private static final Log log = LogFactory.getLog(StatelessComplexEventProcessingService.class);
	private static final int SESSION_EXPIRY_TIME = -90; //Session expiry time in minutes


	NetworkMetricsDBService networkMetricsDBService = ServiceFactory.getMetricsDBServiceInstance();
	LogDBService logDBService = ServiceFactory.getLogDBService();
	SessionDBService sessionDBService = ServiceFactory.getSessionDBService(); 

	private KnowledgeBase kbase;
	ClockTypeOption clockTypeOption;

	public StatelessComplexEventProcessingService(ClockTypeOption clockTypeOption)
	{
		this.clockTypeOption = clockTypeOption;
		kbase = loadRuleBase();
	}

	public NetworkMetricsDBService getMetricsDBService() {
		return networkMetricsDBService;
	}

	public void setMetricsDBService(NetworkMetricsDBService networkMetricsDBService) {
		this.networkMetricsDBService = networkMetricsDBService;
	}

	public void processEvents(Long appId, String fullAppName, List<ClientMetricsEnvelope> messages)
	{
		long cepStart = System.currentTimeMillis();
		StatefulKnowledgeSession session = createSession(clockTypeOption);
		processEvents(appId, fullAppName, messages,session);
		long cepEnd = System.currentTimeMillis();
		if ((cepEnd - cepStart) > 30000)
			log.error("It's taken more than 30 seconds to do CEP for payloads for app " + fullAppName + ". This could result into more than " +
					"once processing of same payload batch");
	}

	public void processEvents(Long appId, String fullAppName, List<ClientMetricsEnvelope> messages, StatefulKnowledgeSession session)
	{       

		WorkingMemoryEntryPoint clientSessionMetricsStream;
		WorkingMemoryEntryPoint clientLogStream;
		WorkingMemoryEntryPoint clientNetworkMetricsStream;
		WorkingMemoryEntryPoint summarySessionMetricsStream;


		WorkingMemoryEntryPoint compactSessionMetricsStream;
		WorkingMemoryEntryPoint compactClientLogStream;
		WorkingMemoryEntryPoint compactNetworkMetricsStream;
		WorkingMemoryEntryPoint modifiedMinutesStream;


		//this.WebServiceMetricsBeanMessageEnvelopeStream = this.session.getWorkingMemoryEntryPoint( "WebServiceMetricsBeanMessageEnvelopeStream" );

		clientLogStream = session.getWorkingMemoryEntryPoint("clientLogStream");
		compactClientLogStream = session.getWorkingMemoryEntryPoint("compactClientLogStream");

		clientSessionMetricsStream = session.getWorkingMemoryEntryPoint("clientSessionMetricsStream");
		summarySessionMetricsStream = session.getWorkingMemoryEntryPoint("summarySessionMetricsStream");
		compactSessionMetricsStream = session.getWorkingMemoryEntryPoint("compactSessionMetricsStream");
		modifiedMinutesStream = session.getWorkingMemoryEntryPoint("modifiedMinutesStream");

		clientNetworkMetricsStream = session.getWorkingMemoryEntryPoint("clientNetworkMetricsStream");
		compactNetworkMetricsStream = session.getWorkingMemoryEntryPoint("compactNetworkMetricsStream");

		List<SummarySessionMetrics> dirtySummarySessionMetrics = new ArrayList<SummarySessionMetrics>();	
		List<CompactClientLog> dirtyCompactClientLog = new ArrayList<CompactClientLog>();
		List<CompactSessionMetrics> dirtyCompactSessionMetrics = new ArrayList<CompactSessionMetrics>();
		List<CompactNetworkMetrics> dirtyCompactNetworkMetrics = new ArrayList<CompactNetworkMetrics>();

		try {

			//session.setGlobal("newOrUpdatedHourlyMetrics", newOrUpdatedHourlyMetrics);

			session.setGlobal("dirtySummarySessionMetrics", dirtySummarySessionMetrics);
			session.setGlobal("dirtyCompactClientLog", dirtyCompactClientLog);
			session.setGlobal("dirtyCompactSessionMetrics", dirtyCompactSessionMetrics);
			session.setGlobal("dirtyCompactNetworkMetrics", dirtyCompactNetworkMetrics);

			session.setGlobal("networkMetricsChartCriteriaService", ServiceFactory.getNetworkMetricsChartCriteriaService());
			session.setGlobal("sessionChartCriteriaService", ServiceFactory.getSessionChartCriteriaService());
			session.setGlobal("logChartCriteriaService", ServiceFactory.getLogChartCriteriaService());
			session.setGlobal("alarmService", ServiceFactory.getAlarmService());


			//session.setGlobal("sessionDBService", sessionDBService);

			session.setGlobal("appId", appId);

			Date currentTimeStamp;

			SessionClock clock = session.getSessionClock();
			currentTimeStamp = new Date(clock.getCurrentTime());

			log.info("Inserting facts at time : " + currentTimeStamp);

			long logCount = 0;

			List<String> list = new ArrayList<String>();
			list.add("hello");

			//session.setGlobal("test",list);

			List<ClientSessionMetrics> clientSessionMetrics = new ArrayList<ClientSessionMetrics>();

			for(ClientMetricsEnvelope message : messages)
			{	
				if (clientLogStream != null) //this could be null if there is no rule file using this stream I think
				{
					if (message.getLogs() != null)
					{
						for(ClientLog clientLog: message.getLogs())
						{
							clientLogStream.insert(clientLog);
							logCount++;
						}
					}
				}

				if(message.getSessionMetrics() != null)
				{
					//clientSessionMetricsStream.insert(message.getSessionMetrics());
					clientSessionMetrics.add(message.getSessionMetrics());
				}

				if (message.getMetrics() != null) {
					for (ClientNetworkMetrics networkMetrics: message.getMetrics()) {
						//Exclude really large from avg latency calculation since it skews the calculation
						//These are still available with raw API call search so they will show up
						if (networkMetrics.getLatency() > 0 && networkMetrics.getLatency() < NetworkMetricsChartCriteriaService.LATENCY_OUTLIER_THRESHOLD) {
							clientNetworkMetricsStream.insert(networkMetrics);						
						} else {
							log.warn ("Excluding Client Network Metrics with really bad latency from aggregation " + networkMetrics.toString());
						}
					}					
				}

			}

			Set<Long> modifiedMinutes = new TreeSet<Long>(); 

			calculateSummarySessionMetrics2(appId,fullAppName, clientSessionMetrics,dirtySummarySessionMetrics,modifiedMinutes);

			if (summarySessionMetricsStream != null) {
				for(SummarySessionMetrics sessionMetrics : dirtySummarySessionMetrics)
				{
					summarySessionMetricsStream.insert(sessionMetrics);
				}
			}
			if (modifiedMinutesStream != null) {

				for(Long m : modifiedMinutes)
				{
					//log.info("mod min" + m);
					modifiedMinutesStream.insert(m);
				}
			}
			log.info("Total number of modifiedMinutes for app " + fullAppName + " is " + modifiedMinutes.size());

			//session.setGlobal("modifiedMinutes", modifiedMinutes);

			log.info("Inserted ClientLog Count : " + logCount);

			session.fireAllRules();
			log.info("Finished Processing events : ");

			if(clientLogStream != null)
				log.info("Client clientLogStream Size :" + clientLogStream.getFactCount());
			if(summarySessionMetricsStream != null)
				log.info("Client summarySessionMetricsStream Size :" + summarySessionMetricsStream.getFactCount());
			if(compactSessionMetricsStream != null)
				log.info("Client compactSessionMetricsStream Size :" + compactSessionMetricsStream.getFactCount());


			log.info("Persisting dirty CompactClientLog records. Size : " + dirtyCompactClientLog.size());
			logDBService.saveCompactLogs(dirtyCompactClientLog);

			log.info("Persisting dirty SummarySessionMetrics records. Size : " + dirtySummarySessionMetrics.size());
			sessionDBService.saveSummarySessionMetricsInBatch(dirtySummarySessionMetrics);

			log.info("Persisting dirty CompactSessionMetrics records. Size : " + dirtyCompactSessionMetrics.size());
			sessionDBService.saveCompactSessionMetrics(dirtyCompactSessionMetrics);

			log.info ("Persisting dirty ClientNetworkMetrics. Size " + dirtyCompactNetworkMetrics.size());
			networkMetricsDBService.saveCompactNetworkMetricsInBatch(dirtyCompactNetworkMetrics);
			session.dispose();
			dirtyCompactClientLog  = null;
			dirtySummarySessionMetrics = null;
			dirtyCompactSessionMetrics = null;
			dirtyCompactNetworkMetrics = null;

		} catch ( Exception e ) {
			log.error("Error in complex event processing for app " + fullAppName, e);
			//e.printStackTrace();

		}	

	}


	public StatefulKnowledgeSession createSession()
	{
		return createSession(clockTypeOption);
	}

	private StatefulKnowledgeSession createSession(ClockTypeOption clockTypeOption) {
		KnowledgeSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();

		conf.setOption( clockTypeOption );
		StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession(conf,null);
		//Added a logger
		session.setGlobal( "log",  LogFactory.getLog("RulesEngine"));
		log.info("Finishing session creation");
		return session;
	}


	private KnowledgeBase loadRuleBase() {
		KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		try {
			for( int i = 0; i < ASSET_FILES.length; i++ ) {
				builder.add( ResourceFactory.newInputStreamResource( StatelessComplexEventProcessingService.class.getResourceAsStream( ASSET_FILES[i] ) ),
						ResourceType.determineResourceType( ASSET_FILES[i] ));
			}
		} catch ( Exception e ) {
			log.fatal("Problem loading rules ", e);
			//System.exit( 0 );
		}
		if( builder.hasErrors() ) {
			log.fatal("Problem loading rules ");
			//System.exit( 0 );
		}
		KnowledgeBaseConfiguration conf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
		conf.setOption( EventProcessingOption.CLOUD );
		conf.setOption( MBeansOption.ENABLED );
		//conf.setOption( MultithreadEvaluationOption.YES );
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase( "Traffic Detection", conf ); 
		kbase.addKnowledgePackages( builder.getKnowledgePackages() );
		return kbase;
	}


	public class TempClientSessionMetrics
	{
		public ClientSessionMetrics firstClientSessionMetric;
		public ClientSessionMetrics lastClientSessionMetric;
		public SummarySessionMetrics ssm;
		public Boolean matchExisted = false;
	}

	/*
	 * Pseudocode : 
		1. Group Client Session metrics by sessionId
		2. For each CSM grouping, find CSM with latest timestamp.
		3. Query DB for matching Sessions
		4a. If no match exists, create a new session
    	4b.	If match exists, but match's end time >= to the end time of the CSM being ingested, do nothing
    	4c.	If the match exists, and the match's end time < to the end time of the CSM being ingested, extend the session.
	 * 
	 * 
	 * 
	 */

	public void calculateSummarySessionMetrics2(Long appId, String fullAppName, List<ClientSessionMetrics> clientSessionMetrics, 
			List<SummarySessionMetrics> dirtySummarySessionMetrics, Set<Long> modifiedMinutes)
	{

		// Empty metrics checker = should not be happening in production
		if(clientSessionMetrics.size() == 0)
		{
			log.error("Something is very odd. Skipping Summary Session Metrics calculation since there is no clientSessionMetrics for app : " + fullAppName);
			return;
		}


		/*For the purpose of backwards compatibility, if sessionId is null, then setting the session id to something meaningful

		for(ClientSessionMetrics csm: clientSessionMetrics)
		{
			if(csm.getSessionId() == null && csm.getDeviceId() != null)
			{
				log.warn("Encountered metrics data for appId " +  appId + " where sessionId was not populated. This indicates potentially usage of an old client : " + csm.getDeviceId() );
				String fakeSessionId = csm.getDeviceId().toString() + "_" + csm.getEndHour().toString();
				csm.setSessionId(fakeSessionId);
			}
		} */

		// Group Client Session metrics by sessionId since there could be different messgaes from same device for the same session
		Function<ClientSessionMetrics, String> sessionFunction = new Function<ClientSessionMetrics, String>()
				{
			@Override
			public String apply(@Nullable ClientSessionMetrics arg0) {
				return arg0.getSessionId();
			}

				};

				ImmutableListMultimap<String, ClientSessionMetrics> csmBySessionId = Multimaps.index(clientSessionMetrics,sessionFunction);


				// 2. For each CSM grouping, find CSM with latest timestamp.
				Comparator<ClientSessionMetrics> endMinuteComparator = new Comparator<ClientSessionMetrics>() {
					@Override
					public int compare(ClientSessionMetrics csm0, ClientSessionMetrics csm1) {
						return csm0.getEndMinute().compareTo(csm1.getEndMinute());

					}
				};


				Map<String,ClientSessionMetrics> latestCSM = new HashMap<String,ClientSessionMetrics>();

				for(String sessionId : csmBySessionId.keySet())
				{
					ClientSessionMetrics latestClientSessionMetric = Collections.max(csmBySessionId.get(sessionId),endMinuteComparator);
					latestCSM.put(sessionId, latestClientSessionMetric);
				}


				//3. Query DB for matching Sessions. 
				//TODO: This potentially could be slow for a high volume app. Need to investigate better approach
				log.info ("Querying summary session table for appId " + fullAppName + " for " + csmBySessionId.keySet().size() + " sessions");
				List<SummarySessionMetrics> matchingSSM 
				= sessionDBService.getSummarySessionsBySessionId(appId, csmBySessionId.keySet());
				log.info("Found " + matchingSSM.size() + " pre-existing sessions in database for app " + fullAppName);

				//		4a. If no match exists, create a new summary session
				//   	4b.	If match exists, but match's end time >= to the end time of the CSM being ingested, do nothing
				//   	4c.	If the match exists, and the match's end time < to the end time of the CSM being ingested, extend the session.

				// Group Summary Session metrics by sessionId
				//Todo : since matchingSSM has distinct summarySessions, following function may not be needed..confirm
				Function<SummarySessionMetrics, String> sessionFunction2 = new Function<SummarySessionMetrics, String>()
						{
					@Override
					public String apply(@Nullable SummarySessionMetrics arg0) {
						return arg0.getSessionId();
					}

						};

						ImmutableListMultimap<String, SummarySessionMetrics> ssmBySessionId = Multimaps.index(matchingSSM,sessionFunction2);

						//ssmBySessionId.keySet();

						//Find new Sessions
						Set<String> newSessions = Sets.difference(latestCSM.keySet(), ssmBySessionId.keySet());
						log.info("Found " + newSessions.size() + " new sessions for app " + fullAppName);
						//Create new SSM

						for(String sessionId : newSessions)
						{
							SummarySessionMetrics ssm = createSummarySessionMetrics(latestCSM.get(sessionId));
							modifiedMinutes.add(ssm.getEndMinute());							
							dirtySummarySessionMetrics.add(ssm);
						}


						//Update SSM
						for(SummarySessionMetrics ssm : matchingSSM)
						{
							if(latestCSM.containsKey(ssm.getSessionId()))
							{
								if (latestCSM.get(ssm.getSessionId()).getEndMinute() > ssm.getEndMinute())
								{
									ssm.setPrevSessionEndTime(ssm.getSessionEndTime());
									ssm.setSessionEndTime(latestCSM.get(ssm.getSessionId()).getTimeStamp());
									ssm.setEndBatteryLevel(latestCSM.get(ssm.getSessionId()).getBatteryLevel());
									addModifiedMinutes(modifiedMinutes, ssm);
									dirtySummarySessionMetrics.add(ssm);
								} else
								{
									log.warn("Got a message that was out of order or the data" +
											"has been within the same minute for app " +  ssm.getFullAppName() + " and SessiondId " + ssm.getSessionId());
								}
							} else
							{
								log.warn("Matching Summary Session Metrics didn't actually find a match for app " +  ssm.getFullAppName() + " SessionId :" + ssm.getSessionId());
							}
						}

	}

	private void addModifiedMinutes(Set<Long> modifiedMinutes, SummarySessionMetrics ssm)
	{

		//if we get data after 5 min, we will assume that it's a new session. Ideally this should be happening on SDK but until we get that 
		//fixed, we will have it here.
		long minSincelastSessionData = ssm.getEndMinute() - ssm.getPrevEndMinute(); 
		if (minSincelastSessionData > 5) {
			log.warn("new data for for app " +  ssm.getFullAppName() + " for session came after 5 min. Not back-calculating dirty minutes in this case. Simply marking latest minute as dirty");
			modifiedMinutes.add(ssm.getEndMinute());
		}
		else {
			
			for(long i = ssm.getPrevEndMinute(); i <= ssm.getEndMinute(); i++)
			{
				modifiedMinutes.add(i);
			}
		}
	}


	private SummarySessionMetrics createSummarySessionMetrics(ClientSessionMetrics clientSessionMetrics)
	{
		SummarySessionMetrics ssm = new SummarySessionMetrics();
		ssm.setFullAppName(clientSessionMetrics.getFullAppName());
		ssm.setSessionId(clientSessionMetrics.getSessionId());
		ssm.setSessionStartTime(clientSessionMetrics.getSessionStartTime());
		ssm.setSessionEndTime(clientSessionMetrics.getTimeStamp());
		ssm.setAppId(clientSessionMetrics.getAppId());
		ssm.setDeviceId(clientSessionMetrics.getDeviceId());
		ssm.setDevicePlatform(clientSessionMetrics.getDevicePlatform());
		ssm.setDeviceModel(clientSessionMetrics.getDeviceModel());
		ssm.setDeviceOperatingSystem(clientSessionMetrics.getDeviceOperatingSystem());
		ssm.setDeviceType(clientSessionMetrics.getDeviceType());
		ssm.setDeviceCountry(clientSessionMetrics.getDeviceCountry());

		ssm.setStartBatteryLevel(clientSessionMetrics.getBatteryLevel());
		ssm.setEndBatteryLevel(clientSessionMetrics.getBatteryLevel());

		ssm.setApplicationVersion(clientSessionMetrics.getApplicationVersion());
		ssm.setAppConfigType(clientSessionMetrics.getAppConfigType());


		ssm.setLocalCountry(clientSessionMetrics.getLocalCountry());
		ssm.setLocalLanguage(clientSessionMetrics.getLocalLanguage());

		ssm.setNetworkCountry(clientSessionMetrics.getNetworkCountry());
		ssm.setNetworkCarrier(clientSessionMetrics.getNetworkCarrier());
		ssm.setNetworkType(clientSessionMetrics.getNetworkType());		
		ssm.setPrevSessionEndTime(clientSessionMetrics.getTimeStamp());
		
		log.info("Created new SSM " + ssm.toString());

		return ssm;
	}


}
