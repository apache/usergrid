package org.apache.usergrid.apm.service.charts.service;

/**
 * This is used for constructing data for bar and pie chart. Returned data is in descending order
 * Added summary data as well since we show that with bar chart as well now.
 * @author prabhat jha
 *
 */

public class AttributeValueChartData implements Comparable<AttributeValueChartData>{
	
	String annotation;
	
	String attribute;
	Long value;
	Double percentage;	
	
	
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
	
	public Long getValue() {
		return value;
	}
	public void setValue(Long value) {
		this.value = value;
	}
	public Double getPercentage() {
		return percentage;
	}
	public void setPercentage(Double percentage) {
		this.percentage = percentage;
	}
	@Override
	public int compareTo(AttributeValueChartData arg0) {
		return (this.getValue() < arg0.getValue() ? 1 : (this.getValue() == arg0.getValue() ? 0 : -1));
		
	}
	
	
	
	
	

}
