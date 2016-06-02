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


public class StatusBean {

	private Long numActiveApps;

	private Long numUsers;

	private Long clientSessionMetricsCount;

	private Long compactSessionMetricsCount;


	private Long queryTimeInMilliSeconds; //in milliseconds


	private String overallStatus;


	public String getOverallStatus() {
		return overallStatus;
	}
	public void setOverallStatus(String overallStatus) {
		this.overallStatus = overallStatus;
	}

	public Long getNumActiveApps() {
		if (null == numActiveApps)
			return 0L;
		return numActiveApps;
	}

	public void setNumActiveApps(Long numActiveApps) {
		this.numActiveApps = numActiveApps;
	}

	public Long getNumUsers() {
		if (null == numUsers)
			return 0L;
		return numUsers;
	}

	public void setNumUsers(Long numUsers) {
		this.numUsers = numUsers;
	}

	public Long getClientSessionMetricsCount() {
		if (null == clientSessionMetricsCount)
			return 0L;
		return clientSessionMetricsCount;
	}

	public void setClientSessionMetricsCount(Long clientSessionMetricsCount) {
		this.clientSessionMetricsCount = clientSessionMetricsCount;
	}

	public Long getCompactSessionMetricsCount() {
		if (compactSessionMetricsCount == null)
			return 0L;
		return compactSessionMetricsCount;
	}

	public void setCompactSessionMetricsCount(Long compactSessionMetricsCount) {
		this.compactSessionMetricsCount = compactSessionMetricsCount;
	}

	public Long getQueryTimeInMilliSeconds() {
		if (null == queryTimeInMilliSeconds)
			return 0L;
		return queryTimeInMilliSeconds;
	}
	public void setQueryTimeInMilliSeconds(Long queryTimeInMilliSeconds) {
		this.queryTimeInMilliSeconds = queryTimeInMilliSeconds;
	}



}
