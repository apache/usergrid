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

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ClientMetricsEnvelope;
import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.model.ClientSessionMetrics;


import com.thoughtworks.xstream.XStream;

/**
 * 
 * The following service injests metrics from SQS and saves them into the DB
 * 
 * @author vadmin
 *
 */
public class MetricsInjestionServiceSQSImpl implements MetricsInjestionService {

	private static final Log log = LogFactory.getLog(MetricsInjestionServiceSQSImpl.class);

	AmazonSQSClient sqsClient;
	private AmazonSQSAsyncClient sqsAsyncClient;
	XStream xStream;
	ObjectMapper objectMapper = new ObjectMapper();


	StatelessComplexEventProcessingService cepService;

	//protected static String BASE_SQS_PATH = "https://queue.amazonaws.com/366243268945/wm_metrics_";
	protected static int MAX_NUMBER_OF_MESSAGES = 10; 
	protected static int MAX_NUMBER_OF_REQUEST_TO_PROCESS = 100; //Enables 1000 simultaneous mobile agents


	NetworkMetricsDBService hibernateService;
	LogDBService logDBService;
	SessionDBService sessionDBService;

	public SessionDBService getSessionDBService() {
		return sessionDBService;
	}

	public void setSessionDBService(SessionDBService sessionDBService) {
		this.sessionDBService = sessionDBService;
	}

	public NetworkMetricsDBService getHibernateService() {
		return hibernateService;
	}

	public void setHibernateService(NetworkMetricsDBService hibernateService) {
		this.hibernateService = hibernateService;
	}

	public LogDBService getLogDBService() {
		return logDBService;
	}

	public void setLogDBService(LogDBService logDBService) {
		this.logDBService = logDBService;
	}

	public AmazonSQSClient getSqsClient() {
		return sqsClient;
	}

	public void setSqsClient(AmazonSQSClient sqsClient) {
		this.sqsClient = sqsClient;
	}


	public void setSqsAsyncClient(AmazonSQSAsyncClient sqsAsyncClient) {
		this.sqsAsyncClient = sqsAsyncClient;
	}

	public AmazonSQSAsyncClient getSqsAsyncClient() {
		return sqsAsyncClient;
	}

	public StatelessComplexEventProcessingService getCepService() {
		return cepService;
	}

	public void setCepService(StatelessComplexEventProcessingService cepService) {
		this.cepService = cepService;
	}


	@Override
	public void injestMetrics(Long applicationId, String fullAppName) {
		/*
		 * Psuedocode : 
		 * 
		 * 1. Pull messages from SQS and store them
		 * 2. Delete messages from SQS
		 * 3. Parse messages to ClientMetricsEnvelope
		 * 4. Sanitize metrics 
		 * 5. Perform aggregations which also Saves aggregated metrics into DB
		 *  
		 */

		List<ReceiveMessageResult> messageResults = getClientDataForApp(fullAppName);
		log.info("Num ReceiveMessageResult for app : " + fullAppName + " is " +  messageResults.size());
		if (messageResults.size() > 0)		{

			try {
				//get object representation of payload from devices
				DataToPersist toPersist = marshallReceiveMessageResults2(messageResults, applicationId, fullAppName);

				// Persisting raw data first
				hibernateService.saveNetworkMetricsInBatch(toPersist.metricsBeans);				
				logDBService.saveLogs(toPersist.clientLogs);
				sessionDBService.saveSessionMetricsInBatch(toPersist.clientSessionMetrics);

				//We will first delete messages from SQS because even if there are problems with CEP or persistence
				//we need to delete these messages to get away from infinite loop of pulling down messages and failing to do anything
				//meaningful with it. If it got here it also means that we have raw data so no data loss.
				purgeSQSQueue(messageResults, fullAppName);

				//Trigger Rules Engine		
				if (toPersist.clientSessionMetrics != null && toPersist.clientSessionMetrics.size() != 0)
					cepService.processEvents(new Long (applicationId), fullAppName,  toPersist.envelopes);
				else {
					log.error ("For some reason this batch of messgaes from SQS did not have any ClientSessionMetrics. Skipping CEP");
				}
				//manually nullify the beans to help with GC
				toPersist.clientLogs = null;
				toPersist.clientSessionMetrics = null;
				toPersist.metricsBeans = null;
				toPersist.envelopes = null;
				toPersist = null;
			} catch (Exception e) {
				log.error ("For application " + fullAppName + 
						" error happened either with marshalling or CEP or persisting ", e);				

			}
			messageResults = null;
		}
	}

