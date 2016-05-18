package org.apache.usergrid.apm.service.charts.service;



public class NetworkRequestsErrorsChartData  implements Comparable<NetworkRequestsErrorsChartData>{ 

	String annotation;
	String attribute;
	Long requests;
	Long errors;
	Double requestPercentage;
	Double errorPercentage;


	public String getAnnotation() {
		return annotation;
	}
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}	

	public Long getRequests() {
		return requests;
	}
	public void setRequests(Long requests) {
		this.requests = requests;
	}
	public Long getErrors() {
		return errors;
	}
	public void setErrors(Long errors) {
		this.errors = errors;
	}
	public Double getRequestPercentage() {
		return requestPercentage;
	}
	public void setRequestPercentage(Double requestPercentage) {
		this.requestPercentage = requestPercentage;
	}
	public Double getErrorPercentage() {
		return errorPercentage;
	}
	public void setErrorPercentage(Double errorPercentage) {
		this.errorPercentage = errorPercentage;
	}

	@Override
	public int compareTo(NetworkRequestsErrorsChartData arg0) {
		return (this.getRequests() < arg0.getRequests() ? 1 : (this.getRequests() == arg0.getRequests() ? 0 : -1));

	}

}
