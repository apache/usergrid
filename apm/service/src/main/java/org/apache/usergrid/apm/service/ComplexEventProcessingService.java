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
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.FactHandle;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.io.ResourceFactory;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.drools.time.SessionClock;

import org.apache.usergrid.apm.model.ActiveURLs;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ClientMetricsEnvelope;
import org.apache.usergrid.apm.model.CompactClientLog;
import org.apache.usergrid.apm.model.CompactNetworkMetrics;
import org.apache.usergrid.apm.model.CompactSessionMetrics;
import org.apache.usergrid.apm.model.SummarySessionMetrics;

public class ComplexEventProcessingService {

//	private static final String[] ASSET_FILES = { "/rules/httptraffic.drl","/rules/ClientLogAlarmProcessor.drl","/rules/HttpMetricsProcessor.drl"};

//private static final String[] ASSET_FILES = {"/rules/ClientLogMetricsCreator.drl"};
	
	//private static final String[] ASSET_FILES = {"/rules/ClientLogMetricsCreator.drl","/rules/SessionProcessor.drl"};
	
	//private static final String[] ASSET_FILES = {"/rules/SessionProcessor.drl"};
	
	//private static final String[] ASSET_FILES = {"/rules/ClientLogMetricsProcessor.drl"};
	
	private static final String[] ASSET_FILES = {
		"/rules/SessionProcessor.drl",
		"/rules/ActiveSessionsProcessor.drl", 
		"/rules/ActiveSessionsByApplicationVersionProcessor.drl",
		"/rules/ActiveSessionsByApplicationConfigTypeProcessor.drl",
		"/rules/ActiveSessionsByDeviceModelProcessor.drl",
		"/rules/ActiveSessionsByDevicePlatformProcessor.drl",
		"/rules/AppErrorsProcessor.drl",
		"/rules/AppErrorsByApplicationVersionProcessor.drl",
		"/rules/AppErrorsByApplicationConfigProcessor.drl",
		"/rules/AppErrorsByDeviceModelProcessor.drl",
		"/rules/AppErrorsByDevicePlatformProcessor.drl"};
	
/*	
	private static final String[] ASSET_FILES = {
		"/rules/SessionProcessor.drl",
		"/rules/ActiveSessionsProcessor.drl"};

		"/rules/ActiveSessionsByApplicationVersionProcessor.drl",
		"/rules/ActiveSessionsByApplicationConfigTypeProcessor.drl",
		"/rules/ActiveSessionsByDeviceModelProcessor.drl",
		"/rules/ActiveSessionsByDevicePlatformProcessor.drl",
		"/rules/AppErrorsProcessor.drl",
		"/rules/AppErrorsByApplicationVersionProcessor.drl",
		"/rules/AppErrorsByApplicationConfigProcessor.drl",
		"/rules/AppErrorsByDeviceModelProcessor.drl",
		"/rules/AppErrorsByDevicePlatformProcessor.drl"};	
	*/
	

	private static final Log log = LogFactory.getLog(ComplexEventProcessingService.class);
	
	private StatefulKnowledgeSession session;


    private WorkingMemoryEntryPoint clientNetworkMetricsStream;
    private WorkingMemoryEntryPoint clientLogStream;
    private WorkingMemoryEntryPoint clientSessionMetricsStream;
    private WorkingMemoryEntryPoint summarySessionMetricsStream;


	private WorkingMemoryEntryPoint compactSessionMetricsStream;
    private WorkingMemoryEntryPoint compactClientLogStream;
    
    /**
     * 
     */
    NetworkMetricsDBService networkMetricsDBService = ServiceFactory.getMetricsDBServiceInstance();
    LogDBService logDBService = ServiceFactory.getLogDBService();
    SessionDBService sessionDBService = ServiceFactory.getSessionDBService(); 

    ClockTypeOption clockTypeOption;
	