	protected List<ReceiveMessageResult> getClientDataForApp(String fullAppName)
	{

		ArrayList<ReceiveMessageResult> messageResults = new ArrayList<ReceiveMessageResult>(MAX_NUMBER_OF_REQUEST_TO_PROCESS);

		try {
			GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest();

			ArrayList<String> attributeNames = new ArrayList<String>(2);

			attributeNames.add("ApproximateNumberOfMessages");
			attributeNames.add("LastModifiedTimestamp");
			//attributeNames.add("All");


			queueAttributesRequest.setAttributeNames(attributeNames);
			String qUrl = AWSUtil.formFullQueueUrl(fullAppName);
			log.info ("Getting APM data from SQS queue" + qUrl);
			queueAttributesRequest.setQueueUrl(qUrl);

			GetQueueAttributesResult queueAttributeResult = sqsClient.getQueueAttributes(queueAttributesRequest);

			String numMessagesString = queueAttributeResult.getAttributes().get("ApproximateNumberOfMessages");

			//Calculate number of "ReceiveMessage" requests need to be made.
			//TODO might need to use AsyncClient in the future to make multiple requests

			int numMessages = Integer.parseInt(numMessagesString);

			log.info("Num Messages to Process " + numMessages + " messages");



			if( numMessages > 0)
			{
				int receiveMessages = 0;
				int receiveRequest = 0;
				int lastNumberOfRecievedMessages = 1;

				while((receiveMessages < numMessages) && (receiveRequest < MAX_NUMBER_OF_REQUEST_TO_PROCESS) && (lastNumberOfRecievedMessages != 0))
				{
					ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
					receiveMessageRequest.setMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES);
					receiveMessageRequest.setQueueUrl(qUrl);

					ArrayList<String> requestMessageAttributeNames = new ArrayList<String>(1);
					requestMessageAttributeNames.add("All");

					receiveMessageRequest.setAttributeNames(requestMessageAttributeNames);

					try {
						ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
						log.info("For application " + fullAppName + " Received " + receiveMessageResult.getMessages().size() + " messages.");
						receiveMessages += receiveMessageResult.getMessages().size();
						//check if any of these messages have been downloaded already. Since SQS is distributed and injestor
						//could have failed before deleting particular message, we check for message read count to 3. In odd
						//case, some messages could get processed at most 3 times.
						List<Message> messages = receiveMessageResult.getMessages();
						String receiveCount = null;
						Message m = null;
						for (Iterator<Message> iter = messages.iterator(); iter.hasNext();) {
							m = iter.next();
							receiveCount = m.getAttributes().get("ApproximateReceiveCount");
							if (receiveCount != null && Integer.valueOf(receiveCount) > 3) {
								log.warn("ReceiveCount of message for app " + fullAppName + " is greater than 3 so going to delete this message before further processing");
								sqsClient.deleteMessage(new DeleteMessageRequest(qUrl,m.getReceiptHandle()));
								iter.remove();
							}
						}

						lastNumberOfRecievedMessages = receiveMessageResult.getMessages().size();
						if (lastNumberOfRecievedMessages > 0)
						{
							messageResults.add(receiveMessageResult);
							receiveRequest++;
						};	
					} catch (Exception ex)
					{
						log.error("Problem getting messages for " + fullAppName , ex);	
					}
				}
			} 
		}catch (AmazonServiceException ce)
		{
			log.error("Problem pulling message from SQS for " + fullAppName , ce);
		}
		catch (Exception e) {
			log.error("Problem getting messages for " + fullAppName , e);
		}