	public ComplexEventProcessingService(ClockTypeOption clockTypeOption)
	{
		this.clockTypeOption = clockTypeOption;
		session = createSession(clockTypeOption);
		//this.WebServiceMetricsBeanMessageEnvelopeStream = this.session.getWorkingMemoryEntryPoint( "WebServiceMetricsBeanMessageEnvelopeStream" );
		this.clientNetworkMetricsStream = this.session.getWorkingMemoryEntryPoint("clientNetworkMetricsStream");
		this.clientLogStream = this.session.getWorkingMemoryEntryPoint("clientLogStream");
		this.compactClientLogStream = this.session.getWorkingMemoryEntryPoint("compactClientLogStream");
		this.clientSessionMetricsStream = this.session.getWorkingMemoryEntryPoint("clientSessionMetricsStream");
		this.summarySessionMetricsStream = this.session.getWorkingMemoryEntryPoint("summarySessionMetricsStream");
		this.compactSessionMetricsStream = this.session.getWorkingMemoryEntryPoint("compactSessionMetricsStream");
	}
	
	public NetworkMetricsDBService getMetricsDBService() {
		return networkMetricsDBService;
	}

	public void setMetricsDBService(NetworkMetricsDBService networkMetricsDBService) {
		this.networkMetricsDBService = networkMetricsDBService;
	}

	public void processEvents(List<ClientMetricsEnvelope> messages)
	{
		//List<CompactNetworkMetrics> newOrUpdatedHourlyMetrics = new ArrayList<CompactNetworkMetrics>();
		
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
			
			
			Date currentTimeStamp;
			
			SessionClock clock = this.getSession().getSessionClock();
			currentTimeStamp = new Date(clock.getCurrentTime());
			
			//session.setGlobal("currentTimeInMin", new Long((currentTimeStamp.getTime() /1000 / 60) -1) );
			
			long expiryMin = (currentTimeStamp.getTime() /1000 / 60) - 30;
			
			if(clientLogStream != null)
			{
				
				for(Object o: this.clientLogStream.getObjects())
				{
					if(o.getClass().equals(ClientLog.class))
					{
						ClientLog log = (ClientLog) o;
						if (log.getEndMinute() < expiryMin)
						{
							this.clientLogStream.retract(this.clientLogStream.getFactHandle(o));
						}
					}
				}
			}			
			
			log.info("Inserting facts at time : " + currentTimeStamp);
			
			long logCount = 0;
			
			
			for(ClientMetricsEnvelope message : messages)
			{	
				if (message.getLogs() != null)
				{
					for(ClientLog clientLog: message.getLogs())
					{
						if(!clockTypeOption.equals(ClockTypeOption.get("pseudo")))
							clientLog.setCorrectedTimestamp(currentTimeStamp);
						else
							clientLog.setCorrectedTimestamp(clientLog.getTimeStamp());
						this.clientLogStream.insert(clientLog);
						logCount++;
					}
				}
				//Inserting the session metrics
				
				if (message.getSessionMetrics() != null)
					this.clientSessionMetricsStream.insert(message.getSessionMetrics());
			}
			
			
			log.info("ClientLog Count : " + logCount);
			session.fireAllRules();
			log.info("Finished Processing events : ");
			
			if(clientLogStream != null)
			log.info("Client clientLogStream Size :" + clientLogStream.getFactCount());
			if(summarySessionMetricsStream != null)
			log.info("Client summarySessionMetricsStream Size :" + summarySessionMetricsStream.getFactCount());
			if(compactSessionMetricsStream != null)
			log.info("Client compactSessionMetricsStream Size :" + compactSessionMetricsStream.getFactCount());

			//log.info("Fact Count :" + session.getFactCount());
			//log.info("WebServiceMetricsBeanMessageEnvelopeStream Count: " + WebServiceMetricsBeanMessageEnvelopeStream.getFactCount());
			//log.info("ClientMetricsStream Count: " + ClientNetworkMetrics.getFactCount());
			
			log.info("Persisting dirty CompactClientLog records. Size : " + dirtyCompactClientLog.size());
			logDBService.saveCompactLogs(dirtyCompactClientLog);
			
			log.info("Persisting dirty SummarySessionMetrics records. Size : " + dirtySummarySessionMetrics.size());
			sessionDBService.saveSummarySessionMetricsInBatch(dirtySummarySessionMetrics);
			
			log.info("Persisting dirty CompactSessionMetrics records. Size : " + dirtyCompactSessionMetrics.size());
			sessionDBService.saveCompactSessionMetrics(dirtyCompactSessionMetrics);
			

        } catch ( Exception e ) {
        	log.error("Error in complex event processing processing of ClientMetricsEnvelope: " + e);
        }
        
        // Now persist the metrics into the HourlyMetricsDAO
        
        
	}
	
	
	public class ExpiryTime 
	{
		Long time;
	}
	
	private ExpiryTime et;
	private FactHandle etFactHandle;
	
	//Note : We will need a way to either control carefully the clock..... Need to think 
	// about this a lot more.
	private StatefulKnowledgeSession createSession(ClockTypeOption clockTypeOption) {
        KnowledgeBase kbase = loadRuleBase();
        
        KnowledgeSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        //conf.setOption( ClockTypeOption.get( "pseudo" ) );
        conf.setOption( clockTypeOption );
        
        
        
        
        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession(conf,null);
        //Added a logger
        session.setGlobal( "log",  LogFactory.getLog("RulesEngine"));
		
        
        log.info("Finishing session creation");
        try{
        session.fireAllRules();
        } catch (RuntimeException e)
        {
        	log.error("Found an exception" + e.toString());
        }
        return session;
    }
	
	private class ActiveUrlComparator implements Comparator<ActiveURLs>{

		@Override
		public int compare(ActiveURLs o1, ActiveURLs o2) {
			if (o1.getCount() > o2.getCount())
			{
				return 1;
			} else if (o1.getCount() < o2.getCount())
			{
				return -1;
			} else
				return 0;
		}
	}

    private KnowledgeBase loadRuleBase() {
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        try {
            for( int i = 0; i < ASSET_FILES.length; i++ ) {
                builder.add( ResourceFactory.newInputStreamResource( ComplexEventProcessingService.class.getResourceAsStream( ASSET_FILES[i] ) ),
                             ResourceType.determineResourceType( ASSET_FILES[i] ));
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 0 );
        }
        if( builder.hasErrors() ) {
            System.err.println(builder.getErrors());
            System.exit( 0 );
        }
        KnowledgeBaseConfiguration conf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        conf.setOption( EventProcessingOption.CLOUD );
        //conf.setOption( MBeansOption.ENABLED );
        //conf.setOption( MultithreadEvaluationOption.YES );
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase( "Traffic Detection", conf ); 
        kbase.addKnowledgePackages( builder.getKnowledgePackages() );
        return kbase;
    }

    public StatefulKnowledgeSession getSession() {
		return session;
	}
    
    public WorkingMemoryEntryPoint getClientNetworkMetricsStream() {
		return clientNetworkMetricsStream;
	}

	public void setClientNetworkMetricsStream(
			WorkingMemoryEntryPoint clientNetworkMetricsStream) {
		this.clientNetworkMetricsStream = clientNetworkMetricsStream;
	}

	public WorkingMemoryEntryPoint getClientLogStream() {
		return clientLogStream;
	}

	public void setClientLogStream(WorkingMemoryEntryPoint clientLogStream) {
		this.clientLogStream = clientLogStream;
	}

	public WorkingMemoryEntryPoint getClientSessionMetricsStream() {
		return clientSessionMetricsStream;
	}

	public void setClientSessionMetricsStream(
			WorkingMemoryEntryPoint clientSessionMetricsStream) {
		this.clientSessionMetricsStream = clientSessionMetricsStream;
	}

	public WorkingMemoryEntryPoint getSummarySessionMetricsStream() {
		return summarySessionMetricsStream;
	}

	public void setSummarySessionMetricsStream(
			WorkingMemoryEntryPoint summarySessionMetricsStream) {
		this.summarySessionMetricsStream = summarySessionMetricsStream;
	}

	public WorkingMemoryEntryPoint getCompactSessionMetricsStream() {
		return compactSessionMetricsStream;
	}

	public void setCompactSessionMetricsStream(
			WorkingMemoryEntryPoint compactSessionMetricsStream) {
		this.compactSessionMetricsStream = compactSessionMetricsStream;
	}

	public WorkingMemoryEntryPoint getCompactClientLogStream() {
		return compactClientLogStream;
	}

	public void setCompactClientLogStream(
			WorkingMemoryEntryPoint compactClientLogStream) {
		this.compactClientLogStream = compactClientLogStream;
	}
    
	
}