		return messageResults;
	}


	public void sendData(String fullAppName, ClientMetricsEnvelope envelope)
	{
		try{
			SendMessageRequest sendMessageRequest = new SendMessageRequest();

			sendMessageRequest.setQueueUrl(AWSUtil.formFullQueueUrl(fullAppName));
			//String message = xStream.toXML(envelope);
			String message = objectMapper.writeValueAsString(envelope);
			sendMessageRequest.setMessageBody(message);
			int length = message.getBytes().length;
			log.info("Sending Message of size : " + length);
			this.sqsAsyncClient.sendMessage(sendMessageRequest);
		} catch (AmazonServiceException ce)
		{
			log.error("Queue does not exisit or is not accessible for AppId " + fullAppName, ce);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			log.error("Some Jackson Error: " + fullAppName, e);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			log.error("Some Jackson Error: " + fullAppName , e);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error ("eror" , e);
		}

	}

	protected DataToPersist marshallReceiveMessageResults2(List<ReceiveMessageResult> receiveMessageResults, Long applicationId, String fullAppName)
	{
		DataToPersist toPersist = new DataToPersist();

		toPersist.metricsBeans = new ArrayList<ClientNetworkMetrics>();
		toPersist.clientLogs = new ArrayList<ClientLog>();
		toPersist.envelopes = new ArrayList<ClientMetricsEnvelope>();
		toPersist.clientSessionMetrics = new ArrayList<ClientSessionMetrics>(); 

		for(ReceiveMessageResult receiveMessageResult : receiveMessageResults)
		{
			for(Message message : receiveMessageResult.getMessages())
			{
				String messageBody = message.getBody();
				Map<String,String> attributes = message.getAttributes();				

				long sentTimestamp = 0;

				if( attributes.containsKey("SentTimestamp"))
				{
					sentTimestamp = Long.parseLong(attributes.get("SentTimestamp"));
				}

				DataToPersist dataSubset = marshallJSONPayloadFromSQS(messageBody,sentTimestamp, applicationId, fullAppName);
				if (dataSubset != null) {
					toPersist.metricsBeans.addAll(dataSubset.metricsBeans);
					toPersist.clientLogs.addAll(dataSubset.clientLogs);
					toPersist.envelopes.addAll(dataSubset.envelopes);
					toPersist.clientSessionMetrics.addAll(dataSubset.clientSessionMetrics);
				}
			}

		}

		return toPersist;
	}

	protected long determineDelta(Date envelopeDate, long sentTimeStamp)
	{

		if (envelopeDate != null || sentTimeStamp == 0)
		{
			long delta = sentTimeStamp - envelopeDate.getTime();

			return delta;
		} else
		{
			return 0;
		}
	}

	protected List<ClientNetworkMetrics> aggregateMetrics(List<ClientNetworkMetrics> metricsBeans)
	{
		//TODO: Need to implement this bit of logic in order not to flood the DB with too much data. 
		//We will need to think about how to "intelligently aggregate" because we still want to p
		//reserve the ability to filter by region / ISP / etc
		return metricsBeans;
	}

	protected void deleteMessageResults(List<ReceiveMessageResult> messageResults, String orgAppName)
	{
		//TODO: Definately want this to use asynchronous client due the the potentially thousands of
		// messages that might get queued up.
		for ( ReceiveMessageResult messageResult : messageResults)
		{

			for(Message message : messageResult.getMessages())
			{
				DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest();
				deleteMessageRequest.setQueueUrl(AWSUtil.formFullQueueUrl(orgAppName));
				deleteMessageRequest.setReceiptHandle(message.getReceiptHandle());
				try {
					sqsClient.deleteMessage(deleteMessageRequest);
				} catch (RuntimeException ce)
				{
					log.error("Cannot delete message for application : " + orgAppName +". Receipt Handle :" + message.getReceiptHandle() , ce);
				}
				catch (Exception e) {
					log.error ("error deleting ", e);
				}
			}
		}
	}



	private class DataToPersist
	{
		public List<ClientNetworkMetrics> metricsBeans;
		public List<ClientLog> clientLogs;
		public List<ClientMetricsEnvelope> envelopes;
		public List<ClientSessionMetrics> clientSessionMetrics;
	}



	/**
	 * 
	 * Uses XStream to parse message from XML to Java object
	 * 
	 * @param messageBody
	 * @return
	 */
	protected DataToPersist marshallJSONPayloadFromSQS(String messageBody, long sentTimestamp, Long applicationId, String fullAppName)
	{

		log.debug("Message string from SQS: " + messageBody);
		DataToPersist toPersist = null;
		boolean invalidPayload = false;	

		Object deserializedObject;
		try {
			deserializedObject = objectMapper.readValue(messageBody,ClientMetricsEnvelope.class);

			if(deserializedObject instanceof ClientMetricsEnvelope) {
				ClientMetricsEnvelope envelope = (ClientMetricsEnvelope)deserializedObject;
				//in case developer fat fingered org/app name or intentionally tried to mess with it, we reject that payload
				//if for whatever reason timestamp is future with respect to GMT then we can not do aggregation so we will reject them

				if (envelope.getInstaOpsApplicationId() == null ||
						envelope.getOrgName() == null || 
						envelope.getAppName() == null || 
						!envelope.getInstaOpsApplicationId().equals(applicationId)) {
					log.error("Message payload does not have valid org, app name or instaOps app Id for " + fullAppName +
							" with app id " + applicationId + ".  Skipping processing of this paylaod.");	
					log.error("Invalid Payload is " + messageBody);
					invalidPayload = true;
				}
				else if (envelope.getSessionMetrics() == null) {
					log.error ("Message payload does not have ClientSessionMetrics . Skipping processing of this payload for " + fullAppName);
					log.error ("Invalid Payload is "  + messageBody);
					invalidPayload = true;
				}
			   if (envelope.getTimeStamp() == null || envelope.getTimeStamp().after(new Date())) {
					log.error("Message envelope for app " + envelope.getFullAppName() + 
							" has null or invalid future timestamp in " + envelope.getTimeStamp() + " Setting it to current time");
					envelope.setTimeStamp(Calendar.getInstance().getTime());					
				}
			 if (!invalidPayload) {
					toPersist = new DataToPersist();
					List<ClientMetricsEnvelope> envelopes = new ArrayList<ClientMetricsEnvelope>();
					List<ClientNetworkMetrics> metricsBeans = new ArrayList<ClientNetworkMetrics>();
					List<ClientLog> logRecords = new ArrayList<ClientLog>();					
					List<ClientSessionMetrics> clientSessionMetrics = new ArrayList<ClientSessionMetrics>();

					toPersist.metricsBeans = metricsBeans;
					toPersist.clientLogs = logRecords;
					toPersist.envelopes = envelopes;
					toPersist.clientSessionMetrics = clientSessionMetrics;

					if (envelope.getSessionMetrics() != null)
						clientSessionMetrics.add(envelope.getSessionMetrics());
					if (envelope.getLogs() != null && envelope.getLogs().size() != 0)
						logRecords.addAll(envelope.getLogs());
					if(envelope.getMetrics() != null && envelope.getMetrics().size() != 0)
						metricsBeans.addAll(envelope.getMetrics());

					envelopes.add(envelope);

					//Removing logic for clock skew..this has not reeally been problem so far. See https://apigeesc.atlassian.net/browse/INSTAOPS-103
					/*long delta = determineDelta(envelope.getSessionMetrics().getTimeStamp(), sentTimestamp);

					if(delta > 120000)
					{
						log.error("Found device with long delta : " + envelope.getSessionMetrics().getDeviceModel() + " . Delta : " + delta);
					}
					 */
					ClientSessionMetrics csm = envelope.getSessionMetrics();
					sanitizeClientSessionMetrics(applicationId, envelope.getFullAppName(), csm);
					denormalizeClientMetrics(csm, metricsBeans,logRecords);
				}
			}

		} catch (JsonParseException e) {			
			log.error("JSON parsing error - Printing entire message string from SQS: " + messageBody);
			log.error("Parsing error", e);
		} catch (JsonMappingException e) {
			log.error("Message string from SQS: " + messageBody);
			log.error("Mapping error ", e);
		} catch (Exception e) {			
			log.error("Message string from SQS: " + messageBody);
			log.error(" Exception ", e);
		}

		return toPersist;

	}	

	private void sanitizeClientSessionMetrics(Long applicationId, String fullAppName, ClientSessionMetrics sm) {

		if(sm.getAppId() == null) {
			log.error("Adding app id on server side since SDK did not set appId in ClientSessionMetrics properly for AppId : " + applicationId + " "+ sm.toString());
			sm.setAppId(applicationId);
		}
		if (sm.getFullAppName() == null ) {
			log.error("Adding fullAppName on server side since SDK did not set fullAppName in ClientSessionMetrics properly for AppId : " + applicationId + " "+ sm.toString());
			sm.setFullAppName(fullAppName);	
		}
		
		if (sm.getAppConfigType() == null)
			sm.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
		if(sm.getApplicationVersion() == null)
			sm.setApplicationVersion(VALUE_UNKNOWN);
		if (sm.getSdkVersion() == null)
			sm.setSdkVersion(VALUE_UNKNOWN);
		if (sm.getSdkType() == null)
			sm.setSdkType(VALUE_UNKNOWN);

		if(sm.getDeviceId() == null) {
			log.error("Device id was set to null for session " + sm.toString() + " Setting it to random UUID");
			sm.setDeviceId((UUID.randomUUID().toString()));
		}
		if(sm.getDeviceModel() == null)
			sm.setDeviceModel(VALUE_UNKNOWN);
		if(sm.getDeviceOSVersion() == null)
			sm.setDeviceOSVersion(VALUE_UNKNOWN);
		if(sm.getDevicePlatform() == null)
			sm.setDevicePlatform(VALUE_UNKNOWN);


		sm.setDeviceOperatingSystem(sm.getDevicePlatform() + " " + sm.getDeviceOSVersion());		

		if(sm.getDeviceType() == null)
			sm.setDeviceType(VALUE_UNKNOWN);

		if(sm.getLongitude() == null)
			sm.setLongitude(0d);
		if(sm.getLatitude() == null)
			sm.setLatitude(0d);
		if(sm.getBearing() ==null)
			sm.setBearing(0f);

		if(sm.getNetworkCarrier() == null)
			sm.setNetworkCarrier(VALUE_UNKNOWN);
		if(sm.getNetworkType() == null)
			sm.setNetworkType(VALUE_UNKNOWN);
		if(sm.getNetworkSubType() == null)
			sm.setNetworkSubType(VALUE_UNKNOWN);
		if(sm.getNetworkExtraInfo() == null)
			sm.setNetworkExtraInfo(VALUE_UNKNOWN);

		if(sm.getSessionId() == null) {
			log.error ("Session Id was null for app " + fullAppName + " for session " + sm.toString() + " setting it to random UUID. It should not be happening");
			sm.setSessionId(UUID.randomUUID().toString()); //if it's really happening then something is wrong with SDK big time.
		}

		if(sm.getSessionStartTime() == null || sm.getSessionStartTime().after(Calendar.getInstance().getTime())) {
			log.error ("Session start time was null or set to future for app " + fullAppName + " setting it to now");
			sm.setSessionStartTime(Calendar.getInstance().getTime());
		}

		if(sm.getTelephonyDeviceId() == null)
			sm.setTelephonyDeviceId(VALUE_UNKNOWN);
		if(sm.getTelephonyNetworkOperator() == null)
			sm.setTelephonyNetworkOperator(VALUE_UNKNOWN);
		if(sm.getTelephonyNetworkOperatorName() == null)
			sm.setTelephonyNetworkOperatorName(VALUE_UNKNOWN);
		if(sm.getTelephonyPhoneType() == null)
			sm.setTelephonyPhoneType(VALUE_UNKNOWN);

		if (sm.getTimeStamp() == null || sm.getTimeStamp().after(Calendar.getInstance().getTime())) {
			log.error ("Session timestamp was null or set to future for app " + fullAppName + " setting it to now");
			sm.setTimeStamp(Calendar.getInstance().getTime());
		}

	}

	protected void purgeSQSQueue(List<ReceiveMessageResult> receiveMessageResults, String orgAppName)
	{
		int numDeletedMessages = 0;
		try {

			String qUrl = AWSUtil.formFullQueueUrl(orgAppName);
			for(ReceiveMessageResult receiveMessageResult : receiveMessageResults)
			{
				for (Message message :receiveMessageResult.getMessages())
				{
					String receiptHandle = message.getReceiptHandle();

					DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest();

					deleteMessageRequest.setQueueUrl(qUrl);
					deleteMessageRequest.setReceiptHandle(receiptHandle);

					sqsClient.deleteMessage(deleteMessageRequest);
					numDeletedMessages++;
				}
				log.info("Deleted " + numDeletedMessages + " messages for application : " + orgAppName);
			}

		} catch (Exception e) {
			log.error("Problem deleting message from SQS for " + orgAppName + ". This needs to be immediately looked into", e);			
		}

	}
	/**
	 * Get a list of all applicationIds
	 * Call injestMetrics for each appConfigId
	 * 
	 */
	@Override
	public void injestAllMetrics() {

		log.info("Call to injestAll" +
				"Metrics");

		ApplicationService applicationService = ServiceFactory.getApplicationService();
		List<App> list = applicationService.getAllApps();
		Collections.shuffle(list);

		for(App model : list)
		{	

			injestMetrics(model.getInstaOpsApplicationId(), model.getFullAppName());

		}
	}


	public XStream getXStream() {
		return xStream;
	}

	public void setXStream(XStream xStream) {
		this.xStream = xStream;
	}

	/** 
	 * @param envelope the envelope that has all the data that needs denomalization
	 * @param clientNetworkMetrics the metrics stats to denormalize that data into
	 * @param
	 * @param
	 * 
	 */
	public static void denormalizeClientMetrics(ClientSessionMetrics clientSessionMetrics, List<ClientNetworkMetrics> clientNetworkMetrics, List<ClientLog> logRecords)
	{
		try {

			if(clientSessionMetrics != null && clientSessionMetrics.getAppId() != null)	{
				for(ClientNetworkMetrics metricsBean : clientNetworkMetrics)
				{
					metricsBean.setAppId(clientSessionMetrics.getAppId());
					metricsBean.setFullAppName(clientSessionMetrics.getFullAppName());


					metricsBean.setDeviceId(clientSessionMetrics.getDeviceId());
					metricsBean.setDeviceModel(clientSessionMetrics.getDeviceModel());
					metricsBean.setDeviceOperatingSystem(clientSessionMetrics.getDeviceOperatingSystem());
					metricsBean.setDevicePlatform(clientSessionMetrics.getDevicePlatform());
					metricsBean.setDeviceOSVersion(clientSessionMetrics.getDeviceOSVersion());
					metricsBean.setDeviceType(clientSessionMetrics.getDeviceType());

					// This code is for properly setting the network type / carrier / country 
					metricsBean.setNetworkCarrier(clientSessionMetrics.getNetworkCarrier());
					metricsBean.setNetworkCountry(clientSessionMetrics.getNetworkCountry());

					//This covers the case where the network sub-type (e.g. LTE / HSPDA / EDGE) 
					if (clientSessionMetrics.getNetworkSubType() == null || clientSessionMetrics.getNetworkSubType().equals(VALUE_UNKNOWN))
					{
						metricsBean.setNetworkType(clientSessionMetrics.getNetworkType());
					} else
					{
						metricsBean.setNetworkType(clientSessionMetrics.getNetworkSubType());
					}

					metricsBean.setLatitude(clientSessionMetrics.getLatitude());
					metricsBean.setLongitude(clientSessionMetrics.getLongitude());

					metricsBean.setApplicationVersion(clientSessionMetrics.getApplicationVersion());
					metricsBean.setAppConfigType(clientSessionMetrics.getAppConfigType());

					//SetsCorrectedTimestamp
					//metricsBean.setEndTime(calculateCorrectedTimestamp(metricsBean.getEndTime(),clockSkewDelta));
					//metricsBean.setStartTime(calculateCorrectedTimestamp(metricsBean.getStartTime(),clockSkewDelta));
					if (metricsBean.getTimeStamp() == null || metricsBean.getTimeStamp().after(Calendar.getInstance().getTime()))
						metricsBean.setTimeStamp(clientSessionMetrics.getTimeStamp());

					metricsBean.setSessionId(clientSessionMetrics.getSessionId());
					metricsBean.setDeviceId(clientSessionMetrics.getDeviceId());
					if (metricsBean.getUrl() == null)
						metricsBean.setDomain(VALUE_UNKNOWN);
					else {
						if (metricsBean.getUrl().length() > 250) //since column is varchar 255					
							metricsBean.setUrl(metricsBean.getUrl().substring(0,249));					
						try {
							String webAddress = metricsBean.getUrl();
							if (webAddress.indexOf("?") != -1)
								webAddress = webAddress.substring(0,webAddress.indexOf('?'));
							metricsBean.setDomain(new URI(webAddress).getHost());							
						} catch (Exception e) {
							metricsBean.setDomain(metricsBean.getUrl().substring(0, Math.min(20,metricsBean.getUrl().length())));
						}
					}
				}

				for(ClientLog logRecord : logRecords)
				{
					logRecord.setAppId(clientSessionMetrics.getAppId());
					logRecord.setFullAppName(clientSessionMetrics.getFullAppName());
					logRecord.setDeviceId(clientSessionMetrics.getDeviceId());
					logRecord.setDeviceModel(clientSessionMetrics.getDeviceModel());
					logRecord.setDeviceOperatingSystem(clientSessionMetrics.getDeviceOperatingSystem());
					logRecord.setDevicePlatform(clientSessionMetrics.getDevicePlatform());
					logRecord.setDeviceOSVersion(clientSessionMetrics.getDeviceOSVersion());
					logRecord.setDeviceType(clientSessionMetrics.getDeviceType());

					// This code is for properly setting the network type / carrier / country 
					logRecord.setNetworkCarrier(clientSessionMetrics.getNetworkCarrier());
					logRecord.setNetworkCountry(clientSessionMetrics.getNetworkCountry());

					//This covers the case where the network sub-type (e.g. LTE / HSPDA / EDGE) 
					if (clientSessionMetrics.getNetworkSubType() == null || clientSessionMetrics.getNetworkSubType().equals(VALUE_UNKNOWN))
					{
						logRecord.setNetworkType(clientSessionMetrics.getNetworkType());
					} else
					{
						logRecord.setNetworkType(clientSessionMetrics.getNetworkSubType());
					}

					logRecord.setNetworkCountry(clientSessionMetrics.getNetworkCountry());

					logRecord.setLatitude(clientSessionMetrics.getLatitude());
					logRecord.setLongitude(clientSessionMetrics.getLongitude());

					logRecord.setApplicationVersion(clientSessionMetrics.getApplicationVersion());
					logRecord.setAppConfigType(clientSessionMetrics.getAppConfigType());

					if (logRecord.getTimeStamp() == null || logRecord.getTimeStamp().after(Calendar.getInstance().getTime()))
						logRecord.setTimeStamp(clientSessionMetrics.getTimeStamp());

					//Fix some long messages

					if(logRecord.getLogMessage().length() > 250)
					{
						logRecord.setLogMessage(logRecord.getLogMessage().substring(0, 249));
					}

					logRecord.setSessionId(clientSessionMetrics.getSessionId());				

				}
			} else
			{
				log.error("ClientSessionMetrics was not properly Populated. It's either null or is missing appId");
			}
		} catch (Exception e)
		{
			log.error("Some Weird exception was caught. Show must go on : " , e);
		}
	}

}
